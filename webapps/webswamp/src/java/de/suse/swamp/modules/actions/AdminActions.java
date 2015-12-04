/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt@suse.de>
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

import java.util.*;

import org.apache.turbine.modules.*;
import org.apache.turbine.om.security.*;
import org.apache.turbine.services.schedule.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.api.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.modules.scheduledjobs.*;
import de.suse.swamp.turbine.services.security.*;
import de.suse.swamp.util.*;

/**
 * Actions for workflow and template management
 *
 * @author <a href="mailto:dbaum@suse.de">Doris Baum</a>
 * @author tschmidt@suse.de
 */
public class AdminActions extends SecureAction {

    /**
     *  IMPORTANT: doXxx methods may only have ONE upper case letter X
     *  after the do, anything else must be lower case.
     */

    /**
     * Reloads the Workflow Templates
     */
    public void doTemplreload(RunData data, Context context) throws Exception {
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        List results = new ArrayList();
        
        // check if we reload all templates or only one type: 
        if (data.getParameters().get("templatename") != null && 
                data.getParameters().get("templateversion") != null){
            results = wfapi.reloadWorkflowDefinitions(uname, 
                    data.getParameters().get("templatename"), 
                    data.getParameters().get("templateversion"));
        } else if (data.getParameters().get("templatename") != null){
            results = wfapi.reloadWorkflowDefinitions(uname, 
                    data.getParameters().get("templatename"));
        } else {
            results = wfapi.reloadAllWorkflowDefinitions(uname);
        }
        
        context.put("workflowreadresults", results );
    }
    
    
    public void doFullgc(RunData data, Context context) throws Exception {
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        long freeMem2 = wfapi.doFullgc(uname);
        context.put("freeMem2", String.valueOf(freeMem2));
    }
    
    public void doEmptycaches(RunData data, Context context) throws Exception {
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        wfapi.doEmptywfcache(uname);
        DataAPI dataapi = new DataAPI();
        dataapi.doEmptyDatacache(uname);
        // empty task-cache
        new TaskAPI().doEmptyTaskcache(uname);
        // empty user-cache
        new SecurityAPI().doEmptyUsercache(uname);
    }
    
    
    public void doChangeuser(RunData data, Context context) throws Exception {
        SecurityAPI secApi = new SecurityAPI();
        String username = data.getUser().getName();
        String targetUser = data.getParameters().getString("targetuser");
        Logger.DEBUG("Changing user from " + username + " to " + targetUser);
        User myuser = new SWAMPTurbineUser(secApi.getUser(targetUser, username));
        // Store the user object.
        data.setUser(myuser);
        // Mark the user as being logged in.
        myuser.setHasLoggedIn(new Boolean(true));
        // Save the User object into the session.
        data.save();
    }
    
 
    
    /**
     * Manually trigger a job of the scheduler
     */
    public void doRunjob(RunData data, Context context) throws Exception {
        
        SWAMPUser user = ((SWAMPTurbineUser) data.getUser()).getSWAMPUser();
        
        if (!de.suse.swamp.core.container.SecurityManager.isGroupMember(user, "swampadmins"))
            throw new SecurityException("No permission (needs group swampadmins)");
            
        int job = data.getParameters().getInt("job");
        JobEntry jobEntry = TurbineScheduler.getJob(job);
        SWAMPScheduledJob schedJob = (SWAMPScheduledJob) ScheduledJobLoader.getInstance().getInstance(jobEntry.getTask()); 
        schedJob.run(jobEntry);        
        
        context.put("history", schedJob.getResults());
        context.put("statusheader", "Success");
        context.put("statusclass", "success");
        context.put("icon", "ok");
    }

    
    /**
     * Manually run a script
     */
    public void doRunScript(RunData data, Context context) throws Exception {
        SWAMPUser user = ((SWAMPTurbineUser) data.getUser()).getSWAMPUser();
        if (!de.suse.swamp.core.container.SecurityManager.isGroupMember(user, "swampadmins"))
            throw new SecurityException("No permission (needs group swampadmins)");

        int wfId = data.getParameters().getInt("wfid");
        String language = data.getParameters().getString("language");
        String script = data.getParameters().getString("script");
        Workflow wf = new WorkflowAPI().getWorkflow(wfId, user.getUserName());
        ResultList results = new ResultList();

        ScriptTemplate scriptTemplate = new ScriptTemplate(script);
        ScriptActionTemplate scriptAction = new ScriptActionTemplate(null, null);
        scriptTemplate.setLanguage(language);
        HashMap parameters = new HashMap();
        parameters.put("wf", wf);
        parameters.put("scriptapi", scriptAction.new ScriptApi(user.getUserName(), wf, results));
        scriptTemplate.setParameters(parameters);
        String result = scriptTemplate.evaluate();
        results.addResult(ResultList.MESSAGE, "Script result: " + result);
        
        context.put("script", script);
        context.put("wfid", "" + wfId);
        context.put("language", language);
        context.put("history", results);
        context.put("statusheader", "Success");
        context.put("statusclass", "success");
        context.put("icon", "ok");
    }
}
