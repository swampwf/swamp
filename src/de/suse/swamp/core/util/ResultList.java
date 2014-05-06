/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.util;

import java.util.*;

public class ResultList {
    
    public static int INFO = -1;
    public static int MESSAGE = 0;
    public static int WARN = 1;
    public static int ERROR = 2;
    
    private ArrayList results = new ArrayList();
	
    public ResultList() { }

    public void addResult(int level, String result){
        results.add(new ResultItem(level, result));
    }

    public void addResult(ResultItem result){
        results.add(result);
    }
    
    public void addResults(ResultList addresults){
        for (Iterator it = addresults.getResults().iterator(); it.hasNext(); ){
            addResult((ResultItem) it.next());    
        }
    }
    
    
    public List getResults(){
        return results;
    }

    
    public List getResults(int minlevel){
        List myresults = new ArrayList();
        for (Iterator it = results.iterator(); it.hasNext(); ) {
            ResultItem item = (ResultItem) it.next();
            if (item.getLevel() >= minlevel) myresults.add(item);
        }
        return myresults;
    }
    
    
    public boolean containsResult(int level, String result) {
        if (results.contains(new ResultItem(level, result)))
            return true;
        else
            return false;
    }
    
    
    public boolean hasErrors() {
        boolean hasError = false;
        for (Iterator it = results.iterator(); it.hasNext(); ) {
            ResultItem item = (ResultItem) it.next();
            if (item.getLevel() == ERROR) {
                hasError = true; 
                break;
            }
        }
        return hasError;
    }

    public void reset () {
        results = new ArrayList();
    }
    
    
    
    public class ResultItem {
        
        private String message;
        private int level;
        
        public ResultItem (int level, String message){
         this.level = level;
         this.message = message;
        }

        public int getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }
        
        public String toString() {
            if (level == WARN) {
                return "Warn: " + message;
            } else if (level == ERROR) {
                return "Error: " + message;
            } else {
                return message;
            }
        }
        
        public boolean equals(Object item) {
            if (item instanceof ResultItem && ((ResultItem) item).getLevel() == level
                    && ((ResultItem) item).getMessage().equals(message))
                return true;
            else
                return false;
        }
        
    }
    
    
}
