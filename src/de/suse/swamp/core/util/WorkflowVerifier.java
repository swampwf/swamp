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

package de.suse.swamp.core.util;

import java.io.*;
import java.util.*;

import org.apache.regexp.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.conditions.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author Thomas Schmidt
 *
 * Verify the integrity of a Workflow bundle
 */

public class WorkflowVerifier {
    
    private SWAMP swamp;
    private String resourcePath;
    String separator = System.getProperty("file.separator");	
	private static List checkedScripts = new ArrayList();
	
    
	public WorkflowVerifier(String basePath) {
        swamp = SWAMP.getInstance();
        this.resourcePath = basePath;
	}

    
    /**
     * Verify the workflow template. 
     * This class does semantic checks on the complete workflow, 
     * where the WorkflowReader just does syntactically check the 
     * workflow definition xml file. 
     * 
     * TODO:
     * - check existance of referenced icons + files
     * 
     * Fatal errors and warnings will get stored in the result object.
     */
    public void verify(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) throws Exception {
        
        if (!(new File(resourcePath).exists())){
            throw new Exception("Resource path: " + resourcePath + " does not exist.");
        }
        
        // check for parent wfs:
        wfTemp.setParentTemplate(getParentTemplate(wfTemp, result, results));
        // we can be sure that all parent-templates are available from here on
        checkVersion(wfTemp, result);
        checkName(wfTemp, result, results);
        checkNodes(wfTemp, result, results);
        checkRoles(wfTemp, result, results);
        checkActionRoles(wfTemp, result);
        checkActionDescription(wfTemp, result, results);
        checkDataeditTargets(wfTemp, result, results);
        checkNotificationTargets(wfTemp, result, results); 
        checkSubworkflowExists(wfTemp, result);
        checkSendEventAction(wfTemp, result, results);
        checkScriptAction(wfTemp, result, results);
        checkDataCondition(wfTemp, result, results);
        checkDataIsValid(wfTemp, result);
        checkStdEvents(wfTemp, result);
        
        checkWorkflowConfig(wfTemp, result);
        
    }   
    
    
    
    /**
     *  check if the required SWAMP version matches the actual SWAMP version
     */
    private void checkVersion(WorkflowTemplate wfTemp, WorkflowReadResult result) {
        String reqVer = wfTemp.getRequiredSWAMPVersion();
        String swampVer = swamp.getProperty("SWAMP_VERSION");
        if (reqVer.compareTo(swampVer) > 0) {
            Logger.DEBUG("" + reqVer.compareTo(swampVer));
            result.addError("Workflow requires SWAMP Version >= " + reqVer + 
                    " (is " + swampVer + ")");
        }
    }
    
    /**
     *  check if the workflows name is valid
     */
    private void checkName(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        if (wfTemp.getName().indexOf("-") >= 0 || wfTemp.getName().indexOf(".") >= 0) {
            result.addWarning("Please don't use the characters \"- .\" in the workflow name. " + 
                    "(should be a valid java package name)");
        }
        try {
            checkReferencedDatabits(wfTemp.getDescription(), wfTemp, result, results);
        } catch (Exception e) {
            result.addError("Description of : " + wfTemp.getName() + 
                    " contains errors: " + e.getMessage());
        }
    }
    
    /**
     *  check for required roles, and if their storeage databits are available
     */
    private void checkRoles(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        if (wfTemp.getWorkflowRole(WorkflowRole.USER) == null){
            result.addError("Role user is not defined.");
        }
        if (wfTemp.getWorkflowRole(WorkflowRole.OWNER) == null){
            result.addError("Role owner is not defined.");
        } else if (!(wfTemp.getWorkflowRole(WorkflowRole.OWNER) instanceof DatabitRole))
            // role owner must have a roledatabit: 
            result.addError("Role owner must have a valid roledatabit to store the owner.");
        if (wfTemp.getWorkflowRole(WorkflowRole.ADMIN) == null){
            result.addError("Role admin is not defined.");
        }
        if (wfTemp.getWorkflowRole(WorkflowRole.STARTER) == null){
            result.addError("Role starter is not defined.");
        }
        for (Iterator it = wfTemp.getRoles().iterator(); it.hasNext(); ){
            WorkflowRole role = (WorkflowRole) it.next();
            role.verify(result, wfTemp, this, results);  
            if (role.getName().equalsIgnoreCase(WorkflowRole.SYSTEMROLE)){
                result.addError(WorkflowRole.SYSTEMROLE + " is a reserved role-name.");
            }
        }
    }
    
    
    /**
     *  check for 1 startnode, >= 1 endnode, 
     *  every node but the startnode needs an incoming edge, 
     *  endnodes should not have leaving edges;
     *  check for duedate references;
     */
    private void checkNodes(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        int startnodes = 0, endnodes = 0;
        for (Iterator it = wfTemp.getAllNodeTemplates().values().iterator(); it.hasNext();) {
            NodeTemplate node = (NodeTemplate) it.next();
            boolean hasIncoming = false;
            if (node.getType().equalsIgnoreCase("end")) {
                endnodes++;
                if (node.getEdgeTempls().size() > 0)
                    result.addError("EndNode: " + node.getName() + " has leaving edges.");
            }
            if (node.getType().equalsIgnoreCase("start"))
                startnodes++;
            else {
                // do we have an incoming edge?
                for (Iterator eit = wfTemp.getAllEdgeTempls().iterator(); eit.hasNext();) {
                    EdgeTemplate edge = (EdgeTemplate) eit.next();
                    if (edge.getToId().equals(node.getName())) {
                        hasIncoming = true;
                        break;
                    }
                }
                if (!hasIncoming) {
                    result.addError("Node: " + node.getName() + " has no incoming edge.");
                }
            }

            if (node.hasDueDate()) {
            	DatabitTemplate dbit = getDatabitTemplate(node.getDueDateReference(), wfTemp, result, results);
                if (dbit == null) {
                    result.addWarning("Duedate databit: " + node.getDueDateReference()
                            + " does not yet exist in the workflow.");
                } else {
                    if (!dbit.getType().equalsIgnoreCase("date")){
                        result.addError("Duedate databit is not of type <date>");
                    }
                }
            }
            
            // check outgoing edge targets
            for (Iterator edgeIt = node.getEdgeTempls().iterator(); edgeIt.hasNext(); ){
                EdgeTemplate edge = (EdgeTemplate) edgeIt.next();
                if (!wfTemp.getAllNodeTemplates().containsKey(edge.getToId()))
                        result.addError("Target node: " + edge.getToId() + " not found");
            }
        }
        
        if (startnodes != 1) {
            result.addError("Workflow has !=1 startnodes.");
        }
        if (endnodes == 0){
            result.addError("Workflow has no endnode.");
        }
    }
    
    /**
     *  check the action description and referenced databits
     */
    private void checkActionDescription (WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext(); ){
            ActionTemplate actionTemplate = (ActionTemplate) it.next();
            String description = actionTemplate.getDescription();
            String longDesc = actionTemplate.getLongDescription();
            try {
                checkScript(new ScriptTemplate(description), wfTemp, result, results);
                checkScript(new ScriptTemplate(longDesc), wfTemp, result, results);
            } catch (Exception e) {
                result.addError("Description of : " + actionTemplate.getName() + 
                        " contains errors: " + e.getMessage());
            }
        }
    }
    
    /**
     *  check if tha action description compiles
     */
    private void checkActionRoles (WorkflowTemplate wfTemp, WorkflowReadResult result) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext(); ){
            ActionTemplate actionTemplate = (ActionTemplate) it.next();
            String roleName = actionTemplate.getRoleName();
            
            if (actionTemplate instanceof UserActionTemplate){
                UserActionTemplate uactionTemplate = (UserActionTemplate) actionTemplate;
                if (uactionTemplate.isRestricted()){ 
                	boolean roleExists = wfTemp.roleExists(roleName);
                	if (roleName == null || !roleExists){
	                    result.addError("Restricted action " + actionTemplate.getName() + 
	                            " needs a valid role to check against.");
                        }
                }
            }
            
            if (roleName != null && !roleName.equals(WorkflowRole.SYSTEMROLE) && 
                    !wfTemp.roleExists(roleName)){
                result.addError("Referenced role: \"" + roleName + "\" from action: " + 
                        actionTemplate.getName() + " is not defined!");
            }
        }
    }
    
	
    /**
     * check if dataedit databits exist, 
     */
    private void checkDataeditTargets(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext(); ){
            ActionTemplate actionTemplate = (ActionTemplate) it.next();
            if (actionTemplate instanceof DataeditActionTemplate) {
                for (Iterator ait = ((DataeditActionTemplate) actionTemplate).getFieldTempls()
                        .iterator(); ait.hasNext();) {
                    FieldTemplate field = (FieldTemplate) ait.next();
                    DatabitTemplate dbit = getDatabitTemplate(field.getPath(), wfTemp, result, results);
                    if (dbit == null) {
                        result.addError(actionTemplate.getName() + " referenced databit: " + field.getPath() + " not available.");
                    }
                }
            }
        }
    }
    
    
    
    /**
     * check if referenced subworkflows exist in startsubworkflowaction 
     * and subsfinishedcondition
     */
    private void checkSubworkflowExists(WorkflowTemplate wfTemp, WorkflowReadResult result) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext(); ){
            ActionTemplate action = (ActionTemplate) it.next();
            if (action instanceof StartSubworkflowActionTemplate) {
            	StartSubworkflowActionTemplate saction = 
            			(StartSubworkflowActionTemplate) action;
            	// must check in the filesystem as the template may not yet be created
            	if (!(new File(swamp.getWorkflowLocation() + System.getProperty("file.separator") + 
            			saction.getSubname() + System.getProperty("file.separator") + 
            			saction.getVersion()).exists())){
            		result.addError("Template for subworkflow: " + saction.getSubname() + 
            				"-" + saction.getVersion() + "not found");
            	}
            }
        }
        for (Iterator it = wfTemp.getAllEdgeTempls().iterator(); it.hasNext(); ){
            EdgeTemplate edge = (EdgeTemplate) it.next();
            for (Iterator cit = edge.getAllConditionTemplates().iterator(); cit.hasNext(); ){
                ConditionTemplate cond = (ConditionTemplate) cit.next();
                if (cond instanceof SubsFinishedConditionTemplate){
                    SubsFinishedConditionTemplate scond = (SubsFinishedConditionTemplate) cond;
                    if (!(new File(swamp.getWorkflowLocation() + System.getProperty("file.separator") + 
                            scond.getSubname() + System.getProperty("file.separator") + 
                            scond.getSubversion()).exists())){
                        result.addError("SubsFinishedCondition Subworkflow: " + scond.getSubname() + 
                                "-" + scond.getSubversion() + "not found");
                    }
                }
            }
        }
    }
    
    
    
    /**
     * check if sendeventaction is valid 
     */
    private void checkSendEventAction(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext(); ){
            ActionTemplate action = (ActionTemplate) it.next();
            if (action instanceof SendEventActionTemplate) {
            	SendEventActionTemplate saction = 
            			(SendEventActionTemplate) action;
            	// databit available?
            	if (saction.getTriggerDatabit() != null){
            		DatabitTemplate dbit = getDatabitTemplate(saction.getTriggerDatabit(), 
            				wfTemp, result, results);
            		if (dbit == null) {
            			result.addError("Sendeventactions databit: " + 
            				saction.getTriggerDatabit() + " not available.");
            		} else if (!dbit.getType().equals("datetime") && !dbit.getType().equals("date")){
            			result.addError("Sendeventactions databit: " + 
                				saction.getTriggerDatabit() + " must be of type date or datetime.");
            		}
            	} 
            	// check for valid offset string
            	String offset = saction.getTriggerOffset();
                if (offset != null && !(offset.startsWith("+") && (offset.endsWith("d") || 
                		offset.endsWith("h") || offset.endsWith("m")) && 
                        (offset.length() > 2))){
                	result.addError("Sendeventactions offsetvalue: " + 
            				saction.getTriggerOffset() + " has wrong format.");
                }
            }
        }
    }
    
    
    /**
     * check if scriptaction is valid 
     */
    private void checkScriptAction(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext();) {
            ActionTemplate action = (ActionTemplate) it.next();
            if (action instanceof ScriptActionTemplate) {
                ScriptActionTemplate saction = (ScriptActionTemplate) action;
                checkScript(saction.getScript(), wfTemp, result, results);
            }
        }
    }

    /**
     * check if script is valid 
     */
    private void checkScript(ScriptTemplate script, WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        try {
            if (!checkedScripts.contains(script.getScript().trim())
                    && Boolean.valueOf(SWAMP.getInstance().getProperty("SCRIPT_VERIFY_ON_STARTUP", "true")).booleanValue()) {
                script.checkScript();
                checkedScripts.add(script.getScript().trim());
            }
            checkReferencedDatabits(script.getScript(), wfTemp, result, results);
        } catch (Exception e) {
            result.addError("Script: " + script.getDescription() + " contains errors: " + e.getMessage());
            //e.printStackTrace();
        }
    }
    
    
    /**
     * check if dataconditions are valid
     */
    private void checkDataCondition(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        for (Iterator it = wfTemp.getAllEdgeTempls().iterator(); it.hasNext(); ){
            EdgeTemplate edge = (EdgeTemplate) it.next();
            for (Iterator cit = edge.getAllConditionTemplates().iterator(); cit.hasNext(); ){
            	ConditionTemplate cond = (ConditionTemplate) cit.next();
            	if (cond instanceof DataConditionTemplate){
            		DataConditionTemplate dcond = (DataConditionTemplate) cond;
            		if (getDatabitTemplate(dcond.getField(), wfTemp, result, results) == null) {
            			result.addError("Datacondition referenced databit: " + dcond.getField() + 
            					" not available.");
                    } else if (dcond.getCheck().equals("regexp")) {
                    	try {
							new RE(dcond.getValue());
						} catch (RESyntaxException e) {
							result.addError("Syntax error in regexp: " + dcond.getValue() + 
        					" Error: " + e.getMessage());
						}
                    }
            	}
            }
        }
    }
    
    
    
    /**
     * check for valid notification receivers and 
     * avaiable mail-templates, 
     */
    private void checkNotificationTargets(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
        for (Iterator it = wfTemp.getAllActionTemplates().iterator(); it.hasNext(); ){
            ActionTemplate action = (ActionTemplate) it.next();
            if (action instanceof UserActionTemplate){
                UserActionTemplate useraction = (UserActionTemplate) action;
                if (!useraction.getNotificationTemplate().equals("")){
                    //Logger.DEBUG("Checking: " + resourcePath + useraction.getNotificationTemplate());
                    String path = resourcePath + separator + useraction.getNotificationTemplate();
                	if (!new File(path).exists()){
                        result.addWarning("Notificationtemplate " + useraction.getNotificationTemplate() + 
                                " does not exist.");
                    } else {
                        try {
                            String script = getStringFromFile(new File(path));
                            checkScript(new ScriptTemplate(script), wfTemp, result, results);
        				} catch (Exception e) {
        					result.addWarning("Notification-template of: " + useraction.getName() + 
                    				" contains errors: " + e.getMessage());
        				}
                    }
                }
            } else if (action instanceof NotifyActionTemplate){
                NotifyActionTemplate naction = (NotifyActionTemplate) action;
                if (!naction.getDbit().equals("") && 
                        getDatabitTemplate(naction.getDbit(), wfTemp, result, results) == null) {
                    result.addError("Databit for notification: " + naction.getDbit() + 
                            " does not exist in workflow"); 
                } else if (!naction.getRecipientrole().equals("") && 
                        wfTemp.getWorkflowRole(naction.getRecipientrole()) == null){
                    result.addError("Role for notification: " + naction.getRecipientrole() + 
                            " not found.");
                }
                String path = resourcePath + separator + naction.getMsgtemplate();
                if (!naction.getMsgtemplate().equals("") && 
                        !new File(path).exists()){
                    result.addError("Notificationtemplate " + naction.getMsgtemplate() + 
                    " does not exist.");
                } else {
                    try {
                        ScriptTemplate script = new ScriptTemplate(getStringFromFile(new File(path)));
                        script.setDescription("Notification: " + naction.getMsgtemplate());
                        checkScript(script, wfTemp, result, results);
    				} catch (Exception e) {
    					result.addError("Notification-template of: " + naction.getName() + 
                				" contains errors: " + e.getMessage());
    				}
                }
            }
        }   
    }
    
    
    
    /**
     * check if the content of the databittemplates is valid.
     */
    private void checkDataIsValid(WorkflowTemplate wfTemp, WorkflowReadResult result) {
        for (Iterator it = wfTemp.getAllDatabitPaths().iterator(); it.hasNext(); ){
            String path = (String) it.next();
            DatabitTemplate dbitTemplate = wfTemp.getDatabitTemplate(path);
            Databit dbit = dbitTemplate.getDatabit();
            if (dbit == null){
            	result.addError("Databit: " + path + " cannot be created. Datatype " 
            			+ dbitTemplate.getType() + " not correctly installed?");
            } else {
	            try {
	            	dbit.checkDataType(dbitTemplate.getDefaultValue());
	            } catch (Exception e) {
	                result.addError("Databit: " + path + " contains invalid data. (" + e.getMessage() + ")");
	            }
            }
        }
    }
    

    /**
     * check for standard events, for example PARENTWF_FINISHED for subworkflows
     */
    private void checkStdEvents(WorkflowTemplate wfTemp, WorkflowReadResult result) {
        if (wfTemp.getParentTemplate() != null && 
                !wfTemp.getWaitingForEvents().contains(Event.PARENTWORKFLOW_FINISHED)){
            result.addWarning("Subworkflow does not handle " + Event.PARENTWORKFLOW_FINISHED + ".");
        }
    }
    
    
    /**
     * TODO: check if the content workflow.conf is valid.
     */
    private void checkWorkflowConfig(WorkflowTemplate wfTemp, WorkflowReadResult result) {

    }
    
    
    private String getStringFromFile(File script) throws Exception {
        FileInputStream file = new FileInputStream(script);
        byte[] b = new byte[file.available()];
        file.read(b);
        file.close();
        return new String(b);
    }
    
 
    private WorkflowTemplate getParentTemplate(WorkflowTemplate wfTemp, WorkflowReadResult result, List results) {
	    WorkflowTemplate parentTemplate = null;
	    String parentWfName = wfTemp.getParentWfName();
	    String parentWfVersion = wfTemp.getParentWfVersion();
	    WorkflowReadResult parentResult = null;
	    
    	// do we have parentwfs that are not loaded yet? --> trigger load
	    if ((parentWfName != null && parentWfVersion != null) && !(parentWfName.equals(wfTemp.getName()) && 
	    		parentWfVersion.equals(wfTemp.getVersion()) )){
		    SWAMP swamp = SWAMP.getInstance();
	        // already loaded in results?
		    parentResult = getWfResultFromResults(results, parentWfName, parentWfVersion);
		    
		    // take parent from Wfmanager in running system
	    	if (parentResult == null && WorkflowManager.isInstantiated() && 
	        		WorkflowManager.getInstance().templateExists(parentWfName, parentWfVersion)){
	        	parentTemplate = WorkflowManager.getInstance().getWorkflowTemplate(parentWfName, parentWfVersion);
	        
	        } else if (parentResult == null) { // not yet loaded, try to load now
	            String workflowLoc = swamp.getWorkflowLocation();
	            File workflowDir = new File(workflowLoc);
	            WorkflowReader wfreader = null;
	            try {
					wfreader = new WorkflowReader(workflowDir);
					Logger.DEBUG("Loading wfTemplate: " + parentWfName + parentWfVersion + " because of dependency.");
					results.add(wfreader.readWorkflow(parentWfName, parentWfVersion, results));
				} catch (Exception e) {
					result.addError("Error loading parentTemplate: " + e.getMessage());
				}
	        }
	        // check if it's loaded into results now (may be already loaded with errors): 
	    	if (parentTemplate == null){
                if (parentResult == null) parentResult = getWfResultFromResults(results, parentWfName, parentWfVersion); 
	    		if ( parentResult != null && !parentResult.hasErrors()){
	    			parentTemplate = parentResult.getTemplate();
	    		} else {
	    			 result.addError("Parent of " + wfTemp.getName() + wfTemp.getVersion() + " (" +  
	    					 parentWfName + parentWfVersion + ") could not be loaded, Reason: " + 
	    					 parentResult.toString());
	    		}
	    	}
	    }
	    return parentTemplate;
    }
    
	    
    public WorkflowReadResult getWfResultFromResults(List results, String name, String version) {
    	WorkflowReadResult wfResult = null;
    	for (Iterator it = results.iterator(); it.hasNext(); ){
    		WorkflowReadResult result = (WorkflowReadResult) it.next();
    		if (result.getWfName().equals(name) && result.getWfVersion().equals(version)) {
    			wfResult = result;
    			break;
    		}
    	}
    	return wfResult;
    }
    
    /**
     * get a DatabitTemplate, also try to load it from the parent workflows
     */
    public DatabitTemplate getDatabitTemplate(String path, WorkflowTemplate wfTemp, 
    		WorkflowReadResult result, List results) {
    	DatabitTemplate dbit = null;
    	if (wfTemp.containsDatabitTemplate(path)){
    		dbit = wfTemp.getDatabitTemplate(path);
    	} else if (getParentTemplate(wfTemp, result, results) != null){
    		dbit = getDatabitTemplate(path, getParentTemplate(wfTemp, result, results), result, results);
    	}
    	return dbit;
    }
    
    
    private void checkReferencedDatabits(String script, WorkflowTemplate wfTemp, 
            WorkflowReadResult result, List results) throws Exception {
        // get referenced databits in style: 
        // $wf.getDatabitValue("patchinfoset.packages")
        // #showdata ($wf "laufzettelset.packages")
        String myScript = new String(script);
        String errors = "";
        while (myScript.indexOf("wf.getDatabitValue(\"") >= 0){
            int pos = myScript.indexOf("wf.getDatabitValue(\"") + 20;
            int endpos = myScript.indexOf('"', pos);
            String path = myScript.substring(pos, endpos);
            if (getDatabitTemplate(path, wfTemp, result, results) == null){
                errors += "Script referenced databit: " + path + " not found\n";
            }
            myScript = myScript.substring(endpos);
        }
        myScript = new String(script);
        while (myScript.indexOf("#showdata ($wf \"") >= 0){
            int pos = myScript.indexOf("#showdata ($wf \"") + 16;
            int endpos = myScript.indexOf('"', pos);
            String path = myScript.substring(pos, endpos);
            if (getDatabitTemplate(path, wfTemp, result, results) == null){
                errors += "Script referenced databit: " + path + " not found\n";
            }
            myScript = myScript.substring(endpos);
        }
        if (!errors.equals("")) throw new Exception(errors);
    }    
    
}
