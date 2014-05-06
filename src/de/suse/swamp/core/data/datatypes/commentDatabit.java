/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt
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

package de.suse.swamp.core.data.datatypes;

import java.text.*;
import java.util.*;

import org.apache.regexp.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * This is the next (empty field) comment of a thread
 *
 * @author Thomas Schmidt
 *
 */

public class commentDatabit extends Databit {
    
    private String type = "comment";
    private String commentAuthor; 
    private Date commentDate;
    

    public commentDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state.intValue());
        setModified(true);
    }


    /**
     * set a value in a databit.
     * @param v - the new value 
     * @param uname - username who did the change 
     * @param force - Force writing of value, even if it is the same
     * @return boolean that indicates if the value was to change. 
     *         If no real change happend, this method returns false.
     * @throws Exception when the given value does not fit the Databits Datatype
     */
    public boolean setValue(String v, boolean force, String uname, ResultList results) throws Exception {
        // only set author + date for new comment: 
    	if (this.getType().equals("comment") && !v.equals(value)) {
    		if (commentAuthor == null) setCommentAuthor(uname);
    		if (commentDate == null) setCommentDate(new Date());
    	}
        // we have to send the DATACHANGED event after the reply was added (for change listeners)
    	boolean ret = super.setValue(v, force, uname, results, false);
        if (ret && getType().equals("comment")) makeComment(uname);
        if (ret) sendEvent(uname, results);
        return ret;
    }
    
    
    /**
     * Checks if the given value v fits this Databit's Type.
     * 
     * @param v
     */
    public String checkDataType(String v) throws ParseException {
        // empty values are allowed
        if (!v.equals("") && !checkedValues.contains(v)) {
            
            // perform basic type checking
            
            // value is valid
            checkedValues.add(v);
        }
		return v;
    }
    

    
    public Field getField(String pathPrefix) {
        String path = name;
        if (!(pathPrefix == null || pathPrefix.equals(""))) {
            path = pathPrefix + "." + name;
        } else {
            path = name;
            Logger.DEBUG("No path prefix for field " + name);
        }
        // Create a field
        Field field = new Field(path, false);
        field.setState(this.state);
        field.setInitValue(value);
        field.setDatatype(getType());
        field.setEditInfo(this.editInfo);
        if (this.getCommentAuthor() != null && 
        		!this.getCommentAuthor().equals(SWAMPUser.SYSTEMUSERNAME) && 
        		this.getCommentDate() != null){ 
        	SimpleDateFormat df = new SimpleDateFormat(datetimeDatabit.dateTimeFormat);
        	field.setLabel(this.getCommentAuthor() + i18n.tr(" at ") + 
        			df.format(this.getCommentDate()));
        } else {
        	field.setLabel(this.getDescription());
        }
        return field;
    }
    

    
    /**
     * Get a field-list of all comments in this thread. 
     * Must be called on the first comment of the thread
     */
    public List getThreadFields(String pathPrefix, boolean replyIsMandatory){
        
        if (!getInitialCommentName().equals(getName())) {
            Logger.ERROR("Called getThreadFields() on wrong field! " + getName()); 
        }
        ArrayList thread = new ArrayList();
        // thread comments are named <dbitname>, <dbitname>1, <dbitname>2, <dbitname>3        
        Field thisfield = new Field("test", false);
        for (Iterator it = this.getDataset().getDatabits().iterator(); it.hasNext(); ){
            Databit bit = (Databit) it.next();
            if (bit.getName().startsWith(getName())){
                thisfield = bit.getField(pathPrefix);
                thread.add(thisfield);
            }
        }
        // make the last field read-write: 
        thisfield.setState(Data.READWRITE);        
        thisfield.setMandatory(replyIsMandatory);
        return thread;
    }   
    
    
	public String getType() {
		return type;
	}

	
    /**
     * This will add a new comment databit with the suffix _XX (comment number)
     * and change the type of this databit to "thread"
     */
    private void makeComment(String uname) throws Exception {
        // generate reply-to name and check if already there: 
        String replyName = getNextCommentName();
        setCommentAuthor(uname);
        setCommentDate(new Date());
        setDescription("");
        this.type = "thread";
        this.setModified(true);
        this.store();
        
        // add reply to field
        Dataset dset = DataManager.loadDataset(this.dSetId);
        if (!dset.containsDatabit(replyName)){
        	SWAMPUser user = SecurityManager.getUser(uname);
            Databit reply = DataManager.createDatabit(replyName, i18n.tr("Reply: ", user), 
            		"comment", "", Data.READWRITE);
            // copy editinfo from first comment: 
            if (this.getEditInfo() != null){
            	DataEditInfo newEditInfo = new DataEditInfo(
            			editInfo.getType(), editInfo.getXsize(), editInfo.getYsize());
            	reply.setEditInfo(newEditInfo);
            }
            dset.addDatabit(reply);
        }
    }
    
    
    private String getNextCommentName(){
        //if the regexp does not match, this is the initial comment, else the first paren: 
        RE regexp =  new RE("^([a-zA-Z_\\-0-9]+?)(\\d*)$");
        String nextComment; 
        if (!regexp.match(getName()) || regexp.getParen(2) == null || regexp.getParen(2).equals("")) {
            nextComment = getName() + "1";
        } else {
            int nextNumber = new Integer(regexp.getParen(2)).intValue() + 1;
            nextComment = regexp.getParen(1) + nextNumber;
        }
        Logger.DEBUG("Next comment for " + getName() + " is " + nextComment);
        return nextComment;
    }
    
    
    public String getInitialCommentName(){
        //if the regexp does not match, this is the initial comment, else the first paren: 
        RE regexp =  new RE("^([a-zA-Z_\\-0-9]+?)(\\d*)$");
        String initialComment; 
        if (!regexp.match(getName())) {
            initialComment = getName();
        } else {
            initialComment = regexp.getParen(1);
        }
        Logger.DEBUG("Initial comment for " + getName() + " is " + initialComment);
        return initialComment;
    }
    
    
	public String getCommentAuthor() {
		return commentAuthor;
	}


	public Date getCommentDate() {
		return commentDate;
	}


	public void setCommentAuthor(String commentAuthor) {
		this.commentAuthor = commentAuthor;
	}


	public void setCommentDate(Date commentDate) {
		this.commentDate = commentDate;
	}
	
}
