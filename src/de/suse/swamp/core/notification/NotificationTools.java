/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.notification;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.velocity.*;
import org.apache.velocity.app.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.history.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


/**
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt; 
 * Helper Methods for generating + sending Notifications
 */
public class NotificationTools {

    private NotificationTools() {

    }


    /**
     * Public Method to send a SWAMP generated Email
     * If MAILDEBUG is set, all mails will be redirected to POSTMASTER.
     * @param to
     * @param subject
     * @param body
     */
    public static String sendMail(List to, HashMap xheaders,
        String subject, String body, List replyTo) throws Exception {

        if (!SWAMP.getInstance().getProperty("MAILDEBUG").equalsIgnoreCase("false")){
            // for debugging set RcMail to postmaster
            Logger.DEBUG("Notification: Replacing " + to.toString() + " with " 
                    + SWAMP.getInstance().getProperty("POSTMASTER"));
            body = "original To: " + to.toString() + "\n\n" + body;
            to.clear();
            to.add(0, SWAMP.getInstance().getProperty("POSTMASTER")); 
        }
    	
        Properties props = System.getProperties();
        String smtpServer = SWAMP.getInstance().getProperty("MAILSERVER");
        
        String from = SWAMP.getInstance().getProperty("POSTMASTER");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.host", smtpServer);
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        // set timeouts
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.mime.charset", "UTF-8");
        
        // Attaching to default Session, or we could start a new one
        Session session = Session.getDefaultInstance(props);
        session.setDebug(false);
        // -- Create a new message --
        MimeMessage msg = new MimeMessage(session);
        // -- Set the FROM and TO fields --
        Address fromAddr = new InternetAddress(from, "SWAMP");
        msg.setFrom(fromAddr);
        
        if (replyTo != null){
            Address[] replyAddr = new InternetAddress[replyTo.size()];
            for (int i = 0; i < replyTo.size(); i++) {
               replyAddr[i] = new InternetAddress((String) replyTo.get(i));
            }
            msg.setReplyTo(replyAddr);
        }
        
        // adressen verify
        for (int i = 0; i < to.size(); i++) {
            InternetAddress[] toadd = InternetAddress.parse((String) to.get(i), false);
            if (toadd.length != 1) throw new Exception("Could not parse \"" + 
            		to.get(i) + "\" as mail adress");
            msg.addRecipient(Message.RecipientType.TO, toadd[0]);
        }
        // -- Set the subject and body text --
        msg.setSubject(subject, "UTF-8");
        msg.setText(body, "UTF-8");
        msg.setSentDate(new Date());

        // No CC Feature at the Moment
        // if (cc != null)
        // msg.setRecipients(Message.RecipientType.CC
        // ,InternetAddress.parse(cc, false));
        // -- Set some other header information --
        
        if (xheaders != null) {
            for (Iterator it = xheaders.keySet().iterator(); it.hasNext(); ){
             String key = (String) it.next();
                msg.setHeader(key, xheaders.get(key).toString());
            }
        }
        msg.setHeader("X-Mailer", "SWAMP Notification Mailer");
        // -- Send the message --
        Transport.send(msg);
        Logger.DEBUG("Notification Mail successfully sent");
        
        return msg.getHeader("Message-Id")[0];
    }

    
    
    /**
     * replaces velocity constructs in <i>text</i>
     * The Workflow instance is accessible by $wf in the text.
     */
    public static String workflowDataReplace(String text, Workflow wf) {
        VelocityContext context = new VelocityContext();
        context.put("wf", wf);
        context.put("wftemplate", wf.getTemplate());
        context.put("bugzilla_url", SWAMP.getInstance().getProperty("BUGZILLA_BROWSERURL"));
        context.put("jira_url", SWAMP.getInstance().getProperty("JIRA_BROWSERURL"));
        context.put("otrs_url", SWAMP.getInstance().getProperty("OTRS_BROWSERURL"));
        context = addLinksToContext(context);
        String ident = "WorkflowReplace";
        StringWriter w = new StringWriter();
        try {
            Velocity.evaluate(context, w, ident, text);
        } catch (Exception e) {
            Logger.ERROR("Could not replace WorkflowData ( wf#" + wf.getId() + " txt: " + 
                    text.substring(0, 10) + "...), Reason: " + e.getMessage());
            e.printStackTrace();
        }
        return w.toString();
    }
    
    
    
    /**
     * replaces velocity constructs in <i>text</i>
     * Accessible velocity variables are: 
     * $wf - the Workflow
     * $not - the notification Object itself
     * $task - the task the notification was caused by
     * $app_link - link to the webswamp server
     * $originator - who caused this message
     *
     * @param notification
     * @return
     */
    public static String notifyReplace(String text, Notification notification) throws Exception {

		WorkflowManager workflowmanager = WorkflowManager.getInstance();
		VelocityContext context = new VelocityContext();        
       
		// Task specific
		if (notification.getTaskid() > 0) {
			WorkflowTask task = null;
			try {
				task = TaskManager.getTask(notification.getTaskid());
				context.put("task", task);
                ArrayList hists = task.getActionTemplate().
                    getHistoryEntries(notification.getWorkflowid(), "");
                if (hists != null && hists.size() > 0){
                    HistoryEntry hist = (HistoryEntry) hists.get(hists.size()-1);
                    context.put("originator", hist.getUserName());
                }
			} catch (Exception e) {
				Logger.ERROR("Illegal Taskid: " + notification.getTaskid() + " in Notification");
			}
		} else {
		    context.put("originator", "<cannot get originator>");
		}

		// Wf- specific
		if (notification.getWorkflowid() > 0) {
			Workflow wf = workflowmanager.getWorkflow(notification.getWorkflowid());
			context.put("wf", wf);
            context.put("wftemplate", wf.getTemplate());
		}     
        context = addLinksToContext(context);
		context.put("not", notification);
		String ident = "NotificationReplace";
		StringWriter w = new StringWriter();

		try {
			Velocity.evaluate(context, w, ident, text);
		} catch (Exception e) {
			throw new Exception("Could not replace NotificationData( not# " + 
                    notification.getId() + "), Reason: " + e.getMessage());
		}
		return w.toString();
	}
 
    
    private static VelocityContext addLinksToContext(VelocityContext context){
        SWAMP swamp = SWAMP.getInstance();
        if (swamp.getProperty("secureAppLink") != null){
            String hoststring = swamp.getProperty("secureAppLink") + "/swamp";
            context.put("secure_app_link", hoststring);
            context.put("secure_webswamp_link", swamp.getProperty("secureAppLink"));
        }
        if (swamp.getProperty("appLink") != null) {
            String hoststring = swamp.getProperty("appLink") + "/swamp";
            context.put("app_link", hoststring);
            context.put("webswamp_link", swamp.getProperty("appLink"));
        }
        return context;
    }
    
    
    
    
    
    
    
}
