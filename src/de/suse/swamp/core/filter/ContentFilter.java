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

package de.suse.swamp.core.filter;

import java.util.*;

import org.apache.commons.lang.*;

import de.suse.swamp.util.*;


/**
 * @author tschmidt
 *
 * Filter by Datapack Content
 */
public class ContentFilter extends DatabaseFilter {

    private String databitPath;
    private String databitValue;
	private String databitValueRegex;
	private String databitValueGreater;
	private String databitValueSmaller;
	private boolean treatNumeric = false;
    
	
    public ContentFilter() {
        super();
    }

    public String getSQL() {
        
        // if we do sorting, then by databitvalue
        if (treatNumeric)
            setOrderColumn("LPAD(dbDatabits.value,10,'0')");
        else 
            setOrderColumn("dbDatabits.value");
        
        // list select columns: 
        ArrayList columns = new ArrayList();
        columns.add("dbWorkflows.wfid");
        
        
        // list needed tables: 
        ArrayList tables = new ArrayList();
        tables.add("dbWorkflows");
        tables.add("dbDataset_Workflow");
        tables.add("dbDatasets rootset");
        tables.add("dbDatabits");
        
        // list conditions:
        ArrayList conditions = new ArrayList();
        if (!getDatabitName().equals("*")){
            conditions.add("dbDatabits.name = '" + StringEscapeUtils.escapeSql(getDatabitName()) + "'");
        }
        conditions.add("dbWorkflows.wfid = dbDataset_Workflow.workflowID");
        conditions.add("dbDataset_Workflow.datasetID = rootset.datasetID");
        
        // generating the databitcondition with wildcards (OR connection)
        // eg rootset.*.* searches 2 levels of databits
        String databitCondition = "(";
        conditions.add("rootset.name = '" + StringEscapeUtils.escapeSql(getRootsetName()) + "'");
        if (getRootsetName().equals("*")){
            Logger.ERROR("Using wildcards for the root dataset not working yet.");
        }
        
        			
        // for each childset we need to go one step deeper 
        // through the dbDatasets_Datasets table
        String childstring = "";
        if (getChildDatasetNames().size() > 0)
            tables.add("dbDataset_Dataset");   
        
        String childsetname = "";
        
        for (int i = 1; i<= getChildDatasetNames().size(); i++ ){
            childstring = "";
            childsetname = StringEscapeUtils.escapeSql((String) getChildDatasetNames().get(i-1));
            for (int j=1; j<=i; j++){
                childstring += "child";
            }
            tables.add("dbDatasets " + childstring + "set");
            // for the first childset, the rootset is "rootset": 
            conditions.add("dbDataset_Dataset.rootDatasetId = rootset.datasetID");
            conditions.add("dbDataset_Dataset.datasetID = " + childstring + "set.datasetID");
            if (!childsetname.equals("*")){
                conditions.add(childstring + "set.name = '" + childsetname + "'");
            } else {
                // dbit condition uninitialzed, -> add rootset
                if (databitCondition.length() > 1 ){
                    databitCondition += " OR ";
                } else if (i == 1) {
                    databitCondition += "dbDatabits.datasetID = rootset.datasetID OR ";
                }
                databitCondition += " dbDatabits.datasetID = " + childstring + "set.datasetID "; 
            }
        }
        databitCondition += ")";
        
        // if we have had no childsets the set to select is the rootset: 
        if (getChildDatasetNames().size() == 0)
            conditions.add("dbDatabits.datasetID = rootset.datasetID");
        else if (databitCondition.length() <= 2)
            conditions.add("dbDatabits.datasetID = " + childstring + "set.datasetID");
        else 
            conditions.add(databitCondition);
        
        
        // if this is an order filter, we don't want this condition
        if (descending == null){
            // filter by regexp ?
            if (this.databitValue != null){
                conditions.add("dbDatabits.value = '" + StringEscapeUtils.escapeSql(this.databitValue) + "'");
            } else if (this.databitValueRegex != null){
                conditions.add("dbDatabits.value REGEXP '" + StringEscapeUtils.escapeSql(this.databitValueRegex) + "'");
            } else if (this.databitValueGreater != null){
                conditions.add("dbDatabits.value > '" + StringEscapeUtils.escapeSql(this.databitValueGreater) + "'");
            } else if (this.databitValueSmaller != null){
                conditions.add("dbDatabits.value < '" + StringEscapeUtils.escapeSql(this.databitValueSmaller) + "'");
            } else {
                Logger.ERROR("Please provide databitValue or databitValueRegex for Contentfilter.");
            }
        }
        
		return buildSQLString(columns, tables, conditions);
    }
    
    
    
    private ArrayList getChildDatasetNames(){
        ArrayList sets = new ArrayList();
        StringTokenizer st = new StringTokenizer(databitPath, ".");
        st.nextToken();
        while (st.countTokens() > 1){
            sets.add(st.nextToken());
        }
        return sets;
    }
    
    
    
    private String getRootsetName(){
        StringTokenizer st = new StringTokenizer(databitPath, ".");
        return st.nextToken();        
    }
    
    
    private String getDatabitName(){
        StringTokenizer st = new StringTokenizer(databitPath, ".");
        String databitName = "";
        while (st.countTokens() > 0){
            databitName = st.nextToken();
            
        }
        return databitName;  
    }
    
    
	
    /**
     * @return Returns the databitName.
     */
    public String getDatabitPath() {
        return databitPath;
    }
    /**
     * @param databitName The databitName to set.
     */
    public void setDatabitPath(String databitPath) {
        this.databitPath = databitPath;
    }
    /**
     * @return Returns the databitValue.
     */
    public String getDatabitValue() {
        return databitValue;
    }
    /**
     * @param databitValue The databitValue to set.
     */
    public void setDatabitValue(String databitValue) {
        this.databitValue = databitValue;
    }
    /**
     * @return Returns the databitValueLike.
     */
    public String getDatabitValueRegex() {
        return databitValueRegex;
    }
    /**
     * @param databitValueLike The databitValueLike to set.
     */
    public void setDatabitValueRegex(String databitValueLike) {
        this.databitValueRegex = databitValueLike;
    }

    /**
     * @param treatNumeric The treatNumeric to set.
     */
    public void setTreatNumeric(boolean treatNumeric) {
        this.treatNumeric = treatNumeric;
    }

    public void setDatabitValueGreater(String databitValueGreater) {
        this.databitValueGreater = databitValueGreater;
    }

    public void setDatabitValueSmaller(String databitValueSmaller) {
        this.databitValueSmaller = databitValueSmaller;
    }
}