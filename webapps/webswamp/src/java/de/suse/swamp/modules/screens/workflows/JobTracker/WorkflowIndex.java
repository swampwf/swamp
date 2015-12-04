/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt@suse.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * $Id$
 */

package de.suse.swamp.modules.screens.workflows.JobTracker;

import java.io.*;
import java.util.*;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.modules.screens.*;
import de.suse.swamp.util.*;

/**
 * JobTracker Index page
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *
 */


public class WorkflowIndex extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        
        String uname = data.getUser().getName();
        WorkflowAPI wfapi = new WorkflowAPI();
        String template =  data.getParameters().get("wftemplate");
        WorkflowTemplate wftemp = wfapi.getWorkflowTemplate(template, uname);
        
        if (template == null){
            throw new UnknownElementException("Template: " + template + " could not be found!");
        }
        
        String version = wftemp.getVersion();
        Collection roles = wfapi.getWorkflowRoles(template, version, uname);
        
        // get the latest jobtracker workflows (latest id): 
        ArrayList filters = new ArrayList();
        PropertyFilter templatefilter = new PropertyFilter();
        templatefilter.addWfTemplate(template);
        filters.add(templatefilter);
        PropertyFilter order = new PropertyFilter();
        order.setWfId(1);
        order.setDescending();
        List ids = wfapi.getWorkflowIds(filters, order, uname);
        if (ids != null && ids.size() > 0){
        	List latestwf = new ArrayList();
        	for (int i = 0; i<6&&i<ids.size(); i++){
        		int wfid = ((Integer) ids.get(i)).intValue();
                try {
                    Workflow wf = wfapi.getWorkflow(wfid, uname);
                    latestwf.add(wf);
                } catch (Exception e) {
                    Logger.DEBUG("Do not show Wf-" + wfid + " on index (no permission)");
                }
        	}
            context.put("latestwf", latestwf);
        }
        
        // get the latest bewerbung workflow (latest id): 
        filters = new ArrayList();
        templatefilter = new PropertyFilter();
        templatefilter.addWfTemplate("application");
        filters.add(templatefilter);
        ids = wfapi.getWorkflowIds(filters, order, uname);
        if (ids != null && ids.size() > 0){
        	List latestbewerbungswf = new ArrayList();
        	for (int i = 0; i<6&&i<ids.size(); i++){
        		int wfid = ((Integer) ids.get(i)).intValue();
                try {
                    Workflow wf = wfapi.getWorkflow(wfid, uname);
                    latestbewerbungswf.add(wf);
                } catch (Exception e) {
                    Logger.DEBUG("Do not show Wf-" + wfid + " on index (no permission)");
                }
        	}
        	context.put("latestbewerbungswf", latestbewerbungswf);
        }
        
        
        // get the latest ratings: 
        filters = new ArrayList();
        templatefilter = new PropertyFilter();
        templatefilter.addWfTemplate("rating");
        templatefilter.setClosed(true);
        filters.add(templatefilter);
        ids = wfapi.getWorkflowIds(filters, order, uname);
        if (ids != null && ids.size() > 0){
            List latestbewertungswf = new ArrayList();
            for (int i = 0; i<6 && i<ids.size(); i++){
                int wfid = ((Integer) ids.get(i)).intValue();
                try {
                    Workflow wf = wfapi.getWorkflow(wfid, uname);
                    latestbewertungswf.add(wf);
                } catch (Exception e) {
                    Logger.DEBUG("Do not show Wf-" + wfid + " on index (no permission)");
                }
            }
            context.put("latestbewertungswf", latestbewertungswf);
        }
        
        
        // check for statistic graph
        String home = new SWAMPAPI().doGetProperty("swamp.home", uname);
        String fs = System.getProperty("file.separator");
        String imagePath = home + fs + ".." + fs + "var" + fs + "statistics" + fs + template + ".png";        
        if (new File(imagePath).exists()){
            context.put("statsImage", "/webswamp/var/statistics/" + template + ".png");
        }
        
        String imagePath2 = home + fs + ".." + fs + "var" + fs + "statistics" + fs + "application.png";        
        if (new File(imagePath2).exists()){
            context.put("statsImage2", "/webswamp/var/statistics/application.png");
        }
        
        // add helplink:
        addHelplink(wftemp, context, uname);
        
        // provide search fields for jobtracker, bewerbung, bewertung
        WorkflowTemplate jobtemplate = wfapi.getWorkflowTemplate(template, uname);
        List paths = new ArrayList();
        paths.add("ausschreibungsdata.roles.hr");
        paths.add("ausschreibungsdata.roles.hiring_manager");
        paths.add("ausschreibungsdata.roles.abtleiter");
        paths.add("ausschreibungsdata.stelle");
        paths.add("ausschreibungsdata.candidate");
        paths.add("ausschreibungsdata.start_date");
        paths.add("ausschreibungsdata.comment");
        context.put("jobtemplate", jobtemplate);
        context.put("jobpaths", paths);

        WorkflowTemplate bewerbertemplate = wfapi.getWorkflowTemplate("application", uname);
        paths = new ArrayList();
        paths.add("bewerbungsdata.bewerberdata.name");
        paths.add("bewerbungsdata.bewerberdata.eingang_am");
        paths.add("bewerbungsdata.results.comment_decision");
        paths.add("bewerbungsdata.results.add_comment_decision");  
        context.put("bewerbungstemplate", bewerbertemplate);
        context.put("bewerbungspaths", paths);
      
        WorkflowTemplate bewertungstemplate = wfapi.getWorkflowTemplate("rating", uname);
        
        paths = new ArrayList();
        paths.add("bewertungsdata.bewerterdata.name");
        paths.add("bewertungsdata.bewerterdata.result");
        paths.add("bewertungsdata.bewerterdata.comment");
        context.put("bewertungstemplate", bewertungstemplate);
        context.put("bewertungspaths", paths);

        context.put("wfroles", roles);
        context.put("wftemplate", wftemp);

    }
}