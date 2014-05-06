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

 /**
  * The results from a dataedit action
  *
  * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
  * @version $Id: DataeditResult.java 7105 2006-02-02 14:31:31Z tschmidt $
  *
  */

import java.util.*;


public class DataeditResult extends Result {

    // key: field path, value: new value
    private HashMap values = new HashMap();
    
    // list with field-paths that failed during task validation
    // key: path - value: erroneous value
    private HashMap errorFields = new HashMap();

    public DataeditResult(int wfId) {
        super(wfId);
    }

    public void setValue(String path, String value) {
        values.put(path, value);
    }

    public HashMap getValues() {
        return values;
    }

    public boolean hasValue(String path) {
        if (values.containsKey(path)) {
            String value = (String) values.get(path);
            if (!value.equals("")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String getValue(String path) {
        return (String) values.get(path);
    }

    
    public void addErrorField(String fieldPath, String value) {
        this.errorFields.put(fieldPath, value);
    }

    /**
     * @return Returns the errorFields.
     */
    public HashMap getErrorFields() {
        return errorFields;
    }




}
