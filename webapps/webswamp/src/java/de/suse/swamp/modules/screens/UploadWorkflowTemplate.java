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

import java.util.*;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;

/**
 * Default WorkflowTemplate Index page
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 *
 */


public class UploadWorkflowTemplate extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        String uname = data.getUser().getName();
        ArrayList errors = new ArrayList();
        ArrayList warnings = new ArrayList();
        
        // get uploaded template: 
        List results = (List) context.get("workflowreadresults");
        if (results == null || results.size() != 1){
            throw new Exception("Error in reading uploaded workflow definition.");
        }
        
        WorkflowReadResult result = (WorkflowReadResult) results.get(0);
        context.put("result", result);
        
        if (!result.hasErrors()){
            WorkflowTemplate wftemp = result.getTemplate();
            
            if (!wftemp.hasRole(uname, "admin")){
                warnings.add("You (" + uname + ") are not in the admin role of your " + 
                        "uploaded workflow. Please add yourself, otherwise you " + 
                        "will not be able to edit it afterwards.");
            }
            WorkflowManager wfman = WorkflowManager.getInstance();
            HashMap templates = wfman.getWorkflowTemplates();
            TreeMap versions = (TreeMap) templates.get(wftemp.getName());
            // templates of that name already exist
            if (versions != null){
                    WorkflowTemplate oldTemp = wfman.getWorkflowTemplate(wftemp.getName());
                    if (oldTemp != null && !oldTemp.hasRole(uname, "admin")){
                        errors.add("You (" + uname + ") are not in the admin role of the " + 
                                "stored workflow " + oldTemp.getName() +  
                                ". Thus you are not allowd to add/update a version.");
                }
            }

            if (wfman.getWorkflowTemplate(wftemp.getName(), wftemp.getVersion()) != null){
                warnings.add("The workflow: " + wftemp.getName() + " with version " + 
                        wftemp.getVersion() + " already exists in the system. " + 
                        "By proceeding, it will be overwritten. Be cautious, this can break your running workflows!");
            }            
        }
        
        context.put("warnings", warnings);
        context.put("errors", errors);
        context.put("standardlogo", "true");
        
    }
}