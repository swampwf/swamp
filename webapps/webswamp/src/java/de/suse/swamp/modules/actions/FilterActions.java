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
import java.util.regex.*;

import org.apache.turbine.om.security.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * Filter related functions (called from webapp)
 *
 * @author tschmidt
 * @version $Id$
 */
public class FilterActions extends SecureAction {

    /**
     *  IMPORTANT: doXxx methods may only have ONE upper case letter X
     *  after the do, anything else must be lower case.
     */

    /**
     * Gets the Databits of a Wf Template
     */
    public static void doGetdatabits(RunData data, Context context) {
        WorkflowManager wfman = WorkflowManager.getInstance();

        String template = data.getParameters().get("filter_template");
        String version = data.getParameters().get("filter_version");
        
        WorkflowTemplate wftemp = null;
        wftemp = wfman.getWorkflowTemplate(template);

        ArrayList databitnames = wftemp.getAllDatabitPaths();
        if (databitnames.size() > 0) {
            context.put("databitnames", databitnames);
        } else {
            Logger.ERROR("Couldn't fetch Databitnames for WfTemplate: "
                    + wftemp.getName());
        }
        context.put("filter_template", data.getParameters().get(
                "filter_template"));
    }



    /**
     * Add a Filter for the currently logged in User
     */
    public void doAddfilter(RunData data, Context context) {
        User user = data.getUser();
        String error = new String();
        DatabaseFilter filter = null;
        String filtertype = data.getParameters().get("filtertype");
        Logger.DEBUG("Creating new Filter of type: " + filtertype);

        // evaluate type of filter: 
        if (filtertype.equals("contentfilter")) {
            try {
                Pattern.compile(data.getParameters().get("filter_regexp"));
            } catch (Exception e) {
                error += "Error in Regular Expression. \n" + e.getMessage();
            }
            filter = new ContentFilter();

        } else if (filtertype.equals("propertyfilter")) {
            int filterstate;
            if (data.getParameters().getBoolean("filter_not")){
                filterstate = -1 * data.getParameters().getInt("filter_state");
            } else {
                filterstate = data.getParameters().getInt("filter_state");
            }
            filter = new PropertyFilter();
            
        } else if (filtertype.equals("taskfilter")) {
            try {
                Pattern.compile(data.getParameters().get("userregex"));
            } catch (Exception e) {
                error += "Error in Regular Expression. \n" + e.getMessage();
            }
            filter = new TaskFilter();
            
        } else {
            error += "Could not set FilterType.";
        }

        ArrayList filters = null;

        if (error.length() == 0) {
            // existing filters: 
            if (data.getUser().getTemp("filters") == null) {
                filters = new ArrayList();
            } else {
                filters = (ArrayList) data.getUser().getTemp("filters");
            }
            try {
                filters.add(filter);
            } catch (Exception e) {
                Logger.ERROR("Couldn't add Filter");
                error += "Couldn\'t add Filter";
            }

            data.getUser().setTemp("filters", filters);
        }

        // Confirm new Filter visually
        if (data.getUser().getTemp("filters") == filters && error.length() == 0) {
            context.put("statusmessage", "Your Filter has been added");
            context.put("statusheader", "Success");
            context.put("statusclass", "success");
            context.put("icon", "ok");
        } else {
            context.put("statusmessage", "Error generating new Filter.\n Reason: " + error);
            context.put("statusheader", "Error");
            context.put("statusclass", "error");
            context.put("icon", "error");
        }
    }

    /**
     * Update a filter identified by its id
     */
    public void doUpdatefilter(RunData data, Context context) {

        ArrayList filters = (ArrayList) data.getUser().getTemp("filters");
        for (int i = 1; i <= data.getParameters().getInt("counter"); i++) {
            int filterid = data.getParameters().getInt("filterid" + i);
            boolean active = data.getParameters().getBoolean("activated" + i);
            for (Iterator it = filters.iterator(); it.hasNext();) {
                DatabaseFilter filter = (DatabaseFilter) it.next();
                if (filter.getId() == filterid) 
                    filter.setActive(active);
            } // Iterator End
        } // end for
    }

    /**
     * Delete a filter identified by its id. 
     */
    public void doDeletefilter(RunData data, Context context) {

        String errormsg = null;
        int removedcount = 0;
        for (int i = 1; i <= data.getParameters().getInt("counter"); i++) {

            boolean removed = false;
            int filterid = data.getParameters().getInt("filterid" + i);

            if (filterid != 0 && data.getParameters().getBoolean("delete" + i)) {

                ArrayList filters = (ArrayList) data.getUser().getTemp("filters");
                for (Iterator it = filters.iterator(); it.hasNext();) {
                    DatabaseFilter filter = (DatabaseFilter) it.next();

                    if (filter.getId() == filterid) {
                        if (filters.remove(filter)) {
                            Logger.DEBUG("Filter #" + filterid + " removed");
                            removed = true;
                            removedcount++;
                            break;
                        } else {
                            errormsg = "Filter with id " + filterid + " could not be removed.";
                        }
                    }
                } // Iterator Ende
                if (!removed) errormsg = "Filter with id " + filterid + " not found in FilterList.";
            }
        }

        // Confirm delete Filter visually
        if (errormsg == null) {
            context.put("statusmessage", removedcount + " Filters have been removed");
            context.put("statusheader", "Success");
            context.put("statusclass", "success");
            context.put("icon", "ok");
        } else {
            context.put("statusmessage", errormsg);
            context.put("statusheader", "Your Filter was not removed.");
            context.put("statusclass", "error");
            context.put("icon", "error");
        }

    }

}