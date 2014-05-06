/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh@suse.de>
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

/**
 * Manage tasks assigned to roles. As soon as an action is
 * encountered by a case, it is associated with a certain role, and a
 * <b>Task</b> is assigned to that role. This task is then passed to the
 * <b>TaskManager</b> that takes care of notifying and storing the active task.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author Thomas Schmidt
 *
 */

import java.util.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public final class TaskManager {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
        "de.suse.swamp.core.container.TaskManager");
    
    // key: workflowid, value: List with ACTIVE tasks
    private static HashMap activeTaskCache = null;
    
    private TaskManager() {
    }

    /**
     * Add a task to the system.
     */
    public static void addTask(WorkflowTask task, String uname, ResultList history)
    	throws Exception {

        // in the moment we only have workflowtasks.
        TaskStorage.storeTask(task);
        TaskManager.addToActiveTaskCache(task);
        task.getResult().setUname(uname);

        // add to workflow history
        if (task.getActionTemplate() instanceof UserActionTemplate) {
            HistoryManager.create("TASK_START", task.getId(), task.getWorkflowId(), uname, null);
        } else if (task.getActionTemplate() instanceof SystemActionTemplate) {
            HistoryManager.create("SYSTEMTASK_START", task.getId(), task.getWorkflowId(), uname, null);
        }
        // create notification
        if (task.getActionTemplate() instanceof UserActionTemplate) {
            UserActionTemplate action = (UserActionTemplate) task.getActionTemplate();
            if (action.isMandatory()){
                history.addResult(ResultList.MESSAGE, "Task \"" +
                        action.getReplacedDescription(task.getWorkflowId()) + "\" activated.");
            } else {
                history.addResult(ResultList.MESSAGE, "optional Task \"" +
                        action.getReplacedDescription(task.getWorkflowId()) + "\" activated.");
            }
            if (!action.getNotificationTemplate().equals("")) {
                try {
                    Workflow wf = WorkflowManager.getInstance().getWorkflow(task.getWorkflowId());

                    // send a notification to all members of the assigned role:
                    NotificationManager.newNotification(task.getId(), task.getWorkflowId(),
                            action.getNotificationTemplate(), "", "", "", task.getRoleName());
                    history.addResult(ResultList.MESSAGE, "Queued notification for " +
                            wf.getRole(task.getRoleName()).getMemberNames(wf) + " about the new Task: "
                            + action.getReplacedDescription(wf.getId()));
                } catch (Exception e) {
                    log.error("Task-Notification failed, Message: "
                            + e.getMessage());
                }
            } else {
                log.debug("No Notification-Template given for Task: "
                        + task.getReplacedDescription());
            }
        } else if (task.getActionTemplate() instanceof SystemActionTemplate) {
            // TimeTriggers should stay active and not be finished if not triggered immediately
            if (task.getActionTemplate() instanceof SendEventActionTemplate){
                task.act(history);
                if (((SendEventActionResult) task.getResult()).isDone()){
                    finishTask(task, uname, history);
                } else {
                    TaskStorage.storeTask(task);
                }
            } else {
                // Systemtasks are done immediately
                finishTask(task, uname, history);
            }
        } else {
            log.fatal("Action for Task " + task.getId() + " undefined.");
        }
    }



    /**
     * Cancels a Task, that means setting its state to CANCELED
     * @param t - Task to cancel
     */
    public static void cancelTask(WorkflowTask t, String userName) throws StorageException {
        synchronized (t) {
	    	t.setState(WorkflowTask.CANCELED);
	        TaskStorage.storeTask(t);
	        if (t.getActionTemplate() instanceof UserActionTemplate) {
	        	HistoryManager.create("TASK_CANCELED", t.getId(), t.getWorkflowId(),
	                userName, null);
	        } else if (t.getActionTemplate() instanceof SystemActionTemplate) {
	        	HistoryManager.create("SYSTEMTASK_CANCELED", t.getId(), t.getWorkflowId(),
		                userName, null);
	        } else {
	        	log.error("Unknown action type!");
	        }
		}
    }

    /**
     * Cancels all open Tasks of this workflow
     * @param t - Task to cancel
     */
    public static void cancelTasks(int wfid, String userName)
        throws StorageException {
    	for (Iterator it = new ArrayList(getActiveTasksForWorkflow(wfid)).iterator(); it.hasNext(); ){
			WorkflowTask t = (WorkflowTask) it.next();
			TaskManager.cancelTask(t, userName);
        }
    }


    /**Queries the DB for Tasks that belog to this Workflow
     *
     * @param workflow
     * @return - all Tasks for that Workflow
     */
    public static List getActiveTasksForWorkflow(int wfid) throws StorageException {
		fillCache();
		List tasks = (List) activeTaskCache.get(new Integer(wfid));
		if (tasks == null) tasks = new ArrayList();
		return tasks;
	}



    public static synchronized void cancelTasksForNode (int wfid,
        Node node, String userName) throws StorageException {
        ArrayList tasks = getActiveTasksForNode(node, wfid);
        for (Iterator iter = tasks.iterator(); iter.hasNext();) {
            WorkflowTask task = (WorkflowTask) iter.next();
            cancelTask(task, userName);
        }
     }



   /**Gets the Tasks of a special Node out of the wfTasks List.
    * @param node - Node for which to get the Tasks
    * @return - ArrayList of Tasks
    */
	public static ArrayList getActiveTasksForNode(Node node, int wfid)
		throws StorageException {
        List tasks = getActiveTasksForWorkflow(wfid);
		ArrayList result = new ArrayList();
	    for (Iterator iter = tasks.iterator(); iter.hasNext();) {
			WorkflowTask tmp = (WorkflowTask) iter.next();
			if (node.getId() == tmp.getNodeId()) {
				result.add(tmp);
			}
		}
	    return result;
	}


	public static ArrayList getActiveUserTasks(int wfid, boolean mandatory) throws StorageException {
        List tasks = getActiveTasksForWorkflow(wfid);
        ArrayList result = new ArrayList();
        for (Iterator iter = tasks.iterator(); iter.hasNext();) {
            WorkflowTask tmp = (WorkflowTask) iter.next();
            if (tmp.getActionTemplate() instanceof UserActionTemplate
                    && ((UserActionTemplate) tmp.getActionTemplate()).
                    isMandatory() == mandatory) {
                result.add(tmp);
            }
        }
        return result;
    }


    public static ArrayList getAllActiveTasks(boolean mandatory) throws StorageException {
        ArrayList result = new ArrayList();
        for (Iterator iter = getAllActiveTasks().iterator(); iter.hasNext();) {
            WorkflowTask tmp = (WorkflowTask) iter.next();
            if (tmp.getActionTemplate() instanceof UserActionTemplate
                    && ((UserActionTemplate) tmp.getActionTemplate()).
                    isMandatory() == mandatory) {
                result.add(tmp);
            }
        }
        return result;
    }


    public static ArrayList getAllActiveTasks(int wfid, String actionName) throws StorageException {
        fillCache();
        List activeTasks = (List) activeTaskCache.get(new Integer(wfid));
        ArrayList result = new ArrayList();
        if (activeTasks != null) {
            for (Iterator iter = activeTasks.iterator(); iter.hasNext();) {
                WorkflowTask tmp = (WorkflowTask) iter.next();
                if (tmp.getActionTemplate().getName().equals(actionName)) {
                    result.add(tmp);
                }
            }
        }
        return result;
    }


    public static List getAllActiveTasks() throws StorageException {
        fillCache();
        List activeTasks = new ArrayList();
        for (Iterator it = activeTaskCache.values().iterator(); it.hasNext(); ) {
            activeTasks.addAll((List) it.next());
        }
        return activeTasks;
    }

    /**
     * Returns all Tasks that were created from this action, the latest at last.
     */
    public static List getAllTasksforAction(ActionTemplate action, int wfid) throws StorageException {
        return TaskStorage.loadAllTasksforAction(action, wfid);
    }


    public static WorkflowTask getTask(int taskId) throws StorageException {
        return TaskStorage.loadTask(taskId);
    }


    /**
     * @param id - Taskid of the Task to finish
     * @throws Exception when Task couldn't be finished
     */
    public static void finishTask(WorkflowTask task,
            String userName, ResultList history) throws StorageException {

        if (task.getState() != WorkflowTask.ACTIVE){
            Logger.WARN("Trying to finish a non-active Task! id="+task.getId(), log);
        } else {
            // everything's fine? call act() so that anything that needs to be saved
            // or written or done gets saved, written, done
            task.getResult().setUname(userName);
            task.act(history);
            task.setState(WorkflowTask.DONE);
            TaskStorage.storeTask(task);
            
            // if this task had an error, do net send it's event
            if (!history.hasErrors()) {
                // extract and fire the events to be fired
                ArrayList events = task.getEvents();
                if (task.getActionTemplate() instanceof UserActionTemplate){
                    history.addResult(ResultList.MESSAGE, "Task \"" + task.getActionTemplate().getReplacedDescription(task.getWorkflowId())
                        + "\" was finished.");
                }
                Event ev = null;
                if (events == null || events.size() == 0){
                    log.debug("Got no Events from Task: " + task.getActionType() + " #" + task.getId());
                } else {
                    ev = (Event) events.get(0);
                }
                if (task.getActionTemplate() instanceof UserActionTemplate){
                	HistoryManager.create("TASK_DONE", task.getId(), task.getWorkflowId(), userName, null);
                } else if (task.getActionTemplate() instanceof SystemActionTemplate){
                        HistoryManager.create("SYSTEMTASK_DONE", task.getId(), task.getWorkflowId(), userName, null);
                } else {
                	Logger.ERROR("Unknown Action type!", log);
                }
    
                // Don't do Event-handling for "NONE" Events
                if (ev != null && !(ev.getType().equalsIgnoreCase("none"))) {
                    // The event manager takes the events and handles them for this
                     // workflow immediately, Storage of Workflow is done inside
                    EventManager.handleWorkflowEvents(events, userName, history);
                }
            }
        }
    }
    
    
    private static void fillCache() throws StorageException {
        if (activeTaskCache == null) {
            long time = System.currentTimeMillis();
            List tasks = TaskStorage.loadAllActiveTasks();
            activeTaskCache = new HashMap();
            for (Iterator it = tasks.iterator(); it.hasNext(); ) {
                WorkflowTask task = (WorkflowTask) it.next();
                addToActiveTaskCache(task);
            }
            Logger.DEBUG("Building activeTaskCache cache took: " + (System.currentTimeMillis() - time) + " ms.", log);
        }
    }
    

    public static void addToActiveTaskCache(WorkflowTask task) throws StorageException {
        TaskManager.fillCache();
        List tasks = (List) activeTaskCache.get(new Integer(task.getWorkflowId()));
        if (tasks == null) {
            tasks = new ArrayList();
        }
        if (!tasks.contains(task)) { 
            tasks.add(task);
            activeTaskCache.put(new Integer(task.getWorkflowId()), tasks);
        }
        Logger.DEBUG("Added task #" + task.getId() + " to activeTaskCache.", log);
    }
    
    
    public static void removeFromActiveTaskCache(int wfId, int taskId) {
        if (activeTaskCache != null && activeTaskCache.containsKey(new Integer(wfId))) {
            List tasks = (List) activeTaskCache.get(new Integer(wfId));
            for (Iterator it = tasks.iterator(); it.hasNext();) {
                WorkflowTask task = (WorkflowTask) it.next();
                if (task.getId() == taskId) {
                    tasks.remove(task);
                    Logger.DEBUG("Removed task #" + task.getId() + " from activeTaskCache.", log);
                    break;
                }
            }
        }
    }
    
    
    public static void removeFromActiveTaskCache(int wfId) {
        if (activeTaskCache != null && activeTaskCache.containsKey(new Integer(wfId))) {
            activeTaskCache.remove(new Integer(wfId));
        }
    }
    
    
    public static void clearCache() {
        activeTaskCache = null;  
    }
    
    public static int getCacheSize() throws StorageException {
        if (activeTaskCache != null) {
            return activeTaskCache.size();
        }
        return 0;
    }

}
