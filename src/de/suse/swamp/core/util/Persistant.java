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

package de.suse.swamp.core.util;

import java.io.*;

import de.suse.swamp.util.*;

 /**
  * Helper Class for determing if an Object has changed and needs 
  * to be updated in the DB or not. 
  * This may save a lot of time.
  *
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  * @version $Id$
  *
  */

public abstract class Persistant implements Serializable {

    private boolean modified = true;
    private int id = 0;

    /**
     * @param i - This ID should only be set from the StorageManager, 
     * and comes from the PrimaryKey of the Database.
     */
    public void setId(int i) {
        if (i > 0 && id == 0){
            this.id = i;
            setModified(true);
        } else if (i > 0 && id != 0){
            Logger.ERROR("Setting ID twice !!");
        } else {
            Logger.ERROR("Illegal setting of ID to " + i + " !!");
        }
    }
    
    /**
     * @return - the id that is representing the Object in the Database. 
     * This ID is unique for that type of Object
     */
    public int getId() {
        return id;
    }
    
    public boolean isModified() {
        return modified;
    }
    
    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
