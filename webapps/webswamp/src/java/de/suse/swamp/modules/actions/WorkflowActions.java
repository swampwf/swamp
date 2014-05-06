/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de>
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

import org.apache.commons.collections.buffer.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.modules.screens.*;
import de.suse.swamp.turbine.services.security.*;
import de.suse.swamp.util.*;

/**
 * Actions for workflow and template management
 *
 * @author <a href="mailto:dbaum@suse.de">Doris Baum</a>
 */
public class WorkflowActions extends SecureAction {

    /**
     *  IMPORTANT: doXxx methods may only have ONE upper case letter X
     *  after the do, anything else must be lower case.
     */

    /**
     * Empty standard-action
     */
    public void doPerform(RunData data, Context context) throws Exception {
        throw new Exception("Workflowactions called without explicit method.");
    }


    /**
     * Starts new workflow of a template
     */
    public void doStartnewworkflow(RunData data, Context context) throws Exception {

            String templateName = data.getParameters().get("templatename");
            String templateVersion = data.getParameters().get("templateversion");

            Logger.DEBUG("Trying to start new " + templateName + " workflow case.");
            String username = data.getUser().getName();
            WorkflowAPI wfapi = new WorkflowAPI();

            if (templateVersion == null || templateVersion.equals("")) {
                templateVersion = wfapi.getWorkflowTemplate(templateName, username).getVersion();
                Logger.DEBUG("No WfVersion specified to start, assuming latest, " +
                        templateVersion);
            }

            ResultList history = new ResultList();
            Workflow wf = wfapi.createWorkflow(templateName, username, 0, null, templateVersion, true, history);

            context.put("statusmessage", "New workflow started with " + "SWAMPID: " + wf.getName());

            context.put("workflow", wf);
            context.put("workflowid", new Integer(wf.getId()));
            context.put("history", history);
            context.put("statusheader", "Success");
            context.put("statusclass", "success");
            context.put("icon", "ok");

            // check if we have one active usertask, and the user is an owner, then redirect to it:
            TaskAPI taskapi = new TaskAPI();
            List tasks = taskapi.getUserTasks(wf.getId(), true, username);
            if (tasks.size() == 1) {
                UserActionTemplate action = (UserActionTemplate) ((WorkflowTask) tasks.get(0)).getActionTemplate();
                if (!action.isRestricted() || wf.hasRole(username, action.getRoleName())) {
                    data.getParameters().add("taskid", ((Task) tasks.get(0)).getId());
                    setTemplate(data, "DisplayTask.vm");
                }
            }
    }




    /**
     * Is called when a task ok comes in.
     */
    public void doTaskok(RunData data, Context context) throws Exception {
        Logger.LOG("doTaskok() from webapp.");
        SWAMPUser user = ((SWAMPTurbineUser) data.getUser()).getSWAMPUser();
        WorkflowAPI wfapi = new WorkflowAPI();
        TaskAPI taskapi = new TaskAPI();
        ResultList history = new ResultList();

        if (data.getParameters().containsKey("taskid")) {
            // get the task we're working on
            int taskId = data.getParameters().getInt("taskid");
            WorkflowTask task = taskapi.doGetTask(taskId, user.getUserName());
            String taskType = null;
            ArrayList validationErrors = new ArrayList();

            // Check for availability of that Task:
            if (task != null && task.getState() == WorkflowTask.ACTIVE) {
                // get the action type of the task
                taskType = task.getActionType();

                // fill in the result for different
                if (taskType.equals("manualtask")) {
                    ManualtaskResult result = (ManualtaskResult) task.getResult();
                    result.setDone(true);

                } else if (taskType.equals("decision")) {
                    int answer = -1;
                    // get the answer given
                    if (data.getParameters().containsKey("answer")) {
                        answer = data.getParameters().getInt("answer");
                        Logger.DEBUG("Answer #" + answer);
                        // if no answer selected, log error
                    } else {
                        Logger.ERROR("in doTaskok: no answer on question given.");
                    }
                    // put selection into result
                    DecisionResult result = (DecisionResult) task.getResult();
                    result.setSelection(answer);
                } else if (taskType.equals("dataedit")) {
                    DataeditResult result = (DataeditResult) task.getResult();
                    context.put("result", result);

                    DataeditActionTemplate action = (DataeditActionTemplate) task.getActionTemplate();
                    HashMap actionFields = action.getAllFields(task.getWorkflowId());
                    Workflow wf = wfapi.getWorkflow(task.getWorkflowId(), user.getUserName());

                    // put all values in the result object
                    for (Iterator iter = actionFields.keySet().iterator();
                        iter.hasNext(); ) {
                        ArrayList setField = (ArrayList) actionFields.get(iter.next());
                        for (Iterator it = setField.iterator(); it.hasNext(); ){
                            Field f = (Field) it.next();
                            String fieldpath = f.getPath();
                            String field = "field_" + fieldpath;
                            if (data.getParameters().containsKey(field)) {
                                // binary data need extra storage
                                if (f.getDatatype().equals("fileref")){
                                    FileItem value = data.getParameters().getFileItem(field);
                                    Logger.DEBUG("Value for key (file)" + field + ": " + value);
                                    // need to store the file now
                                    Databit dbit = wf.getDatabit(fieldpath);
                                    if (DatapackActions.storeFile(dbit, true, value, user.getUserName())){
                                        String fileName = value.getName();
                                        // fix for browsers setting complete path as name: 
                                        if (fileName.indexOf("\\") >= 0)
                                            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                                        if (fileName.indexOf("/") >= 0)
                                            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                                        result.setValue(fieldpath, fileName);
                                    }
                                } else if (f.getDatatype().equalsIgnoreCase("multienum")) {
                                    SWAMPHashSet values = new SWAMPHashSet(data.getParameters().getStrings(field));
                                    result.setValue(fieldpath, values.toString(", "));
                                } else if (f.getDatatype().equalsIgnoreCase("patchdocumd")) {
                                    String value = data.getParameters().get(field);
                                    Logger.DEBUG("Value for key " + field + ": " + value);
                                    result.setValue(fieldpath, value);
                                } else {
                                    String value = StringEscapeUtils.unescapeHtml(
                                    		data.getParameters().get(field));
                                    Logger.DEBUG("Value for key " + field + ": " + value);
                                    result.setValue(fieldpath, value);
                                }
                            } else if (data.getParameters().containsKey("boolean_" + fieldpath)){
                                result.setValue(fieldpath, "false");
                            } else if (!f.isMandatory()){
                            	// don't complain about missing, non-mandatory fields
                            } else {
                                Logger.ERROR("Mandatory field " + fieldpath + " not set.");
                            }
                        }
                    }
                }
                // validate task result
                validationErrors = task.validate();

                // if everything is ok, try to finish the task
                if (validationErrors.size() == 0) {
                    try {
                        taskapi.finishTask(task, user.getUserName(), history);
                    } catch (Exception e) {
                        e.printStackTrace();
                        validationErrors.add(e.getMessage());
                    }
                }

                if (validationErrors.size() == 0) {
                    Logger.LOG("Webapp: Done with working on task with id " + task.getId());

                    WorkflowTask wftask = task;
                    Workflow wf = wfapi.getWorkflow(wftask.getWorkflowId(), user.getUserName());

                    context.put("statusheader", "Success");
                    context.put("statusmessage", "Task \"" + task.getReplacedDescription() + "\" done in workflow "
                            + wf.getName() + ".");

                    context.put("statusclass", "success");
                    context.put("icon", "ok");
                    context.put("history", history);
                    context.put("workflow", wf);

    				// add general Workflow Help
    				SWAMPScreen.addHelplink(wf.getTemplate(), context, user.getUserName());
    				ArrayList helps = new ArrayList();
    				if (context.get("helps") != null) {
    					helps = (ArrayList) context.get("helps");
    				}


                    // add helplinks if there are new Tasks:
                    if (wf.getActiveTasks().size() > 0) {
                        List activeTasks = wf.getActiveTasks();
                        for (Iterator it = activeTasks.iterator(); it.hasNext();) {
                            WorkflowTask helptask = (WorkflowTask) it.next();
                            String helpConext = helptask.getActionTemplate().getHelpContext();
                            if (helpConext != null && !helpConext.equals("")) {
                            	ContextHelp help = new DocumentationAPI().getContextHelp(helpConext, user.getUserName());
                                if (help != null && !helps.contains(help)) {
                                    helps.add(help);
                                }
                            }
                        }
                        context.put("helps", helps);
                    }

                    if (user.getPerm("taskpage", "results").equals("workflow")) {
                        Logger.DEBUG("Doing redirect to workflow page after task for " + user.getUserName());
                        setTemplate(data, "DisplayWorkflow.vm");
                    } else if (user.getPerm("taskpage", "results").equals("previous")) {
                        CircularFifoBuffer pageBuffer = (CircularFifoBuffer)
                        	data.getUser().getTemp("pageBuffer", new CircularFifoBuffer(2));
                        SWAMPHashMap params = (SWAMPHashMap) pageBuffer.get();
                        if (params != null && params.containsKey("template")) {
                        	Logger.DEBUG("Redirect to previous page (" +
                        			params.get("template") + ") for " + user.getUserName());
                        	data.getParameters().clear();
                        	for (Iterator it = params.keySet().iterator(); it.hasNext(); ) {
                        		String key = (String) it.next();
                        		data.getParameters().add(key, (String) params.get(key));
                        	}
                        	setTemplate(data, (String) params.get("template"));
                        } else {
                        	Logger.WARN("Desired redirect not possible, no pageBuffer");
                        }
                    }

                // if there were errors during validation, log the error
                } else {
                    // go back to the Task-Page
                    context.put("taskerror", "true");
                    setTemplate(data, "DisplayTask.vm");

                    Iterator errIter = validationErrors.iterator();
                    String message = "", error;
                    while (errIter.hasNext()) {
                        error = (String) errIter.next();
                        message = message + "<br />" + error;
                        Logger.ERROR(error);
                    }
                    message = message + "<p />Please correct the above mistake!";
                    context.put("statusclass", "error");
                    context.put("statusheader", "Error validating task");
                    context.put("statusmessage", message);
                    context.put("icon", "error");

                    // fix page buffer
                    CircularFifoBuffer pageBuffer = (CircularFifoBuffer)
                		data.getUser().getTemp("pageBuffer", new CircularFifoBuffer(2));
                    pageBuffer.add(pageBuffer.get());
                } // end validation

            } else {
            	// illegal task requested, redirect
            	setTemplate(data, "DisplayTask.vm");
            }

        } else {
            Logger.ERROR("in doTaskok: no task id.");
        } //end taskid
    } //end dotaskok



    /**
     * Gets Workflow.toString() from the given workflowid
     * @param data
     * @param context
     * @throws Exception when toString() couldn't be generated from the Workflow
     */
    public void doGetwftext(RunData data, Context context) throws Exception {
        if (data.getParameters().containsKey("workflowid")) {
            int wfId = data.getParameters().getInt("workflowid");
            WorkflowAPI wfapi = new WorkflowAPI();
            Workflow wf = wfapi.getWorkflow(wfId, data.getUser().getName());
            context.put("wftxt", wf.toString());
        } else {
	        context.put("statusmessage",
	        "No WorkflowId set!.");
	        context.put("statusclass", "error");
	        context.put("statusheader", "");
	        context.put("icon", "error");
	        Logger.ERROR("No WorkflowID set for getting toString().");
        }
    }


    /**
     * Gets Workflow.toXML() from the given workflowid
     * @param data
     * @param context
     * @throws Exception when XML couldn't be generated from the Workflow
     */
    public void doGetwfxml(RunData data, Context context) throws Exception {
            if (data.getParameters().containsKey("workflowid")) {
                int wfId = data.getParameters().getInt("workflowid");
                WorkflowAPI wfapi = new WorkflowAPI();
                Workflow wf = wfapi.getWorkflow(wfId, data.getUser().getName());
                context.put("wfxml", wf.toXML());
            } else {
    	        context.put("statusmessage",
    	        "No WorkflowId set!.");
    	        context.put("statusclass", "error");
    	        context.put("statusheader", "");
    	        context.put("icon", "error");
    	        Logger.ERROR("No WorkflowID set for getting toString().");
            }
        }




	public void doChangenodesactivity(RunData data, Context context) throws Exception {
		int wfid = data.getParameters().getInt("workflowid");
		WorkflowAPI wfapi = new WorkflowAPI();
		Workflow wf = wfapi.getWorkflowForWriting(wfid, data.getUser().getName());
		if (wf != null) {
			ResultList results = new ResultList();
			for (Iterator it = wf.getAllNodes().iterator(); it.hasNext();) {
				Node node = (Node) it.next();
				if (data.getParameters().containsKey(node.getName())) {
					boolean newState = data.getParameters().getBoolean(node.getName());
					if (node.isActive() != newState) {
						Logger.DEBUG("Adminaction: changing " + node.getName()
								+ " state from " + node.isActive() + " to "+ newState);
						if (newState) {
							node.activate(data.getUser().getName(), results);
						} else {
							node.deactivate();
						}
					}
					MileStone mileStone = node.getMileStone();
					if (mileStone != null && data.getParameters().containsKey(mileStone.getName())) {
					    newState = data.getParameters().getBoolean(mileStone.getName());
					    if (mileStone.isDisplayed() != newState) {
					        Logger.DEBUG("Adminaction: changing milestone display " + mileStone.getName()
	                                + " state from " + mileStone.isDisplayed() + " to "+ newState);
					        mileStone.setDisplayed(newState);
					    }
					}
				}
			}
			wfapi.storeWorkflow(wf, data.getUser().getName());
			context.put("history", results);
		} else {
			Logger.ERROR("Illegal Wf-ID: " + wfid + " in doChangenodesactivity");
		}
	}



	public void doRestartworkflow(RunData data, Context context) throws Exception {
		int wfid = data.getParameters().getInt("workflowid");
        WorkflowAPI wfapi = new WorkflowAPI();
        TaskAPI taskapi = new TaskAPI();
		Workflow wf = wfapi.getWorkflowForWriting(wfid, data.getUser().getName());
		taskapi.cancelAllTasks(wfid, data.getUser().getName());
		wf.deactivateAllNodes();
		wf.start(data.getUser().getName(), new ResultList());
	}





    /**
     * Store the uploaded workflow to a temp. directory and verify it
     */
    public void doUploadworkflow(RunData data, Context context) throws Exception {

        org.apache.turbine.util.parser.ParameterParser pp = data.getParameters();
        String separator = System.getProperty("file.separator");
        String tmpdir = System.getProperty("java.io.tmpdir");
        ArrayList results = new ArrayList();
        // storing file
        FileItem fi = pp.getFileItem("filename");
        if (fi == null || fi.getName() == null || fi.getSize() == 0){
            throw new Exception("Empty file uploaded.");
        }

        if (fi.getName().endsWith(".xml") || fi.getName().endsWith(".zip")){
            String uniqueid = String.valueOf(new Date().getTime());
            String  basePath = tmpdir + separator + uniqueid;
            FileUtils.forceMkdir(new File(basePath));
            WorkflowReadResult result = null;

            if (fi.getName().endsWith("xml")){
                File wfTmpFile = new File(basePath + separator + "workflow.xml");
                if (wfTmpFile.exists()) {
					wfTmpFile.delete();
				}
                fi.write(wfTmpFile);
                if (!wfTmpFile.exists()){
                    throw new Exception("Storing of file: " + fi.getName() + " failed.");
                }
            } else {
                // unpack uploaded workflow resource bundle
                de.suse.swamp.util.FileUtils.uncompress(fi.getInputStream(), basePath);
            }
            // read + verify the workflow:
            WorkflowReader reader = new WorkflowReader(new File(basePath));
            result = reader.readWorkflow(basePath);
            results.add(result);
            context.put("uniqueid", uniqueid );
            // clean uploaded stuff on errors
            if (result.hasErrors()){
                FileUtils.deleteDirectory(new File(basePath));
            }
        } else {
            throw new Exception("Only upload of single \"workflow.xml\" and \".zip\" packed workflow bundles is supported.");
        }
        context.put("workflowreadresults", results );
    }


    /**
     * Install an uploaded workflow
     */
    public void doInstallworkflow(RunData data, Context context) throws Exception {
        String separator = System.getProperty("file.separator");
        String tmpdir = System.getProperty("java.io.tmpdir");
        String uniqueid = data.getParameters().get("uniqueid");
        String basePath = tmpdir + separator + uniqueid;

        WorkflowReader reader = new WorkflowReader(new File(tmpdir + separator + uniqueid));
        WorkflowReadResult result = reader.readWorkflow(basePath);

        if (result.hasErrors()){
            throw new Exception("Uploaded template contains errors.");
        } else {
            Logger.DEBUG("No Errors detected, installing new workflow");
            String name = result.getTemplate().getName();
            data.getParameters().add("templatename", name);
            String version = result.getTemplate().getVersion();
            String wfPath = SWAMP.getInstance().getWorkflowLocation() + separator + name + separator + version;
            FileUtils.forceMkdir(new File(wfPath));
            FileUtils.copyDirectory(new File(basePath), new File(wfPath));
            WorkflowManager.getInstance().reloadWorkflowDefinition(name, version);
            context.put("statusmessage", "New workflow: " + name + " with version: " + version + " successfully installed.");
            context.put("statusclass", "success");
            context.put("statusheader", "Workflow installed");
            context.put("icon", "ok");
            // clean uploaded stuff
            FileUtils.deleteDirectory(new File(basePath));
        }
    }


    public void doRemoveworkflow (RunData data, Context context) throws Exception {
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        int wfId = data.getParameters().getInt("wfid", 0);
        wfapi.removeWorkflow(uname, wfId);
        context.put("statusmessage", "Workflow #" + wfId + " successfully removed.");
        context.put("statusclass", "success");
        context.put("statusheader", "Workflow removed");
        context.put("icon", "ok");
    }



    public void doChangeparentid (RunData data, Context context) throws Exception {
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        int wfId = data.getParameters().getInt("wfid", 0);
        int parentId = data.getParameters().getInt("parentid", 0);
        if (wfapi.changeParentId(wfId, parentId, uname)){
            context.put("statusmessage", "Parent ID successfully changed to: " + parentId);
            context.put("statusclass", "success");
            context.put("statusheader", "Workflow parent changed");
            context.put("icon", "ok");
        }
    }


}
