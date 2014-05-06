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

package de.suse.swamp.core.data;

import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

 /**
  * Base class for all kinds of data elements
  *
  * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
  *
  */

public abstract class Data extends Persistant { 
    
	protected I18n i18n = new I18n(getClass(), "de.suse.swamp.i18n.Core");
	
    protected Data(String name, String desc, int state){
        if (name.length() > 0) {
            this.name = name;
        } else {
            Logger.ERROR("Data needs a Name.");
       }
        description = desc;
       if (validStates.containsKey(new Integer(state))){ 
           this.state = state;
       } else {
           Logger.ERROR(state + " is not a valid state for data " + name);
       }
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
        if (this.state != state){
            this.state = state;
            setModified(true);
        }
    }
    
    /** Mode for mapping written states from the XML File. 
     * makes it easier for the user to define his states
     * @param state The state to set.
     */
    public static int toState(String state) {
        if (state.equalsIgnoreCase("read-write")){
            return Data.READWRITE;
        } else if (state.equalsIgnoreCase("read-only")){
            return Data.READONLY;
        } else if (state.equalsIgnoreCase("hidden")){
            return Data.HIDDEN;
        } else {
            Logger.ERROR("Cannot Map Data-state: " + state);
            return Data.READWRITE;
        }
    }
    
    public abstract String toString ();
    
    
    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String d) {
        description = d;
    }
    
    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }
    
    protected String name;
    protected String description;
    protected int state;
    // Valid Data-visibility states
    public final static HashMap validStates;
    static {
        validStates = new HashMap();
        validStates.put(new Integer(Data.READWRITE), "READWRITE");
        validStates.put(new Integer(Data.READONLY), "READONLY");
        validStates.put(new Integer(Data.HIDDEN), "HIDDEN");
        validStates.put(new Integer(Data.ERROR), "ERROR");
    }
    
    public static final int READWRITE = 1;
    public static final int READONLY = 2;
    public static final int HIDDEN = 3;
    public static final int ERROR = 4;  

}
