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

package de.suse.swamp.core.tasks;

/**
 * Base class for all kinds of tasks.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

import java.util.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public abstract class Task {

    // every task has an action related to it
    public abstract String getActionType();

    public abstract ActionTemplate getActionTemplate();

    // ... and a possible result
    public abstract Result getResult();

    /**
     * check if action and result are happy with each other
     * @return ArraYList of error strings, empty when no error
     */
    public abstract ArrayList validate();

    // not all tasks will implement this, but if something has to be done beyond
    // sending events, it will be done here:
    public abstract void act() throws Exception;

    // all tasks may trigger events when they are finished
    public abstract ArrayList getEvents();

    // describe the task for use in frontends
    public abstract String getDescription();
    
    public boolean equals(Task task){
        return (this.id == task.getId());
    }    
    
    /**
     * @param i - This ID should only be set from the StorageManager, 
     * and comes from the PrimaryKey of the Database.
     */
    public final void setId(final int i) {
        if (i > 0 && id == 0){
            this.id = i;
        } else if (i > 0 && id != 0){
            Logger.ERROR("Setting Task-ID twice !!");
        } else {
            Logger.ERROR("Illegal setting of Task-ID to " + i + " !!");
        }
    }
    
    /**
     * @return - the id that is representing the Object in the Database. 
     * This ID is unique for that type of Object
     */
    public final int getId() {
        return id;
    }
    
    private int id = 0;


}