/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt (tschmidt@suse.de)
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

package de.suse.swamp.core.api;

import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;

/**

 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class SecurityAPI {

	
    public void storeUser(SWAMPUser user) throws UnknownElementException,
			SecurityException, StorageException {
        SecurityManager.storeUser(user);
	}
    
    
    public SWAMPUser getAuthenticatedUser(String username, String password) 
        throws StorageException, UnknownElementException, PasswordException {
    	return SecurityManager.getAuthenticatedUser(username, password);
    }
    
    
    public SWAMPUser getUser(String username, String requestor) 
        throws StorageException, UnknownElementException, SecurityException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(requestor), "swampadmins")){
            throw new SecurityException("No permission to get User object!");
        }
        return SecurityManager.getUser(username);
    }   
    
    public void doEmptyUsercache(String uname) throws StorageException, UnknownElementException, SecurityException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(uname), "swampadmins")) {
            throw new SecurityException("Not allowed to truncate user-cache.");
        }
        SecurityManager.clearCache();
    }

    public int doGetUserCacheSize(String username) throws SecurityException, UnknownElementException, StorageException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(username), "swampadmins")) {
            throw new SecurityException("No permission to read Usercache!");
        }
        return SecurityManager.getCacheSize();
    }
    
    public boolean addUserToWfRole(String targetUser, String wfName, String groupName, String username) 
        throws Exception {
        WorkflowTemplate wfTemp = new WorkflowAPI().getWorkflowTemplate(wfName, username);
        WorkflowRole role = wfTemp.getWorkflowRole(groupName);
        if (role == null) {
            throw new UnknownElementException("Role " + groupName + " not found for template: " + wfName);
        }
        if (!wfTemp.hasRole(username, WorkflowRole.ADMIN)) {
            throw new SecurityException("You need to be admin of " + wfName + " to change role memberships");
        }
        if (!(role instanceof DbReferencesRole)) {
            throw new UnknownElementException("Can only change members of DbReferencesRole types");
        }
        return ((DbReferencesRole) role).addMember(targetUser);
    }
    
    
    public boolean removeUserFromWfRole(String targetUser, String wfName, String groupName, String username) 
        throws Exception {
        WorkflowTemplate wfTemp = new WorkflowAPI().getWorkflowTemplate(wfName, username);
        WorkflowRole role = wfTemp.getWorkflowRole(groupName);
        if (role == null) {
            throw new UnknownElementException("Role " + groupName + " not found for template: " + wfName);
        }
        if (!wfTemp.hasRole(username, WorkflowRole.ADMIN)) {
            throw new SecurityException("You need to be admin of " + wfName + " to change role memberships");
        }
        if (!(role instanceof DbReferencesRole)) {
            throw new UnknownElementException("Can only change members of DbReferencesRole types");
        }
        return ((DbReferencesRole) role).removeMember(targetUser);
    
    }
    
    
    
}