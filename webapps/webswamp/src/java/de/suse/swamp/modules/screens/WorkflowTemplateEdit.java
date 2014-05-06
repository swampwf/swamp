/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt
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

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;

/**
 * Default WorkflowTemplate Index page
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 *
 */


public class WorkflowTemplateEdit extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        
        String uname = data.getUser().getName();
        WorkflowAPI wfapi = new WorkflowAPI();
        String template =  data.getParameters().get("templatename");
        WorkflowTemplate wftemp = wfapi.getWorkflowTemplate(template, uname);
        
        if (template == null){
            throw new UnknownElementException("Template: " + template + " could not be found!");
        }
        
        if (wftemp.hasRole(uname, "admin")){
            context.put("isAdmin", "true");
        }
        
        // add helplink:
        addHelplink(wftemp, context, uname);
        context.put("wftemplate", wftemp);
        context.put("templateMap", wfapi.getWorkflowTemplates(uname));
        context.put("standardlogo", "true");

    }
}