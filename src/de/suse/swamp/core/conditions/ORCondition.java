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

/**
 * Represents the OR operator to use in compound conditions.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

public class ORCondition extends Condition {

    public ORCondition(Condition cond1, Condition cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
        setModified(true);
    }

    public void setWorkflowId(int workflowId) {
        cond1.setWorkflowId(workflowId);
        cond2.setWorkflowId(workflowId);
    }

    public void handleEvent(Event e) {
        cond1.handleEvent(e);
        cond2.handleEvent(e);
    }

    public boolean evaluate() {
        return (cond1.evaluate() || cond2.evaluate());
    }

    /**
     * Reset condition to original state. 
     */
    public void reset() {
        cond1.reset();
        cond2.reset();
    }

    public String toString() {
        return "[ORCondition: " + cond1.toString() + " " + cond2.toString() 
        + " " + evaluate() + "]";
    }

    public String getEventString() {
        return "(" + cond1.getEventString() + "\\nOR\\n" + 
        		cond2.getEventString() + ")";
    }

    public String toXML() {
        return "<ORCondition>\n" + cond1.toXML() + "/n" + cond2.toXML() 
        + "\n</ORCondition>";
    }

    public String getConditionType() {
        return "OR";
    }

    public List getChildConditions() {
        ArrayList list = new ArrayList();
        list.add(cond1);
        list.add(cond2);
        return list;
    }

    private Condition cond1;
    private Condition cond2;
}