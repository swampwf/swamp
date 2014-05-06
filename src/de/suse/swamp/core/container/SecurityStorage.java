/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de> 
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

import org.apache.torque.*;
import org.apache.torque.om.*;
import org.apache.torque.util.*;

import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

/**
 * This is the Storage-Manager for everything security-related that needs 
 * to be stored. 
 * 
 * Note for the accessibility of this Methods:
 * "If none of the access modifiers public, protected, or private are specified, 
 * a class member or constructor is accessible throughout the package that 
 * contains the declaration of the class in which the class member is declared, 
 * but the class member or constructor is not accessible in any other package.
 * 
 * so they may only be instantiated by the Manager-Classes.
 * 
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public final class SecurityStorage {

    // extra logger for storage stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.container.Storage");
        
    /**
     * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
     */
    public static SWAMPUser loadUserFromDB(String username) 
        throws StorageException {

        Criteria criteria = new Criteria();
        List userStores;
        SWAMPUser user = null;
        criteria.add(DbusersPeer.LOGIN_NAME, username);

        try {
            userStores = DbusersPeer.doSelect(criteria);
        } catch (Exception e) {
            e.printStackTrace ();
            throw new StorageException("caught exception during DB access: " + 
                    e.getClass().getName() + ": " + e.getMessage (), e);
        }

        if (userStores.size() == 1) {
            user = getSWAMPUser((Dbusers) userStores.get(0));
        }
        return user;
    }
    

    
    /**
     * return list of users which have something LIKE %@exp% in permstorage
     */
    public static List loadUsersWithPerm(String exp) 
        throws StorageException {
        Criteria criteria = new Criteria();
        List userStores;
        criteria.add(DbusersPeer.PERMSTORAGE, (Object) ("%" + exp + "%"), Criteria.LIKE);
        try {
            userStores = DbusersPeer.doSelect(criteria);
        } catch (Exception e) {
            e.printStackTrace ();
            throw new StorageException("caught exception during DB access: " + 
                    e.getClass().getName() + ": " + e.getMessage (), e);
        }
        List users = new ArrayList();
        for (Iterator it = userStores.iterator(); it.hasNext(); ) {
            users.add(getSWAMPUser((Dbusers) it.next()));
        }
        return users;
    }
    
    
    
    
    private static SWAMPUser getSWAMPUser(Dbusers userStore) throws StorageException {
        SWAMPUser user = new SWAMPUser();
        user.setUserName(userStore.getUserName());
        user.setId(userStore.getUserId());
        user.setPasswordHash(userStore.getPasswordMd5());
        user.setModified(userStore.getModified());
        user.setLastLogin(userStore.getLastLogin());
        user.setFirstName(userStore.getFirstName());
        user.setLastName(userStore.getLastName());
        user.setEmail(userStore.getEmail());
        SWAMPHashMap perm = new SWAMPHashMap(userStore.getPermstorage(), ";");
        user.setPermSet(perm);
        user.setModified(false);
        return user;
    }
    
    
    
    /** 
     * Stores / updates the SWAMPUser to the DB.
     */    
    public static void storeUser(SWAMPUser user) throws StorageException {
        if (user.isModified()){        
            Criteria crit = new Criteria();
            Date now = new Date();
            user.setModified(now);
            crit.add(DbusersPeer.LOGIN_NAME, user.getUserName());
            crit.add(DbusersPeer.LAST_LOGIN, user.getLastLogin());
            crit.add(DbusersPeer.MODIFIED, now);
            crit.add(DbusersPeer.PASSWORD_MD5, user.getPasswordHash());
            crit.add(DbusersPeer.FIRST_NAME, user.getFirstName());
            crit.add(DbusersPeer.LAST_NAME, user.getLastName());
            crit.add(DbusersPeer.EMAIL, user.getEmail());
            crit.add(DbusersPeer.PERMSTORAGE, user.getPermSet().toString(";"));
            
            try {
                if (user.getId() <= 0) {
                    crit.add(DbusersPeer.CREATED, now);
                    NumberKey key = (NumberKey) DbusersPeer.doInsert(crit);
                    user.setId(key.intValue());
                    Logger.DEBUG("Inserted User " + user.getUserName(), log);
                } else {
                    crit.add(DbusersPeer.USER_ID, user.getId());
                    DbusersPeer.doUpdate(crit);
                    Logger.DEBUG("Updated User " + user.getUserName(), log);
                }
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }
            user.setModified(false);
        }
    }
    
    
    /**
     * Get a list of SWAMPGroups the user is affiliated with.
     */
    static List getAffiliatedGroups(SWAMPUser user){
        List dbGroups = null;
        ArrayList groups = new ArrayList();
        Criteria crit = new Criteria();
        crit.add(DbusersGroupsPeer.USER_ID, user.getId());
        crit.addJoin(DbusersGroupsPeer.GROUP_ID, DbgroupsPeer.GROUP_ID);
        try {
            dbGroups = DbgroupsPeer.doSelect(crit);
        } catch (TorqueException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (Iterator it = dbGroups .iterator(); it.hasNext(); ){
            Dbgroups dbGroup = (Dbgroups) it.next();
            SWAMPGroup group = new SWAMPGroup(dbGroup.getGroupName());
            groups.add(group);            
        }
        return groups;
    }

    
    /**
     * Only used to check availability of that group
     */
    static SWAMPGroup loadGroup(String groupName){
        SWAMPGroup group = null;
        Criteria crit = new Criteria();
        crit.add(DbgroupsPeer.GROUP_NAME, groupName);
        try {
            List dbGroups = DbgroupsPeer.doSelect(crit);
            if (dbGroups.size() == 1) {
                Dbgroups dbGroup = (Dbgroups) dbGroups.get(0);
                group = new SWAMPGroup(dbGroup.getGroupName());
                group.setId(dbGroup.getGroupId());
            }       
        } catch (TorqueException e) {
            Logger.ERROR("Loading of group: " + groupName + " failed, reason: " + e.getMessage());
            e.printStackTrace();
        }
        return group;
    }
    
    
    /**
     * Load all usernames that belong to that group
     */
    static List loadGroupMemberNames(String group){
        List memberNames = new ArrayList();
        List dbUsers = null;
        Criteria crit = new Criteria();
        crit.addJoin(DbusersGroupsPeer.USER_ID, DbusersPeer.USER_ID);
        crit.addJoin(DbusersGroupsPeer.GROUP_ID, DbgroupsPeer.GROUP_ID);
        crit.add(DbgroupsPeer.GROUP_NAME, group);
        try {
            dbUsers = DbusersPeer.doSelect(crit);
            for (Iterator it = dbUsers .iterator(); it.hasNext(); ){
                Dbusers dbuser = (Dbusers) it.next();
                memberNames.add(dbuser.getUserName());
            }
        } catch (TorqueException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return memberNames;
    }
    
    
    static void addMemberToDbRole(SWAMPGroup group, SWAMPUser user) throws StorageException {
        Criteria crit = new Criteria();
        crit.add(DbusersGroupsPeer.GROUP_ID, group.getId());
        crit.add(DbusersGroupsPeer.USER_ID, user.getId());
        try {
            DbusersGroupsPeer.doInsert(crit);
            Logger.LOG("Added User " + user.getUserName() + " to group: " + group.getName(), log);
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }
    

    static void removeMemberFromDbRole(SWAMPGroup group, SWAMPUser user) throws StorageException {
        Criteria crit = new Criteria();
        crit.add(DbusersGroupsPeer.GROUP_ID, group.getId());
        crit.add(DbusersGroupsPeer.USER_ID, user.getId());
        try {
            DbusersGroupsPeer.doDelete(crit);
            Logger.LOG("Deleted User " + user.getUserName() + " from group: " + group.getName(), log);
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }
}
