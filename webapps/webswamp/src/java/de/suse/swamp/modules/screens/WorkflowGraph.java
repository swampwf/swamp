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
 * Gets workflows from workflow manager and displays them
 *
 * @author Doris Baum &lt;dbaum@suse.de&gt;
 * @version $Id$
 *
 */


import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;

public class WorkflowGraph extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {

        super.doBuildTemplate(data, context);
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = data.getUser().getName();
        
        // either draw from a concrete Workflow, or from a template
        if (data.getParameters().containsKey("workflowid")) {
            int wfid = data.getParameters().getInt("workflowid");
            Workflow workflow = wfapi.getWorkflow(wfid, data.getUser().getName());
            if (workflow != null) {
                context.put("wfid", String.valueOf(wfid));
                context.put("wfname", workflow.getReplacedDescription());
                context.put("graphURL", getGraphURL(workflow, uname, "9,13"));
                context.put("biggraphURL", getGraphURL(workflow, uname, null));
                context.put("templateName", workflow.getTemplateName());
            } else {
                setErrorScreen(data, context, "in WorkflowGraph: invalid Workflow-ID.");
            }
        } else if (data.getParameters().containsKey("workflowtemplate")
                && data.getParameters().containsKey("workflowversion")) {
            String wftemplate = data.getParameters().get("workflowtemplate");
            String wfversion = data.getParameters().get("workflowversion");
            WorkflowTemplate wftmpl = null;
            wftmpl = wfapi.getWorkflowTemplate(wftemplate, wfversion, data.getUser().getName());
            if (wftmpl != null) {
                Workflow wf = null;
                try {
                    wf = wftmpl.getWorkflow();
                } catch (Exception e) {
                    setErrorScreen(data, context, "Connot get Workflow from Template " + wftemplate + wfversion);
                }
                context.put("graphURL", getGraphURL(wf, uname, "9,13"));
                context.put("biggraphURL", getGraphURL(wf, uname, null));
                context.put("templateName", wftmpl.getName());
                context.put("templateVersion", wftmpl.getVersion());
                context.put("wfname", wftmpl.getName());
            } else {
                setErrorScreen(data, context, "in WorkflowGraph: invalid Workflow-ID.");
            }

        } else {
            Logger.ERROR("ERROR in WorkflowGraph: no workflow id or template.");
            setErrorScreen(data, context, "in WorkflowGraph: no Workflow-ID " + " or Template-Name given.");
        }
    }

    
    
    
    /**
     * FIXME: maybe we have to move this to the webapp package, 
     * because we are not using the API here, and the URL is 
     * webapp specific.
     */
    private String getGraphURL(final Workflow workflow, String uname, String size) throws Exception {
        String SWAMPHome = new SWAMPAPI().doGetProperty("swamp.home", uname);
        WorkflowDraw drawer = WorkflowDraw.getWorkflowDraw();
        
        String workflowgraph = drawer.drawWorkflow(workflow, size);
        
        String relpath = workflowgraph.replaceAll(SWAMPHome, "");
        relpath = relpath.replaceAll("/$", "");
        relpath = relpath.replaceAll("^/", "");
        // Dirty, because of swamp.home set to /web-inf dir.
        relpath = relpath.replaceAll("\\.\\./", "");

        String contextPath = WebSWAMP.relativeAppLink;

        String graphURL = contextPath + "/" + relpath;
        return graphURL;
    }

}
