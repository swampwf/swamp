/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de>
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
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * In-Memory for Task related stuff
 */
public class MemoryTaskFilter extends MemoryFilter {

    // filter for assigned person
    private String taskOwner;
    // only filter for mandatory tasks
    private boolean mandatoryOnly = false;
    
    public MemoryTaskFilter() {
        super();
    }


    public List getFilteredList (List idList) {
        List filteredList = new ArrayList();
        filteredList.addAll(idList);
        List taskList = new ArrayList();
        List taskWfIdList = new ArrayList();
        String taskOwnerMail = taskOwner + "@";
        
        try {
            if (mandatoryOnly){
                taskList = TaskManager.getAllActiveTasks(true);
            } else {
                taskList = TaskManager.getAllActiveTasks();
            }
        } catch (StorageException e) {
            Logger.ERROR("Error fetching filtered Task-list: " + e.getMessage());
        }
        for (Iterator it = taskList.iterator(); it.hasNext(); ){
            WorkflowTask task = (WorkflowTask) it.next();
            if (taskOwner != null){
                // check for direct match and mail prefix match: 
                for (Iterator userIt = task.getUsersForRole().iterator(); userIt.hasNext(); ){
                    String user = (String) userIt.next();
                    if (user.equals(taskOwner) || user.startsWith(taskOwnerMail)){
                        taskWfIdList.add(new Integer(task.getWorkflowId()));
                    }
                }
            } else {
                taskWfIdList.add(new Integer(task.getWorkflowId()));
            }
        }
        filteredList.retainAll(taskWfIdList);
        return filteredList;
    }
    
    public String toString() {
        return "";
    }
    
    public String toHTML() {
        return this.toString();
    }


    public void setTaskOwner(String taskOwner) {
        this.taskOwner = taskOwner;
    }


    public void setMandatoryOnly(boolean mandatoryOnly) {
        this.mandatoryOnly = mandatoryOnly;
    }

}