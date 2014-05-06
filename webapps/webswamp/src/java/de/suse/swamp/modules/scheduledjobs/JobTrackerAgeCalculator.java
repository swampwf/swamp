/*
 * SWAMP Workflow Administration and Management Platform
 * 
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de>
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published by the
 * Free Software Foundation.
 * 
 */

package de.suse.swamp.modules.scheduledjobs;

import java.util.*;

import org.apache.turbine.services.schedule.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


public class JobTrackerAgeCalculator extends SWAMPScheduledJob {

    public JobTrackerAgeCalculator() {
    }

    /**
     * Run the Jobentry from the scheduler queue. From ScheduledJob.
     * @param job The job to run.
     */
    public void run(JobEntry job) throws Exception {
        results.reset();
    	Date start = new Date(System.currentTimeMillis());
        // get running applications: 
        PropertyFilter filter = new PropertyFilter();
        filter.addWfTemplate("application");
        filter.addNodeNotActive("canceled");
        filter.addNodeNotActive("theend"); 
        
        WorkflowManager wfman = WorkflowManager.getInstance();
        List wfs = wfman.getWorkflows(filter, null);
        int counter = 0;
        
        for (Iterator it = wfs.iterator(); it.hasNext(); ){
            Workflow wf = (Workflow) it.next();
            try {
                // only works for wf version > 0.1
                if (wf.containsDatabit("bewerbungsdata.inactive_count")){
                    Databit dbit = wf.getDatabit("bewerbungsdata.inactive_count");
                    int age = new Integer (Integer.parseInt(dbit.getValue())).intValue();
                    
                    // if candidate is invited (active node: interview_fazit)
                    // and bewerbungsdata.results.interview_when is in future, don't increment
                    if (wf.getNode("interview_fazit").isActive() && 
                    		wf.containsDatabit("bewerbungsdata.results.interview_when") && 
                    		!wf.getDatabitValue("bewerbungsdata.results.interview_when").equals("") && 
                    		((datetimeDatabit) wf.getDatabit("bewerbungsdata.results.interview_when")).
                    		    getValueAsDate().after(new Date()) ){
                    	Logger.DEBUG("Skipping application " + wf.getId() + " scheduled for meeting.");
                    } else {
                        dbit.setValue(String.valueOf(++age), "system");
                        counter++;
                    }
                    // send reminder to hr + dep-leader every 14 days:
                    if (age > 0 && age % 14 == 0){
                            Notification notification = new Notification();
                            notification.setRcRole("parent.hr_responsible");
                            notification.setWorkflowid(wf.getId());
                            notification.setTemplateFile("notifications/getting_old");
                            NotificationManager.addNotification(notification);
                            notification = new Notification();
                            notification.setRcRole("parent.resp_depleader");
                            notification.setWorkflowid(wf.getId());
                            notification.setTemplateFile("notifications/getting_old");
                            NotificationManager.addNotification(notification);
                    }
                }
            } catch (Exception e) {
                Logger.ERROR("Failure in setting inactiveCount of Wf #: " + wf.getName());
                Logger.ERROR(e.getMessage());
                e.printStackTrace();
            }
        }
        
        results.addResult(ResultList.MESSAGE, "Incremented " + counter + " application ages.");
        Logger.DEBUG("Scheduled Task " + job.getTask()
                + " ran @: " + start + " and incremented " + counter + " application ages.");
    }
}

