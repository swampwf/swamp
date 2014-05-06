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

/**
 * Wait for an event to happen and resolve to true then.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

import java.util.*;

import de.suse.swamp.core.workflow.*;

public class EventCondition extends Condition {

    private int workflowId;
    private Event event;
    private boolean state;
    
    public EventCondition(Event event, boolean state) {
        this.event = event;
        this.state = state;
        setModified(true);
    }

    public void setWorkflowId(int workflowId) {
        if (this.workflowId == 0) {
            this.workflowId = workflowId;
        }
    }

    public void handleEvent(Event e) {
        if (e.equals(event)) {
            setState(true);
        }
    }

    public boolean evaluate() {
        return state;
    }


    /**
     * Reset condition to original state.
     */
    public void reset() {
        setState(false);
    }

    public String toString() {
        return "[EventCondition: " + event.getType() + " " + state + "]";
    }

    public String getEventString() {
        String eventString = event.getType();
        if (state)
            eventString += " (true)";
        return eventString;
    }

    public String toXML() {
        return "<EventCondition type=\"" + event.toXML() + 
        "\" state=\"" + state + "\" />";
    }

    public String getConditionType() {
        return "EVENT";
    }

    public List getChildConditions() {
        return new ArrayList();
    }
    
    private void setState(boolean newState) {
        if (this.state != newState){
            this.state = newState;   
            setModified(true);
        }
    }
    
}