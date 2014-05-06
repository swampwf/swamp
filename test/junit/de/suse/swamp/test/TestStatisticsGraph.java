/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt
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

package de.suse.swamp.test;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import junit.framework.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

import java.util.*;


public class TestStatisticsGraph extends TestCase {

	private WorkflowManager wfman;
	private SWAMP swamp;
	
	
	protected void setUp() throws Exception {
		super.setUp();
		//Logger.log.setLevel(org.apache.log4j.Level.DEBUG);
		//wfman = WorkflowManager.getInstance();
		//swamp = SWAMP.getInstance();
		//Assert.assertTrue(wfman != null);
		//Assert.assertTrue(swamp != null);
	}

	
	

	public void testStatistic() {

		XYSeries series = new XYSeries("Running Workflows");
		series.add(1995, 0.5);
		series.add(2000, 3.0);
		series.add(2010, 20.0);
		series.add(2020, 50.0);
		XYDataset dataset = new XYSeriesCollection(series);

		JFreeChart chart = ChartFactory.createTimeSeriesChart("test", "time", "value", dataset, false, false, false);

        JFreeChart chart4; 
        DefaultPieDataset dataset2 = new DefaultPieDataset();
        // Initialize the dataset
        dataset2.setValue( "California", new Double( 10.0 ) );
        dataset2.setValue( "Arizona", new Double( 8.0 ) );
        dataset2.setValue( "New Mexico", new Double( 8.0 ) );
        dataset2.setValue( "Texas", new Double( 40.0 ) );
        dataset2.setValue( "Louisiana", new Double( 8.0 ) );
        dataset2.setValue( "Mississippi", new Double( 4.0 ) );
        dataset2.setValue( "Alabama", new Double( 2.0 ) );
        dataset2.setValue( "Florida", new Double( 20.0 ));
        
                chart4 = ChartFactory.createPieChart3D(
                        "Driving Time Spent Per State (3D with Transparency)", // The chart title
                        dataset2,         // The dataset for the chart
                        true,          // Is a legend required?
                        true,          // Use tooltips
                        false          // Configure chart to generate URLs?
                      );
                      PiePlot3D plot4 = ( PiePlot3D )chart4.getPlot();
                      plot4.setForegroundAlpha( 0.6f );
                      
                      
                      
                      
        
		try {
		  ChartUtilities.saveChartAsPNG(new java.io.File("test.png"), chart, 500, 300);
          
          ChartUtilities.saveChartAsPNG(new java.io.File("test2.png"), chart4, 500, 300);
		} catch (java.io.IOException exc) {
		    System.err.println("Error writing image to file");
		}
		
    }
	
	

	
    
    
	public void testShutdown() {
    }
	
	
	private void logMessage(String msg){
		Logger.log.setLevel(org.apache.log4j.Level.DEBUG);
		Logger.DEBUG(msg);
		Logger.log.setLevel(org.apache.log4j.Level.OFF);
	}
	
}
