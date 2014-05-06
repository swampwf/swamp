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

import de.suse.swamp.core.notification.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

/**
 * This is the Storage-Manager for everything that needs
 * to be stored from the NotificationManager.
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
 *
 * @version $Id$
 */

public final class NotificationStorage {

    // extra logger for storage stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.container.Storage");

	/**
	 * @return - list of Notifications that have not yet bneen sent successfully
	 */
	public static ArrayList loadUnsentNotifications() throws TorqueException {
        ArrayList notifications = new ArrayList();
        Criteria crit = new Criteria();
        Notification notification = null;
        List dbnotifies = null;
        crit.add(DbnotificationsPeer.DELIVERED, null);
        try {
            dbnotifies = DbnotificationsPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.BUG("Could not select Notifications, Exception: " + e, log);
            throw e;
        }
        
        for (Iterator it = dbnotifies.iterator(); it.hasNext();) {
            Dbnotifications dbnotification = (Dbnotifications) it.next();
            notification = new Notification();
            notification.setCreated(dbnotification.getCreated());
            notification.setId(dbnotification.getId());
            notification.setNotifymethod(dbnotification.getNotifymethod());
            notification.setNotifytype(dbnotification.getNotifytype());
            notification.setRcMail(dbnotification.getRcMail());
            notification.setRcName(dbnotification.getRcName());
            notification.setReplyTo(new SWAMPHashSet(dbnotification.getReplyto(), ",").toList());
            notification.setTaskid(dbnotification.getTaskid());
            notification.setTemplateFile(dbnotification.getTemplatefile());
            notification.setWorkflowid(dbnotification.getWorkflowid());
            notifications.add(notification);
        }
        return notifications;
    }


    /**
     * Loads a Notification from the Database
     *
     * @param id -
     *            NotificationID
     * @return - the Notification Object
     */
    static Notification loadNotification(int id) {

        Criteria crit = new Criteria();
        Notification notification = null;
        ArrayList dbnotifies = null;

        try {
            crit.add(DbnotificationsPeer.ID, id);
            dbnotifies = (ArrayList) DbnotificationsPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.BUG("Could not select Notification, Exception: " + e, log);
        }

        if (dbnotifies.size() != 1) {
            Logger.ERROR("!=1 Notifications with id: " + id, log);
        }
        Dbnotifications dbnot = (Dbnotifications) dbnotifies.get(0);
        notification = new Notification();
        notification.setCreated(dbnot.getCreated());
        notification.setDelivered(dbnot.getDelivered());
        notification.setId(dbnot.getId());
        notification.setNotifymethod(dbnot.getNotifymethod());
        notification.setNotifytype(dbnot.getNotifytype());
        notification.setRcMail(dbnot.getRcMail());
        notification.setRcName(dbnot.getRcName());
        notification.setReplyTo(new SWAMPHashSet(dbnot.getReplyto(), ",").toList());
        notification.setTemplateFile(dbnot.getTemplatefile());
        notification.setTaskid(dbnot.getTaskid());
        notification.setWorkflowid(dbnot.getWorkflowid());
        notification.setMessageId(dbnot.getMessageid());
        return notification;
    }


    /**
     * storage of Notifies
     *
     * @param notification
     */
    static void storeNotification(Notification notification) {
    	if (notification.getRcMail() == null ||
    			notification.getRcMail().equals("")){
    		Logger.WARN("Skipping storage of notification without email + " +
    				"(username: " + notification.getRcName() + ")", log);
    		return;
    	}

        Criteria ncrit = new Criteria();
        ncrit.add(DbnotificationsPeer.CREATED, notification.getCreated());
        ncrit.add(DbnotificationsPeer.DELIVERED, notification.getDelivered());
        ncrit.add(DbnotificationsPeer.NOTIFYTYPE, notification.getNotifytype());
        ncrit.add(DbnotificationsPeer.NOTIFYMETHOD, notification.getNotifymethod());
        ncrit.add(DbnotificationsPeer.RC_MAIL, notification.getRcMail());
        ncrit.add(DbnotificationsPeer.RC_NAME, notification.getRcName());
        ncrit.add(DbnotificationsPeer.REPLYTO, new SWAMPHashSet(notification.getReplyTo()).toString(","));
        ncrit.add(DbnotificationsPeer.TEMPLATEFILE, notification.getTemplateFile());
        if (notification.getTaskid() > 0) {
			ncrit.add(DbnotificationsPeer.TASKID, notification.getTaskid());
		}
        if (notification.getWorkflowid() > 0) {
			ncrit.add(DbnotificationsPeer.WORKFLOWID, notification.getWorkflowid());
		}
        ncrit.add(DbnotificationsPeer.MESSAGEID, notification.getMessageId());
        if (notification.getId() > 0) {
            // update required
            try {
                ncrit.add(DbnotificationsPeer.ID, notification.getId());
                DbnotificationsPeer.doUpdate(ncrit);
                Logger.DEBUG("Notification with id " + notification.getId() +
                		" was updated", log);
            } catch (TorqueException e) {
                Logger.ERROR("Could not update notification: " + e.getMessage(), log);
            }
        } else {
            // We need to insert.
            try {
                NumberKey key = (NumberKey) DbnotificationsPeer.doInsert(ncrit);
                notification.setId(key.intValue());
                Logger.DEBUG("Notification with id " + notification.getId() + " inserted", log);
            } catch (TorqueException e) {
                Logger.ERROR("Could not update Notifications: " + e.getMessage(), log);
            }
        }
    }


	/**
	 * Loads the last 50 messageids of mails previously send from this workflow
	 */
    static ArrayList loadMessageIDs(int wfId) {
		ArrayList messageIds = new ArrayList();
		Criteria crit = new Criteria();
		ArrayList dbmessageIDs = null;
		try {
			crit.add(DbnotificationsPeer.WORKFLOWID, wfId);
			crit.addDescendingOrderByColumn(DbnotificationsPeer.ID);
			crit.setLimit(50);
			dbmessageIDs = (ArrayList) DbnotificationsPeer.doSelect(crit);
			for (Iterator it = dbmessageIDs.iterator(); it.hasNext();) {
				Dbnotifications dbnot = (Dbnotifications) it.next();
				if (dbnot.getMessageid() != null) {
					messageIds.add(dbnot.getMessageid());
				}
			}
		} catch (TorqueException e) {
			Logger.BUG("Could not select dbmessageIDs, Exception: " + e, log);
		}
		return messageIds;
	}


}
