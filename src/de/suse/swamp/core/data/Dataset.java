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

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;
import org.apache.commons.collections.map.*;

/**
 * The Dataset contains Databits (which contain the information) 
 * and and may contain other datasets.
 *
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class Dataset extends Data {
	
    
    // cache for already loaded databits; key=path, value=Databit
    private LRUMap dataBitCache = new LRUMap(20);
    // ID-cache for already loaded datasets;
    private HashSet dataSetIdCache = new HashSet();

    public Dataset(String n, String d, int state) {
        super(n,d,state);
        //Logger.DEBUG("Created a data set " + n);
        setModified(true);
    }
    

    /**
     * @return a list with all Databits that are contained in this dataset.
     */
    public List getDatabits() {
        List bits = DataManager.getDatabitsForSet(this.getId());
        List cachedBits = new ArrayList();
        for (Iterator it = bits.iterator(); it.hasNext();) {
            Databit bit = (Databit) it.next();
            if (!dataBitCache.containsKey(bit.getName())) {
                dataBitCache.put(bit.getName(), bit);
                cachedBits.add(bit);
            } else {
                bit = (Databit) dataBitCache.get(bit.getName());
                cachedBits.add(bit);
            }
        }
        return cachedBits;
    }

    
    /** Searches for the requested Databit.
     * @param field the path of the databit, relative to this dataset
     * @return the requested Databit if found, null if not
     */
    public Databit getDatabit(String field) {
        Databit dbit = null;
        // query the databit-cache
        if (dataBitCache.containsKey(field)) {
            dbit = (Databit) dataBitCache.get(field);
        } else {
            // Logger.DEBUG("Looking for Databit in fieldpath " + field);
            StringTokenizer st = new StringTokenizer(field, ".");
            if (st.countTokens() == 0) {
                Logger.BUG("Requesting empty fieldname in set: " + this.name);
            } else if (st.countTokens() == 1) {
                // the databit is located in this dataset
                String databitName = st.nextToken();
                dbit = DataManager.loadDatabit(this.getId(), databitName);
                dataBitCache.put(field, dbit);
            } else if (st.countTokens() > 1) {
                String datasetName = st.nextToken();
                // redirecting request to the next dataset
                Dataset dataset = (Dataset) getDataset(datasetName);
                String newField = field.substring(datasetName.length() + 1);
                if (dataset != null) {
                    dbit = dataset.getDatabit(newField);
                }
            }
        }
        return dbit;
    }
        
    
    /**
     * @return attached dataset of the given name
     */
    public Dataset getDataset(String datasetName) {
        Dataset dSet = null;
        StringTokenizer st = new StringTokenizer(datasetName, ".");
        if (st.countTokens() == 0) {
            Logger.BUG("Requesting empty dsetname in dset: " + this.getId());
        } else if (st.countTokens() == 1) {
            dSet = DatasetCache.getInstance().getByName(this.dataSetIdCache, datasetName);
            if (dSet == null) {
                // Set not yet in idcache or global cache, load + store it
                dSet = DataManager.loadChildDataset(getId(), datasetName);
                if (dSet != null){
                    this.dataSetIdCache.add(new Integer(dSet.getId()));
                }
            }
        } else if (st.countTokens() > 1) {
            String firstSet = st.nextToken();
            String targetSet = datasetName.substring(firstSet.length() + 1);
            // redirecting request to the next dataset
            dSet = getDataset(firstSet);
            dSet = dSet.getDataset(targetSet);
        }
        return dSet;
    }
    
    
    /**
     * Evaluates if the requested Databit exists in this set
     * 
     * @param path
     * @return
     */
    public boolean containsDatabit(String path) {
        return this.getDatabit(path) == null ? false : true;
    }
        
    
    public boolean containsDataset(String name) {
        return this.getDataset(name) == null ? false : true;
    }
    
    private List getDatasets() {
        List dsets = DataManager.loadDatasetsforDataset(getId());
        if (dsets != null && dsets.size() > 0) {
            for (Iterator it = dsets.iterator(); it.hasNext();) {
                this.dataSetIdCache.add(new Integer(((Dataset) it.next()).getId()));
            }
        }
        return dsets;
    }


    
    /** This method sets values in the dataset. It is able to handle
     *  bit names with dots ie. bits that are member of a set. For 
     *  example, the databit with path info CD-Info.Name is the bit
     *  'name' in the dataset 'CD-Info'. 
     * 
     * @param field - the path of the databit to save.
     * @param value - the databits value.
     * @param value - user who triggered the change
     * @return true if the bit was changed, false if not
     * @throws Exception if incompatible datatype
     */
    public boolean setValue(String ifield, String value, String uname) throws Exception {
        boolean ret = false;
        Databit dbit = getDatabit(ifield);
        if (dbit != null){
            ret = dbit.setValue(value, uname);
        } else {
            Logger.ERROR("Databit " + ifield + " not found in set " + this.getName());
        }
        return ret;
    }

    public String getValue(String ifield) {
        Databit dbit = getDatabit(ifield);
        return dbit.getValue();
    }
    
    
    /** adding a Databit to this Set.
     * @param databit
     */
    public void addDatabit(Databit databit) {
        databit.setDSetId(getId());
        DataManager.storeDataBit(databit, SWAMPUser.SYSTEMUSERNAME);
        dataBitCache.put(databit.getName(), databit);
    }



    /** adding a Dataset as a child to this set
     * @param dataset
     */
    public void addDataset(Dataset dataset) {
        String name = dataset.getName();
        if (containsDataset(name)) {
            Logger.ERROR("Cannot add a dataset: " + name + " twice to set " + getName());
        } else {
            if (getId() <= 0) {
                Logger.LOG("Dataset not yet persisted, adding to db.");
                DataManager.storeDataSet(this);
            } else if (dataset.getId() <= 0) {
                Logger.LOG("Dataset to add not yet persisted, adding to db.");
                DataManager.storeDataSet(dataset);
            }
            Logger.DEBUG("Adding Dataset " + dataset.getName() + " as a child " + 
                    "to set " + this.getName());
            DataManager.addDatasetToDataset(getId(), dataset.getId());
            this.dataSetIdCache.add(new Integer(dataset.getId()));
        }
    }
    
    
    
    
    /**
	 * For the DisplayWorkflow GUI we need a LinkedHashmap with all Fields of
	 * this Dataset. key: datasetpath value: ArrayList with included
	 * Databit-fields
	 * 
	 * @param set
	 * @return
	 */
    protected LinkedHashMap getFields(String path, LinkedHashMap hm) {
        // add databits
        ArrayList fieldList = new ArrayList();
        for (Iterator it = getDatabits().iterator(); it.hasNext();) {
            Databit bit = (Databit) it.next();
            Field bitField = bit.getField(path);
            // if set is read-only -> all fields are read-only
            if (this.getState() == Data.READONLY) {
                bitField.setState(Data.READONLY);
            }
            fieldList.add(bitField);
        }
        hm.put(path, fieldList);
        // recursively add Databits from sets
        for (Iterator it = getDatasets().iterator(); it.hasNext();) {
            Dataset dataset = (Dataset) it.next();
            hm = dataset.getFields(path + "." + dataset.getName(), hm);
        }
        return hm;
    }

    
    /**
     * For the DisplayWorkflow GUI we need a LinkedHashmap with all Fields of
     * this Dataset. (Includes also fields of sub-datasets)
     * key: datasetpath 
     * value: ArrayList with included Databit-fields
     */ 
    public LinkedHashMap getAllFields() {
        LinkedHashMap hm = new LinkedHashMap();
        hm = this.getFields( this.getName(), hm);
        return hm;
    }
    
    

    
    
    
    /**
     * creates a list of the paths of all databits in all included sets
     * relative to this dataset. If you need the full paths use 
     * <i>Workflow.getAllDatabitPaths()</i>
     * @return a list of all databit paths in the set.
     */
    public ArrayList getAllDatabitPaths() {
        ArrayList bitPaths = new ArrayList();
        for (Iterator it = this.getDatasets().iterator(); it.hasNext(); ){
            Dataset dataset = (Dataset) it.next();
           for (Iterator it2 = dataset.getAllDatabitPaths().iterator(); it2.hasNext(); ){
              bitPaths.add(dataset.getName() + "." + (String) it2.next()); 
           }
        }
        for (Iterator it = this.getDatabits().iterator(); it.hasNext(); ){
           Databit dbit = (Databit) it.next(); 
           bitPaths.add(dbit.getName()); 
        }
        return bitPaths;
    }   
    
    
    
    /**
     * Returns a String representing the Dataset object
     * 
     * @return a multiline String showing Dataset data.
     */
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append("Dataset " + getName() + ":" + "\n");
        for (Iterator it = getDatabits().iterator(); it.hasNext();) {
            Databit db = (Databit) it.next();
            ret.append("  " + db.toString() + "\n");
        }
        return ret.toString();
    }

    
    /**
     * Returns a XML representing the Dataset object
     * 
     * @return a multiline XML showing Dataset data.
     */
    public String toXML() {
        StringBuffer sb = new StringBuffer();
        sb.append("<Dataset name=\"" + name + "\" desc=\"" + description + "\">\n");
        for (Iterator iter = getDatabits().iterator(); iter.hasNext();) {
            sb.append("\n" + ((Databit) iter.next()).toXML());
        }
        sb.append("</Dataset>\n");
        return new String(sb);
    }


    public void store(){
        DataManager.storeDataSet(this);
    }
    
}
