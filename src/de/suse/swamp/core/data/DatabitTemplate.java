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
import de.suse.swamp.core.workflow.*;

/**
 * The Databit
 *
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 *
 */

public class DatabitTemplate extends Data implements ExtDescribable {


    private ArrayList enumvalues = new ArrayList();
	// datatype, for example: number, boolean, person, see @Databit.java
    private String type = "";
    private String dfltValue = "";
    private String shortDescription;
    private String longDescription; 
    
    private DataEditInfoTemplate editInfo = null;
	
	
	
	public DatabitTemplate(String name, String desc, String type, int state) {
        super(name, desc, state);
        this.type = type;
    }

    
    public Databit getDatabit() {
        Databit newdb = DataManager.createDatabit(name, description, type, dfltValue, state);
        if (editInfo != null) {
            newdb.setEditInfo(editInfo.getDataEditInfo());
        }
        if (newdb != null && newdb instanceof enumDatabit){
        	((enumDatabit) newdb).setEnumvalues(enumvalues);
        }
        return newdb;
    }


    public void setType(String t) {
        type = t;
    }


    public void setDefaultValue(String d) {
        dfltValue = d;
    }
    
    public String getDefaultValue() {
        return dfltValue;
    }

    
     public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("DatabitTemplate: ");
		buf.append(name);
		return buf.toString();
	}
     
    /**
	 * @return
	 */
    public DataEditInfoTemplate getEditInfo() {
        return editInfo;
    }

    /**
     * @param info
     */
    public void setEditInfoTemplate(DataEditInfoTemplate info) {
        editInfo = info;
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

	public String getType() {
		return type;
	}
    /**
     * @param shortDescription The shortDescription to set.
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
    
    public boolean hasShortDescription() {
        return this.shortDescription == null ? false : true;
    } 
    
    /**
     * @return Returns the shortDescription.
     */
    public String getShortDescription() {
        return shortDescription;
    }
    
    public ArrayList getValueAsList() {
        ArrayList values = new ArrayList();
        StringTokenizer st = new StringTokenizer(dfltValue, ",");
        while (st.hasMoreTokens()){
            values.add(st.nextToken().trim());
        }
        return values;
    }


    public String getLongDescription() {
        return longDescription;
    }


    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    // helpcontext not yet used for databits
    public String getHelpContext() {
        return "";
    }

    // helpcontext not yet used for databits
    public void setHelpContext(String helpContext) {
    }
    
    
    
}