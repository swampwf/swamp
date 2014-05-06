/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt
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

package de.suse.swamp.turbine.services.security;

import org.apache.turbine.om.security.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;
import java.util.*;


/**
 * @author tschmidt
 * Extending the TurbineUser for wrapping around the SWAMPUser.
 * Everything in here should implement Serializable to be able to
 * restore sessions correctly.
 */
public class SWAMPTurbineUser extends TurbineUser {

    private SWAMPUser user;

    public int getId() {
        return this.user.getId();
    }

    public SWAMPTurbineUser(SWAMPUser user){
        super();
        this.user = user;
    }

    public SWAMPTurbineUser(){
        super();
        this.user = new SWAMPUser();
        user.setUserName("anonymous");
    }

    public String getName() {
        return user.getUserName();
    }

    public String getFirstName(){
        return user.getFirstName();
    }

    public String getLastName(){
        return user.getLastName();
    }

    public String getEmail(){
        return user.getEmail();
    }

    public boolean isGroupMember(String groupName){
        return de.suse.swamp.core.container.SecurityManager.
        isGroupMember(user, groupName);
    }

    public boolean hasRoleInWorkflow(String rolePath, Workflow wf){
        return wf.hasRole(user.getUserName(), rolePath);
    }

    public boolean hasRoleInWorkflow(String role, WorkflowTemplate wft){
        return wft.hasRole(user.getUserName(), role);
    }

    public boolean hasRoleInWorkflow(String role, String templateName)
        throws UnknownElementException, SecurityException {
        try {
            WorkflowTemplate t = new WorkflowAPI().getWorkflowTemplate(templateName, this.getName());
            return t.hasRole(user.getUserName(), role);
        } catch (Exception e) {
            Logger.ERROR("Cannot evaluate role: " + e.getMessage());
            return false;
        }
    }


    public Object getPerm(String name) {
        return this.user.getPerm(name);
    }

    public Object getPerm(String name, String def) {
        return this.user.getPerm(name, def);
    }

    public ArrayList getPermArray(String name) {
        return this.user.getPermAsArray(name);
    }


    // Turbine will try to write an accesscounter to the userstore.
    // We prevent it by overloading the method.
    public void setPerm(String name, Object value) {
        if (!name.equals("_access_counter") && !name.equals("LOGIN_NAME")) {
    		Logger.ERROR("Please use setPerm(String, String) for: " + name + " " + value);
        }
    }

    public void setPerm(String name, String value)
    	throws UnknownElementException, SecurityException, StorageException {
		this.user.setPerm(name, value);
    }


    public void setPermArray(String name, ArrayList value)
    throws SecurityException, StorageException, UnknownElementException {
        this.user.setPerm(name, value);
    }


	public SWAMPUser getSWAMPUser() {
		return user;
	}

    public void setPass(String pw)
    throws PasswordException, SecurityException, StorageException, UnknownElementException {
        this.user.setPassword(pw);
    }

    public void setFirstname(String name)
    throws SecurityException, StorageException, UnknownElementException {
        this.user.setFirstName(name);
    }

    public void setLastname(String name)
    throws SecurityException, StorageException, UnknownElementException {
        this.user.setLastName(name);
    }

    public void setMail(String name)
    throws SecurityException, StorageException, UnknownElementException {
        this.user.setEmail(name);
    }
}
