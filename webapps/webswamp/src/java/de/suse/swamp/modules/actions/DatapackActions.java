/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Klaas Freitag <freitag@suse.de>
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

package de.suse.swamp.modules.actions;

import java.io.*;
import java.util.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.lang.*;
import org.apache.turbine.util.*;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 *
 * This module provides WebApp driven actions on the datapack of
 * the workflow.
 */
public class DatapackActions extends SecureAction {

	private final static String fs = System.getProperty("file.separator");
	
    /**
     *  IMPORTANT: doXxx methods may only have ONE upper case letter X
     *  after the do, anything else must be lower case.
     */

    /**
     * Save the entire datapack of the workflow after manual edit.
     */
    public void doSavedatapack(RunData data, Context context) throws Exception {
        
        WorkflowAPI wfapi = new WorkflowAPI();
        DataAPI dataapi = new DataAPI();
        
        int wfID = data.getParameters().getInt("workflowid");
        String userName = data.getUser().getName();
        Workflow wf = wfapi.getWorkflow(wfID, userName);

        Logger.DEBUG("Wf-" + wfID + ": Saving Datapack");
        context.put("workflowid", data.getParameters().get("workflowid"));
        ArrayList errors = new ArrayList();
        String message = "";

        // prepare a Result Object for highlighting error fields
        DataeditResult result = new DataeditResult(wfID);

        org.apache.turbine.util.parser.ParameterParser pp = data.getParameters();
        int count = 0;
        ArrayList bugIdFields = new ArrayList();
        ResultList changeResults = new ResultList();

        for (Iterator it = pp.keySet().iterator(); it.hasNext();) {
            String fieldpath = "";
            String field = (String) it.next();
            try {
                // only check for dataset-paths
                if (field.startsWith("field_")) {
                    fieldpath = field.substring(6);
                    if (wf.containsDatabit(fieldpath)) {
                        Databit dbit = wf.getDatabit(fieldpath);
                        String dbitType = dbit.getType();
                        // handle file uploads:
                        if (dbitType.equalsIgnoreCase("fileref")) {
                            FileItem fi = pp.getFileItem(field);
                            if (storeFile(dbit, true, fi, userName)) {
                                String fileName = fi.getName();
                                // fix for browsers setting complete path as name: 
                                if (fileName.indexOf("\\") >= 0) fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                                if (fileName.indexOf("/") >= 0) fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                dataapi.doUpdateDataBitValue(wf.getId(), fieldpath, fileName, false, userName, changeResults);
                                count++;
                            }
                        } else if (dbitType.equalsIgnoreCase("bugzilla")) {
                            bugIdFields.add(field);
                        } else if (dbitType.equalsIgnoreCase("multienum")) {
                            SWAMPHashSet values = new SWAMPHashSet(pp.getStrings(field));
                            if (dataapi.doUpdateDataBitValue(wf.getId(), fieldpath, values.toString(", "), false, userName, changeResults)) {
                                count++;
                            }
                        } else if (dbitType.equalsIgnoreCase("patchdocumd")) {
                            String value = pp.get(field);
                        } else {
                            String value = StringEscapeUtils.unescapeHtml(pp.get(field));
                            if (dataapi.doUpdateDataBitValue(wf.getId(), fieldpath, value, false, userName, changeResults)) {
                                count++;
                            }
                        }
                    } else {
                        Logger.WARN("Trying to edit non-existant Databit: " + fieldpath);
                    }
                // workaround for empty select fields
                } else if (field.startsWith("select_")) {
                    if (!pp.containsKey("field_" + field.substring(7))) {
                        // nothing selected
                        if (wf.containsDatabit(field.substring(7))) {
                            if (dataapi.doUpdateDataBitValue(wf.getId(), field.substring(7), "", false, userName, changeResults)) {
                                count++;
                            }
                        }
                    }                    
                // workaround for checkboxes from gui
                } else if (field.startsWith("boolean_")) {
                    if (!pp.containsKey("field_" + field.substring(8))) {
                        // checkbox not checked
                        if (wf.containsDatabit(field.substring(8))) {
                            if (dataapi.doUpdateDataBitValue(wf.getId(), field.substring(8), "false", false, userName, changeResults)){
                                count++;
                            }
                        }
                    }
                }
            } catch (Exception e1) {
                result.addErrorField(fieldpath, pp.get(field));
                errors.add(e1.getMessage());
            }
        }
            
        // handle bugzilla ids at last to not overwrite fetched child fields.
        for (Iterator it = bugIdFields.iterator(); it.hasNext();) {
            String field = (String) it.next();
            String fieldpath = field.substring(6);
            try {
                String value = StringEscapeUtils.unescapeHtml(pp.get(field));
                if (dataapi.doUpdateDataBitValue(wf.getId(), fieldpath, value, false, userName, changeResults)) {
                    count++;
                    message += "Bugzilla data has been updated.\n";
                } else if (pp.containsKey("refresh_bugzilla")) {
                    // force refresh:
                    dataapi.doUpdateDataBitValue(wf.getId(), fieldpath, value, true, userName, changeResults);
                    message += "Bugzilla data has been refreshed.\n";
                }
            } catch (Exception e1) {
                result.addErrorField(fieldpath, pp.get(field));
                errors.add(e1.getMessage());
            }
        }

        context.put("result", result);
        Logger.DEBUG("doSavedatapack changed " + String.valueOf(count) + " dbits");
        if (count == 1) {
            message += "Changed 1 databit.\n";
            
        } else if (count > 1){
            message += "Changed " + String.valueOf(count) + " databits.\n";
        
        } else {
            message += "No Workflow-data has been changed.\n";
        }

        if (errors.size() == 0) {
            context.put("statusheader", "Data saved");
            context.put("statusclass", "success");
            context.put("statusmessage", message);
        } else {
            message += "Error while saving Workflow data:\n";
            for (Iterator it = errors.iterator(); it.hasNext();) {
                message += "- " + (String) it.next() + "\n";
            }
            context.put("statusmessage", message);
            context.put("statusheader", "Error");
            context.put("statusclass", "error");
            context.put("icon", "error");
            data.getParameters().setString("dataedit", "true");
        }
        context.put("history", changeResults);
    }

    
    
    
    
    public void doAdddatabit(RunData data, Context context) throws Exception {
    		context.put("adddatabit", "true");
    }
    
    
    /**
     * Step 2 of databit creation, check valid path and name, prepare content
     * addition
     */
    public void doCreatedatabit(RunData data, Context context) throws Exception {
		context.put("adddatabit", "true");
		
		String uname = data.getUser().getName();
		WorkflowAPI wfapi = new WorkflowAPI();
		int wfid = data.getParameters().getInt("workflowid");
		Workflow wf = wfapi.getWorkflow(wfid, uname);
		
		String dsetname = data.getParameters().get("dsetname");
		context.put("dsetname", dsetname);
		Dataset dset = wf.getDataset(dsetname);
		
		String dbitname = data.getParameters().get("dbitname");
		context.put("dbitname", dbitname);
		
		if (dbitname == null || dbitname.equals("")){
			throw new Exception("Please enter a non-empty databit name");
		}
		
		if (dset.containsDatabit(dbitname)){
			throw new Exception("Databit " + dsetname + "." + dbitname + 
					" already exists in workflow #" + wfid);
		}
		
		String dataType = data.getParameters().get("datatype");
        context.put("datatype", dataType);
        String datashortdesc = data.getParameters().get("datashortdesc");
        context.put("datashortdesc", datashortdesc);
        String datadesc = data.getParameters().get("datadesc");
        context.put("datadesc", datadesc);
		
    }    
    
    
    
	/**
	 * add created databit to workflow backend
	 */
	public void doSavedatabit(RunData data, Context context) throws Exception {
        DataAPI dataApi = new DataAPI();
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        ParameterParser params = data.getParameters();
        int wfid = params.getInt("workflowid");
        String dsetname = params.get("dsetname");
        String dbitname = params.get("dbitname");
        String datatype = params.get("datatype");
        String datashortdesc = params.get("datashortdesc");
        String datadesc = params.get("datadesc");
        String dbitvalue ="";
        dataApi.doAddDatabit(wfid, dsetname, dbitname, dbitvalue, uname, datatype, datashortdesc, datadesc);
        Workflow wf = wfapi.getWorkflow(wfid, uname);
        Databit dbit = wf.getDataset(dsetname).getDatabit(dbitname);
        // file upload? 
        if (params.getFileItem("filename") != null){
            FileItem fi = params.getFileItem("filename");
            if (!storeFile(dbit, false, fi, uname)){
                throw new Exception("Empty file uploaded!");
            }
            dbitvalue = fi.getName();
            
        } else if (params.get("dbitvalue") != null){
            dbitvalue = params.get("dbitvalue");
        } else {
            throw new Exception("Error while adding databit.");
        }
        dbit.setValue(dbitvalue);
        context.put("statusheader", "Data added");
        context.put("statusclass", "success");
        context.put("statusmessage", "Databit " + dsetname + "." + dbitname + " has been added.");
	}


    
    
    public static boolean storeFile(Databit dbit, boolean overwrite, FileItem fi, String uname) 
        throws Exception {
        // skip empty uploads: 
        if (fi != null && !fi.getName().trim().equals("") && fi.getSize() > 0){
            String fileDir = new SWAMPAPI().doGetProperty("ATTACHMENT_DIR", uname);
            
            if (!(new File(fileDir)).canWrite()){
                throw new Exception("Cannot write to configured path: " + fileDir);
            }
            
            String fileName = fi.getName();
            // fix for browsers setting complete path as name: 
            if (fileName.indexOf("/") >= 0) fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            if (fileName.indexOf("\\") >= 0) fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);

            File file = new File(fileDir + fs + dbit.getId() + "-" + fileName); 
            if (!overwrite) {
                if (!file.createNewFile()){
                    throw new Exception("Cannot write to file: " + file.getName() + 
                            ". File already exists?");
                }
            } else {
                if (file.exists()){
                    file.delete();
                }
                // if its a file with a new name, delete the old one: 
                File oldFile = new File(fileDir + fs + dbit.getId() + "-" + dbit.getValue());
                if (oldFile.exists()){
                    Logger.DEBUG("Deleting old file: " + oldFile.getPath());
                    oldFile.delete();
                }
            }
            
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(fi.get());
            stream.close();
            return true;
        } else {
            return false;
        }
    }
        

}
