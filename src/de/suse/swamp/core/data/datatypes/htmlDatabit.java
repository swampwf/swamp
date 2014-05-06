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

import java.io.*;
import java.text.*;

import org.w3c.tidy.*;

/**
 * Databit holding valid html content. 
 *
 * @author Thomas Schmidt
 */

public class htmlDatabit extends stringDatabit {
    

    public htmlDatabit(String name, String desc, String value, Integer state) throws Exception {
        super (name, desc, value, state);
        setModified(true);
    }
    
    /**
     * Checks if the given value v fits this Databit's Type.
     * 
     * @param v
     */
    public String checkDataType(String v) throws ParseException {
        if (!v.equals("") && !checkedValues.contains(v)) {
            StringWriter err = new StringWriter(150);
            try {
                Tidy tidy = new Tidy();
                tidy.setShowWarnings(false);
                tidy.setQuiet(true);
                //tidy.getOnlyErrors();
                tidy.setErrout(new PrintWriter(err));
                tidy.parse(new ByteArrayInputStream(v.getBytes("UTF-8")), new ByteArrayOutputStream());
                if (tidy.getParseErrors() > 0) {
                    throw new Exception(err.toString().trim());
                }
            } catch (Exception e) {
                throw new ParseException("Cannot parse content: " + e.getMessage(), 0);
            }
            
            // value is valid
            checkedValues.add(v);
        }
		return v;
    }

}
