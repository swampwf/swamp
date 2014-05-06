/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de>
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

package de.suse.swamp.modules.actions;

import java.util.*;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.modules.screens.*;
import de.suse.swamp.turbine.services.security.*;

/**
 * @author tschmidt
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ViewActions extends SecureAction {



    /**
     * Add a Column for the currently logged in User
     */
    public void doAddcolumn(RunData data, Context context) throws Exception {

        SWAMPTurbineUser user = (SWAMPTurbineUser) data.getUser();
        String template = data.getParameters().get("wftemplate");
        String defaultview = data.getParameters().get("defaultview");
        String column = data.getParameters().get("column");

        if (!column.equals("")
                && (defaultview.equalsIgnoreCase("task") || defaultview
                        .equalsIgnoreCase("workflow"))) {
            if (defaultview.equalsIgnoreCase("workflow")) {
                Workflows workflowsScreen = new Workflows();
                List templateNames = new ArrayList();
                templateNames.add(template);
                workflowsScreen.populateColumns(context, data, templateNames);
                HashMap displayedcolumns = (HashMap) context.get("displayedcolumns");
                ArrayList columns = (ArrayList) displayedcolumns.get(template);
                // remove for not adding double-columns
                columns.remove(column);
                columns.add(column);
                user.setPermArray("wfcolumns_" + template, columns);
            }

            context.put("statusmessage", "Column " + column + " was added.");
            context.put("statusheader", "Success");
            context.put("statusclass", "success");
            context.put("icon", "ok");
        } else {
            context.put("statusmessage", "Error adding Column " + column);
            context.put("statusheader", "Error");
            context.put("statusclass", "error");
            context.put("icon", "error");
        }
    }



    /**
     * Remove a column from the displayed List 
     */
    public void doDeletecolumn(RunData data, Context context) throws Exception {
    	SWAMPTurbineUser user = (SWAMPTurbineUser) data.getUser();
        String template = data.getParameters().get("wftemplate");
        String defaultview = data.getParameters().get("defaultview");
        String column = data.getParameters().get("column");
        String error = new String();
        String success = new String();
        if (defaultview.equalsIgnoreCase("workflow")) {
            Workflows workflowsScreen = new Workflows();
            List templateNames = new ArrayList();
            templateNames.add(template);
            workflowsScreen.populateColumns(context, data, templateNames);
            HashMap displayedcolumns = (HashMap) context.get("displayedcolumns");
            ArrayList columns = (ArrayList) displayedcolumns.get(template);
            if (columns.remove(column)) {
                success = "Workflow-Column " + column + " was removed.";
                user.setPermArray("wfcolumns_" + template, columns);
            } else {
                error = "Workflow-Column " + column + " couldn't be removed";
            }

        } else {
            error = "No view defined (task | workflow)";
        }

        if (error.length() > 1) {
            context.put("statusmessage", error);
            context.put("statusheader", "Error");
            context.put("statusclass", "error");
            context.put("icon", "error");
        } else {
            context.put("statusmessage", success);
            context.put("statusheader", "Success");
            context.put("statusclass", "success");
            context.put("icon", "ok");
        }
    }
    
    
    /**
     * Resets the columns of a special Template in Workflow/Task-View to 
     * the defaults.
     */
    public void doResetcolumns(RunData data, Context context) throws Exception {
    	SWAMPTurbineUser user = (SWAMPTurbineUser) data.getUser();
        String template = data.getParameters().get("wftemplate");
        new WorkflowAPI().getWorkflowTemplate(template, user.getName());
        ArrayList columns = user.getPermArray("wfcolumns_" + template);
        columns.clear();
        user.setPerm("wfcolumns_" + template, null);
        user.setPerm("wforder_" + template, null);
    }
    
    
    /**Setting the order-criteria for a specific Wf-template in task- or 
     * workflow view
     * @param data
     * @param context
     */
    public void doSetorder(RunData data, Context context) throws Exception {
        SWAMPTurbineUser user = (SWAMPTurbineUser) data.getUser();
        org.apache.turbine.util.parser.ParameterParser pp = data.getParameters();
        Object[] keys = pp.getKeys();
        for (int i=0; i<keys.length; i++){
            String param = (String) keys[i];
            if (param.startsWith("wforder_") || param.startsWith("wfdirection_")){
                user.setPerm(param, data.getParameters().get(param));
            }
        }
    }
}