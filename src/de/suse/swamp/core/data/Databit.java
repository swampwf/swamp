/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Klaas Freitag <freitag@suse.de>
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

package de.suse.swamp.core.data;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * The Databit
 *
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 * @author Thomas Schmidt
 *
 */

public abstract class Databit extends Data implements Describable {

    
    protected int dSetId;
    protected String value = "";
    protected DataEditInfo editInfo = null;

    //  save a list of already checked and valid values
    protected ArrayList checkedValues = new ArrayList();    
    

    public Databit(String name, String desc, String value, int state) throws Exception {
        super (name, desc, state);
        this.value = value;
        setModified(true);
    }
    
    
    public boolean setValue(String v, boolean force, String uname) throws Exception {
        return setValue(v, force, uname, new ResultList(), true);
    }
    
    public boolean setValue(String v, boolean force, String uname, ResultList results) throws Exception {
        return setValue(v, force, uname, results, true);
    }
    
    /**
     * set a value in a databit. This will automatically store it to the db.
     * @param v - the new value 
     * @param uname - username who did the change 
     * @param force - Force writing of value, even if it is the same
     * @return boolean that indicates if the value was to change. 
     *         If no real change happend, this method returns false.
     * @throws Exception when the given value does not fit the Databits Datatype
     */
    public boolean setValue(String v, boolean force, String uname, ResultList results, boolean sendEvent) throws Exception {
        boolean ret = false;
        if (v == null) Logger.ERROR("Tried to assign null to " + this.getName());
        if (value != null && (!value.equals(v) || force)) {
            try {
               v = checkDataType(v);
            } catch (Exception e) {
                Logger.ERROR("Couldn't set Databit " + this.name + " to " + v 
                		+ ": Cause: " + e.getMessage());
                throw new Exception("Couldn't set Databit " + this.name + 
                        ": \nCause: " + e.getMessage());
            }
            String oldval = new String(value);
            this.value = v;
            Logger.DEBUG("Set Databit " + this.name + " to " + v);
            setModified(true);
            checkedValues.clear();
            DataManager.storeDataBit(this, uname);
            if (!oldval.equals(v)){
                ret = true;
                if (sendEvent) {
                    sendEvent(uname, results);
                }
            }
        }
        return ret;
    }
    
    
    /**
     * Send the DATA_CHANGED event to all workflows that attach this databit
     */
    protected void sendEvent(String uname, ResultList results) throws Exception {
        HistoryManager.create(Event.DATACHANGED, this.getId(), 0, uname, value);
        List ids = DataManager.loadWorkflowIdsForDataset(this.getDSetId());
        Logger.DEBUG("Databit: " + this.getName() + " sending event " + Event.DATACHANGED + " to: " + ids);
        String fieldName = getName();
        // if this is part of a thread, the listeners will listen on the initial comment
        if (getType().equals("comment") || getType().equals("thread"))
            fieldName = ((commentDatabit) this).getInitialCommentName();
        
        for (Iterator it = ids.iterator(); it.hasNext(); ){
            int targetId = ((Integer) it.next()).intValue();
            EventManager.handleWorkflowEvent(new DataChangedEvent(Event.DATACHANGED, fieldName, 0, targetId), uname, results);
        }
    }
        
    
    public boolean setValue(String v, String uname) throws Exception {
        return setValue(v, false, uname);
    }
    
    /**
     * set the databits value and persists it to the database.
     */
    public boolean setValue(String v) throws Exception {
        return setValue(v, SWAMPUser.SYSTEMUSERNAME);
    }
        
    
    public String getValue() {
        return value;
    }
    
    
    /**
     * Checks if the given value v fits this Databit's Type.
     * 
     * @param v
     */
    public abstract String checkDataType(String v) throws Exception;
    
    
    /**
     * @return - An ArrayList of the comma-seperated values 
     */
    public SWAMPHashSet getValueAsList() {
        return new SWAMPHashSet(value, ",");
    }
    
    
    /**
     * Add one or more values to the databit
     */
    public void addValue(String newValue, String uname) throws Exception {
    	SWAMPHashSet values = getValueAsList();
        SWAMPHashSet newValues = new SWAMPHashSet(newValue, ",");
        values.addAll(newValues);
    	setValue(values.toString(", "), uname);
    }
    
    public void addValue(String newValue) throws Exception {
        addValue(newValue, SWAMPUser.SYSTEMUSERNAME);
    }
    
    public String getName() {
        return name;
    }

    
    /**
     * Returns a String representing the Databit object
     * 
     * @return a multiline String showing Databit data.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\n[Databit ").append(name).append(" id=").append(getId());
        buf.append("\nvalue= ").append(value).append(", type=").append(getType()).
            append("\nDesc.: ").append(description).append(" state=").append(state);
        buf.append("]");
        return buf.toString();
    }

    
    /**
     * Returns a XML representing the Databit object
     * 
     * @return a multiline XML showing Databit data.
     */
    public String toXML() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n<Databit name=\"" + name + "\" desc=\"" + description + 
                "\" type=\"" + getType() + "\" value=\"" + value + " \" " + 
                "state=\"" + state + "\" />");
        return new String(sb);
    }


    /**
     * @return the associated edit info
     */
    public DataEditInfo getEditInfo() {
        return editInfo;
    }

    /**
     * set the data edit info.
     * 
     * @param info 
     */
    public void setEditInfo(DataEditInfo info) {
        editInfo = info;
        setModified(true);
    }

    
    /** 
     * Get the field for this databit. 
     * Overwrite in sub-classes for special stuff like 
     * enumerations, comment-threads...
     */
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
        field.setLabel(this.getDescription());
        return field;
    }
    
    /**
     * @return Returns the dSetId.
     */
    public int getDSetId() {
        return this.dSetId;
    }
    
    /**
     * @param setId The dSetId to set.
     */
    public void setDSetId(int setId) {
        this.dSetId = setId;
        setModified(true);
    }
    
    
    public void store(){
        DataManager.storeDataBit(this, SWAMPUser.SYSTEMUSERNAME);
    }
    
    protected Dataset getDataset(){
        return DataManager.loadDataset(dSetId);
    }

    
	public String getType() {
		String type = this.getClass().getName();
		type = type.substring(0, this.getClass().getName().indexOf("Databit"));
		type = type.substring(type.lastIndexOf('.') + 1);
		return type;
	}
    
}
