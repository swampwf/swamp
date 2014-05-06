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

public class bugzillaDatabit extends numberDatabit {

    public bugzillaDatabit(String name, String desc, String value, Integer state) throws Exception {
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
            
            // allow plain numbers and "<bugzilla-identifier>#414756"
            int bugid = getBugId(v);
            
            // perform basic type checking
            // if this is a new databit, we may not have a dataset yet.
            Dataset dset = null;
            if (getId() > 0 && getType().equals("bugzilla")) {
                dset = getDataset();
            }
            try {
                BugzillaTools bTools = new BugzillaTools();
                bTools.fetchBugzillaInfo(dset, bugid);
            } catch (SAXParseException spe) {
                throw new InvalidArgumentException(i18n.tr("Unable to fetch data from: ") + 
                        SWAMP.getInstance().getProperty("BUGZILLA_QUERYURL") + bugid + "\n"
                        + i18n.tr("Parsing error in line ") + spe.getLineNumber() + i18n.tr(", column ")
                        + spe.getColumnNumber() + "\n" + i18n.tr("Reason: ") + spe.getMessage());
            } catch (Exception e) {
                throw new InvalidArgumentException(i18n.tr("Unable to fetch data from: ") + 
                        SWAMP.getInstance().getProperty("BUGZILLA_QUERYURL") + bugid + "\n" + 
                        i18n.tr("Reason: ") + e.getMessage());
            }
            // value is valid
            checkedValues.add(v);
        }
		return v;
    }
    
    
    private int getBugId(String bugIdent) throws NumberFormatException {
        int bugid;
        if (bugIdent.indexOf('#') >= 0)
            bugid = new Integer(bugIdent.substring(bugIdent.indexOf('#') + 1, bugIdent.length())).intValue();
        else
            bugid = new Integer(bugIdent).intValue();
        return bugid;
    }
    
    
    public int getValueAsInt() throws Exception {
        return getBugId(this.value);
    }
    
}
