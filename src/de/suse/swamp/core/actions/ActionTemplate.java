/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Thomas Schmidt (tschmidt [at] suse.de)
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

 * In addition, as a special exception, Novell Inc. gives permission to link the
 * code of this program with the following applications:
 *
 * - All applications of the Apache Software Foundation
 *
 * and distribute such linked combinations.
 */


/**
   Action.java - any action a node in a workflow can contain
 */

package de.suse.swamp.core.actions;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


public abstract class ActionTemplate implements ExtDescribable {

    protected String name = "";
    protected String description = "";
    protected String longDescription = "";
    protected String helpContext = "";
	protected NodeTemplate nodeTemplate;
    // set default role to system
    protected String roleName = WorkflowRole.SYSTEMROLE;


    public abstract String getType() ;
    public abstract ArrayList validate(Result r);
    public abstract void act(Result r, ResultList history);
    public abstract ArrayList getEvents(Result r);

	public ActionTemplate (String name, String role, NodeTemplate nodeTemplate){
		this.name = name;
		this.nodeTemplate = nodeTemplate;
        this.roleName = role;
	}


	/**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return Returns the helpText.
     */
    public String getHelpContext() {
        return helpContext;
    }
    /**
     * @param helpText The helpText to set.
     */
    public void setHelpContext(String helpText) {
        this.helpContext = helpText;
    }
    /**
     * @return Returns the longDescription.
     */
    public String getLongDescription() {
        return longDescription;
    }
    /**
     * @param longDescription The longDescription to set.
     */
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    public String getReplacedDescription(int workflowId) {
			Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
			return NotificationTools.workflowDataReplace(this.description, wf);
	}


    /**
	 * @return - the LongDescription with replaced Datapck content
	 */
    public String getReplacedLongDescription(int workflowId) {
        Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
        return NotificationTools.workflowDataReplace(longDescription,wf);
    }
    /**
     * @return Returns the role.
     */
    public String getRoleName() {
        return roleName;
    }


    /**
     * Get all history entries that match the wfid and prefix.
     * The most recent entry is the last in the list.
     */
    public ArrayList getHistoryEntries(int wfid, String prefix) {
        ArrayList entries = new ArrayList();
        List tasks = null;
        try {
            tasks = TaskManager.getAllTasksforAction(this, wfid);
        } catch (Exception e) {
            Logger.ERROR("Error loading Tasks for Action " + this.getName() +
                    "Reason: " + e.getMessage());
        }
        if (tasks != null){
            for (Iterator it = tasks.iterator(); it.hasNext(); ){
                WorkflowTask task = (WorkflowTask) it.next();
                entries.addAll(HistoryManager.getHistoryEntries(wfid,
                        task.getId(), prefix));
            }
        }
        return entries;
    }



    public List getActiveTasks(int wfid) throws Exception {
        ArrayList tasks = new ArrayList();
        for (Iterator it =  TaskManager.getAllTasksforAction(this, wfid).iterator(); it.hasNext(); ){
            WorkflowTask task = (WorkflowTask) it.next();
            if (task.getState() == WorkflowTask.ACTIVE){
                tasks.add(task);
            }
        }
        return tasks;
    }

    
    public List getAllTasks(int wfid) throws Exception {
        List tasks = TaskManager.getAllTasksforAction(this, wfid);
        return tasks;
    }


    /**
     * Returns a task for this action.
     * For UserActions: 
     * If there already was a task, it gets re-activated to have the same task-id.
     */
    public Task createTask(int wfId) {
        WorkflowTask task = null;
        try {
            List tasks = TaskManager.getAllTasksforAction(this, wfId);
            if (this instanceof UserActionTemplate && tasks.size() > 0){
                task = (WorkflowTask) tasks.get(tasks.size()-1);
                task.reset();
            } else {
            	Workflow wf = WorkflowManager.getInstance().getWorkflow(wfId);
                task = new WorkflowTask(this, wfId, wf.getNode(nodeTemplate.getName()).getId());
            }
        } catch (Exception e) {
            Logger.ERROR("Creation of tasks for #" + wfId + " action: " +
                    getName() + " failed: " + e.getMessage());
            e.printStackTrace();
        }
        return task;
    }
}
