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
  * The value a list of referenced role names. 
  * Roles from a parent workflow can be referenced by "parent.<rolename>"
  *
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  */

import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class ReferencesRole extends WorkflowRole {

    // merging other roles in this role
    private SWAMPHashSet roleRefs;

    
    public ReferencesRole(String name, boolean restricted){
        super(name, restricted);
        roleRefs = new SWAMPHashSet();
    }

    

    public SWAMPHashSet getMemberNames(Workflow wf) throws Exception {
        SWAMPHashSet values = new SWAMPHashSet();
        for (Iterator it = roleRefs.iterator(); it.hasNext(); ){
        	String roleName = (String) it.next();
        	WorkflowRole role = wf.getTemplate().getWorkflowRole(roleName);
        	if (role == null)
                throw new NoSuchElementException("Role: " + roleName + " does not exist in wf " + wf.getName());
            values.addAll(role.getMemberNames(wf.getWorkflowForRole(roleName)));
        }
        return values;
    }
    

    public SWAMPHashSet getMemberNames(WorkflowTemplate wf) throws NoSuchElementException {
        SWAMPHashSet values = new SWAMPHashSet();
        for (Iterator it = roleRefs.iterator(); it.hasNext();) {
            String roleName = (String) it.next();
            WorkflowRole role = wf.getWorkflowRole(roleName);
            if (role == null)
                throw new NoSuchElementException("Role: " + roleName + " does not exist in wf " + wf.getName());
            values.addAll(role.getMemberNames(wf.getTemplateForRole(roleName)));
        }
        return values;
    }
    
    
    public boolean isStaticRole(WorkflowTemplate wfTemp){
    	boolean isStatic = true; 
		for (Iterator it = roleRefs.iterator(); it.hasNext(); ){
			String roleName = (String) it.next();
			WorkflowRole refRole = wfTemp.getWorkflowRole(roleName);
			if (refRole != null && !refRole.isStaticRole(
					wfTemp.getTemplateForRole(roleName))){
				isStatic = false;
			}
		}
    	return isStatic;
    }


	/**
	 * get all databits that are used in this role (including referenced roles)
	 * To be able to create a non static database filter
	 */
	public List getAllRoleDatabits(WorkflowTemplate wf) {
		ArrayList bitPaths = new ArrayList();
		for (Iterator it = roleRefs.iterator(); it.hasNext(); ){
			String roleName = (String) it.next();
			WorkflowRole role = wf.getWorkflowRole(roleName);
			if (role instanceof DatabitRole) {
			    bitPaths.add(((DatabitRole) role).getRoleDatabit());
			} else if (role instanceof ReferencesRole) {
			    bitPaths.addAll(((ReferencesRole) role).getAllRoleDatabits(wf.getTemplateForRole(roleName)));
		    }
		}
		return bitPaths;
	}

    public void addValue(String roleRef) {
        this.roleRefs.add(roleRef);
    }

    
    public void verify(WorkflowReadResult result, WorkflowTemplate wfTemp, WorkflowVerifier verifier, List results) {
        // referenced roles will get checked themselves, just look if they exist: 
        for (Iterator refit = roleRefs.iterator(); refit.hasNext();) {
            String refRole = (String) refit.next();
            if (!wfTemp.roleExists(refRole))
                result.addError("Referenced role: " + refRole + " not found.");
        }
    }

    public SWAMPHashSet getRoleRefs() {
        return roleRefs;
    }
            
}

