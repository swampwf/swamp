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
 * Displays a workflow instance
 *
 * @author Doris Baum &lt;dbaum@suse.de&gt;
 * @author Thomas Schmidt
 */

import java.util.*;

import org.apache.turbine.services.pull.*;
import org.apache.turbine.util.*;
import org.apache.turbine.util.uri.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.api.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;

public class DisplayWorkflow extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {

		super.doBuildTemplate(data, context);
		if (data.getParameters().containsKey("workflowid")) {
			int wfid = data.getParameters().getInt("workflowid");
			String uname = data.getUser().getName();
			WorkflowAPI wfapi = new WorkflowAPI();
			Workflow workflow = wfapi.getWorkflow(wfid, uname);

			if (workflow != null) {
				context.put("datasets", workflow.getDatasets());

				WorkflowTemplate wftemp = workflow.getTemplate();
				TaskAPI taskapi = new TaskAPI();

                ArrayList tasks = taskapi.getUserTasks(workflow.getId(), true, uname);
                ArrayList optionalTasks = taskapi.getUserTasks(workflow.getId(), false, uname);
                ArrayList nods = workflow.getAllNodes();

				context.put("nodes", nods);
				context.put("tasks", tasks);
				context.put("optionalTasks", optionalTasks);
				context.put("workflow", workflow);
                context.put("masterworkflow", wfapi.getWorkflow(workflow.getMasterParentWfId(), uname));
				context.put("showdata", data.getParameters().getBooleanObject("showdata", Boolean.FALSE));
				context.put("dataedit", data.getParameters().getBooleanObject("dataedit", Boolean.FALSE));
				context.put("showadmin", data.getParameters().getBooleanObject("showadmin", Boolean.FALSE));
                context.put("subwflist", new ArrayList());

				// provide active time triggers:
                ArrayList timeTasks = new ArrayList();
                List allTasks = taskapi.getTasksForWorkflow(wfid, uname);
                for (Iterator it = allTasks.iterator(); it.hasNext(); ){
                    WorkflowTask task = (WorkflowTask) it.next();
                    if (task.getActionTemplate() instanceof SendEventActionTemplate){
                        timeTasks.add(task);
                    }
                }
                context.put("timeTasks", timeTasks);

                // provide the fields of the attached datasets and highlight
				// error-fields, add rootset
				LinkedHashMap rootmap;
				try {
                    Dataset dset = workflow.getDataset(workflow.getTemplate().getDefaultDsetName());
					rootmap = dset.getAllFields();
				} catch (RuntimeException e) {
					e.printStackTrace();
					throw new Exception("Error getting Dataset of Workflow #" + workflow.getId());
				}

                // if we have errors, highlight the fields:
				setErrorFields(context, rootmap);

				// add other datasets
				ArrayList setmaps = new ArrayList();
				for (Iterator it = workflow.getDatasets().iterator(); it.hasNext(); ){
					String name = ((Dataset) it.next()).getName();
					if (!name.equals(workflow.getTemplate().getDefaultDsetName())){
						Dataset dset = workflow.getDataset(name);
						LinkedHashMap map = dset.getAllFields();
						setErrorFields(context, map);
						setmaps.add(map);
					}
				}
				context.put("rootmap", rootmap);
				context.put("setmaps", setmaps);

				addHelplink(wftemp, context, uname);

                String histURL = getHistoryURL(data, wfid, 1);
                context.put("historyLabel", "show");

                if (data.getParameters().containsKey("history")) {
                    int showHistory = data.getParameters().getInt("history");
                    if (showHistory > 0) {
                        	provideHistoryList(data, context);
                        context.put("showHistory", "1");
                        context.put("historyLabel", "hide");
                        context.put("histType", data.getParameters().get("historyType"));
                        histURL = getHistoryURL(data, wfid, 0);
                    }
                }
                context.put("toggleHistoryURL", histURL);
                
                // set webswamp interface to current workflow type
                Workflow masterwf = wfapi.getWorkflow(workflow.getMasterParentWfId(), uname);
                SwampUIManager uitool = (SwampUIManager) TurbinePull.getTool(context, "ui");
                SwampUIManager.setInterface(data.getUser(), masterwf.getTemplateName());
                uitool.init(data.getUser());

            } else {
                setErrorScreen(data, context, "in DisplayWorkflow: invalid Workflow-ID.");
            }
        } else {
            Logger.ERROR("ERROR in DisplayWorkflow: no workflow id.");
            setErrorScreen(data, context, "in DisplayWorkflow: no Workflow-ID.");
        }
    }

    private String getHistoryURL(final RunData data, final int wfID, final int showFlag) {
        TemplateURI uri = new TemplateURI(data, "DisplayWorkflow.vm");
        uri.addPathInfo("workflowid", wfID);
        uri.addPathInfo("history", showFlag);
		uri.addPathInfo("historyType", "Tasks");
        return uri.getAbsoluteLink();
    }



    private ArrayList provideHistoryList(RunData data, Context context) throws Exception {
        ArrayList historyEntries = new ArrayList();
        String historyType = data.getParameters().get("historyType");
        int wfid = data.getParameters().getInt("workflowid");
        HistoryAPI histapi = new HistoryAPI();
        String uname = data.getUser().getName();
        ArrayList histlist = null;
        if (historyType == null || historyType.equals("")){
            histlist =  histapi.getHistoryEntries(wfid, uname);
        } else if (historyType.equals("Tasks")){
            histlist =  histapi.getHistoryEntries(wfid, "TASK", uname);
        } else if (historyType.equals("SystemTasks")){
            histlist =  histapi.getHistoryEntries(wfid, "SYSTEMTASK", uname);
        } else if (historyType.equals("Events")){
            histlist =  histapi.getHistoryEntries(wfid, "EVENT", uname);
        } else if (historyType.equals("Notifications")){
            histlist =  histapi.getHistoryEntries(wfid, "NOTIFICATION", uname);
        } else if (historyType.equals("Nodes")){
            histlist =  histapi.getHistoryEntries(wfid, "NODE", uname);
        } else if (historyType.equals("Data")){
            // provide a list of "watched" databit-ids:
            Workflow wf = new WorkflowAPI().getWorkflow(wfid, uname);
            ArrayList bitIds = new ArrayList();
            for (Iterator it = wf.getAllDatabitPaths().iterator(); it.hasNext(); ){
                Databit dbit = wf.getDatabit((String) it.next());
                if (dbit.getState() != Data.HIDDEN){
                    bitIds.add(new Integer(dbit.getId()));
                }
            }
            histlist =  histapi.getHistoryEntries(bitIds, "DATA", uname);
        }
        context.put("historyList", histlist);
        return historyEntries;
    }




    private void setErrorFields(Context context, LinkedHashMap map){
        // if we have errors, highlight the fields:
        if (context.get("result") != null) {
            HashMap errorFields = ((DataeditResult) context.get("result")).getErrorFields();

            // go through all errorfields:
            for (Iterator it = errorFields.keySet().iterator(); it.hasNext(); ) {
                String errorFieldPath = (String) it.next();
                String errorFieldValue = (String) errorFields.get(errorFieldPath);

                for (Iterator it2 = map.keySet().iterator(); it2.hasNext(); ){
                    String set = (String) it2.next();
                    ArrayList bits = (ArrayList) map.get(set);
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
    }





}
