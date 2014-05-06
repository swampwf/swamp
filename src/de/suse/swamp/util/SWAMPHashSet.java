/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt [at] suse.de>
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

public class SWAMPHashSet extends LinkedHashSet {
    
    public SWAMPHashSet() {
       super();
    }
    
    public SWAMPHashSet (String list, String separator) {
       super();
       this.add(list, separator);
    }

    public SWAMPHashSet (Object obj, String separator) {
       super();
       if (obj != null)
       this.add(obj.toString(), separator);
    }

    public SWAMPHashSet(String list) {
        super();
        this.add(list, ",");
    }
    
    public SWAMPHashSet (List list) {
        super();
        this.addAll(list);
     }
    
    public SWAMPHashSet (Object[] list) {
        super();
        for (int i = 0; i<list.length; i++){
            this.add(list[i]);
        }
     }
        
    public void add(String list, String separator) {
       if (list != null && !list.equals("") && separator != null) {
            StringTokenizer st = new StringTokenizer(list, separator);
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (token != null && token.length() > 0){
                     this.add(token);
                }
            }
        }
    }
    
    public String toString(){
        StringBuffer val = new StringBuffer();
        int count = 0;
        for(Iterator it = this.iterator(); it.hasNext(); ){
            if (count > 0)
                val.append(", ");
            val.append((String) it.next());
            count++;
        }     
        return val.toString();
    }
    
    
    public String toString(String divide){
        StringBuffer val = new StringBuffer();
        int count = 0;
        for(Iterator it = this.iterator(); it.hasNext(); ){
            if (count > 0)
                val.append(divide);
            val.append(it.next().toString());
            count++;
        }
        return val.toString();
    }
    
    
    public List toList(){
        ArrayList list = new ArrayList();
        for(Iterator it = this.iterator(); it.hasNext(); ){
            list.add(it.next());
        }
        return list;    
    }
    
    
    /**
     * Check if a String is included, ignoring his cases.
     */
    public boolean containsIgnoreCase (String test){
        boolean contained = false;
        for (Iterator it = this.iterator(); it.hasNext(); ){
            Object o = it.next();
            if (o instanceof  String && ((String) o).toLowerCase().equals(test.toLowerCase())){
                contained = true;
                break;
            }
        }
        return contained; 
    }

    
    /**
     * returns true if it contains an element starting with any string from @check
     */
    public boolean containsAnyStartingWith(List check) {
        for (Iterator it = this.iterator(); it.hasNext();) {
            Object o = it.next();
            for (Iterator checkIt = check.iterator(); checkIt.hasNext();) {
                Object checkString = checkIt.next();
                if (o instanceof String && checkString instanceof String) {
                    if (((String) o).toLowerCase().startsWith(((String) checkString).toLowerCase())) {
                        Logger.LOG("Found " + o + " starting with: " + checkString + " in set.");
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * returns true if it contains an element starting with any string from @check
     */
    public boolean containsAnyOf(List check) {
        for (Iterator it = this.iterator(); it.hasNext();) {
            Object o = it.next();
            for (Iterator checkIt = check.iterator(); checkIt.hasNext();) {
                Object c = checkIt.next();
                if (o.equals(c)) {
                    Logger.LOG("SWAMPHashSet.containsAnyOf: Found match: " + o);
                    return true;
                }
            }
        }
        return false;
    }

}
