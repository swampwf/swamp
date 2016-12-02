/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt
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
 * Base class for all SWAMP screens
 *
 * @author Thomas Schmidt
 */

import java.util.*;

import org.apache.commons.collections.buffer.*;
import org.apache.turbine.modules.screens.*;
import org.apache.turbine.util.*;
import org.apache.velocity.app.*;
import org.apache.velocity.context.*;
import org.apache.velocity.tools.generic.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class SWAMPScreen extends VelocitySecureScreen {


    public void doBuildTemplate(RunData data, Context context) throws Exception {
    	provideData(data, context);
    }

    public void provideData(RunData data, Context context) throws Exception {
        SWAMPAPI swampapi = new SWAMPAPI();
        String uname = data.getUser().getName();
        // make stuff available to all pages:
        context.put("datastates", new FieldMethodizer("de.suse.swamp.core.data.Data"));
        context.put("taskstates", new FieldMethodizer("de.suse.swamp.core.tasks.WorkflowTask"));
        // provide Bugzilla URL
        context.put("bugzilla_url", swampapi.doGetProperty("BUGZILLA_BROWSERURL", uname));
        // Jira URL
        context.put("jira_url", swampapi.doGetProperty("JIRA_BROWSERURL", uname));
        // OTRS URL
        context.put("otrs_url", swampapi.doGetProperty("OTRS_BROWSERURL", uname));
        // provide Session id:
        context.put("sessionid", data.getRequest().getRequestedSessionId());
        // pointer to Turbine configuration
        context.put("turbineconf", org.apache.turbine.Turbine.getConfiguration());
        context.put("adminEmail", swampapi.doGetProperty("POSTMASTER", uname));
        context.put("swampVersion", swampapi.doGetProperty("SWAMP_VERSION", uname));
        context.put("data", data);
        context.put("swampuser", data.getUser());
        context.put("date", new DateTool());

        // if there is a helppage available, propagade it.
        // may be overridden by special help pages, set from the templates
        ArrayList helps = new ArrayList();
        // there may already be helplinks
        if (context.get("helps") != null) {
            helps = (ArrayList) context.get("helps");
        }
        ContextHelp help = new DocumentationAPI().
            getContextHelp("help." + data.getScreen(), uname);
        if (help != null) {
            helps.add(help);
            context.put("helps", helps);
        }

        // switch for print-view
        if (data.getParameters().containsKey("printview")){
            data.setLayoutTemplate("PrintLayout.vm");
            context.put("printview", data.getParameters().get("printview"));
        }

        // store last page parameters temporary
        CircularFifoBuffer pageBuffer = (CircularFifoBuffer)
        	data.getUser().getTemp("pageBuffer", new CircularFifoBuffer(2));

        org.apache.turbine.util.parser.ParameterParser params = data.getParameters();
        SWAMPHashMap map = new SWAMPHashMap();
        for (Iterator it = params.keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
        	if (!key.equals("action")) {
				map.put(key, params.getObject(key));
			}
        }
        pageBuffer.add(map);
        data.getUser().setTemp("pageBuffer", pageBuffer);

    }


    protected boolean isAuthorized(RunData data) throws Exception {
        boolean isAuthorized = false;
		return isAuthorized;
	}


    /**
     * Setting the error screen and display an error message
     */
    protected void setErrorScreen(RunData data, Context context,
            String errormsg) {
        context.put("errormsg", errormsg);
        setTemplate(data, "Error.vm");
    }



    /**
     * adding a workflowtemplate helplink
     */
    public static void addHelplink(WorkflowTemplate wfTemp, Context context, String uname)
    		throws Exception {
		ArrayList helps = new ArrayList();
		if (context.get("helps") != null) {
			helps = (ArrayList) context.get("helps");
		}

		if (wfTemp.getHelpContext() != null) {
			ContextHelp help = null;
			DocumentationAPI docuapi = new DocumentationAPI();
			String helppath = "workflows." + wfTemp.getName() + "." + wfTemp.getHelpContext();
			help = docuapi.getContextHelp(helppath, uname);
			if (help != null && !helps.contains(help)) {
				helps.add(help);
			} else {
				Logger.ERROR("Did not find helpfile in path: " + helppath);
			}
		}
		context.put("helps", helps);
	}

}
