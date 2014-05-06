/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt (tschmidt@suse.de)
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

package de.suse.swamp.core.actions;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class StartSubworkflowActionTemplate extends SystemActionTemplate {

    // template-name of the subworkflow to create
    private String subname;
    private String subversion;
	
    public StartSubworkflowActionTemplate(String name, String subname, 
			String subversion, NodeTemplate nodeTemplate) {
        super (name, nodeTemplate);
        this.subname = subname;
        this.subversion = subversion;
    }

	
    public void act(Result result) {
        act(result, new ResultList());
    }    
    
    /**
     * Is called when a node with a StartSubworkflowAction is entered.
     * It will - start the Subworkflow in state prepared, 
     *         - copy datapackvalues with the same field-identifier to 
     *           the subworkflows datapack 
     *         - Attach the Workflows Datasets to the Subworkflow
     *         - start the subworkflow
     */
    public void act(Result result, ResultList history) {
    	WorkflowManager wfman = WorkflowManager.getInstance();
    	int parentId = result.getWorkflowId();
    	try {
			wfman.createWorkflow(subname, result.getUname(), parentId, null, getVersion(), 
					true, history);
		} catch (Exception e) {
            Logger.ERROR("Unable to start Subworkflow. Reason: " + e.getMessage());
            history.addResult(ResultList.ERROR, "Starting Subworkflow failed! Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    /**
     * @return an ArrayList with error strings, empty if everything went fine
     */
    public ArrayList validate(Result result) {
        // TODO: SystemTasks aren't validated 
        return new ArrayList();
    }

    public String toXML() {
        return "<StartSubWorkflowAction name=\"" + name + "\" subname=\"" + 
        subname + "\" />";
    }

    
    // does not send events right now
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        events.add(new Event("NONE", result.getWorkflowId(), result.getWorkflowId()));
        return events;
    }

    
    public String getType() {
        return "StartSubWorkflowAction";
    }

    
    /**
     * @return Returns the subname.
     */
    public String getSubname() {
        return this.subname;
    }

    public String getVersion() {
        return this.subversion;
    }

}