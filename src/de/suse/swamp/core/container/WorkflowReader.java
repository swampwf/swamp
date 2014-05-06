/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de>
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

package de.suse.swamp.core.container;

/**
 * Class for reading workflow definitions from a specified location. 
 */

import java.io.*;
import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class WorkflowReader {

    File resourcePath;
    String separator = System.getProperty("file.separator");
    String xmlFileName = "workflow.xml";
    String configFileName = "workflow.conf";
    
    
   public WorkflowReader(File resourcepath) throws Exception {
       if (!resourcepath.exists()){
           throw new Exception("Workflow directory: " + 
                   resourcepath.getPath() + " does not exist.");
       }
       this.resourcePath = resourcepath;
   }
    
   
   
   /**
     * Reads a workflow resource from the expected location 
     * (<resourcepath>/name/version ) and returns the result. 
     * If the result does not contain errors, the workflow-template 
     * is available from the result and can be added to the list 
     * of available workflows.
     */
    public WorkflowReadResult readWorkflow(String name, String version, List results) {
    	WorkflowReadResult result = new WorkflowReadResult();
        result.setWfName(name);
        result.setWfVersion(version);
    	String basePath = resourcePath + separator + name + separator + version;
    	WorkflowVerifier verifier = new WorkflowVerifier(basePath);
    	// only read if not read yet. Break circular dependencies
    	if (verifier.getWfResultFromResults(results, name, version) != null){
    		return verifier.getWfResultFromResults(results, name, version);
    	}
        Logger.LOG("Reading workflow: " + name + " version: " + version);
        File definitionFile = new File(basePath + separator + xmlFileName);

        if (!definitionFile.exists()) {
            Logger.ERROR("No workflow.xml found.");
            result.addError("workflow.xml file not found.");
        } else {
            WorkflowTemplate wfTemp = null;
            try {
                wfTemp = new WorkflowXMLReader().readWorkflowDef(definitionFile);
                 
                verifier.verify(wfTemp, result, results);
                if (!wfTemp.getName().equals(name)) {
                    result.addError("Workflow name does not match directory name. " 
                            + wfTemp.getName() + "!=" + name);
                }
                if (!wfTemp.getVersion().equals(version)) {
                    result.addError("Workflow version does not match directory name. " 
                            + wfTemp.getVersion() + "!=" + version);
                }
                if (!result.hasErrors()){
                    result.setTemplate(wfTemp);
                }
            } catch (Exception e) {
                Logger.ERROR("Error during workflow verification: " + e.getMessage());
                result.addError("Error during workflow verification: " + e.getMessage());
            }
            
            try {
                File configFile = new File(resourcePath + separator + name + separator + version + separator
                        + configFileName);
                wfTemp.setWfProps(configFile);
            } catch (Exception e) {
                result.addWarning("No workflow.conf found.");
            }
        }
        return result;
    }
   
   
    
    /**
     * Reading workflow from a plain workflow.xml file
     */
    public WorkflowReadResult readWorkflow(String basePath) {
        WorkflowTemplate wfTemp = null;
        WorkflowReadResult result = new WorkflowReadResult();
        try {
            File definitionFile = new File(basePath + separator + xmlFileName);
            wfTemp = new WorkflowXMLReader().readWorkflowDef(definitionFile);
            result.setWfName(wfTemp.getName());
            result.setWfVersion(wfTemp.getVersion());
            WorkflowVerifier verifier = new WorkflowVerifier(basePath);
            verifier.verify(wfTemp, result, new ArrayList());
            if (!result.hasErrors()) {
                result.setTemplate(wfTemp);
            }
        } catch (FileNotFoundException e) {
            Logger.ERROR(e.getMessage());
            result.addError("Could not find file: " + xmlFileName + ".");
        } catch (Exception e2) {
            Logger.ERROR(e2.getMessage());
            result.addError(e2.getMessage());
        }
        return result;
    }
    
    
    /**
     * Reads all workflow versions that are found at 
     * (<resourcepath>/name ) and a list of results. 
     */
   public List readWorkflowType(String name, List results){
       File[] workflowVersions = new File(resourcePath.getAbsolutePath() + 
               separator + name).listFiles();
       Arrays.sort(workflowVersions);
       for (int j = 0; j < workflowVersions.length; j++) {
           File version = workflowVersions[j];
           WorkflowReadResult result = null;
           if (version.isDirectory() && !version.getName().startsWith(".")) {
               result = readWorkflow(name, version.getName(), results);
               Logger.DEBUG(result.toString());
               // only add if not pre-loaded yet
               String basePath = resourcePath + separator + name + separator + version;
               WorkflowVerifier verifier = new WorkflowVerifier(basePath);
               if (verifier.getWfResultFromResults(results, name, version.getName()) == null)
                       results.add(result);
           }
       }
       return results;
   }
   
   
   
   /**
    * Reads all workflow resources that are found at 
    * (<resourcepath> ) and a list of results. 
    */
   public ArrayList readAllWorkflows(){
       ArrayList results = new ArrayList();
       File[] workflowDefs = resourcePath.listFiles();       
       for (int i=0; workflowDefs != null && i < workflowDefs.length; i++) {
        if (workflowDefs[i].isDirectory() && 
                   !workflowDefs[i].getName().startsWith(".")) {
               Logger.LOG("Have found a SWAMP Workflow Resource:" + workflowDefs[i]);
               readWorkflowType(workflowDefs[i].getName(), results);
           }
       }
       return results;
   }
   
   
   
   
} 
