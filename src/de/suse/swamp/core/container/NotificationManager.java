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

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.util.*;


/**
 * The SWAMP NotificationManager notifies different 
 * persons about different changes in swamp
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt; 
 */
public final class NotificationManager {

    private NotificationManager() {
    } 

    
    public static Notification loadNotification(int id){
        return NotificationStorage.loadNotification(id);
    }
    
    
    /**
     * Method is called when a Notifytask becomes activated. 
     * (A Node with a notify-task gets activated)
     */
    public static void newNotifyTask(WorkflowTask task) throws Exception {
        Logger.DEBUG("New NotifyTask triggered, now creating notification");

        NotifyActionTemplate notifyaction = (NotifyActionTemplate) task.getActionTemplate();
        Notification notification = new Notification();
        notification.setRcMail(notifyaction.getEmail());
        notification.setDbit(notifyaction.getDbit());
        notification.setRcRole(notifyaction.getRecipientrole());
        notification.setTaskid(task.getId());
        notification.setWorkflowid(task.getWorkflowId());
        notification.setRcName(notifyaction.getRecipient());
        notification.setTemplateFile(notifyaction.getMsgtemplate());
        
        addNotification(notification);
    }

   

    /**
     * Method can be called from anywhere to send a Notification
     */
    public static void newNotification(int taskid, int wfid, String templatefile, 
            String dbit, String username, String email, String role)
            throws Exception {
        Logger.DEBUG("Creating new Notification");
        Notification notification = new Notification();
        notification.setRcName(username);
        notification.setRcMail(email);
        notification.setRcRole(role);
        notification.setDbit(dbit);
        notification.setTaskid(taskid);
        notification.setWorkflowid(wfid);
        notification.setTemplateFile(templatefile);

        try {
            addNotification(notification);
        } catch (Exception e) {
            Logger.ERROR(e.getMessage());
            throw new Exception(e.getMessage());
        }
    }
    
    
    
     /**
     * Method for sending a not Workflow or Task related Notification.
     */
    public static void newNotification(String templatefile, 
            String username, String email) throws Exception {
        Logger.DEBUG("Creating new Notification from " + templatefile);

        Notification notification = new Notification();
        notification.setRcName(username);
        notification.setRcMail(email);
        notification.setTemplateFile(templatefile);

        try {
            addNotification(notification);
        } catch (Exception e) {
            Logger.ERROR(e.getMessage());
            throw new Exception(e.getMessage());
        }
    }
    
    
    
    
    
    /**
     * Method is called when a new Workflow is created, to inform the owner, and
     * those who have requested an notify and maybe others.
     */
    /*
    public void newWorkflow(Workflow workflow) {

    }
    */



    /**
     * Method is called when a new Notification should be created in the
     * notifications List. 
     * Adds Userinfos + Prefs and the Content
     * 
     * @param Notification to add to Notification-List
     */
    public static void addNotification(Notification notification) throws Exception {
        notification.setCreated(new Date());
        // set the right userinfo for mailing to SWAMPUser, 
        // Username from Databit or to a plain MailAddress
        int userinforesult = 0;
        try {
            userinforesult = notification.setRecipients();
        } catch (Exception e1) {
                Logger.ERROR("Wf-" + notification.getWorkflowid() + 
                    ": Could not set recipients for Notification " + 
                    ">" + notification.getSubject());
            throw e1;
        }
        
        if (userinforesult > 0) {
            try {
                NotificationStorage.storeNotification(notification);
            } catch (Exception e) {
                Logger.ERROR(e.toString());
                throw new Exception(e.getMessage());
            }
            // only send notification asynchronous from scheduler
            //sendNotifies();
        }
    }
    
    
    
    
    /**
     * Iterate through Notify List and send them
     */
    public static synchronized void sendNotifies() throws Exception {
        // send notifies that are "mail" and "now" an !delivered
        //Logger.DEBUG("Sendnotifies() called, searching for Notifies to send... ");
        ArrayList notifies = NotificationStorage.loadUnsentNotifications();
        
        for (Iterator it = notifies.iterator(); it.hasNext(); ) {
            Notification notification = (Notification) it.next();
            if (notification.getNotifytype().equals("now") 
                    && notification.getDelivered() == null) {
                // send now
                if (notification.getNotifymethod().equals("mail")) {
                    Logger.DEBUG("Trying to send Notification with id="
                            + notification.getId());
                    try {
                        ArrayList to = new ArrayList();
                        // ArrayList cc = new ArrayList();
                        to.add(0, notification.getRcMail());
                        String subject = notification.getSubject();
                        String body = notification.getContent();

                        // add XHeaders
                        HashMap headers = new HashMap();
                        headers.put("X-SWAMP", notification.getXheader());

                        StringBuffer referencesString = new StringBuffer();
                        ArrayList references = NotificationStorage.loadMessageIDs(notification.getWorkflowid());
                        if (references != null && references.size() > 0) {
                            for (Iterator refit = references.iterator(); refit.hasNext();) {
                                referencesString.append(refit.next().toString()).append(" ");
                            }
                            headers.put("References", referencesString.toString());
                        }

                        String msgId = NotificationTools
                                .sendMail(to, headers, subject, body, notification.getReplyTo());
                        notification.setDelivered(new Date());
                        // save the automatically set Message-Id to have it as reference
                        notification.setMessageId(msgId);
                        NotificationStorage.storeNotification(notification);
                        HistoryManager.create("NOTIFICATION_SENT", notification.getId(), notification.getWorkflowid(),
                                SWAMPUser.SYSTEMUSERNAME, null);
                    } catch (Exception e) {
                        Logger.ERROR("Sending of Notification #" + notification.getId() + " failed, reason: "
                                + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    Logger.ERROR("Notifymethod: " + notification.getNotifymethod() + 
                            " not implemented yet.");
                }
            } else { 
                Logger.ERROR("Notifytype: " + notification.getNotifytype() + 
                        " not implemented yet.");
            }      
        }
    }

}

