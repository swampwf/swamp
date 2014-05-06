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
 * Template for the representation of a node in a workflow
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

import java.util.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.notification.*;

public class NodeTemplate implements ExtDescribable {

    private String name;
    private String description;
    private String longDescription = "";
    private String helpContext = "";
    // path to the duedate databit
    private String dueDateReference;
    private MileStoneTemplate mileStoneTemplate;
    private ArrayList actionTempls = new ArrayList();
    private ArrayList edgeTempls = new ArrayList();
    private String type;
    
    
    public NodeTemplate(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * @return - the real Node Object
     */
    public Node getNode() {
        Node node = new Node(this);
        if (mileStoneTemplate != null) {
            node.setMileStone(this.mileStoneTemplate.getMileStone(node));
        }
        return node;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public ArrayList createEdges(HashMap nodeLookup) {
        ArrayList newEdges = new ArrayList();
        for (Iterator iter = edgeTempls.iterator(); iter.hasNext(); ) {
            EdgeTemplate edgeTempl = (EdgeTemplate) iter.next();
            newEdges.add(edgeTempl.getEdge(nodeLookup));
        }
        return newEdges;
    }

    public void addEdgeTempl(EdgeTemplate edgeTempl) {
        edgeTempls.add(edgeTempl);
    }

    public ArrayList getEdgeTempls() {
        return edgeTempls;
    }

    public void addActionTempl(ActionTemplate actionTempl) {
        actionTempls.add(actionTempl);
    }

    /**
     * @return Returns the helpText.
     */
    public String getHelpContext() {
        return helpContext;
    }

    /**
     * @param helpText The helpText to set.
     */
    public void setHelpContext(String helpText) {
        this.helpContext = helpText;
    }

    /**
     * @return Returns the shortDescription.
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * @param shortDescription The shortDescription to set.
     */
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

	/**
	 * @return Returns the reference to the data bit specifying the dueDate.
	 */
	public String getDueDateReference() {
		return dueDateReference;
	}
	
    public boolean hasDueDate() {
        if (getDueDateReference() != null)
            return true;
        else
            return false;
    }
	
	/**
	 * @param dueDateReference The databit specifying the due Date.
	 */
	public void setDueDateReference(String dueDateReference) {
		this.dueDateReference = dueDateReference;
	}

	/**
     * @param mileStoneTemplate The mileStoneTemplate to set.
     */
    public void setMileStoneTemplate(MileStoneTemplate mileStoneTemplate) {
        this.mileStoneTemplate = mileStoneTemplate;
    }
	/**
	 * @return Returns the mileStoneTemplate.
	 */
	public MileStoneTemplate getMileStoneTemplate() {
		return mileStoneTemplate;
	}

	public ActionTemplate getActionTemplate(String name){
        ActionTemplate actionTemp = null;
        for (Iterator it = actionTempls.iterator(); it.hasNext(); ){
            ActionTemplate temp = (ActionTemplate) it.next();
            if (temp.getName() != null && temp.getName().equals(name)){
                actionTemp = temp;
                break;
            }
        }
        return actionTemp;
    }

	public ArrayList getActionTemplates(){
		return actionTempls;
	}
	
    public String getReplacedDescription(int workflowId) {
        Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
        return NotificationTools.workflowDataReplace(this.description, wf);
    }

    /**
     * @return - the LongDescription with replaced Datapck content
     */
    public String getReplacedLongDescription(int workflowId) {
        Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
        return NotificationTools.workflowDataReplace(longDescription, wf);
    }
    
    
    
    public boolean isStartNode(){
        return (getType().equalsIgnoreCase("start"));      
    }
    
    
    public boolean isEndNode(){
        return (getType().equalsIgnoreCase("end"));
    }
    
}