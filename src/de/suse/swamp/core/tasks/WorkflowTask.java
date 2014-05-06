/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.tasks;

import java.util.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 */
public class WorkflowTask extends Task {

    protected Result result;
    protected ActionTemplate actionTemplate;
    private int workflowId;
    private int nodeId;
    private int state = WorkflowTask.ACTIVE;

    // Valid Task states
    public final static HashMap validStates;
    static {
        validStates = new HashMap();
        validStates.put(new Integer(WorkflowTask.ACTIVE), "ACTIVE");
        validStates.put(new Integer(WorkflowTask.DONE), "DONE");
        validStates.put(new Integer(WorkflowTask.REMOVED), "REMOVED");
        validStates.put(new Integer(WorkflowTask.CANCELED), "CANCELED");
    }

    public static final int ACTIVE = 1;
    public static final int DONE = 2;
    public static final int REMOVED = 3;
    public static final int CANCELED = 4;


    /**
     * Only use this to create a new Object.
     * To request a task from an action, use: <actiontemplate>.getTask()
     */
    public WorkflowTask(ActionTemplate action, int workflowId, int nodeId) {
        this.actionTemplate = action;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        setResult(action, workflowId);
    }

    public String getActionType() {
        return actionTemplate.getType();
    }

    public ActionTemplate getActionTemplate() {
        return actionTemplate;
    }

    public int getWorkflowId() {
        return workflowId;
    }

    public Workflow getWorkflow() {
        return WorkflowManager.getInstance().getWorkflow(workflowId);
    }

    public String getDescription() {
        return "WorkflowTask: " + actionTemplate.getDescription();
    }

    public String getReplacedDescription() {
        return actionTemplate.getReplacedDescription(workflowId);
    }


    public void act(){
        actionTemplate.act(result, new ResultList());
    }

    public void act(ResultList history){
        actionTemplate.act(result, history);
    }

    public Result getResult() {
        return result;
    }

    public ArrayList validate() {
        return actionTemplate.validate(result);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[Task (" + getActionTemplate().getType() + "): ");
        sb.append(getReplacedDescription());
        sb.append(" Id: " + getId() + " ]");
        return new String(sb);
    }


    public String toXML() {
        StringBuffer sb = new StringBuffer();
        sb.append("<Task id=" + this.getId());
        sb.append(" state=" + state + ">" );
        return new String(sb);
    }



    public ArrayList getEvents() {
        ArrayList events = actionTemplate.getEvents(result);
        return events;
    }


    /**
     * @return Returns the state.
     */
    public int getState() {
        return state;
    }

    /**
     * @param state The state to set.
     */
    public void setState(int newstate) {
        if (validStates.get(new Integer(newstate)) != null){
            // update task cache on state changes: 
            if (newstate != state) {
                if (newstate == ACTIVE) {
                    try {
                        TaskManager.addToActiveTaskCache(this);
                    } catch (StorageException e) {
                        Logger.ERROR("Error adding task to cache: "  + e.getMessage());
                    }
                }
                else TaskManager.removeFromActiveTaskCache(getWorkflowId(), getId());
            }
            this.state = newstate;
        } else {
            Logger.ERROR("Trying to set illegal Task-State=" + newstate);
        }

    }


    public String getRoleName() {
        return getActionTemplate().getRoleName();
    }


    /**
     * @return - list of users that are in the role that is assigned to this task
     */
    public SWAMPHashSet getUsersForRole() {
        String roleName = getActionTemplate().getRoleName();
        SWAMPHashSet members = new SWAMPHashSet();
        if (roleName != null && !roleName.equals(WorkflowRole.SYSTEMROLE)){
        	Workflow wf = WorkflowManager.getInstance().getWorkflow(this.getWorkflowId());
            WorkflowRole role = wf.getRole(getActionTemplate().getRoleName());
            if (role != null) {
                try {
                    members = role.getMemberNames(wf);
                } catch (Exception e) {
                    Logger.ERROR("Can't get users for role: " + e.getMessage());
                }
            }
        }
        return members;
    }


    private void setResult(ActionTemplate action, int workflowId) {
        /* Depending on the action type, we generate the proper result */
        if (action instanceof DecisionActionTemplate) {
            this.result = new DecisionResult(workflowId);
        } else if (action instanceof DataeditActionTemplate) {
            this.result = new DataeditResult(workflowId);
        } else if (action instanceof ManualtaskActionTemplate) {
            this.result = new ManualtaskResult(workflowId);
        } else if (action instanceof NotifyActionTemplate) {
            this.result = new NotifyActionResult(workflowId);
            ((NotifyActionResult) result).setTask(this);
        } else if (action instanceof SendEventActionTemplate) {
            this.result = new SendEventActionResult(workflowId);

        } else if (action instanceof CustomActionTemplate ||
                action instanceof StartSubworkflowActionTemplate ||
                action instanceof ScriptActionTemplate) {
            result = new Result(workflowId);
        } else {
            Logger.BUG("WorkflowTask: Unknown ActionType " + action.getType());
        }
    }


    public void reset() {
        setState(ACTIVE);
        setResult(actionTemplate, workflowId);
    }

	public int getNodeId() {
		return nodeId;
	}

}