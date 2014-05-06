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

package de.suse.swamp.core.conditions;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.workflow.*;

public class SubsFinishedCondition extends Condition {

    // name of the subworkflows to wait for
    private String subname;
    private String subversion;
    // the workflow object the condition is part of (needed to send Event to parent)
    private int workflowId;
    private boolean state;


    public SubsFinishedCondition(String subname, String subversion, boolean state) {
        this.subname = subname;
        this.subversion = subversion;
        this.state = state;
        setModified(true);
    }


    public void setWorkflowId(int workflowId) {
        if (this.workflowId == 0) {
            this.workflowId = workflowId;
        }
    }

    
    /** A State of true (^= 1 in DB) means that this Edge is "open".
     * @param newState
     */
    private void setState(boolean newState) {
        if (this.state != newState){
            this.state = newState;   
            setModified(true);
        }
    }
   
    
    /** Waiting for the Event SUBWORKFLOW_FINISHED that indicates that one 
     * appending Subworkflow was finished. If that Subworkflow was the last 
     * one running of the expexted Subworkflow-type, this Condition will 
     * resolve.
     */
    public void handleEvent(Event e) {
        if (e.getType().equals(Event.SUBWORKFLOW_FINISHED)) {
            WorkflowManager wfman = WorkflowManager.getInstance();
            Workflow wf = wfman.getWorkflow(e.getSenderWfId());
            if (wf.getTemplateName().equals(this.subname)) {
                boolean tempstate = true;
                // check if this was the last running subworkflow of that type:
                Workflow parent = wfman.getWorkflow(workflowId);
                for (Iterator it = parent.getSubWorkflows().iterator(); it.hasNext();) {
                    Workflow subwf = (Workflow) it.next();
                    if (subwf.isRunning()) {
                        tempstate = false;
                        break;
                    }
                }
                setState(tempstate);
            }
        }
    }

    
    /**
     * Evaluate Databit with check-method and value
     */
    public boolean evaluate() {
        return state;
    }

    /**
     * Reset condition - does nothing. A data condition can't be and doesn't
     * need to be reset.
     */
    public void reset() {
        setState(false);
    }

    public String toString() {
        return "[SubsFinishedCondition: subname=" + subname + " state=" + state + "]";
    }

    public String getEventString() {
            return "all subworkflows finished";
    }

    public String toXML() {
        return "<DataCondition subname=\"" + subname + "\" state=\"" + state + "\"/>";
    }

    public String getConditionType() {
        return "SUB";
    }

    public List getChildConditions() {
        ArrayList list = new ArrayList();
        return list;
    }

    /**
     * @return Returns the subname.
     */
    public String getSubname() {
        return this.subname;
    }


	public String getSubversion() {
		return subversion;
	}
}