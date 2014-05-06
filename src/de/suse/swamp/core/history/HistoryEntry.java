/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Klaas Freitag <freitag [at] suse.de>
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

package de.suse.swamp.core.history;

import java.text.*;
import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;
import de.suse.swamp.core.security.*;

/**
 * @author Klaas Freitag &lt;freitag@suse.de&gt; 
 * 
 */
public class HistoryEntry {

    
    private int id;
    // db-id of affected item, workflow, task...
    private int itemID;
    private String type;
    private String userName;
    private Date when;
    // additional data for this historyentry. 
    // for DATA_ it's the old field value
    private String data;
    
    
    
    public HistoryEntry(int id, int itemID, String type, String userName, 
            Date date, String data) {
        this.id = id;
        this.itemID = itemID;
        this.type = type;
        this.userName = userName;
        this.when = new Date(date.getTime());
        this.data = data;
        
        if (!validTypes.containsKey(type)){
            Logger.ERROR("Tried to write unsupported History Entry Type " + type);
        }
    }

    
    public static HashMap getValidTypes(){
        return validTypes;
    }
   
    /* static constructor to fill the Hash of valid history types. */
    private final static HashMap validTypes;
    static {
        validTypes = new HashMap();
        validTypes.put("NODE_ENTER", "Node %name was entered");
        validTypes.put("NODE_LEAVE", "Node %name was left");
        validTypes.put("WORKFLOW_STARTED", "Workflow %name started");
        validTypes.put("TASK_WORKFLOWSTART", "Workflow %name started");
        validTypes.put("TASK_WORKFLOWCLOSE", "Workflow %name ended");
        validTypes.put("TASK_START", "User task %name activated");
        validTypes.put("TASK_DONE", "User task %name was finished");
        validTypes.put("TASK_CANCELED", "User task %name was canceled");
        validTypes.put("TASK_REMOVED", "User task %name was removed");
        validTypes.put("SYSTEMTASK_START", "System task %name activated");
        validTypes.put("SYSTEMTASK_DONE", "System task %name was finished");
        validTypes.put("SYSTEMTASK_CANCELED", "System task %name was canceled");
        validTypes.put("SYSTEMTASK_REMOVED", "System task %name was removed");
        validTypes.put("EVENT_SENT", "Event %name was sent from workflow #");
        validTypes.put("EVENT_RECEIVE", "Event %name was received from workflow #");
        validTypes.put("EVENT_HANDLED", "Event %name from workflow # was handled");
        validTypes.put("NOTIFICATION_SENT", "Notification was sent to %name");
        validTypes.put(Event.DATACHANGED, "%name was changed to: %value");
    }

    
    /*
     * Methods to get the contents off the History entries *
     */
    public String getWho() {
        SWAMPUser user = null;
        if (userName != null && !userName.equals("")) {
            try {
                user = SecurityManager.getUser(this.userName);
            } catch (Exception e) {
                Logger.ERROR("HistoryEntry: Unable to get User from username: " + 
                        this.userName);
                return "ERROR: Unable to get Username for login '" + userName + "'";
            }
            return user.getFirstName() + " " + user.getLastName();
        } else {
            return SWAMPUser.SYSTEMUSERNAME;
        }

    }

    
    public String getWhat(int wfid) {
        String what = "";
        try {
            what = (String) validTypes.get(type);
        } catch (RuntimeException e) {
            Logger.ERROR("Unknown History Type: " + type + "!");
        }
            
        Workflow wf = WorkflowManager.getInstance().getWorkflow(wfid);
        if (type.startsWith("TASK_WORKFLOW")) {
            String wfName = wf.getName();
            what = what.replaceAll("%name", wfName);
        } else if (type.startsWith("NODE")) {
            Node n = wf.getNode(itemID);
            if (n != null) {
                what = what.replaceAll("%name", n.getName());
            }
        } else if (type.startsWith("TASK") || type.startsWith("SYSTEMTASK")) {
            WorkflowTask t = null;
            try {
                t = TaskManager.getTask(itemID);
            } catch (Exception e1) {
                Logger.ERROR("History: Couldn't load Task: " + itemID );
                e1.printStackTrace();
            }
            if (t != null) {
                String desc = t.getReplacedDescription();
                if (desc == null || desc.length() == 0){
                    desc = t.getActionTemplate().getName();
                }
                what = what.replaceAll("%name", desc);
            } else {
                Logger.ERROR("Could not get Task #" + itemID + " for History");
            }
        } else if (type.startsWith("EVENT")) {
            Event event = EventManager.loadEventFromHistory(itemID);
            what = what.replaceAll("%name", event.getType());
            if (event.getSenderWfId() != wfid && event.getSenderWfId() > 0) {
                what = what.replaceAll("#", "#" + event.getSenderWfId() );
            } else {
                what = what.replaceAll(" from workflow #", "");
            }
                
        } else if (type.startsWith("NOTIFICATION")) {
            Notification notification = NotificationManager.loadNotification(itemID);
            what = what.replaceAll("%name", notification.getRcMail());
            
        } else if (type.startsWith(Event.DATACHANGED)) {
            Databit dbit = wf.getDatabit(this.itemID);
            what = what.replaceAll("%name", dbit.getName());
            what = what.replaceAll("%value", this.data);
        } else {
            Logger.ERROR("History: Undefined Replacement-rules for: " + type);
            what = "Undefined Replacement-rules for " + type;
        }

        return what;
    }

    
    
    /**
     * TODO
     * @return possible Actions that may be done with this Historyentry, e.g.: 
     * Go back to that state...
     */
    public String getAction() {
        return "";
    }
    
    
    public Date getWhen() {
        return new Date(when.getTime());
    }

    
    public String getWhenString() {
        DateFormat df1 = new SimpleDateFormat(datetimeDatabit.dateTimeFormat);
        return df1.format(when);
    }
    
    
    public String getHistoryID() {
        return String.valueOf(id);
    }
    
    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return Returns the itemID.
     */
    public int getItemID() {
        return itemID;
    }
    

    /**
     * @return Returns the userName.
     */
    public String getUserName() {
        return userName;
    }
}
