/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.filter;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * Filter the Workflow list by workflows attributes
 */
public class PropertyFilter extends DatabaseFilter {

    // list of wftemplate names, will get combined with OR
    private List wfTemplates = new ArrayList();
	private String wfVersion;
	private int parentWfId;
	private ArrayList nodesActive = new ArrayList();
	private ArrayList nodesNotActive = new ArrayList();
	private Boolean closed;
	private int wfId = -1;
    
    
    public PropertyFilter() {
        super();
    }

	
    public String getSQL(){
        ArrayList columns = new ArrayList();
        ArrayList tables = new ArrayList();
        ArrayList conditions = new ArrayList();
        
        columns.add("dbWorkflows.wfid");
        tables.add("dbWorkflows");
        
        
        if (wfTemplates.size() > 0){
            StringBuffer templatesql = new StringBuffer();
            boolean isFirst = true;
            for (Iterator it = wfTemplates.iterator(); it.hasNext(); ) {
                if (isFirst){
                    templatesql.append("(dbWorkflows.templatename = '" + it.next() + "'");
                    isFirst = false;
                } else { 
                    templatesql.append("OR dbWorkflows.templatename = '" + it.next() + "'");
                }
            }
            templatesql.append(")");
            conditions.add(templatesql.toString());
            if (descending != null) {
                orderColumn = "dbWorkflows.templatename";  
            } 
        }
        if (parentWfId != 0){
            if (descending == null){
                conditions.add("dbWorkflows.parentwfid = '" + parentWfId + "'");
            } else {
                orderColumn = "dbWorkflows.parentwfid";
            }
        }
        if (wfId > -1){
            if (descending == null){
                conditions.add("dbWorkflows.wfid = " + wfId + ""); 
            } else {
                orderColumn = "dbWorkflows.wfid";
            }
        }
        if (nodesActive.size() > 0){ 
            if (descending == null){
                for (int i = 0; i<nodesActive.size(); i++){
                    tables.add("dbNodes nodesA"+i);
                    conditions.add("nodesA"+i+".activity = 1"); 
                    conditions.add("nodesA"+i+".name = '" + nodesActive.get(i) + "'");
                    conditions.add("nodesA"+i+".workflowid = dbWorkflows.wfid");
                }
            } else {
                orderColumn = "dbWorkflows.wfid";
            }
        }
        if (nodesNotActive.size() > 0){ 
            if (descending == null){
                for (int i = 0; i<nodesNotActive.size(); i++){
                    tables.add("dbNodes nodesN"+i);
                    conditions.add("nodesN"+i+".name = '" + nodesNotActive.get(i) + "'"); 
                    conditions.add("nodesN"+i+".activity = 0"); 
                    conditions.add("nodesN"+i+".workflowid = dbWorkflows.wfid");
                }
            } else {
                orderColumn = "dbWorkflows.wfid";
            }
        } 
        if (closed != null){ 
            if (descending == null){
                if (closed.booleanValue()){
                    tables.add("dbNodes");
                    conditions.add("dbNodes.activity = 1"); 
                    conditions.add("dbNodes.isendnode = 1");
                    conditions.add("dbNodes.workflowid = dbWorkflows.wfid");
                } else {
                    WorkflowManager wfman = WorkflowManager.getInstance();
                    if (wfTemplates.size() != 1){
                        //FIXME...
                        Logger.ERROR("Sorry, generating filter for running/closed workflows only works when filtering for one template atm.");
                    } else {
                        WorkflowTemplate wftemp = null;
                        if (wfVersion != null){
                            wftemp = wfman.getWorkflowTemplate((String) wfTemplates.get(0), wfVersion);
                        } else {
                            wftemp = wfman.getWorkflowTemplate((String) wfTemplates.get(0));
                        }
                        // get the endnodes;
                        ArrayList endNodes = wftemp.getAllNodeTemplates("end"); 
                        for (int i = 0; i < endNodes.size(); i++){
                            tables.add("dbNodes nodesN"+i);
                            conditions.add("nodesN"+i+".name = '" + ((NodeTemplate) endNodes.get(i)).getName() + "'"); 
                            conditions.add("nodesN"+i+".activity = 0"); 
                            conditions.add("nodesN"+i+".workflowid = dbWorkflows.wfid");
                        }
                    }
                }
            } else {
                orderColumn = "dbWorkflows.wfid";
            }
        }
        
        return buildSQLString(columns, tables, conditions);
    }

    

	public List getWfTemplates() {
		return wfTemplates;
	}

	public void addWfTemplate(String wfTemplate) {
	    if (wfTemplate != null)
	        this.wfTemplates.add(wfTemplate);
	}
    
    public void setWfTemplates(List templates) {
        this.wfTemplates = templates;
    }

	public int getParentWfId() {
		return parentWfId;
	}


	public void setParentWfId(int parentWfId) {
		this.parentWfId = parentWfId;
	}


	public String getWfVersion() {
		return wfVersion;
	}


	public void setWfVersion(String wfVersion) {
		this.wfVersion = wfVersion;
	}
	public int getWfId() {
		return wfId;
	}


	public void setWfId(int wfId) {
		this.wfId = wfId;
	}

    /**
     * @return Returns the nodeActive.
     */
    public ArrayList getNodesActive() {
        return nodesActive;
    }
    /**
     * @param nodeActive The nodeActive to set.
     */
    public void addNodeActive(String nodeActive) {
        this.nodesActive.add(nodeActive);
    }
    /**
     * @return Returns the nodeNotActive.
     */
    public ArrayList getNodeNotActive() {
        return nodesNotActive;
    }
    /**
     * @param nodeNotActive The nodeNotActive to set.
     */
    public void addNodeNotActive(String nodeNotActive) {
        this.nodesNotActive.add(nodeNotActive);
    }


    /**
     * @return Returns the nodeTypeNotActive.
     */
    public boolean isClosed() {
        return closed.booleanValue();
    }


    /**
     * @param nodeTypeNotActive The nodeTypeNotActive to set.
     */
    public void setClosed(boolean closed) {
        this.closed = Boolean.valueOf(closed);
    }
}