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

package de.suse.swamp.core.filter;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


/**
 * @author tschmidt
 *
 * Filter for a person in a Role of a Workflow
 */
public class RoleFilter extends DatabaseFilter {

    private String search;
    private String role;
	private String template;
    
	
    public RoleFilter(String search, String role, String template) {
        super();
        this.search = search;
        this.role = role; 
        this.template = template;
    }

    public String getSQL(){
    	WorkflowTemplate wfTemp = WorkflowManager.getInstance().getWorkflowTemplate(template);
    	WorkflowRole wfRole = wfTemp.getWorkflowRole(role);
    	DatabaseFilter filter = null;
    	if (wfTemp.hasRole(search, WorkflowRole.ADMIN)) {
            //TODO: add fake filter, can be removed when we are ready for filters that don't filter...
            filter = new PropertyFilter();
            ((PropertyFilter) filter).addWfTemplate(template);
    	// check if we can evaluate it statically
    	} else if (wfRole.isStaticRole(wfTemp)) {
	    	try {
	    		// if included in this role, or in static admin role
				if (wfTemp.hasRole(search, role)){
					//TODO: add fake filter, can be removed when we are ready for filters that don't filter...
					filter = new PropertyFilter();
					((PropertyFilter) filter).addWfTemplate(template);
				}
			} catch (Exception e) {
				Logger.ERROR("Cannot evaluate role value: " + e.getMessage());
			}
    	} else if (wfRole instanceof DatabitRole) {
            String path = ((DatabitRole) wfRole).getRoleDatabit();
            filter = new ContentFilter();
            ((ContentFilter) filter).setDatabitPath(path);
            ((ContentFilter) filter).setDatabitValueRegex("(" + search + "$|" + search + ",)");
    	} else if (wfRole instanceof ReferencesRole && 
    	                ((ReferencesRole) wfRole).getAllRoleDatabits(wfTemp).size() == 1){
	        String path = (String) ((ReferencesRole) wfRole).getAllRoleDatabits(wfTemp).get(0);
	        filter = new ContentFilter();
	        ((ContentFilter) filter).setDatabitPath(path);
	        ((ContentFilter) filter).setDatabitValueRegex("(" + search + "$|" + search + ",)");
    	} else {
    		// FIXME: would need to create an OR filter with all databitpaths here.
    		Logger.ERROR("Filtering for multiple reference roles containing non-static roles not implemented yet!");
    	}
    	if (filter == null){
			filter = new PropertyFilter();
			((PropertyFilter) filter).setWfId(0);
    	}
    	return filter.getSQL();
    }
    
       
}