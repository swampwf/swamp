/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt@suse.de>
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

import de.suse.swamp.core.data.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * Systemdatabit
 * Extends Databit to represent Workfloe internal Data 
 * in accessible over the Databit namespace.
 *
 * @author Thomas Schmidt
 */

public class SystemDatabit extends Databit {

    
    // storing path for being write System property when saving it
    private String path = null;
    private Workflow wf = null;
    private final static String type = "system";
    
    
    public SystemDatabit(String name, String desc, String value, int state) 
    	throws Exception{
        super (name, desc, value, state);
        setModified(true);
    }

    
    public boolean setValue(String v, boolean force, String uname) throws Exception {
    	return false;
    }
    
    
    public String checkDataType(String v) throws ParseException {
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
        field.setDatatype(SystemDatabit.type);
        field.setEditInfo(this.editInfo);
        field.setLabel(this.getDescription());
        return field;
    }
    
    
    /**
     * @return Returns the path.
     */
    public String getPath() {
        return this.path;
    }
    /**
     * @param path The path to set.
     */
    public void setPath(String path) {
        this.path = path;
    }
    /**
     * @return Returns the wf.
     */
    public Workflow getWf() {
        return this.wf;
    }
    /**
     * @param wf The wf to set.
     */
    public void setWf(Workflow wf) {
        this.wf = wf;
    }


	public String getType() {
		return type;
	}
}
