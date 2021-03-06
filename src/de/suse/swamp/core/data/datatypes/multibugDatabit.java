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
import org.xml.sax.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;

/**
 * The Databit
 *
 * @author Thomas Schmidt
 *
 */

public class multibugDatabit extends numberDatabit {
    

    public multibugDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state);
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
            Dataset dset = null;
            // perform basic type checking
            // if this is a new databit, we may not have a dataset yet.
            if (getId() > 0){
        	dset = getDataset();
            }
            while (st.hasMoreTokens()) {
                String tokenvalue = st.nextToken().trim();

		try {
		    BugzillaTools bTools = new BugzillaTools();
		    bTools.fetchBugzillaInfo(dset, new Integer(tokenvalue).intValue());
		} catch (SAXParseException spe) {
		    throw new InvalidArgumentException(i18n.tr("Unable to YYYY fetch data from: ") + 
						       SWAMP.getInstance().getProperty("BUGZILLA_QUERYURL") + tokenvalue + "\n" + 
						       i18n.tr("Parsing error in line ") + spe.getLineNumber() + i18n.tr(", column ") + spe.getColumnNumber() + "\n" + 
						       i18n.tr("Reason: ") + spe.getMessage());
		} catch (Exception e) {
		    e.printStackTrace();
		    throw new InvalidArgumentException(i18n.tr("Unable to XXXX fetch data from: ") + 
						       SWAMP.getInstance().getProperty("BUGZILLA_QUERYURL") + tokenvalue + "\n" + 
						       i18n.tr("Reason: ") + e.getMessage());
		}
		// value is valid
	    }
	}
        checkedValues.add(v);
	    return v;
    }
    
}
