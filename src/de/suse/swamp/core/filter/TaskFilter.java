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

import org.apache.commons.lang.*;

import de.suse.swamp.core.tasks.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * Filter for assigned + involved users
 */
public class TaskFilter extends DatabaseFilter {

    // filter for workflows with active tasks contained in: 
    private SWAMPHashSet actionNames ;
    // filter for running finished tasks
    private Boolean isRunning = null ;
    
    public TaskFilter() {
        super();
    }


    public String getSQL(){
        ArrayList columns = new ArrayList();
        ArrayList tables = new ArrayList();
        ArrayList conditions = new ArrayList();
        
        columns.add("dbWorkflows.wfid");
        tables.add("dbWorkflows");
        tables.add("dbTasks");
        conditions.add("dbTasks.workflowID  = dbWorkflows.wfid");
        
        if (actionNames != null){
            if (descending == null){ 
                conditions.add("dbTasks.state = " + WorkflowTask.ACTIVE);
                StringBuffer sqlActions = new StringBuffer();
                for (Iterator it = actionNames.iterator(); it.hasNext(); ){
                    if (sqlActions.length() > 0) sqlActions.append(","); 
                    sqlActions.append("'").append(StringEscapeUtils.escapeSql((String) it.next())).append("'");
                }
                conditions.add("dbTasks.actionName IN (" + sqlActions.toString() + ")");
            } else {
                orderColumn = "dbTasks.actionName";
            }
        } else if (isRunning != null){
            if (descending == null){ 
                if (isRunning == Boolean.TRUE){
                    conditions.add("dbTasks.state = " + WorkflowTask.ACTIVE);
                }
            } else {
                orderColumn = "dbWorkflows.wfid";
            }
                
        } else {
            Logger.ERROR("Trying to use uninitialized Filter!");
        }
        
        return buildSQLString(columns, tables, conditions);
    }
    
    
    
    public String toString() {
        return "";
    }
    
    public String toHTML() {
        return this.toString();
    }


    public void setActionNames(SWAMPHashSet actionNames) {
        this.actionNames = actionNames;
    }


    public void setIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
    }


}