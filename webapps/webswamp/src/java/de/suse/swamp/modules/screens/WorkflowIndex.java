/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt
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

import java.io.*;
import java.util.*;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * Default WorkflowTemplate Index page
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */


public class WorkflowIndex extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        
        String uname = data.getUser().getName();
        WorkflowAPI wfapi = new WorkflowAPI();
        String template =  data.getParameters().get("wftemplate");
        WorkflowTemplate wftemp = wfapi.getWorkflowTemplate(template, uname);
        
        if (template == null){
            throw new UnknownElementException("Template: " + template + " could not be found!");
        }        
        
        String version = wftemp.getVersion();
        Collection roles = wfapi.getWorkflowRoles(template, version, uname);
        
        ArrayList filters = new ArrayList();
        
        PropertyFilter templatefilter = new PropertyFilter();
        templatefilter.addWfTemplate(template);
        filters.add(templatefilter);        
        int count = wfapi.getWorkflowIds(filters, null, uname).size();
        
        PropertyFilter closedfilter = new PropertyFilter();
        closedfilter.setClosed(true);
        filters.add(closedfilter);
        
        int closedcount = wfapi.getWorkflowIds(filters, null, uname).size();
        int runningcount =  count - closedcount;
        
        // get the latest workflow (latest id): 
        filters = new ArrayList();
        templatefilter.setWfId(1);
        templatefilter.setDescending();
        templatefilter.setLimit(10);
        List ids = wfapi.getWorkflowIds(filters, templatefilter, uname);
        if (ids != null && ids.size() > 0){
        	List latestwf = new ArrayList();
        	for (int i = 0; i<ids.size(); i++){
        		int wfid = ((Integer) ids.get(i)).intValue();
                try {
                    Workflow wf = wfapi.getWorkflow(wfid, uname);
                    latestwf.add(wf);
                } catch (SecurityException e) {
                    Logger.DEBUG("Do not show Wf-" + wfid + 
                    		" on index (no permission for " + uname + ")");
                }
        	}
            context.put("latestwf", latestwf);
        }
        
        // check for statistic graph
        String home = new SWAMPAPI().doGetProperty("swamp.home", uname);
        String fs = System.getProperty("file.separator");
        String imagePath = home + fs + ".." + fs + "var" + fs + "statistics" + fs + template + ".png";        
        if (new File(imagePath).exists()){
            context.put("statsImage", "/webswamp/var/statistics/" + template + ".png");
        }
        
        // add helplink:
        addHelplink(wftemp, context, uname);
        
        context.put("dbitpaths", wfapi.getAllDatabitTemplates(template, uname).keySet());
        context.put("dbits", wfapi.getAllDatabitTemplates(template, uname));
        
        context.put("runningcount", String.valueOf(runningcount));
        context.put("closedcount", String.valueOf(closedcount));
        context.put("wfroles", roles);
        context.put("wftemplate", wftemp);

    }
}