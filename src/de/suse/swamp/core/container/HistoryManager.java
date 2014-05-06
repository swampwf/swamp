/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Klaas Freitag <freitag@suse.de>
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
import de.suse.swamp.core.history.*;
import de.suse.swamp.util.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;

/**
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 *
 * This object provides the history of workflows.
 * All methods are static because Historymanager 
 * has no member variables. 
 */
public class HistoryManager {
    

    private HistoryManager() {
    }
        
    public static ArrayList getWorkflowHistory(int wfID) {
        ArrayList resList = null;
        resList = StorageManager.loadWorkflowHistory(wfID);
        return resList;
    }
    
    
    public static ArrayList getHistoryEntries(int wfID, int affectedId, String typeStart) {
        ArrayList resList = null;
        resList = StorageManager.loadHistoryEntries(wfID, affectedId, typeStart);
        return resList;
    }
    
    public static ArrayList getHistoryEntries(ArrayList affectedIds, String typeStart) {
        ArrayList resList = null;
        resList = StorageManager.loadHistoryEntries(affectedIds, typeStart);
        return resList;
    }
    
    
    
    public static ArrayList getHistoryEntries(int wfID, String typeStart) {
        ArrayList resList = null;
        resList = StorageManager.loadHistoryEntries(wfID, typeStart);
        return resList;
    }
    
    
    
    /**
     * This creates an entry into the workflow history table. 
     * @param type - a string defining the type of history entry. Note
     * that the id given in affectedID parameter must correspond to the
     * type. For example, if the type is TASK_WORKFLOWSTART, the affected ID
     * must point to the workflow table.
     *
     * @param affectedID - an foreign key to a table of affected wf parts.
     * @param wfID - WorkflowId
     * @param userId - UserId from SWAMP-UserStore Table. 
     * 0 means done by System, 
     * -1 means Unknown, and is called from Code where we don't have Users yet. 
     * FIXME: When we are done there shouldn't be entries with -1.
     */
    public static void create(String type, int affectedID, int wfID, 
            String userName, String data) throws StorageException {
        HashMap valid = HistoryEntry.getValidTypes();
        
        // check for valid type: 
        if (!valid.containsKey(type)) {
            Logger.BUG("Trying to write unknown history entry: " + type);
            throw new StorageException("Trying to write unknown history entry: " + type);
        } else if (affectedID <= 0) {
            Logger.BUG("HistoryEntry must contain an affectedID!");
            throw new StorageException("HistoryEntry must contain an affectedID!");
        }

        // check for valid user: 
        String histUser = "";
        if (!userName.equalsIgnoreCase(SWAMPUser.SYSTEMUSERNAME)){
            SWAMPUser user;
            try {
				user = SecurityManager.getUser(userName);
			} catch (UnknownElementException e) {
				throw new StorageException("Cannot store history-entry with " + 
                        " invalid user: " + userName);
			}
            if (user == null){
                throw new StorageException("Cannot store history-entry with " + 
                        " invalid user: " + userName);
            }
            histUser = user.getUserName();
        }
        StorageManager.createHistoryEntry(type, wfID, affectedID, histUser, data);
    }
    

    
}
