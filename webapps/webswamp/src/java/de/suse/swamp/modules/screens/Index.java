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

import java.io.*;
import java.util.*;

import org.apache.turbine.services.pull.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.modules.actions.*;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;

/**
 * Index page
 *
 * @author Doris Baum &lt;dbaum@suse.de&gt;
 * @author Thomas Schmidt
 */


public class Index extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);

		try {
			ContextHelp help = new DocumentationAPI().
				getContextHelp("help.Index", data.getUser().getName());
			ArrayList helps = new ArrayList();
			helps.add(help);
			context.put("helps", helps);
		} catch (StorageException e) {
			Logger.ERROR("Unable to fetch Help for Index");
		}
        
        String uname = data.getUser().getName();
        String home = new SWAMPAPI().doGetProperty("swamp.home", uname);
        String fs = System.getProperty("file.separator");
        String imagePath = home + fs + ".." + fs + "var" + fs + "statistics" + fs + "workflows.png";
        context.put("standardlogo", "true");
        
        if (new File(imagePath).exists()){
            context.put("statsImage", "/webswamp/var/statistics/workflows.png");
        }
        
        // reset interface
        SwampUIManager uitool = (SwampUIManager) TurbinePull.getTool(context, "ui");
        SwampUIManager.setInterface(data.getUser(), "");
        uitool.init(data.getUser());
        
        // provide a list of valid workflowtemplates for generating the menue
        ArrayList myTemplateNames = new ArrayList();
        try {
            ArrayList mytemplates = new ArrayList();
            HashMap templates = new WorkflowAPI().getWorkflowTemplates(uname);
            for (Iterator it = templates.keySet().iterator(); it.hasNext(); ){
                String tempname = (String) it.next(); 
                TreeMap versionMap = (TreeMap) templates.get(tempname); 
                WorkflowTemplate latest = (WorkflowTemplate) versionMap.get(versionMap.lastKey());
                mytemplates.add(latest);
                myTemplateNames.add(latest.getName());
            }
            context.put("latesttemplates", mytemplates);
        } catch (NoSuchElementException e) {
            // the user may be logged-in + invalid, we have to log him out then
            // user got deleted for example
            new LoginActions().doLogoutuser(data, context);
        }  

        context.put("templates", new SWAMPHashSet(myTemplateNames));
    }
}