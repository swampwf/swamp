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
 * This Page secures all other screens
 *
 * @author Thomas Schmidt
 * @version $Id$
 *
 */

import javax.servlet.http.*;

import org.apache.commons.lang.*;
import org.apache.turbine.*;
import org.apache.turbine.services.velocity.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

public class SecureScreen extends SWAMPScreen {

    protected void doBuildTemplate(RunData data) throws Exception {
        if (isAuthorized(data)) {
            super.doBuildTemplate(data);
        } else {
            provideData(data,  TurbineVelocity.getContext(data));
        }
    }
    
    public void doBuildTemplate(RunData data, Context context) throws Exception {
		super.doBuildTemplate(data, context);
    }

    
	/**
     * Overide this method to perform the security check needed.
     *
     * @param data Turbine information.
     * @return True if the user is authorized to access the screen.
     * @exception Exception a generic exception.
     */
    public boolean isAuthorized(RunData data) throws Exception {
		boolean isAuthorized = false;
        
        if (data.getUser().hasLoggedIn()) {
			isAuthorized = true;
		} else {
			// this url the user wants to see, but he has no right to.
			HttpServletRequest req = data.getParameters().getRequest();
			StringBuffer query = new StringBuffer();
			for (int i = 0; i < data.getParameters().getKeys().length; i++){
				String key = (String) data.getParameters().getKeys()[i];
				query.append(key).append("=").
					append(StringEscapeUtils.escapeHtml(data.getParameters().get(key))).append("&");
			}
			
			// set a message 
			if (data.getMessage() == null && !query.toString().equals("template=Index.vm&")) {
				data.setMessage("You need to login for viewing the requested page.<br />"
						+ "After login you will get redirected there.");
			}
			// forwarding to login page and setting parameter for coming back
			data.setScreen(Turbine.getConfiguration().getString("screen.login"));
			data.setScreenTemplate(Turbine.getConfiguration().getString("template.login"));
			data.setLayoutTemplate("DefaultLayout.vm");

			data.getUser().setTemp("query", req.getRequestURL().append("?").append(query));
			isAuthorized = false;
		}
        return isAuthorized;
    }
    
}
