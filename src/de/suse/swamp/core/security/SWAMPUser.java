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

package de.suse.swamp.core.security;

/**
 *
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 */

import java.io.*;
import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

public class SWAMPUser extends Persistant implements Serializable {


    public static final String SYSTEMUSERNAME = "System";

    /** The value for the userName field */
    private String userName;
    /** The value for the password field */
    private String passwordHash;
    /** The value for the firstName field */
    private String firstName;
    /** The value for the lastName field */
    private String lastName;
    /** The value for the email field */
    private String email;
    /** The value for the createDate field */
    private Date createDate;
    private Date modifyDate;
    /** The value for the lastLogin field */
    private Date lastLogin;
    /** Storage for permdata values  */
    private SWAMPHashMap permSet = new SWAMPHashMap();


    public SWAMPUser() {
        super();
        setModified(true);
    }


    public String getUserName() {
        return userName;
    }


    public void setUserName(String v) {
        if (this.userName == null || !this.userName.equals(v)) {
            this.userName = v;
            setModified(true);
        }
    }


    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPassword(String v) throws PasswordException, StorageException {
    	String hash = SWAMPDBUserManager.toMD5(v);
    	setPasswordHash(hash);
    }


    public void setPasswordHash(String v) throws StorageException {
        if (this.passwordHash == null || !this.passwordHash.equals(v)) {
            this.passwordHash = v;
            setModified(true);
        }
    }


    public String getFirstName() {
        return firstName;
    }


    public void setFirstName(String v) throws StorageException {
        if (this.firstName == null || !this.firstName.equals(v)) {
            this.firstName = v;
            setModified(true);
        }
    }

    public String getLastName() {
        return lastName;
    }


    public void setLastName(String v) throws StorageException {
        if (this.lastName == null || !this.lastName.equals(v)) {
            this.lastName = v;
            setModified(true);
        }
    }

    public String getEmail() {
        return email;
    }


    public void setEmail(String v) throws StorageException {
        if (this.email == null || !this.email.equals(v)) {
            this.email = v;
            setModified(true);
        }
    }


    public Date getModified() {
        return modifyDate;
    }


    public void setModified(Date v) throws StorageException {
        if (this.modifyDate == null || !this.modifyDate.equals(v)) {
            this.modifyDate = v;
            setModified(true);
        }
    }


    public Date getCreateDate() {
        return createDate;
    }


    public Date getLastLogin() {
        return lastLogin;
    }


    public void setLastLogin(Date v) throws StorageException {
        if (this.lastLogin == null || !this.lastLogin.equals(v)){
            this.lastLogin = v;
            setModified(true);
        }
    }


    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("Dbusers:\n");
        str.append("UserId = ")
        .append(getId())
        .append("\n");
        str.append("UserName = ")
        .append(getUserName())
        .append("\n");
        return(str.toString());
    }

    /**
     * Get a value from the users permanent storage
     */
    public String getPerm(String ident){
        return (String) this.permSet.get(ident);
    }

    /**
     * Get a value from the users permanent storage
     * and use <i>defaultval</i> if not found
     */
    public String getPerm(String ident, String defaultval) {
    	String val = defaultval;
    	if (this.permSet.containsKey(ident)){
    		val = (String) this.permSet.get(ident);
    	}
        return val;
    }

    public void setPerm(String name, String value) throws StorageException {
        if (value == null) {
            this.permSet.remove(name);
        } else {
            this.permSet.put(name, value);
        }
        setModified(true);
        Logger.DEBUG("Setting " + name + "=" + getPerm(name) +
        		" for user: " + this.userName);
        de.suse.swamp.core.container.SecurityManager.storeUser(this);
    }


    public void setPerm(String name, ArrayList values) throws StorageException {
        StringBuffer value = new StringBuffer();
        int count = 0;
        for (Iterator it = values.iterator(); it.hasNext(); ){
            if (count > 0) {
				value.append(", ");
			}
            value.append((String) it.next());
            count++;
        }
        this.setPerm(name, value.toString());
        setModified(true);
        de.suse.swamp.core.container.SecurityManager.storeUser(this);
    }


    public ArrayList getPermAsArray(String ident){
        ArrayList values = new ArrayList();
        String val = getPerm(ident);
        if (val != null){
	        StringTokenizer st = new StringTokenizer(val, ",");
	        while(st.hasMoreTokens()){
	            values.add(st.nextToken().trim());
	        }
        }
        return values;
    }


    /**
     * @return Returns the permSet.
     */
    public SWAMPHashMap getPermSet() {
        return permSet;
    }


    /**
     * @param permSet The permSet to set.
     */
    public void setPermSet(SWAMPHashMap permSet) {
        this.permSet = permSet;
        setModified(true);
    }



}
