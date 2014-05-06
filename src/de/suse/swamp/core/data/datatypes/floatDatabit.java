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

import de.suse.swamp.core.data.*;
import de.suse.swamp.util.*;

/**
 * float Databit type
 *
 * @author Thomas Schmidt
 *
 */

public class floatDatabit extends Databit {
    

    public floatDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state.intValue());
        setModified(true);
    }

    
    /**
     * Checks if the given value v fits this Databit's Type.
     * 
     * @param v
     */
    public String checkDataType(String v) throws Exception {
        // empty values are allowed
        if (!v.equals("") && !checkedValues.contains(v)) {
        	try {
				Float f = Float.valueOf(v);
			} catch (NumberFormatException e) {
				Logger.WARN("Cannot assign " + v + " to type float: " + e.getMessage());
				throw new NumberFormatException("Cannot cast " + v + " to type float: " + e);
			}
            checkedValues.add(v);
        }
		return v;
    }


}
