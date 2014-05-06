/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.util;

import java.util.*;

import de.suse.swamp.core.filter.*;
import de.suse.swamp.util.*;


/**
 * generates a list of filter objects out of a Hashmap
 *
 * @author Thomas Schmidt
 */

public class MapToFilterlist {

    private StringBuffer filterText = new StringBuffer();
    
    // remind some filters, so that workflows.vm display can be configured
    private String templateName = null;
    private String taskOwner = null;
    private boolean optionalTasks = false;
    
    
    // use one common filter for node activities
    PropertyFilter nodeFilter;   
    
    public MapToFilterlist(){ }
    
    /**
     * @param data - Hashmap with key: filteridentifier, key: value of filter
     * X means that more than one filter of that type are possible, each marked 
     * with another integer.
     * Examples: 
     * key: systemfilter_wftemplate - value: WorkflowTemplate name (comma seperated list)
     * key: systemfilter_parentid - value: id of parent workflow 
     * key: systemfilter_wfclosed - value: true | false
     * key: systemfilterX_nodeActive - value: name of the node
     * key: systemfilterX_nodeNotActive - value: name of not active node
     * 
     * key: systemfilterX_databit - value: databitpath
     * key: systemfilterX_bitvalue - value: value to match with
     * key: systemfilterX_bitregex - value: regexp to match with
     * key: systemfilterX_bitgreater - value: will check with value < databitvalue
     * key: systemfilterX_bitsmaller - value: will check with value > databitvalue
     * 
     * key: systemfilterX_role - value: rolename
     * key: systemfilterX_rolevalue - value: value to match with
     * 
     * key: systemfilter_hasactivetasks - value: true  
     * key: systemfilter_taskowner - value: username
     * key: systemfilterX_tasks - value: comma seperated tasknames (workflow must have a task of the list active)
     * key: systemfilter_mandatorytasksonly - value: true/false
     * 
     * @return - ArrayList of Filter Objects
     */
    public ArrayList getFilters(Map data) throws IllegalArgumentException {
        
        ArrayList filters = new ArrayList();
        
        // Systemfilter filters for a Wf-Template
        if (data.containsKey("systemfilter_wftemplate")) {
            PropertyFilter filter = new PropertyFilter();
            String templateString = (String) data.get("systemfilter_wftemplate");
            List templates = new SWAMPHashSet(templateString, ",").toList();
            filter.setWfTemplates(templates);
            filters.add(filter);
            filterText.append("Template = " + data.get("systemfilter_wftemplate") + " && ");
            if (templates.size() == 1){
                templateName = (String) templates.get(0);
            }
        } else {
            Logger.WARN("Please always provide a Filter for a Workflow template type");
            //throw new IllegalArgumentException("Cannot create filters without a systemfilter_wftemplate");
        }
        
        // Systemfilter filters for parent-id
        if (data.containsKey("systemfilter_parentid")) {
            try {
                int parentid = Integer.parseInt((String) data.get("systemfilter_parentid").toString());
                PropertyFilter filter = new PropertyFilter();
                filter.setParentWfId(parentid);
                filters.add(filter);
                filterText.append("Parent-ID = " + parentid + " && ");
            } catch (NumberFormatException e) { 
                throw new IllegalArgumentException(data.get("systemfilter_parentid") + " is not a valid " + 
                        "parent-id to filter for.");
            }
        }
        
        
        if (data.containsKey("systemfilter_wfclosed") && !data.get("systemfilter_wfclosed").equals("")) {
            PropertyFilter filter = new PropertyFilter();
            String wfclosed = data.get("systemfilter_wfclosed").toString();
            boolean closed = Boolean.valueOf(wfclosed).booleanValue();
            filter.setClosed(closed);
            filter.addWfTemplate(templateName);
            filters.add(filter);
            if (closed){
                filterText.append("workflow closed && ");
            } else {
                filterText.append("workflow running && ");
            }
        }     
        
        for (int y = 0; y<=10; y++){
            
            String i = "";
            // also get the filter without id:
            if (y>0) { i = String.valueOf(y); }
            
            String filterIdent = "systemfilter" + i + "_nodeActive";
            if (data.containsKey(filterIdent) && !data.get(filterIdent).equals("")) {
                getNodeFilter(templateName).addNodeActive((String) data.get(filterIdent));
                filterText.append("active Node: " + data.get(filterIdent) + " && ");
            }
    
            filterIdent = "systemfilter" + i + "_nodeNotActive";
            if (data.containsKey(filterIdent) && !data.get(filterIdent).equals("")) {
                getNodeFilter(templateName).addNodeNotActive((String) data.get(filterIdent));
                filterText.append("inactive Node: " + data.get(filterIdent) + " && ");
            }
            
            // Filter for a certain Databit
            if (data.containsKey("systemfilter" + i + "_databit") && 
                    !data.get("systemfilter" + i + "_databit").equals("")) {
                String bit = (String) data.get("systemfilter" + i + "_databit");
                if (data.containsKey("systemfilter" + i + "_bitvalue")) {
                    String bitcontent = (String) data.get("systemfilter" + i + "_bitvalue").toString();
                    ContentFilter filter = new ContentFilter();
                    filter.setDatabitPath(bit);
                    filter.setDatabitValue(bitcontent);
                    filters.add(filter);
                    filterText.append("Databit <i>" + bit + "</i> = " + bitcontent + " && ");
                } else if (data.containsKey("systemfilter" + i + "_bitregex")) {
                    String bitcontent = (String) data.get("systemfilter" + i + "_bitregex").toString(); 
                    ContentFilter filter = new ContentFilter();
                    filter.setDatabitPath(bit);
                    filter.setDatabitValueRegex(bitcontent);
                    filters.add(filter);
                    filterText.append("Databit <i>" + bit + "</i> ~= " + bitcontent + " && ");
                } else if (data.containsKey("systemfilter" + i + "_bitsmaller")) {
                    String bitcontent = (String) data.get("systemfilter" + i + "_bitsmaller").toString(); 
                    ContentFilter filter = new ContentFilter();
                    filter.setDatabitPath(bit);
                    filter.setDatabitValueSmaller(bitcontent);
                    filters.add(filter);
                    filterText.append("Databit <i>" + bit + "</i> < " + bitcontent + " && ");
                } else if (data.containsKey("systemfilter" + i + "_bitgreater")) {
                    String bitcontent = (String) data.get("systemfilter" + i + "_bitgreater").toString(); 
                    ContentFilter filter = new ContentFilter();
                    filter.setDatabitPath(bit);
                    filter.setDatabitValueGreater(bitcontent);
                    filters.add(filter);
                    filterText.append("Databit <i>" + bit + "</i> > " + bitcontent + " && ");
                } else {
                    Logger.ERROR("Must specify (systemfilter_bitvalue || systemfilter_bitregex ||" + 
                            " systemfilter_bitsmaller || systemfilter_bitgreater) for databit filter!");
                }
            }
            
            // Filter for a role containing a String (username)
            if (data.containsKey("systemfilter" + i + "_role") && !data.get("systemfilter" + i + "_role").equals("")) {
                String role = (String) data.get("systemfilter" + i + "_role");
                if (data.containsKey("systemfilter" + i + "_rolevalue") && 
                        data.containsKey("systemfilter_wftemplate")) {
                    String rolecontent = (String) data.get("systemfilter" + i + "_rolevalue");
                    RoleFilter filter = new RoleFilter(rolecontent, role, (String) data.get("systemfilter_wftemplate"));
                    filters.add(filter);
                    filterText.append("Role <i>" + role + "</i> contains " + rolecontent + " && ");
                } else {
                    Logger.ERROR("Must specify systemfilter_role and systemfilter_rolevalue and systemfilter_wftemplate for role filter!");
                }
            }
            
            // Filter for a special active task
            filterIdent = "systemfilter" + i + "_tasks";
            if (data.containsKey(filterIdent) && !data.get(filterIdent).equals("")) {
                String taskNames = (String) data.get(filterIdent);
                TaskFilter filter = new TaskFilter();
                filter.setActionNames(new SWAMPHashSet(taskNames,","));
                filters.add(filter);
                filterText.append("Active Task: <i>" + taskNames + "</i> && ");
            }
           
        }
        
      
        // Filter for workflows that have a task in status active
        if (data.containsKey("systemfilter_hasactivetasks") && 
                !data.get("systemfilter_hasactivetasks").equals("")) {
            TaskFilter filter = new TaskFilter();
            filter.setIsRunning(Boolean.TRUE);
            filters.add(filter); 
        }
        
        // Filter for a task-owner
        if (data.containsKey("systemfilter_taskowner") && 
                !data.get("systemfilter_taskowner").equals("")) {            
            String taskOwner = (String) data.get("systemfilter_taskowner");
            MemoryTaskFilter taskfilter = new MemoryTaskFilter();
            taskfilter.setTaskOwner(taskOwner);
            // honor combination of taskowner and mandatorytask filter: 
            if (data.containsKey("systemfilter_mandatorytasksonly") && 
                    !data.get("systemfilter_mandatorytasksonly").equals("")) {
                taskfilter.setMandatoryOnly(true);
            }
            filters.add(taskfilter);
            this.taskOwner = taskOwner;
            filterText.append("Assigned User: <i>" + taskOwner + "</i> && ");
        }
        
        // filter for optional tasks: 
        if (data.containsKey("systemfilter_mandatorytasksonly") && 
                !data.get("systemfilter_mandatorytasksonly").equals("")) {
            MemoryTaskFilter taskfilter = new MemoryTaskFilter();
            taskfilter.setMandatoryOnly(true);
            filters.add(taskfilter);
            filterText.append("no optional tasks && ");
        }
        
        
        
        // add nodeFilter if used: 
        if (nodeFilter != null) filters.add(nodeFilter);
        
        return filters;
    }

    
    public StringBuffer getFilterText() {
        return filterText;
    }
    
    
    private PropertyFilter getNodeFilter(String wfTemplate){
        if (this.nodeFilter == null){
            nodeFilter = new PropertyFilter();
            nodeFilter.addWfTemplate(wfTemplate);            
        }
        return this.nodeFilter;
    }

    public String getTemplateName() {
        return templateName;
    }

    public boolean isOptionalTasks() {
        return optionalTasks;
    }

    public String getTaskOwner() {
        return taskOwner;
    }
    
    
    
}
