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

package de.suse.swamp.util;


import java.util.*;

/**
 * Global I18nCache to avoid reloading of resource files. 
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class I18nCache {
    
    // Map for caching i18n objects. key: path, value: i18n Object
    private Hashtable cache = new Hashtable();
    private static I18nCache i18nCache = null;
    
    
    private I18nCache() {
    }
    

    public static synchronized I18nCache getInstance() {
        if (i18nCache == null) {
        	i18nCache = new I18nCache();
        }
        return i18nCache;
    }
    
    
    public org.xnap.commons.i18n.I18n getI18n(String id){
        return (org.xnap.commons.i18n.I18n) cache.get(id);
    }

    public void add(org.xnap.commons.i18n.I18n i18n, String path) {
        cache.put(path, i18n);
    }
    
    public boolean contains(String id){
        return cache.containsKey(id);
    }
    
    public void clearCache(){
        cache.clear();        
    }
    
}
