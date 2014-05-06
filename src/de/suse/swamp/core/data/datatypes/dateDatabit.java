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

import java.text.*;
import java.util.*;

import de.suse.swamp.core.data.*;
import de.suse.swamp.util.*;

/**
 * The Databit
 *
 * @author Thomas Schmidt
 *
 */

public class dateDatabit extends Databit {

    // format of date
    public final static String dateFormat = "yyyy-MM-dd";
    // java std format of date
    

    public dateDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state.intValue());
        setModified(true);
    }


    
    /**
     * Checks if the given value v fits this Databit's Type.
     * 
     * @param v
     */
    public String checkDataType(String v) throws ParseException {
        // empty values are allowed
        if (!v.equals("") && !checkedValues.contains(v)) {
            DateFormat df = new SimpleDateFormat(dateFormat);
            // perform basic type checking
            try {
                Date d = df.parse(v);
                v = df.format(d);
            } catch (Exception e) {
                throw new ParseException(i18n.tr("Field ") + this.name
                        + i18n.tr(" must contain a date in the format: ") + dateFormat, 0);
            }
            // value is valid
            checkedValues.add(v);
        }
        return v;
    }
    
    
    /**
     * @return the value as Date object
     * @throws Exception
     *             when the value cannot be parsed into a date object.
     */
    public Date getValueAsDate() throws ParseException {
        DateFormat df = new SimpleDateFormat(dateFormat);
        Date d = null;
        try {
            d = df.parse(value);
        } catch (ParseException e) {
            Logger.ERROR("Was not able to parse:" + value + " into a valid Date.");
            throw e;
        }
        return d;
    }

    
    /**
     * Set the value to the given date. 
     * type date will internally be stored as "yyyy-MM-dd"
     */
    public boolean setValue(Date date, String uname) throws Exception {
        DateFormat df = new SimpleDateFormat(dateFormat);
        String newVal = df.format(date);
        return setValue(newVal, uname);
    }

}
