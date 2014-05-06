/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh [at] suse.de>
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

package de.suse.swamp.core.workflow;

import java.util.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.util.*;

/**
 * Template for the creation of fields to be edited by the Dataedit
 * action.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 *
 */

public class Field {

    // This are the allowed enumvalues for that field
    private ArrayList enumvalues = new ArrayList();
    private String path;
    private boolean mandatory;
    private String initValue = "";
    private String label;
    private String datatype;
    private int state = Data.READWRITE;
    private DataEditInfo editInfo = null;
    
    
    
    public Field(String path, boolean mandatory) {
        this.path = path;
        this.mandatory = mandatory;
    }

    public String getPath() {
        return path;
    }
    

    public String getSetName() {
        int pathend = path.lastIndexOf(".");
        if (pathend > 0) {
            return path.substring(0, pathend);
        } else {
            Logger.ERROR("Please provide full-pathname for Field " + path);
            return path;
        }
    }

    
    public boolean isMandatory() {
        return mandatory;
    }


    /**
     * @return
     */
    public String getInitValue() {
        return initValue;
    }

    /**
     * @param string
     */
    public void setInitValue(String string) {
        initValue = string;
    }
    
    public SWAMPHashSet getInitValues() {
        SWAMPHashSet initValues = new SWAMPHashSet();
        if (initValue.equals("")) initValues.add(initValue);
        else initValues.add(initValue, ",");
        return initValues;
    }

    /**
     * @return the label to display for the field
     */
    public String getLabel() {
        if (label != null && label.length() > 0) {
            return label;
        } else {
            return path;
        }
    }

    /**
     * set the label of the field. The label is displayed as a 
     * descriptive string in front of the edit field.
     * 
     * @param string - the new label
     */
    public void setLabel(String string) {
        label = string;
    }


    /**
     * @param mandatory The mandatory to set.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return Returns the enumvalues.
     */
    public ArrayList getEnumvalues() {
        return enumvalues;
    }

    /**
     * @param enumvalues The enumvalues to set.
     */
    public void setEnumvalues(ArrayList enumvalues) {
        this.enumvalues = enumvalues;
    }
    /**
     * @return Returns the datatype.
     */
    public String getDatatype() {
        return datatype;
    }
    /**
     * @param datatype The datatype to set.
     */
    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    /**
     * @return Returns the state.
     */
    public int getState() {
        return state;
    }
    /**
     * @param state The state to set.
     */
    public void setState(int state) {
        this.state = state;
    }
    
    
    public String toString(){
        return "Field: " + this.getLabel() + " State: " + 
        Data.validStates.get(new Integer(this.getState())) + 
       " path: " + path + " value: " + initValue;
    }
    
    /**
     * @return Returns the editInfo.
     */
    public DataEditInfo getEditInfo() {
        return this.editInfo;
    }
    /**
     * @param editInfo The editInfo to set.
     */
    public void setEditInfo(DataEditInfo editInfo) {
        this.editInfo = editInfo;
    }


}