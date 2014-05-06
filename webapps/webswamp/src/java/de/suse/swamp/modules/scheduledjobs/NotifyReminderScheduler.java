/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de>
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

import java.util.*;

import org.apache.turbine.services.schedule.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


public class NotifyReminderScheduler extends SWAMPScheduledJob {

    public NotifyReminderScheduler() {
    }

    /**
     * Send daily reminders and weekly reminders on monday
     * 
     * @param job - The job to run.
     */
    public void run(JobEntry job) throws Exception {
        results.reset();
    	Date start = new Date(System.currentTimeMillis());
        
    	// iterate over all users with reminder setting
    	List users = SecurityManager.loadUsersWithPerm("_reminder=");
    	for (Iterator it = users.iterator(); it.hasNext(); ) {
    	    SWAMPUser user = (SWAMPUser) it.next();
    	    for (Iterator permIt = user.getPermSet().keySet().iterator(); permIt.hasNext(); ) {
    	        String permKey = (String) permIt.next();
    	        if (permKey.endsWith("_reminder")) {
    	            String setting = user.getPerm(permKey);
    	            String wfName = permKey.substring(0, permKey.length() - 9);
    	            WorkflowTemplate wfTemp = WorkflowManager.getInstance().getWorkflowTemplate(wfName);
    	            
    	            // is it a weekly or daily subscription?
    	            if (wfTemp != null && (setting.equals("daily") || 
    	                    ( setting.equals("weekly") && 
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) ) ) {
    	                List wfs = getOpenTaskWfs(user.getUserName(), wfTemp.getName());
                        if (wfs.size() > 0) {
        	                Logger.DEBUG("Sending " + setting.equals("weekly") + " " +
        	                		"summary for user " + user.getUserName() + ", wf: " + wfName);
        	                List receiver = new ArrayList();
        	                receiver.add(user.getEmail());
        	                NotificationTools.sendMail(receiver, null, "Daily summary for workflow: " + wfName, 
        	                        getSummary(wfs, user.getUserName()), null);
    	                }
    	            }
    	        }
    	    }
    	}
    	
        results.addResult(ResultList.MESSAGE, "Went through notification queue.");
        Logger.DEBUG("Scheduled job : " + job.getTask() + " ran @: " + start);
    }
    
    
    
    private List getOpenTaskWfs(String username, String templateName) {
        ArrayList filters = new ArrayList();
        MemoryTaskFilter taskfilter = new MemoryTaskFilter();
        taskfilter.setTaskOwner(username);
        taskfilter.setMandatoryOnly(true);
        filters.add(taskfilter);
        
        PropertyFilter filter = new PropertyFilter();
        filter.setWfTemplates(new SWAMPHashSet(templateName, ",").toList());
        filters.add(filter);
        
        List wfs = WorkflowManager.getInstance().getWorkflows(filters, null);
        return wfs;
    }
    
    
    
    
    private String getSummary (List wfs, String username) throws Exception {
        StringBuffer content = new StringBuffer();
        for (Iterator it = wfs.iterator(); it.hasNext(); ) {
            Workflow wf = (Workflow) it.next();
            content.append("Open tasks for Workflow: " + wf.getName() + ": " + wf.getReplacedDescription() + ": \n");
            for (Iterator taskIt = wf.getActiveTasks(true).iterator(); taskIt.hasNext(); ) {
                WorkflowTask task = (WorkflowTask) taskIt.next();
                if (task.getUsersForRole().contains(username)) {
                    content.append("- " + task.getReplacedDescription() + " (" + 
                            SWAMP.getInstance().getProperty("appLink") + "/task/" + task.getId() + ")\n");
                }   
            }
            content.append("\n");
        }
        return content.toString();
    }
    
    
}

