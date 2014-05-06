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
  * A template to represent and create a connection between two nodes in a
  * workflow.
  *
  * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
  * @version $Id$
  *
  */

import java.util.*;

import de.suse.swamp.core.conditions.*;

public class EdgeTemplate {

    // no templates here, to avoid circular reference. The node templates are
    // kept by the workflow template, the references are correctly built in
    // getEdge().
    private String fromId;
    private String toId;
    private ConditionTemplate condTempl;
    
    
    public EdgeTemplate(String fromId, String toId) {
        this.fromId = fromId;
        this.toId = toId;
    }

    public EdgeTemplate(String fromId, String toId, ConditionTemplate condTempl) {
        this.fromId = fromId;
        this.toId = toId;
        this.condTempl = condTempl;
    }

    public Edge getEdge(HashMap nodeLookup) {
        Node from = (Node) nodeLookup.get(fromId);
        Node to = (Node) nodeLookup.get(toId);
        return new Edge(from, to, condTempl.getCondition());
    }


    public void setCondTempl(ConditionTemplate condTempl) {
        this.condTempl = condTempl;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }
    
    public ArrayList getAllConditionTemplates(){
    	ArrayList condTemplates = new ArrayList();
    	return condTempl.addAllConditionTemplates(condTemplates);
    }


}
