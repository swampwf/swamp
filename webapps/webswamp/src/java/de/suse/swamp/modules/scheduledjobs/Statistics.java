/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de>
 * Copyright (c) 2006 Novell Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA 
 *
 * In addition, as a special exception, Novell Inc. gives permission to link the
 * code of this program with the following applications:
 *
 * - All applications of the Apache Software Foundation 
 *
 * and distribute such linked combinations.
 */

package de.suse.swamp.modules.scheduledjobs;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import org.apache.turbine.services.schedule.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;
import org.jfree.data.time.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.history.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

/**
 * Generating statistic graphs
 */
public class Statistics extends SWAMPScheduledJob {

    WorkflowManager wfman = WorkflowManager.getInstance();
    SWAMP swamp = SWAMP.getInstance();
    String fs = System.getProperty("file.separator");
    String statPath = swamp.getSWAMPHome() + fs + ".." + fs + "var" + fs + "statistics"; 
    
    public Statistics() {
    }

    /**
     * Run the Jobentry from the scheduler queue. From ScheduledJob.
     * @param job - The job to run.
     */
    public void run(JobEntry job) throws Exception {
        results.reset();
    	Date start = new Date(System.currentTimeMillis());
        generateStatistics();
        results.addResult(ResultList.MESSAGE, "Re-generated statistic graphs.");
        Logger.DEBUG("Scheduled job statistics" + " ran @: " + start);
    }
    
    
    public void generateStatistics() throws Exception {
        System.setProperty("java.awt.headless", "true"); 
        //generateFrontpageGraph();
        storeWorkflowStats();
        generateAllWorkflowGraphs();
    }
    
    
    
    /**
     * Generating the graphs that show the amount of running finished wfs over the time.
     */
    protected void generateAllWorkflowGraphs() throws Exception {
        List names = wfman.getWorkflowTemplateNames();
        for (Iterator it = names.iterator(); it.hasNext(); ){
            String tempName = (String) it.next();
            // generate Graph for the last year
            Date lastYear = new Date((new Date()).getTime() - (1000l * 3600l * 24l * 365l));
            generateWorkflowGraph(tempName, lastYear, null);
        }
    }

    /**
     * Generating the graphs that show the amount of running finished wfs over the time.
     */
    protected void generateWorkflowGraph(String templateName, Date startDate, Date endDate) throws Exception {
        List stats = StatisticStorage.loadStats(templateName, startDate, endDate);
        // only generate if we have stats: 
        if (stats != null && stats.size() > 0) {
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            TimeSeriesCollection avgdataset = new TimeSeriesCollection();
            TimeSeries serie = new TimeSeries("running workflows", Day.class);
            TimeSeries avgserie = new TimeSeries("average age", Day.class);
            for (Iterator datait = stats.iterator(); datait.hasNext();) {
                Dbstatistics statisticItem = (Dbstatistics) datait.next();
                serie.addOrUpdate(new Day(statisticItem.getDate()), statisticItem.getRunningcount());
                avgserie.addOrUpdate(new Day(statisticItem.getDate()), statisticItem.getAvgage() / (3600 * 24));
            }
            dataset.addSeries(serie);
            avgdataset.addSeries(avgserie);

            JFreeChart chart = ChartFactory.createTimeSeriesChart("Running " + templateName + " workflows", "Date", "running workflows",
                    dataset, false, false, false);

            // modify chart appearance
            chart.setBackgroundImageAlpha(0.5f);
            XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.lightGray);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinePaint(Color.white);
            plot.getRangeAxis().setLabelPaint(Color.blue);

            // add the second line: 
            final NumberAxis axis2 = new NumberAxis("Avg. age in days");
            axis2.setLabelPaint(Color.red);
            plot.setRangeAxis(1, axis2);
            plot.setDataset(1, avgdataset);
            plot.mapDatasetToRangeAxis(1, 1);

            final XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
            renderer2.setDrawOutlines(false);
            renderer2.setDrawSeriesLineAsPath(true);
            renderer2.setBaseShapesVisible(false);
            plot.setRenderer(1, renderer2);

            File image = new File(statPath + fs + templateName + ".png");
            if (image.exists())
                image.delete();
            try {
                ChartUtilities.saveChartAsPNG(image, chart, 750, 200);
            } catch (Exception e) {
                Logger.ERROR("Error generating graph for " + templateName + ", e: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
    }
    
    
    protected void storeWorkflowStats() {
        List names = wfman.getWorkflowTemplateNames();
        for (Iterator it = names.iterator(); it.hasNext(); ){
            String template = (String) it.next();
            // get running wfs: 
            PropertyFilter templatefilter = new PropertyFilter();
            templatefilter.setClosed(false);
            templatefilter.addWfTemplate(template);
            List wfIds = wfman.getWorkflowIds(templatefilter, null);
            // calculate avg age:
            long agesum = 0, agewfs = 0;
            for (Iterator wfit = wfIds.iterator(); wfit.hasNext(); ){
                int wfid = ((Integer) wfit.next()).intValue();
                ArrayList hist = HistoryManager.getHistoryEntries(wfid, "TASK_WORKFLOWSTART");
                if (hist != null && hist.size() > 0){
                    agesum += new Date().getTime() - ((HistoryEntry) hist.get(hist.size()-1)).getWhen().getTime();
                    agewfs++;
                }
            }
            if (agewfs > 0){
                long avg = (agesum/agewfs) / 1000;
                try {
                    StatisticStorage.storeStats(avg, wfIds.size(), template);
                } catch (StorageException e) {
                    Logger.ERROR("Cannot store statistics: " + e.getMessage());
                }
            }
        }
    }
    
    
    
    /**
     * Generating the frontpage graph that shows the total amount of different wf types
     */
    protected void generateFrontpageGraph() {
        // count the number of workflows of each type: 
        List names = wfman.getWorkflowTemplateNames();
        DefaultPieDataset dataset = new DefaultPieDataset();
        int wfcount = 0;
        
        for (Iterator it = names.iterator(); it.hasNext(); ){
            String template = (String) it.next();
            PropertyFilter templatefilter = new PropertyFilter();
            templatefilter.addWfTemplate(template);
            int count = wfman.getWorkflowIds(templatefilter, null).size();
            if (count > 0){
                dataset.setValue( template, new Double(count));
            }
            wfcount += count;
        }
        
        // only generate graph if workflows exist 
        if (wfcount > 0){
            JFreeChart chart = ChartFactory.createPieChart3D(
                    "Workflows inside SWAMP", dataset, false, false, false);
            PiePlot3D plot = ( PiePlot3D )chart.getPlot();
            plot.setForegroundAlpha( 0.6f );
            File image = new File(statPath + fs + "workflows.png");
            if (image.exists()) image.delete();
            try {
                ChartUtilities.saveChartAsPNG(image, chart, 500, 350);
            } catch (java.io.IOException exc) {
                Logger.ERROR("Error writing image to file: " + exc.getMessage());
            }  
        }
    }
    
}

