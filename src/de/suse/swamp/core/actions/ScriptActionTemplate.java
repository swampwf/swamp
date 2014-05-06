/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt (tschmidt@suse.de)
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

package de.suse.swamp.core.actions;

import java.util.*;

import org.apache.velocity.app.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.history.HistoryEntry;
import de.suse.swamp.core.tasks.WorkflowTask;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * ActionTemplate for script actions, that means actions 
 * that specify a piece of scripting code that gets executed when the 
 * action gets activated.
 * 
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;  
 */


public class ScriptActionTemplate extends SystemActionTemplate implements Scriptable {
	
	private ScriptTemplate script;
	
	
	public ScriptActionTemplate(String name, NodeTemplate nodeTemplate) {
		super (name, nodeTemplate);
	}

    public String getType() {
        return "script";
    }

    public ArrayList validate(Result result) {
        ArrayList errors = new ArrayList();
        // SystemTasks aren't validated
        // The script itself is validated on workflow load.
        return errors;
    }

    public void act(Result result, ResultList history) {
        //Logger.DEBUG("Starting ScriptAction with script: " + this.script);
        WorkflowManager wfMan = WorkflowManager.getInstance();
        Workflow wf = wfMan.getWorkflow(result.getWorkflowId());
        try {
            HashMap parameters = new HashMap();
            parameters.put("wf", wf);
            parameters.put("uname", result.getUname());
            parameters.put("bTools", new BugzillaTools());
            parameters.put("scriptapi", new ScriptApi(result.getUname(), wf, history));
            parameters.put("hist", history);
            parameters.put("datastates", new FieldMethodizer("de.suse.swamp.core.data.Data"));
            parameters.put("taskstates", new FieldMethodizer("de.suse.swamp.core.tasks.WorkflowTask"));
            parameters.put("executor", new Executor()); 
            script.setParameters(parameters);
            String resultString = script.evaluate();
            // maybe the workflow was altered, we need to save it.
            WorkflowManager.storeWorkflow(wf);
            String output = "Scriptaction: \"" + this.getDescription() + "\" was done."; 
            history.addResult(ResultList.MESSAGE, output);
        } catch (Exception e) {
            Logger.ERROR("Could not run script, error: " + e.getMessage());
            Logger.ERROR("Script was: " + script.getScript());
            history.addResult(ResultList.ERROR, "Scriptaction: \"" + this.getDescription() + "\" - FAILED:  \n" + e.getMessage());
        }        
    }
        
    
    public void act(Result result) {
        act(result, new ResultList());
    }

	
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        events.add(new Event("none", result.getWorkflowId(), result.getWorkflowId()));
        return events;
    }

    /**
     * @return Returns the script.
     */
    public ScriptTemplate getScript() {
        return script;
    }

    /**
     * @param script The script to set.
     */
    public void setScript(ScriptTemplate script) {
        this.script = script;
    }	
    
    
    /* inner class to expose some api methods to the script. 
     * Cannot invoke the api directly from the script because of 
     * possible fake usernames -> permission hi-jacking */
    public class ScriptApi {
        
        String uname;
        ResultList hist;
        Workflow wf;
        
        public ScriptApi(String uname, Workflow wf, ResultList hist){
            this.uname = uname;
            this.hist = hist;
            this.wf = wf;
        }
        
        public Workflow createSubWorkflow(String name, String version) throws Exception {
            WorkflowAPI wfapi = new WorkflowAPI();
            return wfapi.createWorkflow(name, uname, wf.getId(), null, version, true, this.hist);
        }

        public Workflow createSubWorkflow(String name, String version, int parentId, boolean started) throws Exception {
            WorkflowAPI wfapi = new WorkflowAPI();
            return wfapi.createWorkflow(name, uname, parentId, null, version, started, this.hist);
        }
        
        
        public void sendEvent(String eventString, int wfid) throws Exception {
            EventManager.handleWorkflowEvent(new Event(eventString, wf.getId(), wfid), uname, new ResultList());
        }
        
        public void sendNotification(String templatefile, String databit) throws Exception {
            NotificationManager.newNotification(0, wf.getId(), templatefile, databit, null, null, null);
        }
        
        public String getWfConfigItem(String name) {
            return wf.getTemplate().getConfigItem(name);            
        }
        
        public String getSWAMPProperty(String name) {
            return SWAMP.getInstance().getProperty(name);
        }

        public Map getLatestTaskDataFromHistory(List taskTemplateNames) throws Exception {
            String taskName = "", userName = "";
            HistoryAPI histapi = new HistoryAPI();
            ArrayList histlist = histapi.getHistoryEntries(wf.getId(), "TASK_DONE", uname);
            Date lastDate = new Date(0);
            for (Iterator histIt = histlist.iterator(); histIt.hasNext(); ){
                HistoryEntry h = (HistoryEntry) histIt.next();
                Date hDate = h.getWhen();
                WorkflowTask t = TaskManager.getTask(h.getItemID());
                for (Iterator tNameIt = taskTemplateNames.iterator(); tNameIt.hasNext();) {
                    String tName = tNameIt.next().toString();
                    if (t.getActionTemplate().getName().equals(tName)
                            && hDate.after(lastDate)) {
                        taskName = tName;
                        userName = h.getUserName();
                        lastDate = hDate;
                        break;
                    } //if
                } //for
            } //for

            String userEmail = de.suse.swamp.core.container.SecurityManager.getUser(userName).getEmail();
            Map hTaskData = new HashMap();
            hTaskData.put("taskname", taskName);
            hTaskData.put("username", userName);
            hTaskData.put("useremail", userEmail);
            hTaskData.put("taskdate", lastDate.toString());
            return hTaskData;
        } //method

    }

    }
