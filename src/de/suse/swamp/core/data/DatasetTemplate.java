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
import de.suse.swamp.util.*;

/**
 * The template for the dataset
 *
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 * @version $Id$
 *
 */

public class DatasetTemplate extends Data {

    // key: databitTempl.name(), value: databitTempl
    private LinkedHashMap dbitTempls = new LinkedHashMap();
    // key: datasetTempl.name, value: datasetTempl
    private LinkedHashMap dsetTempls = new LinkedHashMap();
	
	
    public DatasetTemplate(String n, String d, int state) {
        super(n,d,state);
    }    
    
    
    /** Creates a Dataset out of this Template and stores it in the DB.
     * @return
     */
    public Dataset createDataset() {
        // store root dataset
        Dataset rootSet = new Dataset(this.name, this.description, this.state);
        DataManager.storeDataSet(rootSet);
        // recursively store attached datasets and databits
        rootSet = this.addData(rootSet, dsetTempls, dbitTempls);
        return rootSet;
    }
    
    
    private Dataset getDataset(){
        return new Dataset(name, description, state);
    }
        
        
    private Dataset addData(Dataset parentSet, LinkedHashMap dsetTempls, 
            LinkedHashMap dbitTempls) {
        
        // add Sets
        for (Iterator it = dsetTempls.values().iterator(); it.hasNext(); ){
            DatasetTemplate dSetTemplate = (DatasetTemplate) it.next();
            Dataset dSet = dSetTemplate.getDataset();
            DataManager.storeDataSet(dSet);
            parentSet.addDataset(addData(dSet, dSetTemplate.dsetTempls, 
                    dSetTemplate.dbitTempls));
        }
        
        // add Databits
        for (Iterator it = dbitTempls.values().iterator(); it.hasNext(); ){
            DatabitTemplate dbitTemp = (DatabitTemplate) it.next();
            Databit dBit = dbitTemp.getDatabit();
            parentSet.addDatabit(dBit);
        }
        return parentSet;
    }
       
    
    
   
    /**Add another DatasetTemplate to this DatasetTemplate
     * @param datasetTempl
     */
    public void addDatasetTempl(DatasetTemplate datasetTempl) {
        if (dsetTempls.containsKey(datasetTempl.getName())) {
            dsetTempls.remove(datasetTempl.getName());
            Logger.ERROR("dataset-template " + datasetTempl.getName() + 
                    " already exists, using new one.");
        }
        dsetTempls.put(datasetTempl.getName(), datasetTempl);
    }
    
    
    /**Add a DatabitTemplate to this DatasetTemplate
     * @param bitTempl
     */
    public void addDatabitTempl(DatabitTemplate bitTempl) {
        /* If the databit already exists, remove it first. */
        if (dbitTempls.containsKey(bitTempl.getName())) {
            dbitTempls.remove(bitTempl.getName());
            Logger.ERROR("databit-template " + bitTempl.getName() + 
                    " already exists, using new one.");
        }
        dbitTempls.put(bitTempl.getName(), bitTempl);
    }


    public String toString() {
       /* 
        StringTable dt = new StringTable(getName(), 60);
        StringBuffer ret = new StringBuffer();

        ret.append("Dataset " + getName() + ":");
        ret.append('\n');
        Iterator iterator = data.values().iterator();

        // for( Enumeration e = elements(); e.hasMoreElements(); ) {
        while (iterator.hasNext()) {
            Databit db = (Databit) iterator.next();
            ret.append("  " + db.toString()).append('\n');
        }

        return ret.toString();
        */
        return "";
    }


    /**
     * creates a list of the paths of all databits in all included sets
     * relative to this dataset. If you need the full paths use 
     * <i>Workflow.getAllDatabitPaths()</i>
     * @return a list of all databit paths in the set.
     */
    public ArrayList getAllDatabitPaths() {
        ArrayList bitPaths = new ArrayList();
        for (Iterator it = dsetTempls.keySet().iterator(); it.hasNext();) {
            String dataset = (String) it.next();
            for (Iterator it2 = ((DatasetTemplate) dsetTempls.get(dataset)).getAllDatabitPaths().iterator(); it2
                    .hasNext();) {
                bitPaths.add(dataset + "." + (String) it2.next());
            }
        }
        for (Iterator it = dbitTempls.keySet().iterator(); it.hasNext();) {
            String dbit = (String) it.next();
            bitPaths.add(dbit);
        }
        return bitPaths;
    }
    
    /** Searches for the requested Databit.
     * @param field the path of the databit, relative to this dataset
     * @return the requested DatabitTemplate if found, null if not
     */
    public DatabitTemplate getDatabitTemplate(String field) {
        DatabitTemplate dbit = null;

            StringTokenizer st = new StringTokenizer(field, ".");
            if (st.countTokens() == 0) {
                Logger.BUG("Requesting empty fieldname in set: " + this.name);
            } else if (st.countTokens() == 1) {
                // the databit is located in this dataset
                String databitName = st.nextToken();
                dbit = (DatabitTemplate) dbitTempls.get(databitName);
            } else if (st.countTokens() > 1) {
                String datasetName = st.nextToken();
                // redirecting request to the next dataset
                DatasetTemplate dataset = (DatasetTemplate) dsetTempls.get(datasetName);
                String newField = field.substring(datasetName.length() + 1);
                if (dataset != null) {
                    dbit = dataset.getDatabitTemplate(newField);
                }
            }
        return dbit;
    }

}