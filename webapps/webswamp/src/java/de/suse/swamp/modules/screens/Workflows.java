/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Doris Baum <dbaum@suse.de>
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

package de.suse.swamp.modules.screens;

import java.util.*;

import org.apache.turbine.services.pull.*;
import org.apache.turbine.util.*;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.turbine.services.security.*;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;


/**
 * Gets workflows from workflow manager and prepares the list for display
 *
 * @author Doris Baum &lt;dbaum@suse.de&gt;
 * @author Thomas Schmidt
 */

public class Workflows extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        
        long time = System.currentTimeMillis();
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        SWAMPTurbineUser user = (SWAMPTurbineUser) data.getUser();
        ParameterParser pp = data.getParameters();

        // get an Array of Workflow Templates (for filters)   
        ArrayList templateIdList = wfapi.getAllWorkflowTemplateNames(uname);
        context.put("wftmpls", templateIdList);

        MapToFilterlist urlToFilter = new MapToFilterlist();
        ArrayList filters = urlToFilter.getFilters(
                Workflows.parameterParserToHashMap(pp));
        StringBuffer filterText = urlToFilter.getFilterText();
        context.put("filters", filters);
        context.put("filtercount", String.valueOf(filters.size()));
        context.put("systemFilterText", filterText);   
        
        // do we have a multiple workflow type view?
        boolean multipleview = false;
        if (urlToFilter.getTemplateName() == null){
            multipleview = true;
        } else {
            WorkflowTemplate wfTemp = wfapi.getWorkflowTemplate(urlToFilter.getTemplateName(), uname);
        	// automatically add filter for workflow user if needed
            if (wfTemp.getWorkflowRole(WorkflowRole.USER).isRestricted()) {
                RoleFilter filter = new RoleFilter(uname, WorkflowRole.USER, urlToFilter.getTemplateName());
                filters.add(filter);
            }
            context.put("mastertemplatename", urlToFilter.getTemplateName());
            SwampUIManager uitool = (SwampUIManager) TurbinePull.getTool(context, "ui");
            SwampUIManager.setInterface(data.getUser(), wfTemp.getMasterParentTemplate().getName());
            uitool.init(data.getUser());
        }
        context.put("multipleview", new Boolean(multipleview));
        
        // configure task display: 
        if (urlToFilter.getTaskOwner() != null){
            context.put("taskOwner", urlToFilter.getTaskOwner());
        }
        
        // restrict the displayed tasks: 
        if (pp.containsKey("systemfilter_displayedtasks")){
            SWAMPHashSet displayedTasks = new SWAMPHashSet();
            displayedTasks.add(pp.get("systemfilter_displayedtasks"), ",");
            context.put("displaytasks", displayedTasks);
        }
        
        
        // FIXME:
        // more than one workflowtype on one page not possible, because of
		// sorting criteria
        // sorting: get the order criteria from users permstorage -> wf-defaults -> default
        // need a template-type to load the default sorting criteria! 
        // else automatically sorted by wf-id
        DatabaseFilter order = this.getOrderFilter(pp.
                getString("systemfilter_wftemplate"), data, context);
        
        
        List workflowIDList = wfapi.getWorkflowIds(filters, order, uname);
        int numberOfWorkflows = workflowIDList.size();
        
        // setting displaysubwfs user option
        HashMap displaysubwfsSet = new HashMap();
        // name: templatename, value: LinkedList with workflows
        HashMap wflists = new HashMap();
        // list of templates where the subworkflow-display config dropdown is shown
        List displaySubworkflowsDropdown = new ArrayList();
        for (Iterator it = templateIdList.iterator(); it.hasNext(); ){
            String templateName = (String) it.next();
            String displaysubwfs = "systemfilter_displaysubwfs_" + templateName;
            String value; 
            // manual user-change:
            if (pp.containsKey("subwfsuserchange_" + templateName)){
                value = pp.get(displaysubwfs);
                user.setPerm(displaysubwfs, pp.get(displaysubwfs));
            // systemfilter parameter setting
            } else if (pp.containsKey(displaysubwfs)){
                value = pp.get(displaysubwfs);
            // fetch from usersession (only for masterworkflow or for all in multi-view)
            } else if (templateName.equals(urlToFilter.getTemplateName()) 
                    && user.getPerm(displaysubwfs) != null) {
                value = (String) user.getPerm(displaysubwfs);
            // default to none in multipleview
            } else if (multipleview) {
                value = "none";
            } else {
                value = "all";
            }
            // Logger.DEBUG("Set " + displaysubwfs + " - " + value);
            displaysubwfsSet.put(displaysubwfs, value);
            wflists.put(templateName, new LinkedList());
            // check if we need to show the subworkflow dropdown
            if (WorkflowManager.getInstance().getWorkflowTemplate(templateName).getSubworkflowTemplates().isEmpty()) {
                displaySubworkflowsDropdown.add(templateName);
            }
        }
        context.put("displaysubwfsSet", displaysubwfsSet);
        
        int wfsPerPage = 30, wfOffset = 0;
        HashMap wfsPerPageMap = new HashMap();
        for (Iterator it = templateIdList.iterator(); it.hasNext(); ){
            String template = (String) it.next();
            String wfsPerPageParam = "systemfilter_wfsPerPage_" + template;
            String wfsPerPageUser = "wfsPerPage_" + template;
            if (pp.containsKey(wfsPerPageUser) && 
                    pp.getInt(wfsPerPageUser) > 0 ){
                wfsPerPage = pp.getInt(wfsPerPageUser);
                user.setPerm(wfsPerPageParam, String.valueOf(wfsPerPage));
            } else if (pp.containsKey(wfsPerPageParam) && 
                    pp.getInt(wfsPerPageParam) > 0 ){
                wfsPerPage = pp.getInt(wfsPerPageParam);
            } else if (data.getUser().getPerm(wfsPerPageParam) != null) {
                try {
                    wfsPerPage = new Integer((String) user.getPerm(wfsPerPageParam)).intValue();
                } catch (NumberFormatException e) {
                    Logger.ERROR("Invalid wfsPerPage value:" + user.getPerm(wfsPerPageParam) + " in " 
                            + user.getName() + "s permdata!");
                    wfsPerPage = 30;
                }
            }
            wfsPerPageMap.put(wfsPerPageParam, new Integer(wfsPerPage));
        }
        context.put("wfsPerPageMap", wfsPerPageMap);
        if (urlToFilter.getTemplateName() != null){
            wfsPerPage = ((Integer) wfsPerPageMap.get("systemfilter_wfsPerPage_" + 
                    urlToFilter.getTemplateName())).intValue();
        }
        
        
        if (!multipleview){ 
            if (pp.containsKey("wfOffset") && 
                    pp.getInt("wfOffset") > 0 && 
                    pp.getInt("wfOffset") < workflowIDList.size()){
                wfOffset = pp.getInt("wfOffset");
            } else {
                wfOffset = 0;
            }
            context.put("wfOffset", new Integer(wfOffset));        
            ArrayList pages = new ArrayList();
            for (int i=1, total=numberOfWorkflows/wfsPerPage; i<=total; i++)
                pages.add(new Integer(i));
            
            if ((numberOfWorkflows % wfsPerPage) > 0) 
                pages.add(new Integer(numberOfWorkflows/wfsPerPage + 1));
            
            
            context.put("currentPage", new Integer(wfOffset / wfsPerPage +1));
            context.put("pages", pages);
            
            
            if ((wfOffset + wfsPerPage) > workflowIDList.size())
                workflowIDList = workflowIDList.subList(wfOffset, workflowIDList.size());
            else 
                workflowIDList = workflowIDList.subList(wfOffset, wfOffset + wfsPerPage);
        }        

        // iterate over all found workflows, sort them into a list of their template, check permissions.
        int wfcount = 0;
        
        for (Iterator it = workflowIDList.iterator(); it.hasNext();) {
            int wfid = ((Integer) it.next()).intValue();
            // only add those that he is allowed to see
            try {
                Workflow wf = wfapi.getWorkflow(wfid, uname);
                wfcount++;
                ((LinkedList) wflists.get(wf.getTemplateName())).add(wf);
            } catch (SecurityException e) {
                Logger.DEBUG("Removed wf-" + wfid + " (no permission for " + uname + ")");
            }
        }
        context.put("displayedWorkflows", new Integer(wfcount));
        context.put("numberOfWorkflows", new Integer(numberOfWorkflows));
        context.put("displaySubworkflowsDropdown", displaySubworkflowsDropdown);
        
        // Check each List for its Order Criteria and displayed Columns
        // we also need to add all possible subtypes
        if (urlToFilter.getTemplateName() != null) {
            List templates = wfapi.getSubworkflowTemplateNames(urlToFilter.getTemplateName(), uname);
            templates.add(urlToFilter.getTemplateName());
            populateColumns(context, data, templates);
        } else {
            populateColumns(context, data, wfapi.getWorkflowTemplateNames(uname));
        }
        
        // Set var for changing displayed columns
        if (pp.containsKey("editcolumns")) {
            String editcolumntemplate = pp.get("editcolumns");
            context.put("editcolumns", editcolumntemplate);
            
            // list of possible databit columns:
            WorkflowTemplate wfTemp = wfapi.getWorkflowTemplate(editcolumntemplate, uname);
            List pathList = wfTemp.getAllDatabitPaths();
            if (wfTemp.getParentTemplate() != null)
                pathList.addAll(wfTemp.getParentTemplate().getAllDatabitPaths());
            pathList.addAll(DataToWfPropertyMapper.getAllPropertyPaths(wfTemp));
            
            context.put("columndatabitnames", pathList);
        }
        
        context.put("wfs", wflists);
        context.put("wfman", WorkflowManager.getInstance());
        Logger.DEBUG("Preparing Workflow-View took " + 
                (System.currentTimeMillis() - time) + " ms.");
    }




    /**This Method sets the Columns that should be displayed per WorkflowGroup.
     * @param context
     * @param data
     * @param wfs Array that contains ArrayLists of Workflows from the same Template
     * @return gives back the Array
     * @templateNames - only generate the columns for these templates
     */
    public void populateColumns(Context context, RunData data, List templateNames) throws Exception {
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        HashMap displayedColumns = new HashMap();
        SWAMPTurbineUser SWAMPUser = (SWAMPTurbineUser) data.getUser();
        ParameterParser pp = data.getParameters();
        HashMap wfIcons = new HashMap();
        
        for (Iterator tempit = templateNames.iterator(); tempit.hasNext();) {
            String template = (String) tempit.next();
            displayedColumns.put(template, new ArrayList());

            WorkflowTemplate wfTemplate;
            // We may have a page that theoretically displays subworkflows, to which this user has no permission. 
            // They won't get shown anyway, so avoid the errormessage and show what he is allowed to see
            try {
                wfTemplate = wfapi.getWorkflowTemplate(template, uname);
            } catch (Exception e) {
                Logger.WARN("Workflows.vm: Subworkflowpermission missing: " + e.getMessage());
                continue;
            }

            if (wfTemplate.containsConfigItem("icon")) {
                wfIcons.put(template, wfTemplate.getConfigItem("icon"));
            }

            context.put("defaultview", "workflow");
            // load user-specific columns for the actual wf-template
            ArrayList columns = SWAMPUser.getPermArray("wfcolumns_" + template);
            ArrayList templateColumns = (ArrayList) displayedColumns.get(template);
            String columnsParam = "systemfilter_columns_" + template;

            // checking for parameter config
            if (pp.containsKey(columnsParam) && !pp.get(columnsParam).equals("")) {
                templateColumns.clear();
                StringTokenizer st = new StringTokenizer(pp.get(columnsParam), ",");
                while (st.hasMoreTokens()) {
                    templateColumns.add(st.nextToken().trim());
                }
                // disable "edit columns" button for fixed views
                context.put("noeditcolumns", "true");

                // Check if there are User-defined Columns:
            } else if (columns != null && columns.size() > 0) {
                templateColumns.clear();
                for (Iterator it = columns.iterator(); it.hasNext();) {
                    templateColumns.add(((String) it.next()).trim());
                }

                // checking for Workflow config
            } else if (wfTemplate.containsConfigItem("displayedcolumns_workflowview")
                    && wfTemplate.getConfigItemAsList("displayedcolumns_workflowview").size() > 0) {
                templateColumns.clear();
                templateColumns.addAll(wfTemplate.getConfigItemAsList("displayedcolumns_workflowview"));

                // falling back to pre-defined Workflow-Columns
            } else {
                templateColumns.clear();
                templateColumns.add("column_workflowid");
                templateColumns.add("column_workflowdescription");
                templateColumns.add("column_nexttasks");
                templateColumns.add("column_state");
            }
        }
        context.put("displayedcolumns", displayedColumns);
        context.put("wficons", wfIcons);
    }
    
    
    
    /**
     * set the Filter for ordering the WorflowList
     */
    private DatabaseFilter getOrderFilter(String template, 
            RunData data, Context context) throws Exception {

        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        SWAMPTurbineUser user = (SWAMPTurbineUser) data.getUser();
        // we need a defined templatefilter to do sorting in the DB.
        if (template == null || !wfapi.getAllWorkflowTemplateNames(uname).contains(template)) {
			Logger.DEBUG("No WfTemplate-name present, will enable default ordering.");
            // will enable default ordering
            return null;
        } else {

            WorkflowTemplate wfTemplate = wfapi.getWorkflowTemplate(template, uname);
            String orderBy;

            String userOrder = (String) user.getPerm("wforder_" + template);
            String configOrder = wfTemplate.containsConfigItem("sortby_workflowview") ? 
                    wfTemplate.getConfigItem("sortby_workflowview") : null;
            String defaultOrder = "column_workflowid";

            String direction = ((String) user.getPerm("wfdirection_" + template));
            if (direction == null || direction.equals("")) {
                if (wfTemplate.containsConfigItem("direction_workflowview"))
                    direction = wfTemplate.getConfigItem("direction_workflowview");
            }
            if (direction == null || direction.equals("")) {
                direction = "descending";
            }

            if (userOrder != null && !userOrder.equals("")) {
                orderBy = userOrder;
            } else if (configOrder != null && !configOrder.equals("")) {
                orderBy = configOrder;
            } else {
                orderBy = defaultOrder;
            }
            Logger.DEBUG("Ordering Workflowlist " + template + " by " + orderBy + 
                    " " + direction);

            // put in context for display
            context.put("orderBy", orderBy);
            context.put("direction", direction);

            if (orderBy.equals("column_workflowid")) {
                PropertyFilter order = new PropertyFilter();
                order.setWfId(1);
                // set correct direction
                if (direction.toLowerCase().equals("descending")) {
                    order.setDescending();
                } else {
                    order.setAscending();
                }
                return order;

            } else if (orderBy.equals("column_workflowdescription")) {
                // not implemented, difficult because of script replacement inside
                return null;

            } else if (orderBy.equals("column_nexttasks") || orderBy.equals("column_progress")) {
                // not implemented, difficult because of script replacement inside
                return null;

            } else {

                // checking for System databit
                if (orderBy.startsWith("System.")){ 
                    if (orderBy.endsWith("dueDate")) {
                

                    ContentFilter order = new ContentFilter();
                    order.setDatabitPath(wfTemplate.getNodeTemplate(orderBy.substring(7, orderBy.length() - 8))
                            .getDueDateReference());
                    // set correct direction
                    if (direction.toLowerCase().equals("descending")) {
                        order.setDescending();
                    } else {
                        order.setAscending();
                    }
                    return order;
                    
                    } else {
                        return null;
                    }

                // assuming Databit column
                } else if (wfTemplate.containsDatabitTemplate(orderBy)) {
                    ContentFilter order = new ContentFilter();
                    order.setDatabitPath(orderBy);
                    // Databits of type number must not be ordered alphabetically!
                    DatabitTemplate dbit = wfTemplate.getDatabitTemplate(orderBy);
                    if (dbit != null && dbit.getType().equals("number") || dbit.getType().equals("bugzilla")) {
                        order.setTreatNumeric(true);
                    }
                    
                    // set correct direction
                    if (direction.toLowerCase().equals("descending")) {
                        order.setDescending();
                    } else {
                        order.setAscending();
                    }
                    return order;
                } else { 
                    return null;
                }
            }
        } // end template != null
    }

    
    
    public static HashMap parameterParserToHashMap(ParameterParser pp){
        HashMap map = new HashMap();
        for (int i=0; i<pp.getKeys().length; i++){
            String key = (String) pp.getKeys()[i]; 
            map.put(key, pp.get(key));
        }
        return map;
    }
    
    
}
