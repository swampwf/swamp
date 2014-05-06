/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt (tschmidt@suse.de)
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

package de.suse.swamp.core.api;

import java.util.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;


/**

 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 */

public class TaskAPI {


	/**
	 * Reading Tasks is unrestricted, all Workflow-users may read all Tasks.
	 */
	public WorkflowTask doGetTask(int taskid, String username) throws SecurityException,
		UnknownElementException, StorageException {
		WorkflowTask t = TaskManager.getTask(taskid);
		if (t == null){
			throw new UnknownElementException("Task with ID: " + taskid + " could not be found");
		}
        // check if workflow user
        new WorkflowAPI().getWorkflow(t.getWorkflowId(), username);
		return t;
	}


	/**
	 * If the Task has the Flag "restricted", only the associated group may finish it.
	 * FIXME: we using Task as parameter here, this will probably not work from remote.
	 * But it is needed because the tasks result includes the changes to be saved.
	 */
    public void finishTask(WorkflowTask task, String username, ResultList history)
    	throws SecurityException, UnknownElementException, StorageException {
    	canFinishTask(task.getId(), username);
        TaskManager.finishTask(task, username, history);
    }

    /**
     * Check if a user is allowed to finish a task.
     * If not, an exception is thrown
     */
    public void canFinishTask(int taskId, String username)
        throws SecurityException, UnknownElementException, StorageException {
        WorkflowTask checkTask = doGetTask(taskId, username);
        Workflow wf = checkTask.getWorkflow();
        if (!(checkTask.getActionTemplate() instanceof UserActionTemplate)){
            throw new UnknownElementException("Cannot work on a systemtask. (wrong task-id " + taskId + "?)");
        }
        UserActionTemplate actionTemplate = (UserActionTemplate) checkTask.getActionTemplate();
        if (actionTemplate.isRestricted() && !wf.hasRole(username, actionTemplate.getRoleName())) {
			throw new SecurityException("You are not authorized to work on this Task! " +
                    "Required role: " + actionTemplate.getRoleName());
		}
    }


    public void cancelAllTasks(int wfid, String username) throws SecurityException,
        UnknownElementException, StorageException {

        Workflow wf = new WorkflowAPI().getWorkflow(wfid, username);
        if (!wf.hasRole(username, WorkflowRole.ADMIN)){
            throw new SecurityException("Must be admin to cancel all Tasks!");
        }
        TaskManager.cancelTasks(wfid, username);
    }


	/**
	 * Returns the Tasks of this Workflow if the User is allowed to read the workflow
	 */
	public ArrayList getUserTasks(int wfid, boolean mandatory, String username)
        throws SecurityException, UnknownElementException, StorageException {
		// check wf-permissions (wf-user)
		new WorkflowAPI().getWorkflow(wfid, username);
        // FIXME: check single Task permissions
		ArrayList tasks = TaskManager.getActiveUserTasks(wfid, mandatory);
		return tasks;
	}

    /**
     * Returns all active Tasks of this Workflow if the User is allowed to read the workflow
     */
    public List getTasksForWorkflow(int wfid, String username)
        throws SecurityException, UnknownElementException, StorageException {
        // check wf-permissions (wf-user)
        new WorkflowAPI().getWorkflow(wfid, username);
        // FIXME: check single Task permissions
        List tasks = TaskManager.getActiveTasksForWorkflow(wfid);
        return tasks;
    }

    public int doGetTaskCacheSize(String username) throws SecurityException, 
        UnknownElementException, StorageException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(username), "swampadmins")){
            throw new SecurityException("No permission to read Taskcache!");
        }
        return TaskManager.getCacheSize();
    }   
    
    
    public void doEmptyTaskcache(String uname) throws StorageException, UnknownElementException, SecurityException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(uname), "swampadmins")) {
            throw new SecurityException("Not allowed to truncate task-cache.");
        }
        TaskManager.clearCache();
    }
    
}