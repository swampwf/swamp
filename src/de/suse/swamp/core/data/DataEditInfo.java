/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Klaas Freitag <freitag@suse.de>
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

package de.suse.swamp.core.data;

import de.suse.swamp.core.util.*;

/**
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 *
 * Class to provide editor info for databits. This info 
 * helps the frontend to decide which kind of widget to use,
 * how large it should be etc. 
 * 
 */
public class DataEditInfo extends Persistant {
    private String type;
    private int xsize;
    private int ysize;
    
    public DataEditInfo(String type, int xsize, int ysize) {
        this.type = type;
        setXsize(xsize);
        setYsize(ysize);
        setModified(true);
    }

    /**
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * @return
     */
    public int getXsize() {
        return xsize;
    }

    /**
     * @return
     */
    public int getYsize() {
        return ysize;
    }

    /**
     * @param string
     */
    public void setType(String string) {
        type = string;
        setModified(true);
    }

    /**
     * @param i
     */
    public void setXsize(int i) {
        xsize = i;
        setModified(true);
    }

    /**
     * @param i
     */
    public void setYsize(int i) {
        ysize = i;
        setModified(true);
    }


}
