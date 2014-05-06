/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.util;

import java.util.*;

/**
 * @author Thomas Schmidt
 * make a Comma separated String to a Hashset with unique elements
 *
 */

public class SWAMPHashMap extends HashMap {
    
    public SWAMPHashMap() {
       super();
    }
    
    public SWAMPHashMap (String list, String separator) {
       super();
       this.add(list, separator);
    }
    
    
    public void add(String list, String separator) {
       if (list != null && separator != null) {
            StringTokenizer st = new StringTokenizer(list, separator);
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (token != null && token.length() > 0){
                    // token must be a key=value pair now
                    StringTokenizer st2 = new StringTokenizer(token, "=");
                    if (st2.countTokens() != 2){
                        Logger.ERROR("Please use valid SWAMPHashMap format: " 
                                + "key=value1");
                    } else {
                        put(st2.nextToken(), st2.nextToken());
                    }
                }
            }
        }
    }
    
    
    
    public String toString(String divide){
        StringBuffer val = new StringBuffer();
        int count = 0;
        for(Iterator it = this.keySet().iterator(); it.hasNext(); ){
            if (count > 0)
                val.append(divide);
            String key = (String) it.next();
            val.append(key).append("=").append(this.get(key));
            count++;
        }
        return val.toString();
    }

    
    
    /**
     * Extending the get() method with a default parameter 
     * that is returned if the requested key was not found
     * @param key
     * @param dflt
     * @return
     */
    public Object get (String key, Object dflt){
    	if (this.get(key) != null){
    		return this.get(key);
    	} else {
    		return dflt;
    	}
    }
    
    
}
