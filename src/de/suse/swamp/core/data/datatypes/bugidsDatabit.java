/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt
 * Copyright (c) 2007 Novell Inc.
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

import java.util.*;

/**
 * Databit holding multiple bug ids. 
 *
 * @author Thomas Schmidt
 */

public class bugidsDatabit extends bugzillaDatabit {
    

    public bugidsDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state);
        setModified(true);
    }

    
    public String checkDataType(String v) throws Exception {
        // empty values are allowed
        if (!v.equals("") && !checkedValues.contains(v)) {
            // may be a comma-separated list
            StringTokenizer st = new StringTokenizer(v, ",");
            while (st.hasMoreTokens()) {
                String tokenvalue = st.nextToken().trim();
                super.checkDataType(tokenvalue);
            }
            // value is valid
            checkedValues.add(v);
        }
        return v;
    }
    
    
}
