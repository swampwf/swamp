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

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * This Class is the representation of a Notification that is to be send 
 * at a user. This must not happen by Email, and may happen at a time specified 
 * by the User. 
 */
public class Notification implements Cloneable {

    private Date created;
    private Date delivered = null;
    private int id;
    // method may be mail, www, ...
    private String Notifymethod;
    // type is immediately, daily, ...
    private String Notifytype;
    private String dbit;
    private String RcMail;
    private String RcName;
    private String RcRole;
    private String templateFile;
    private int Taskid;
    private int Workflowid;
    private String content;
    private String subject;
    private String xheader;
    private String messageId;
    private List replyTo = new ArrayList();

    public static final String STDSUBJECT = "Message from SWAMP";
    public static final String STDXHEADER = "SWAMP Notification";
    


    /**
     * @return Returns the created.
     */
    public Date getCreated() {
        return created;
    }


    /**
     * @param created
     *                   The created to set.
     */
    public void setCreated(Date created) {
        this.created = created;
    }


    /**
     * @return Returns the delivered.
     */
    public Date getDelivered() {
        return delivered;
    }


    /**
     * @param delivered
     *                   The delivered to set.
     */
    public void setDelivered(Date delivered) {
        this.delivered = delivered;
    }


    /**
     * @param i - This ID should only be set from the StorageManager, 
     * and comes from the PrimaryKey of the Database.
     */
    public final void setId(final int i) {
        if (i > 0 && id == 0){
            this.id = i;
        } else if (i > 0 && id != 0){
            Logger.ERROR("Setting Notification-ID twice !!");
        } else {
            Logger.ERROR("Illegal setting of Notification-ID to " + i + " !!");
        }
    }
    
    /**
     * @return - the id that is representing the Object in the Database. 
     * This ID is unique for that type of Object
     */
    public final int getId() {
        return id;
    }


    /**
     * @return Returns the notifymethod.
     */
    public String getNotifymethod() {
        return Notifymethod;
    }


    /**
     * @param notifymethod
     *                   The notifymethod to set.
     */
    public void setNotifymethod(String notifymethod) {
        Notifymethod = notifymethod;
    }


    /**
     * @return Returns the notifytype.
     */
    public String getNotifytype() {
        return Notifytype;
    }


    /**
     * @param notifytype
     *                   The notifytype to set.
     */
    public void setNotifytype(String notifytype) {
        Notifytype = notifytype;
    }


    /**
     * @return Returns the rcMail.
     */
    public String getRcMail() {
        return RcMail;
    }


    /**
     * @param rcMail
     *                   The rcMail to set.
     */
    public void setRcMail(String rcMail) {
        RcMail = rcMail;
    }


    /**
     * @return Returns the rcName.
     */
    public String getRcName() {
        return RcName;
    }


    /**
     * @param rcName
     *                   The rcName to set.
     */
    public void setRcName(String rcName) {
        RcName = rcName;
    }


    /**
     * @return Returns the taskid.
     */
    public int getTaskid() {
        return Taskid;
    }


    /**
     * @param taskid The taskid to set.
     */
    public void setTaskid(int taskid) {
        Taskid = taskid;
    }


    /**
     * @return Returns the template.
     */
    public String getTemplateFile() {
        return templateFile;
    }


    /**
     * @param template The template to set.
     */
    public void setTemplateFile(String template) {
        templateFile = template;
    }


    /**
     * @return Returns the workflowid.
     */
    public int getWorkflowid() {
        return Workflowid;
    }


    /**
     * @param workflowid  The workflowid to set.
     */
    public void setWorkflowid(int workflowid) {
        Workflowid = workflowid;
    }


    /**
     * @return Returns the xheader.
     */
    public String getXheader() throws Exception {
    	prepareContentString();
    	if (xheader == null || xheader.equals("")){
    		return STDXHEADER;
    	}
        return xheader;
    }

    
    /**
     * @return Returns the dbit.
     */
    public String getDbit() {
        return dbit;
    }
    /**
     * @param dbit The dbit to set.
     */
    public void setDbit(String dbitpath) {
        this.dbit = dbitpath;
    }
    
    
    
    private void prepareContentString() throws Exception {
		if (this.content == null || this.subject == null || this.xheader == null) {
			long time = System.currentTimeMillis();
			String foundContent = new String();
			// Replace Text from Template file
			if (getTemplateFile() != null && !getTemplateFile().equals("")) {
				String path;
                 //open template file (take from workflow-specific path or general path)
                if (this.Workflowid > 0){
                    Workflow wf = WorkflowManager.getInstance().getWorkflow(Workflowid);
                    path = wf.getTemplate().getRessourcesPath();
                } else {
                    path = SWAMP.getInstance().getProperty("TEMPLATES_LOCATION");
                }
                
				File file = new File(path + System.getProperty("file.separator")
						+ getTemplateFile());
				foundContent = FileUtils.getText(file);
			} else {
				Logger.ERROR("You have to set either a Template File, or a Template String");
			}

            if (foundContent == null || foundContent.length() == 0){
                throw new Exception ("Could not generate content from template: " + getTemplateFile());                
            }

			// extract subject and xheader
			int index = foundContent.indexOf("subject=");
			int index2 = foundContent.indexOf("\n", index + 1);
			String foundSubject = new String();

			if (index >= 0) {
				foundSubject = foundContent.substring(index + 8, index2);
				foundContent = foundContent.substring(0, index)
						+ foundContent.substring(index2 + 1);
			}

			String foundXheader = new String();
			index = foundContent.indexOf("xheader=");
			index2 = foundContent.indexOf("\n", index + 1);

			if (index >= 0) {
				foundXheader = foundContent.substring(index + 8, index2);
				foundContent = foundContent.substring(0, index)
						+ foundContent.substring(index2 + 1);
			}
			
			this.content = NotificationTools.notifyReplace(foundContent, this);
			this.subject = NotificationTools.notifyReplace(foundSubject, this).replaceAll("[\r\n]", "");
			this.xheader = NotificationTools.notifyReplace(foundXheader, this).replaceAll("[\r\n]", "");

			Logger.DEBUG("prepareContentString() took "
					+ String.valueOf(System.currentTimeMillis() - time) + "ms");
		}
	}
    
    
    
    /**
	 * Adds the Content from the Template File (set in notification.template) to
	 * Notification Content. *
	 * 
	 * @param Notification
	 */
    public String getContent() throws Exception {
        prepareContentString();
        return content;
    }

    /**
     * Detect type of getting the recipient (databit, email, swampuser, role)
     * The referenced Databit may contain a comma-seperated List of 
     * swamp users, or comma-separated email-addresses.
     */
     public synchronized int setRecipients() throws Exception {
     
         WorkflowManager wfman = WorkflowManager.getInstance();
         SWAMPHashSet recipients = null;
         
         if (getRcRole() != null && !getRcRole().equals("") && this.Workflowid > 0) {
             Workflow wf = wfman.getWorkflow(this.Workflowid);
             Logger.DEBUG("Mailing to Users from Role: " + getRcRole()); 
             recipients = wf.getRole(getRcRole()).getMemberNames(wf);
         } else if (getDbit() != null && !getDbit().equals("") && this.Workflowid > 0) {
        	 Logger.DEBUG("Mailing to User from dbit: " + this.getDbit());
             Workflow wf = wfman.getWorkflow(this.Workflowid);
             if (wf.containsDatabit(this.getDbit())){
                 Databit dbit = wf.getDatabit(this.getDbit()); 
                 recipients = dbit.getValueAsList();
                 Logger.DEBUG("Found values: " + recipients);
             } else {
                 throw new Exception("Databit " + getDbit() + " does not exist "
                         + "in Wf-" + getWorkflowid());
             } 
         }
         
         if (recipients != null){
               for (Iterator it = recipients.iterator(); it.hasNext();) {
                    // generate new Notification Object:
                    Notification notificationclone = (Notification) this.clone();
                    notificationclone.setDbit(null);
                    notificationclone.setRcMail(null);
                    notificationclone.setRcName(null);
                    notificationclone.setRcRole(null);

                    String value = (String) it.next();
                    if (!value.equals("")) {
                        // User from Database?
                        if (value.indexOf('@') < 0) {
                            // assuming Database User
                            notificationclone.setRcName(value);
                        } else {
                            // assuming Email-Address
                            notificationclone.setRcMail(value);
                        }
                        NotificationManager.addNotification(notificationclone);
                    } else {
                        Logger.ERROR("Empty value in Recipient-Array "
                                + this.getDbit() + " detected");
                    }
                }
                return 0;
             
         } else if (getRcMail() != null && !getRcMail().equals("")) {
            Logger.DEBUG("Mailing to User from email: " + getRcMail());
            setNotifymethod("mail");
            setNotifytype("now");
        } else if (getRcName() != null && !getRcName().equals("")) {
             Logger.DEBUG("Mailing to User from SWAMPUser: " + getRcName()); 
             this.addUserInfo();
       
        } else { 
            Logger.ERROR("Could not determine Recipient data!");
        }
         
         Logger.DEBUG("Notification: Mailing to " + getRcMail());
         return 1;
     }
    
    
    

    /**
      * Adding Userinfo from SWAMP Users 
      * set Receiver EmailAdress, and pfreferences for method: mail/www...
      * and type: digest/urgent...
      * @throws Exception
      */
     private void addUserInfo() throws Exception {
                 
 	        SWAMPUser recp;
             try {
                 recp = SecurityManager.getUser(getRcName());
             } catch (Exception e) {
                 e.printStackTrace();
                 Logger.ERROR("Couldn't get user: " + getRcName());
                 throw new Exception(e);
             }
             
             setRcMail(recp.getEmail());
 	        
 	        // TODO: Add userprefs here

 	        Logger.DEBUG("Added Recp. Email " + getRcMail());
 	        
 	        // if the user has no userprefs, set to standard, mail - now.
 	        // only mail implemented. 
 	        if (recp.getPerm("Notifymethod") == "mail")
 	            setNotifymethod("mail");
 	        else 
 	            setNotifymethod("mail");
 	        
 	        if (recp.getPerm("Notifytype") == "now")
 	            setNotifytype("now");
 	        else if (recp.getPerm("Notifytype") == "digest")
 	            setNotifytype("digest");
 	        else 
 	            setNotifytype("now"); 
     }
    
    
     
    /**
     * @return Returns the subject.
     */
    public String getSubject() throws Exception {
    	prepareContentString();
    	if (subject == null || subject.equals("")){
    		return STDSUBJECT;
    	}
    	if (subject.length() > 100) subject = subject.substring(0, 100) + "...";
        return subject;
    }

    

    /**
     * @return Returns the rcRole.
     */
    public String getRcRole() {
        return this.RcRole;
    }
    /**
     * @param rcRole The rcRole to set.
     */
    public void setRcRole(String rcRole) {
        this.RcRole = rcRole;
    }
    
    
    /* Clone the Notification, but don't copy references
     */
    public Object clone(){
        Notification not = new Notification();
        not.setCreated(new Date());
        if (this.getDbit() != null ) not.setDbit(new String(this.getDbit()));
        if (this.getNotifymethod() != null ) not.setNotifymethod(new String(this.getNotifymethod()));
        if (this.getNotifytype() != null ) not.setNotifytype(new String(this.getNotifytype()));
        if (this.getRcMail() != null ) not.setRcMail(new String(this.getRcMail()));
        if (this.getRcName() != null ) not.setRcName(new String(this.getRcName()));
        if (this.getRcRole() != null ) not.setRcRole(new String(this.getRcRole()));
        not.setTaskid(this.getTaskid());
        if (this.getTemplateFile() != null ) not.setTemplateFile(new String(this.getTemplateFile()));
        not.setWorkflowid(this.getWorkflowid());       
        return not;
    }
    
    
    /**
     * @return Returns the messageId.
     */
    public String getMessageId() {
        return this.messageId;
    }
    /**
     * @param messageId The messageId to set.
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }


    public List getReplyTo() {
        return replyTo;
    }


    public void addReplyTo(String replyTo) {
        this.replyTo.add(replyTo);
    }

    public void setReplyTo(List replyTo) {
        this.replyTo = replyTo;
    }
}
