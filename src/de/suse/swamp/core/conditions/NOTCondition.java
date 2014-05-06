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
 * A compound condition with one operand, negating it.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

public class NOTCondition extends Condition {

    public NOTCondition(Condition cond) {
        this.cond = cond;
        setModified(true);
    }

    public void setWorkflowId(int workflowId) {
        cond.setWorkflowId(workflowId);
    }

    public void handleEvent(Event e) {
        cond.handleEvent(e);
    }

    public boolean evaluate() {
        return (!cond.evaluate());
    }

    public void reset() {
        cond.reset();
    }

    public String toString() {
        return "[NOTCondition: " + cond.toString() + " " + evaluate() + "]";
    }

    public String getEventString() {
        return "(NOT " + cond.getEventString() + ")";
    }

    public String toXML() {
        return "<NOTCondition>\n" + cond.toXML() + "\n</NOTCondition>";
    }

    public String getConditionType() {
        return "NOT";
    }

    public List getChildConditions() {
        ArrayList list = new ArrayList();
        list.add(cond);
        return list;
    }

    private Condition cond;
}