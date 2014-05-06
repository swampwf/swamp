/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt
 * Copyright (c) 2007 Novell Inc.
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

import org.apache.turbine.util.*;
import org.apache.velocity.app.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;
import de.suse.swamp.util.FileUtils;

/**
 * Generating Data for Ajax Requests
 */
public class AjaxData extends SecureAction {

    private org.apache.log4j.Logger log = org.apache.log4j.Logger
        .getLogger(AjaxData.class.getName());
    private static String fs = System.getProperty("file.separator");


    public void doPerform(RunData data, Context context) throws Exception {
    }


    /**
	 * Creates Usertasks HTML table
	 */
    public void doGetusertasks(RunData data, Context context) throws Exception {

        String username = data.getUser().getName();
        // provide personalized task list:
        WorkflowAPI wfapi = new WorkflowAPI();
        ArrayList filters = new ArrayList();
        MemoryTaskFilter taskfilter = new MemoryTaskFilter();
        taskfilter.setTaskOwner(username);
        taskfilter.setMandatoryOnly(true);
        filters.add(taskfilter);

        List wfs = wfapi.getWorkflows(filters, null, username);
        HashMap wfmap = new HashMap();
        int wfcount = 0;
        for (Iterator it = wfs.iterator(); it.hasNext(); ) {
            Workflow wf = (Workflow) it.next();
            if (!wfmap.containsKey(wf.getTemplateName())) {
            	wfmap.put(wf.getTemplateName(), new ArrayList());
            }
            ((ArrayList) wfmap.get(wf.getTemplateName())).add(wf);
            wfcount++;
        }

        ArrayList columns = new ArrayList();
        columns.add("column_wfdesc_nolink");
        columns.add("column_nexttasks");
        context.put("columns", columns);
        context.put("wfcount", new Integer(wfcount));
        context.put("wfmap", wfmap);
        context.put("alltemplates", wfapi.getAllWorkflowTemplateNames(username));

        // parse the Mytasks.content.vm template and provide used variables:
        File mytasksfile = new File(new SWAMPAPI().doGetProperty("swamp.home", username) + fs + ".." +
                fs + "templates" + fs + "app" + fs + "parts" + fs + "Mytasks.content.vm");
        sendResponse(data, context, FileUtils.getText(mytasksfile));
    }


    /**
     * Creates workflowstack HTML table
     */
    public void doGetwfstack(RunData data, Context context) throws Exception {
        String username = data.getUser().getName();
        int masterworkflowid = data.getParameters().getInt("masterworkflowid");
        context.put("masterworkflow", new WorkflowAPI().getWorkflow(masterworkflowid, username));
        context.put("highlightid", data.getParameters().getIntObject("highlightid"));
        context.put("stackmode", data.getParameters().getString("stackmode", "instance"));
        sendResponse(data, context, "#showwfstack($masterworkflow $!highlightid $!stackmode)");
    }

    /**
     * Creates bug info popup content
     */
    public void doGetBugInfo(RunData data, Context context) throws Exception {
        String username = data.getUser().getName();
        int bugId = data.getParameters().getInt("bugid");
        Hashtable bugInfo = new BugzillaTools().getBugData(bugId, new SWAMPHashSet("long_desc").toList());
        context.put("bugInfo", bugInfo);
        File bugInfoFile = new File(new SWAMPAPI().doGetProperty("swamp.home", username) + fs + ".." + fs + "templates"
                + fs + "app" + fs + "parts" + fs + "BugInfo.vm");
        sendResponse(data, context, FileUtils.getText(bugInfoFile));
    }
    
    
    private void sendResponse(RunData data, Context context, String templateText) throws IOException {
        // avoid processing of ScreenTemplate with permission checks etc.
        data.declareDirectResponse();
        data.setLayout("DirectResponseLayout");
        data.setCharSet("UTF-8");
        data.getResponse().setCharacterEncoding("utf-8");
        PrintWriter out = data.getResponse().getWriter();
        StringWriter w = new StringWriter();
        Velocity.evaluate(context, w, "Ajaxrender", templateText);
        // send result back to browser:
        out.println(w);
        out.flush();
        out.close();
    }

}
