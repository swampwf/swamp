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

import java.util.*;

import org.apache.jcs.access.exception.*;

import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

/**
 * This Databit represents a person (login or email) 
 * or a list of them
 *
 * @author Thomas Schmidt
 *
 */

public class personDatabit extends Databit {
    

    public personDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state.intValue());
        setModified(true);
    }


    
    /**
     * Checks if the given value v fits this Databit's Type.
     * 
     * @param v
     */
    public String checkDataType(String v) throws InvalidArgumentException, Exception {
        // empty values are allowed
        if (!v.equals("") && !checkedValues.contains(v)) {
            
            // datatype person may be a comma-separated
            // email, or loginname list
            StringTokenizer st = new StringTokenizer(v, ",");
            while (st.hasMoreTokens()) {
                String tokenvalue = st.nextToken().trim();
                // valid SWAMP user?
                if (tokenvalue.indexOf('@') < 0) {
                    try {
                        SecurityManager.getUser(tokenvalue);
                    } catch (UnknownElementException e) {
                        throw new InvalidArgumentException(i18n.tr("Unknown User: ") + tokenvalue
                                + i18n.tr(" in field ") + this.getName());
                    } catch (Exception e) {
                    	Logger.ERROR("Exception from backend: " + e.getMessage());
                    	e.printStackTrace();
                    	throw e;
                    }
                } else {
                    // assuming Email-Address                    
                    if (!(org.apache.commons.validator.EmailValidator.getInstance().isValid(tokenvalue))) {
                        throw new InvalidArgumentException(tokenvalue
                                + i18n.tr(" is not a valid E-Mail address."));
                    }
                }
            }
            // value is valid
            checkedValues.add(v);
        }
		return v;
    }
    

}
