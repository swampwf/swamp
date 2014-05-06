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

package de.suse.swamp.core.security.roles;

 /**
  * Representation of a role in a Workflow. 
  * Default roles are "user", "owner", "admin", "starter"
  *
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  */

import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public abstract class WorkflowRole implements Describable {

    public static final String SYSTEMROLE = "system";
    public static final String ADMIN = "admin";    
    public final static String STARTER = "starter";
    public final static String USER = "user";
    public final static String OWNER = "owner";
    
    private String name;
    private boolean restricted;
    private String description;
    
    public WorkflowRole(String name, boolean restricted){
        this.name = name;
        this.restricted = restricted;
    }


    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return if the role is restricted, that means if only 
     * assigned members are allowed to do connected tasks
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * Check if a user has this role in a workflow instance.
     * This really checks only for explicit membership in this role, 
     * and does not honour admin or owner states
     */
    public boolean hasRole(String username, Workflow wf) throws Exception {
        boolean hasRole = false;
        if (!this.isRestricted()){
            hasRole = true;
        } else {
            if (this.getMemberNames(wf.getWorkflowForRole(this.getName())).containsIgnoreCase(username)){
                hasRole = true;
            }
        }
        return hasRole;
    }
    
    
    /**
     * Check if a user has this role in a workflow template.
     * This really checks only for explicit membership in this role, 
     * and does not honour admin or owner states
     */
    public boolean hasRole(String username, WorkflowTemplate wftemp) 
    	throws Exception {
        boolean hasRole = false;
        if (!this.isRestricted()){
            hasRole = true;
        } else {
            if (this.getMemberNames(wftemp.getTemplateForRole(this.getName())).containsIgnoreCase(username)){
                hasRole = true;
            }
        }
        return hasRole;
    }

    
    public void setDescription(String desc){
        this.description = desc;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
		if (!(this.description == null)) {
			return description;
		} else {
			return name;
		}
	}
    
    
    /**
     * check for a static role. a static role has the same value 
     * for all instances of a workflow template. 
     * If it is a merged role it is only static when all referenced 
     * roles are static (are no databit referencing roles)
     */
    public abstract boolean isStaticRole(WorkflowTemplate wfTemp);

    
    public String toString(){
        return "WorkflowRole " + name;
        
    }
    
    /**
     * Get the values of this role in the provided workflow instance.
     */
    public abstract SWAMPHashSet getMemberNames(Workflow wf) throws Exception;
    
    
    /**
     * Get the values of this role in the provided workflow template.
     */
    public abstract SWAMPHashSet getMemberNames(WorkflowTemplate wf) throws NoSuchElementException;
    
    
    /**
     * Add the value to this role, that may be a databitpath, a value 
     * the name of a referenced role, depending on the role type
     */
    public abstract void addValue(String value);

    
    /**
     * Helper method for the verifier to check if the role is initialized correctly
     */
    public abstract void verify(WorkflowReadResult result, WorkflowTemplate wfTemp, WorkflowVerifier verifier, List results);
}

