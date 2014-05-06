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

import de.suse.swamp.core.workflow.*;

public class ANDCondition extends Condition {
    
    // list of included conditions
    private List conditions = new ArrayList();

    public ANDCondition(List conditions) {
        this.conditions = conditions;
        setModified(true);
    }

    public void setWorkflowId(int workflowId) {
        for (Iterator it = conditions.iterator(); it.hasNext(); ){
        ((Condition) it.next()).setWorkflowId(workflowId);
        }
    }

    public void handleEvent(Event e) {
        for (Iterator it = conditions.iterator(); it.hasNext(); ){
            ((Condition) it.next()).handleEvent(e);
        }
    }

    
    /* 
     * Evaluate if all included conditions are true
     */
    public boolean evaluate() {
        boolean result = true;
        for (Iterator it = conditions.iterator(); it.hasNext(); ){
            if (!((Condition) it.next()).evaluate()){
                result  = false;
                break;
            }
        }
        return result;
    }

    /**
     * Reset condition to original state.
     */
    public void reset() {
        for (Iterator it = conditions.iterator(); it.hasNext(); ){
            ((Condition) it.next()).reset();
        }
    }

    public String toString() {
        String ident = "[ANDCondition: ";
        for (Iterator it = conditions.iterator(); it.hasNext(); ){
            ident += ((Condition) it.next()).toString() + " ";
        }
        ident += evaluate() + "]";
        return ident;
    }

    public String getEventString() {
        String ident = "(";
        boolean first = true;
        for (Iterator it = conditions.iterator(); it.hasNext(); ){
            if (first){first = false;} else {ident += "\\nAND\\n";}
            ident += ((Condition) it.next()).getEventString() + " ";
        }
        ident += ")";
        return ident;
    }

    public String toXML() {
        return "<ANDCondition>";
    }

    public String getConditionType() {
        return "AND";
    }

    public List getChildConditions() {
        return conditions;
    }

}