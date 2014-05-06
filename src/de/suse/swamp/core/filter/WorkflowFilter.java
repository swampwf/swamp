/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.filter;

import java.util.*;

import de.suse.swamp.util.*;

/**
 * @author tschmidt
 * 
 * Base class of all filters
 */
public abstract class WorkflowFilter {

    boolean activated = true;
    // indicates that this is a "order" filter and sets the direction
    Boolean descending = null;
    int id;

    public WorkflowFilter() {
        //  generate an id for referencing object in html code.
        this.id = new Date().hashCode();

    }

    public boolean isActivated() {
        return activated;
    }

    public void setActive(boolean active) {
        this.activated = active;
        Logger.DEBUG("Setting filter #" + this.id + " to " + active);
    }
    
    
    
    public void setDescending (){
        descending = new Boolean(true);
    }
    
    public void setAscending (){
        descending = new Boolean(false);
    }
    

    public int getId() {
        return id;
    }

}