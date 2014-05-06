/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt@suse.de> 
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

package de.suse.swamp.core.container;

import java.util.*;

import org.apache.commons.collections.map.*;

import de.suse.swamp.core.security.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

/**
 * The SecurityManager handles authenticating stuff. 
 * It has a static reference to the underlying 
 * Usermanager interface that is configured by the parameter 
 * AUTH_CLASS in the defaults file. Though we can configure 
 * dynamically whether to use LDAP or a DB as backend. 
 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 */

public class SecurityManager {

    private static Class authStorageClass;
    private static UserManagerIface userManagerInstance;
    // cache valid loaded users; key: username; value: SWAMPUser-object
    private static LRUMap userCache = new LRUMap(300);
    

    static {
        try {
            authStorageClass = Class.forName(SWAMP.getInstance().getProperty("AUTH_CLASS"));
            userManagerInstance = (UserManagerIface) authStorageClass.newInstance();
            Logger.DEBUG("Using " + userManagerInstance.toString() + " for authenticating");
        } catch (Exception e) {
            Logger.ERROR("Could not instantiate configured AUTH_CLASS "
                    + SWAMP.getInstance().getProperty("AUTH_CLASS"));
            e.printStackTrace();
        }
    }
    
    
    /**
     * @param username
     * @param password
     * @return
     * @throws StorageException
     * @throws UnknownElementException
     * @throws PasswordException
     */
    public static SWAMPUser getAuthenticatedUser(String username, String password) 
        throws StorageException, UnknownElementException, PasswordException {
        userManagerInstance.authenticateUser(username, password);
        SWAMPUser user = userManagerInstance.loadUser(username);
        userCache.put(username, user);
        user.setLastLogin(new Date());
        return user;
    }


   public static SWAMPUser getUser(String username) throws StorageException, UnknownElementException {
       SWAMPUser user;
       if (userCache.containsKey(username)) {
           user = (SWAMPUser) userCache.get(username);
       } else {
           user = userManagerInstance.loadUser(username);
           userCache.put(username, user);
       }
       return user;
    }


   /**
    * Load user which have a variable that matches @regex set in their permstorage (preferences)
    */
   public static List loadUsersWithPerm(String regex) throws StorageException, UnknownElementException {
       List dbUsers = SecurityStorage.loadUsersWithPerm(regex);
       List users = new ArrayList(); 
       for (Iterator it = dbUsers.iterator(); it.hasNext(); ) {
           SWAMPUser user = (SWAMPUser) it.next();
           users.add(getUser(user.getUserName()));
       }
       return users;
   }
   
   
   public static void storeUser(SWAMPUser user) throws StorageException {
       userCache.put(user.getUserName(), user);
       SecurityStorage.storeUser(user);
    }
   

   /**
    * Checks if a User is affiliate in the named group
    */
   public static boolean isGroupMember(SWAMPUser user, String groupName) {
		boolean result = false;
		if (user != null && groupName != null) {
			if (SecurityStorage.getAffiliatedGroups(user).contains(new SWAMPGroup(groupName)))
				result = true;
			else
				result = false;
		}
		return result;
	}


   public static SWAMPGroup getGroup(String groupName) {
        return SecurityStorage.loadGroup(groupName);
    }
   
   
   public static List loadGroupMemberNames(String group){
       return SecurityStorage.loadGroupMemberNames(group);
   }
   
   public static void clearCache() {
       userCache.clear();
   }
   
   public static int getCacheSize() {
       return userCache.size();
   }
   
   public static void addMemberToDbRole(String dbGroup, SWAMPUser user) throws StorageException {
       SecurityStorage.addMemberToDbRole(SecurityStorage.loadGroup(dbGroup), user);
   }

   public static void removeMemberFromDbRole(String dbGroup, SWAMPUser user) throws StorageException {
       SecurityStorage.removeMemberFromDbRole(SecurityStorage.loadGroup(dbGroup), user);
   }
   
}