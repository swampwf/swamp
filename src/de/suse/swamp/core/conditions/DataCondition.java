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

package de.suse.swamp.core.conditions;

import java.util.*;

import org.apache.regexp.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class DataCondition extends Condition {

    // the piece of data to check
    private String field;
    
    // the type of check ("regexp" | "changed")
    private String check;

    // compare the content of "field" with 
    // (change listeners use it to remember the last known value, 
    // comment change listeners use it to remember the last known thread length)
    private String value;
    
    // the workflow object the condition is part of (needed to access the datapack)
    private int workflowId;

    private boolean state;

    public DataCondition(String field, String check, String value, boolean state) {
        this.field = field;
        this.check = check;
        this.value = value;
        this.state = state;
        setModified(true);
    }

    public void setWorkflowId(int workflowId) {
        if (this.workflowId == 0) {
            this.workflowId = workflowId;
        }
    }

   
    /** Empty here, because the Datacondition doesn't need an Event, 
     * it checks its Condition on each evaluate()
     */
    public void handleEvent(Event e) {
    }

    
    /**
     * Evaluate Databit with check-method and value
     */
    public boolean evaluate() {
		Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
		Databit dbit = wf.getDatabit(field);
		if (dbit != null) {
			String checkvalue = dbit.getValue();
			setState(false);
			if (this.check.equals("regexp")) {
                // FIXME: Jakarta Regexps collapse on very large Strings.
                if (checkvalue.length() > 150) {
                    checkvalue = checkvalue.substring(0, 149);
                    Logger.DEBUG("Shortened String " + field + " for regexp eval.");
                }
                RE re = new RE(this.value);
				try {
                    // match on empty regexp returns true...
                    if (!(this.value.equals("") && !checkvalue.equals("")) && re.match(checkvalue)) {
						setState(true);
					}
				} catch (Exception e) {
					Logger.ERROR("Error in checking Regexp: " + this.value + " for Databit " + field);
				}
			} else if (this.check.equals("changed")) {
                // if the change listener targets a comment, fire on each new reply
                if (dbit.getType().equals("comment") || dbit.getType().equals("thread")){  
                    int knownThreadLength;
                    List thread = ((commentDatabit) dbit).getThreadFields("test", false);
                    int threadLength = thread.size();
                    try {
                        knownThreadLength = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // cannot get number. maybe old string, set to current length
                        Logger.DEBUG("Change Listener value is no number: " + value + " setting to: " + threadLength);
                        knownThreadLength = threadLength;
                    }
                    Logger.DEBUG("Change listener (" + getField() + "): known thread length: " + knownThreadLength + 
                            ", current thread length: " + threadLength);
                    if (knownThreadLength < threadLength){
                        Logger.DEBUG("Change Listener detected new comment in " + field);
                        setState(true);
                    }
                    value = "" + threadLength;
                    setModified(true);
                } else {
    				if (!value.equals(checkvalue)) {
    					setState(true);
    					value = checkvalue;
    					setModified(true);
    				}
                }
			}
			return state;
		} else {
			Logger.ERROR("Databit " + field + " not found for Datacondition!");
			return false;
		}
	}

    /**
	 * Reset condition - If watching a databit for changes, the initial value is set now, 
	 * if watching a thread, the current thread length.
	 */
    public void reset() {
        if (this.check.equals("changed")) {
            Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
            Databit dbit = wf.getDatabit(field);
            if (dbit.getType().equals("comment") || dbit.getType().equals("thread")){  
                List thread = ((commentDatabit) dbit).getThreadFields("test", false);
                this.value = "" + thread.size();
            } else if (dbit != null){
                this.value = dbit.getValue();
            }
        }
        setState(false);
    }

    public String toString() {
        return "[DataCondition: " + field + " " + check + " " + value + 
        	" " + state + "]";
    }

    public String getEventString() {
        String eventString = null;
        if (this.check.equals("regexp")){
            eventString = this.field + "~=" + this.value;
        } else if (this.check.equals("changed")){
            eventString = this.field + " changed";
        }
        if (state)
            eventString += " (true)";
        return eventString;
    }

    public String toXML() {
        return "<DataCondition field=\"" + field + "\" check=\"" + check + 
        	"\" value=\"" + value + "\" state=\"" + state + "\" />";
    }

    public String getConditionType() {
        return "DATA";
    }

    public List getChildConditions() {
        ArrayList list = new ArrayList();
        return list;
    }

    /**
     * @return Returns the check.
     */
    public String getCheck() {
        return check;
    }

    /**
     * @return Returns the field.
     */
    public String getField() {
        return field;
    }

    /**
     * @return Returns the value.
     */
    public String getValue() {
        return value;
    }

    public boolean getState() {
        return this.state;
    }
    
    
    private void setState(boolean newState) {
        if (this.state != newState){
            this.state = newState;   
            setModified(true);
        }
        
    }
}