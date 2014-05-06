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
  * Takes a groupname from the database for 
  * referencing role members
  *
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  */

import java.util.*;

import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class DbReferencesRole extends WorkflowRole {

    // names of database groups
    private SWAMPHashSet groupRefs;

    
    public DbReferencesRole(String name, boolean restricted){
        super(name, restricted);
        groupRefs = new SWAMPHashSet();
    }


    public SWAMPHashSet getMemberNames(Workflow wf) throws Exception {
        return getMemberNames(wf.getTemplate());
    }
    

    public SWAMPHashSet getMemberNames(WorkflowTemplate wf) throws NoSuchElementException {
        return getMemberNames();
    }
    
    
    public SWAMPHashSet getMemberNames() throws NoSuchElementException {
        SWAMPHashSet values = new SWAMPHashSet();
        for (Iterator it = groupRefs.iterator(); it.hasNext(); ){
            String groupName = (String) it.next();
            values.addAll(SecurityManager.loadGroupMemberNames(groupName));
        }
        return values;
    }
    
    
    public boolean isStaticRole(WorkflowTemplate wfTemp){
    	boolean isStatic = true; 
    	return isStatic;
    }


    public void addValue(String groupRef) {
        groupRefs.add(groupRef);
    }
 
    
    public void verify(WorkflowReadResult result, WorkflowTemplate wfTemp, WorkflowVerifier verifier, List results) {
        //  check for the groups in db
        for (Iterator it = this.groupRefs.iterator(); it.hasNext(); ){
            String groupName = (String) it.next();
            if (SecurityManager.getGroup(groupName) == null) {
                result.addError("Group " + groupName + " is not available in the database.");
            }
        }
    }
    
    /**
     * Adds a member to this role
     */
    public boolean addMember(String username) throws Exception {
        boolean memberAdded = false;
        SWAMPUser user = SecurityManager.getUser(username);
        if (groupRefs.size() != 1) {
            throw new Exception ("Cannot add member to DbRef role with multiple db groups");
        }
        if (!getMemberNames().contains(username)) {
            SecurityManager.addMemberToDbRole((String) groupRefs.toList().get(0), user);
            memberAdded = true;
        }
        return memberAdded;
    }
    
    
    /**
     * Remove a member from this role
     */
    public boolean removeMember(String username) throws Exception {
        boolean memberRemoved = false;
        SWAMPUser user = SecurityManager.getUser(username);
        if (groupRefs.size() != 1) {
            throw new Exception ("Cannot remove member from DbRef role with multiple db groups");
        }
        if (getMemberNames().contains(username)) {
            SecurityManager.removeMemberFromDbRole((String) groupRefs.toList().get(0), user);
            memberRemoved = true;
        }
        return memberRemoved;
    }
    
}

