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

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;

public abstract class Condition extends Persistant {

    /**
     * central method to let condition handle events - needs to be overwritten
     * by subclasses
     *
     * @param e the event to process.
     */
    public abstract void handleEvent(Event e);

    /**
     * every component of a workflow object gets a reference to the same
     */
    public abstract void setWorkflowId(int WorkflowId);

    /**
     * evaluation method
     */
    public abstract boolean evaluate();

    public abstract void reset();

    /**
     * returns the type of the condition. Needs to be reimplemented.
     * 
     * @return the condition type as string
     */
    public abstract String getConditionType();

    /**
     * get a list of nested conditions.
     * 
     * @return list of nested conditions.
     */
    public abstract List getChildConditions();

    public abstract String toXML();

    public abstract String toString();

    /** This String is generated to be displayed in the generated Workflow graph
     * @return
     */
    public abstract String getEventString();
}