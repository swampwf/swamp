/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt
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


import java.util.*;

import org.apache.commons.collections.map.*;

/**
 * Global DatasetCache for fast data access.
 * Workflows and Datasets maintain a cache with the IDs 
 * of their attached Datasets and may query the global cache 
 * for a Dataset. If they don't find it here, it gets loaded and 
 * added here. This cache is based on a "Least recently used" Map 
 * to have control over the size.
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 *
 */

public class DatasetCache {
    
    // Map for caching Datasets. key: DB-ID, value: Dataset Object
    private Map cache = Collections.synchronizedMap(new LRUMap(10000));
    private static DatasetCache datasetCache = null;
    
    
    private DatasetCache() {
    }
    
    /**
     * Use this method to get access to the global DatasetCache object.
     * @return A reference to the global WorkflowManager object
     */
    public static synchronized DatasetCache getInstance() {
        if (datasetCache == null) {
            datasetCache = new DatasetCache();
        }
        return datasetCache;
    }
    
    
    public Dataset getDataset(int id){
        return (Dataset) cache.get(new Integer(id));
    }

    public void add(List dsets) {
        for (Iterator it = dsets.iterator(); it.hasNext(); ){
            add((Dataset) it.next());
        }   
    }

    public void add(Dataset set) {
        // add only if already stored in DB: 
        Integer id = new Integer(set.getId());
        if (set != null && set.getId() > 0 && !cache.containsKey(id)){
            cache.put(id, set);
        }
    }

    /**
     * Queries the cache for a dataset with <i>datasetName</i> 
     * and an ID inside <i>list</i>. Called from Workflow and 
     * Dataset that maintain a cache for their included 
     * datasets itself.  
     * @return - Dataset if found, null if not.
     */
    public Dataset getByName(Set list, String datasetName) {
        for (Iterator it=list.iterator(); it.hasNext(); ){
            Integer id = (Integer) it.next();   
            if (cache.containsKey(id) && 
                    ((Dataset) cache.get(id)).getName().equals(datasetName)){
                //Logger.DEBUG("Cache hit for Dset: " + datasetName);
                return (Dataset) cache.get(id);
            }
        }
        return null;
    }

    
    public boolean contains(int dsetId){
        return cache.containsKey(new Integer(dsetId));
    }
    
    public void remove(int dsetId){
        cache.remove(new Integer(dsetId));
    }
    
    
    public void clearCache(){
        cache.clear();        
    }
    
    public String toString(){
        StringBuffer string = new StringBuffer();
        for (Iterator it = this.cache.keySet().iterator(); it.hasNext(); ){
            string.append(it.next()).append(", ");            
        }
        return string.toString();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
}
