/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Doris Baum <dbaum@suse.de>
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

package de.suse.swamp.modules.screens;

/**
 * Displays a task
 *
 * @author Doris Baum &lt;dbaum@suse.de&gt;
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

import java.util.*;

import org.apache.turbine.services.pull.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.api.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.history.*;
import de.suse.swamp.core.security.SWAMPUser;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.turbine.services.security.SWAMPTurbineUser;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;

public class DisplayTask extends SecureScreen {
	
	I18n i18n = new I18n(getClass(), "de.suse.swamp.webswamp.i18n.Webswamp");

    public void doBuildTemplate(RunData data, Context context) throws Exception {

        super.doBuildTemplate(data, context);
        SWAMPUser user = ((SWAMPTurbineUser) data.getUser()).getSWAMPUser();
        
        if (data.getParameters().containsKey("taskid")) {
            int taskId = data.getParameters().getInt("taskid");
            TaskAPI taskapi = new TaskAPI();
            String uname = data.getUser().getName();
            
            //kick user directly if he has no right
            taskapi.canFinishTask(taskId, uname);
            
            WorkflowTask task = null;
            WorkflowAPI wfapi = new WorkflowAPI();
            DocumentationAPI docuapi = new DocumentationAPI();
            HistoryAPI histapi = new HistoryAPI();            
            String taskType = new String();
            int wfid = 0;
            try {
                task = taskapi.doGetTask(taskId, uname);
            	wfid = task.getWorkflowId();
                taskType = task.getActionType();
            } catch (RuntimeException e) {
                setErrorScreen(data, context, i18n.tr("No Task could be found for " + 
                        "TaskId: ", user) + taskId + "\n");
                throw e;
            }
            
            Workflow wf = wfapi.getWorkflow(wfid, uname);
            context.put("workflowid", new Integer(wfid));
            context.put("workflow", wf);
            context.put("masterwf", wfapi.getWorkflow(wf.getMasterParentWfId(), uname));
            context.put("subwflist", new ArrayList());
            
            // add workflows helplink
            addHelplink(wf.getTemplate(), context, uname); 
            
            if (task.getState() != WorkflowTask.ACTIVE){
                context.put("statusclass", "error");
                context.put("icon", "error");
                context.put("statusheader", i18n.tr("This Task is not active.", user));
                data.setScreenTemplate("Status.vm");
                StringBuffer statusmessage = new StringBuffer();
                statusmessage.append("<b>").append(i18n.tr("History of this Task:", user)).append("</b> \n");
                ArrayList histEntries = histapi.getHistoryEntries(wfid, taskId, "TASK_DONE", uname);
                for (Iterator it = histEntries.iterator(); it.hasNext(); ){
                    HistoryEntry entry = (HistoryEntry) it.next();
                    statusmessage.append("\n<b>" + entry.getWhenString() + ":</b> \n");
                    statusmessage.append(entry.getWhat(wfid) + "\n");
                    statusmessage.append(i18n.tr("done by: ", user) + entry.getWho() + "\n");
                }
                if (histEntries.size() == 0){
                	statusmessage.append("\n<b>No history entries found.</b> \n");
                }
                context.put("statusmessage", statusmessage.toString());
            
            // don't Display SystemAction Details
            } else if (task.getActionTemplate() instanceof SystemActionTemplate) {
                setErrorScreen(data, context, i18n.tr("DisplayTask is not supported " + 
                        "for showing SystemActions", user));
                Logger.ERROR("Tried to display System Task #" + task.getId());
            // Task is ACTIVE
            } else {
            
               UserActionTemplate uaction = (UserActionTemplate) task.getActionTemplate();
                // create Textoutput for the description: 
                
                if (uaction.getLongDescription().length() > 0){
                    String desc = uaction.getReplacedLongDescription(task.getWorkflowId());
                    context.put("desc", desc);
                }
                
                
               // add task-helplink:
                String actionhelpPath = uaction.getHelpContext();
        		ArrayList helps = new ArrayList();
        		if (context.get("helps") != null) {
        			helps = (ArrayList) context.get("helps");
        		}
        		ContextHelp help = null;
                if (actionhelpPath != null && !actionhelpPath.equals("")){
	                try {
	                	help = docuapi.getContextHelp(actionhelpPath, uname);
	                } catch (StorageException e1) {
	                    Logger.ERROR("Unable to fetch help for context: " + actionhelpPath);
	                }
                }
                if (help != null){
                    helps.add(help);
                    context.put("helps", helps);
                }
                
                if (taskType.equals("dataedit")) {
                    
                    DataeditActionTemplate action = (DataeditActionTemplate) task.getActionTemplate();
                    LinkedHashMap fields = action.getAllFields(task.getWorkflowId());

                    // if we have errors, highlight the fields:
                    if (context.get("result") != null) {
                        HashMap errorFields = ((DataeditResult) context.get("result")).getErrorFields();
                        
                        // go through all errorfields:
                        for (Iterator it = errorFields.keySet().iterator(); it.hasNext(); ) {
                            String errorFieldPath = (String) it.next();
                            String errorFieldValue = (String) errorFields.get(errorFieldPath);
                            
                            for (Iterator it2 = fields.keySet().iterator(); it2.hasNext(); ){
                                String set = (String) it2.next();
                                ArrayList bits = (ArrayList) fields.get(set);
                                for (Iterator it3 = bits.iterator(); it3.hasNext(); ){
                                    Field f = (Field) it3.next();
                                    if ((f.getPath()).equals(errorFieldPath)){
                                        f.setState(Data.ERROR);
                                        f.setInitValue(errorFieldValue);
                                    }
                                }
                            }
                        }
                    }
                    
                    context.put("title", action.getReplacedDescription(task.getWorkflowId()));
                    context.put("setnames", fields.keySet());
                    
                    // set values of fields if this is a second try
                    for (Iterator it = fields.keySet().iterator(); it.hasNext(); ){
                        ArrayList setfields = (ArrayList) fields.get(it.next());
                        for (Iterator it2 = setfields.iterator(); it2.hasNext(); ){
                            Field f = (Field) it2.next(); 
                            String fieldpath = f.getPath();
                            
                            if (data.getParameters().containsKey("field_" + fieldpath)) {
                                String value = data.getParameters().get("field_" + fieldpath);
                                f.setInitValue(value);
                            } else if (data.getParameters().containsKey("boolean_" + fieldpath)) {
                                // checkbox not checked
                                if (!data.getParameters().containsKey("field_" + fieldpath)) {
                                    f.setInitValue("false"); 
                                }
                            }
                        }   
                    }                   
                    context.put("setmap", fields);
                }
            }
            
            // set webswamp interface to current workflow type
            Workflow masterwf = wfapi.getWorkflow(wf.getMasterParentWfId(), uname);
            SwampUIManager uitool = (SwampUIManager) TurbinePull.getTool(context, "ui");
            SwampUIManager.setInterface(data.getUser(), masterwf.getTemplateName());
            uitool.init(data.getUser());

            context.put("task", task);
        } else {
            setErrorScreen(data, context, i18n.tr("in DisplayTask: no task id.", user));
            Logger.ERROR("in DisplayTask: no task id.");
        }
    }

}
