/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Tschmidt tschmidt@suse.de
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

package de.suse.swamp.core.actions;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class NotifyActionTemplate extends SystemActionTemplate {

    
    private String msgtemplate;
    private String msgtext;
    private String xreason;
    private String recipient;
    private String email;
    private String dbit;
    private String recipientrole;
    private String subject;
    
    
    public NotifyActionTemplate(String name, String msgtemplate, 
                    String msgtext, String xreason, String recipient, 
                    String email, String dbit, String recipientrole, 
                    String subject, NodeTemplate nodeTemplate) {
		super(name, nodeTemplate);
        this.msgtemplate = msgtemplate;
        this.msgtext = msgtext;
        this.xreason = xreason;
        this.recipient = recipient;
        this.email = email;
        this.dbit = dbit;
        this.recipientrole = recipientrole;
        this.subject = subject;
    }

	 
    public void act(Result result) {
        act(result, new ResultList());
    }
    
    /**
     * is called when a node with a notifyaction is entered
     */
    public void act(Result result, ResultList history) {
        try {
            WorkflowTask task = ((NotifyActionResult) result).getTask();
            Logger.DEBUG("Acting Notify-Task + " + task.getId());
            NotificationManager.newNotifyTask(task);
            Workflow wf = WorkflowManager.getInstance().getWorkflow(result.getWorkflowId());
            
            String debug = "";
            if (!SWAMP.getInstance().getProperty("MAILDEBUG").equalsIgnoreCase("false")){
            	debug = " (debug redirect to: " + SWAMP.getInstance().getProperty("POSTMASTER") + ")";
            }
            
            String hist = "Notification (" + getMsgtemplate().replaceAll("notifications/", "") + ") has been queued for: ";
            if (recipient != null && !recipient.equals("")){
                hist += recipient;
                history.addResult(ResultList.MESSAGE, hist + debug);
            } else if (recipientrole != null && !recipientrole.equals("")){
                String val = wf.getRole(recipientrole).getMemberNames(wf).toString(", ");
                if (val != null && !val.equals("")){
                    hist += val + " (role " + recipientrole + ")"; 
                    history.addResult(ResultList.MESSAGE, hist + debug);
                }
            } else if (dbit != null && !dbit.equals("")){
                String val = wf.getDatabitValue(dbit);
                if (val != null && !val.equals("")){
                    hist += val; 
                    history.addResult(ResultList.MESSAGE, hist + debug);
                }
            } else if (email != null && !email.equals("")){
                hist += email;
                history.addResult(ResultList.MESSAGE, hist + debug);
            } else {
                Logger.ERROR("Unable to determine Notification target for " + this.getName());
            }
        } catch (Exception e) {
        	history.addResult(ResultList.ERROR, "Notification not send: " + e.getMessage());
        }
    }

    
    /**
     * @return an ArrayList with error strings, empty if everything went fine
     */
    public ArrayList validate(Result result) {
        // TODO: SystemTasks aren't validated 
        return new ArrayList();
    }


    
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        return events;
    }

    
    public String getType() {
        return "NotifyTaskAction";
    }


    /**
     * @return Returns the email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return Returns the msgtemplate.
     */
    public String getMsgtemplate() {
        return msgtemplate;
    }

    /**
     * @return Returns the msgtext.
     */
    public String getMsgtext() {
        return msgtext;
    }

    /**
     * @return Returns the recipient.
     */
    public String getRecipient() {
        return recipient;
    }

    /**
     * @return Returns the xreason.
     */
    public String getXreason() {
        return xreason;
    }

    /**
     * @return Returns the dbit.
     */
    public String getDbit() {
        return dbit;
    }

    /**
     * @return Returns the subject.
     */
    public String getSubject() {
        return subject;
    }
    /**
     * @return Returns the recipientrole.
     */
    public String getRecipientrole() {
        return recipientrole;
    }
}