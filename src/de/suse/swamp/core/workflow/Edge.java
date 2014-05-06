/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh [at] suse.de>
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

package de.suse.swamp.core.workflow;

 /**
  * A connection between two nodes in a workflow
  *
  * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
  * @version $Id$
  *
  */

import de.suse.swamp.core.conditions.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.util.*;

public class Edge extends Persistant {

    private int workflowId;
    // cache for the condition
    private Condition cond;
    private Node from;
    private Node to;


    /**
     * Constructor called from Storage on restoring object
     */
    public Edge(int id, Node from, Node to) {
        this.from = from;
        this.to = to;
        this.setId(id);
        setModified(true);
    }


    /**
     * Constructor called from Edgetemplate where we have no ID yet
     */
    public Edge(Node from, Node to, Condition cond) {
        this.from = from;
        this.to = to;
        this.cond = cond;
        setModified(true);
    }


    public Condition getCondition() {
        // condition already loaded from DB yet?
        if (cond == null) {
            cond = WorkflowManager.loadCondition(this);
        }
        return cond;
    }

    public boolean isConditionLoaded() {
    	return (cond != null);
    }

    public void setWorkflowId(int workflowId) {
        if (this.workflowId == 0) {
            this.workflowId = workflowId;
        }
    }

    /**
     * @param e the event to send to its conditions
     * @return the new active node or null
     */
    public Node handleEvent(Event e) {
        getCondition().handleEvent(e);
        if (getCondition().evaluate()) {
            return to;
        }
        return null;
    }

    /**
     * Reset edge to original state. A node may be visited more than once in the
     * lifetime of a workflow object (loops are perfectly valid constructs in
     * workflow definitions). Depending on their type, the conditions have to be
     * reset when the node this edge is belonging to is left.
     */
    public void reset() {
        getCondition().reset();
    }

    public Node getFrom() {
        return from;
    }

    public Node getToNode() {
        return to;
    }

    public String toString() {
        return "[Edge: " + getFrom().getName() + "->" + to.getName() + " "
                + getCondition() + "]";
    }

    public String toXML() {
        return "<Edge from=\"" + getFrom() + "\" to=\"" + to.getName()
                + "\">\n" + getCondition().toXML() + "\n</Edge>";
    }


    /**
     * @return Returns the workflow.
     */
    public int getWorkflowId() {
        return workflowId;
    }


}
