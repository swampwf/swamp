/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Thomas Schmidt <tschmidt@suse.de>
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

package de.suse.swamp.core.container;

import java.util.*;

import org.apache.torque.*;
import org.apache.torque.util.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

/**
 * This is the Task-Storage-Manager of the SWAMP project. It offers save
 * and restore methods for Task objects.
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *
 * @version $Id$
 */

final class TaskStorage {

    // extra logger for storage stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.container.TaskStorage");

    /**
     * Storing of Tasks.
     * @param task - The Task to store
     * @return - the id of the stored Task.
     */
    static int storeTask(WorkflowTask task) throws StorageException {

        Dbtasks dbtask = new Dbtasks();
        int taskid = task.getId();
        String taskActionName = task.getActionTemplate().getName();

        // Objects with an id <= 0 are new.
        boolean isnew = taskid <= 0 ? true : false;
        dbtask.setNew(isnew);

        Logger.LOG("Storing Task: #" + taskid + " (action:  "
                + task.getActionType() + " #" + taskActionName + ")", log);

        try {
            // Creating dbtasks database object
            dbtask.setActionname(taskActionName);
            dbtask.setNodeid(task.getNodeId());
            dbtask.setWorkflowid(task.getWorkflowId());
            dbtask.setTaskid(taskid);
            dbtask.setState(task.getState());
            dbtask.save();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.ERROR("Error in storing Task #" + taskid + "("
                    + task.getActionType() + ")", log);
            Logger.ERROR("Message: " + e.getMessage(), log);
            throw new StorageException("Could not store Task", e);
        }

        // Set Taskid for new Tasks
        if (isnew) {
			task.setId(dbtask.getTaskid());
		}

        return task.getId();
    }


    static List loadAllTasksforWorkflow(int wfid) throws StorageException {
        log.debug("Loading all tasks for wf #" + wfid);
        Criteria crit = new Criteria();
        crit.add(DbtasksPeer.WORKFLOWID, wfid);
        crit.addAscendingOrderByColumn(DbtasksPeer.TASKID);
        return loadTasks(crit);
    }


    static List loadAllTasksforAction(ActionTemplate action, int wfid) throws StorageException {
        log.debug("Loading all tasks for action " + action.getName() + " in wf #" + wfid);
        Criteria crit = new Criteria();
        crit.add(DbtasksPeer.WORKFLOWID, wfid);
        crit.add(DbtasksPeer.ACTIONNAME, action.getName());
        crit.addAscendingOrderByColumn(DbtasksPeer.TASKID);
        return loadTasks(crit);
    }



    static List loadAllActiveTasks() throws StorageException {
        log.debug("Loading all active tasks");
    	Criteria crit = new Criteria();
        crit.add(DbtasksPeer.STATE, WorkflowTask.ACTIVE);
        crit.addAscendingOrderByColumn(DbtasksPeer.TASKID);
        return loadTasks(crit);
    }


    /**
     * @param id - the Taskid to load the Task from
     * @return - the retrieved Task Object
     */
    static WorkflowTask loadTask(int id) throws StorageException {
        log.debug("Loading task #" + id);
        Criteria crit = new Criteria();
        crit.add(DbtasksPeer.TASKID, id);
        List tasks = loadTasks(crit);
        if (tasks.size() != 1){
            Logger.ERROR("!=1 Task with id=" + id, log);
            return null;
        }
         return (WorkflowTask) tasks.get(0);
    }




    /**
     * @param crit - Criteria-Object for the SQL-Statement that fits
     * to the Tasks we want to load.
     * @return - ArrayList of selected Tasks
     * @throws Exception - when something in the Task-Properties is broken
     */
    private static List loadTasks(Criteria crit) throws StorageException {
        log.debug("Loading tasks by criteria: " + crit.toString());
        List dbtasks = new ArrayList();
        List tasks = new ArrayList();
        HashSet wfids = new HashSet();
        try {
            dbtasks = DbtasksPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.BUG("Could not select task list " + e, log);
        }
        WorkflowManager wfman = WorkflowManager.getInstance();
        for (Iterator it = dbtasks.iterator(); it.hasNext();) {
            Dbtasks t = (Dbtasks) it.next();
            wfids.add(new Integer(t.getWorkflowid()));
        }
        // bulk-load workflows
        WorkflowManager.getInstance().getWorkflows(wfids);
        for (Iterator it = dbtasks.iterator(); it.hasNext();) {
        	Dbtasks t = (Dbtasks) it.next();
            Workflow wf = null;
	        try {
	            wf = wfman.getWorkflow(t.getWorkflowid());
	        } catch (Exception e) {
	            throw new StorageException("Could not get workflow from task-reference " +
	                    "(wfid=" + t.getWorkflowid() + ", taskid=" + t.getTaskid() + ")");
	        }
            ActionTemplate action = wf.getTemplate().getActionTemplate(t.getActionname());
            if (action != null) {
                WorkflowTask task = new WorkflowTask(action, t.getWorkflowid(), t.getNodeid());
                task.setId(t.getTaskid());
                task.setState(t.getState());
                tasks.add(task);
            } else {
                Logger.ERROR("Unable to load action for workflowtask (" + t.getActionname() + " in node " +
                		t.getNodeid() + ", wfid: " + wf.getId() + ").", log);
                throw new StorageException("Unable to create workflowtask " + "without Action.");
            }
        }
        return tasks;
    }


    static protected void removeTasks(Workflow wf) throws TorqueException {
        Criteria crit = new Criteria();
        crit.add(DbtasksPeer.WORKFLOWID, wf.getId());
        DbtasksPeer.doDelete(crit);
    }

}
