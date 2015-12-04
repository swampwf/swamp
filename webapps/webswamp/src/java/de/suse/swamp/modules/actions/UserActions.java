/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de>
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

import org.apache.turbine.*;
import org.apache.turbine.modules.actions.*;
import org.apache.turbine.services.pull.*;
import org.apache.turbine.services.pull.util.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.turbine.services.security.*;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;

/**
 * Actions for changing Skins and User-Interfaces
 * Unsecure by intention, to be called on the login page
 *
 * @author <a href="mailto:dbaum@suse.de">Doris Baum</a>
 */
public class UserActions extends VelocityAction {

    /**
     *  IMPORTANT: doXxx methods may only have ONE upper case letter X
     *  after the do, anything else must be lower case.
     */

    public void doPerform( RunData data,Context context )
        throws Exception
    {
    }


    /**
     * Sets a new Skin for the current Session/User
     */
    public void doSkin(RunData data, Context context) throws Exception {

        if (data.getParameters().get("skin") != null) {
            String skin = data.getParameters().get("skin");
            UIManager uitool = (UIManager) TurbinePull.getTool(context, "ui");
            UIManager.setSkin(data.getUser(), skin);
            uitool.init(data.getUser());
        }
    }

    /**
     * Sets a new Interface for the current Session/User
     */
    public void doInterface(RunData data, Context context) throws Exception {
        if (data.getParameters().get("interface") != null) {
            String interfaceName = data.getParameters().get("interface");
            SwampUIManager uitool = (SwampUIManager) TurbinePull.getTool(context, "ui");
            SwampUIManager.setInterface(data.getUser(), interfaceName);
            uitool.init(data.getUser());
        }
    }

    /**
     * Change the users preferences
     */
    public void doChangeprefs(RunData data, Context context) throws Exception {
        SWAMPUser user = ((SWAMPTurbineUser) data.getUser()).getSWAMPUser();

    	String message = "";
    	I18n i18n = new I18n(getClass(), "de.suse.swamp.webswamp.i18n.Webswamp");


        if (data.getParameters().containsKey("lang") &&
                !data.getParameters().get("lang").equals(user.getPerm("lang"))) {
            user.setPerm("lang", data.getParameters().get("lang"));
            message += i18n.tr("Changed language.", user) + "\n";
        }

        String taskPageParam = data.getParameters().get("taskpage");
        if (taskPageParam != null &&
                !taskPageParam.equals(user.getPerm("taskpage", "results"))) {
            user.setPerm("taskpage", taskPageParam);
            message += i18n.tr("Changed default redirect after task.", user) + "\n";
        }

        WorkflowAPI wfapi = new WorkflowAPI();
        List wfNames = wfapi.getAllWorkflowTemplateNames(user.getUserName());
        for (Iterator it = wfNames.iterator(); it.hasNext(); ) {
            String wfName = (String) it.next();
            String reminderParam = data.getParameters().get(wfName + "_reminder");
            if (reminderParam != null &&
                    !reminderParam.equals(user.getPerm(wfName + "_reminder", "none"))) {
                user.setPerm(wfName + "_reminder", reminderParam);
                message += i18n.tr("Changed reminder of workflow " + wfName + " to: " + reminderParam, user) + "\n";
            }
        }

    	String passwordHash = user.getPasswordHash();
    	String uname = user.getUserName();
    	// check if userprefs are locked:
    	if (data.getParameters().containsKey("pw") || data.getParameters().containsKey("firstname") ||
    	        data.getParameters().containsKey("lastname") || data.getParameters().containsKey("email")) {
            if (new SWAMPAPI().doGetProperty("AUTH_CLASS", uname)
                    .equals("de.suse.swamp.core.security.SWAMPLDAPUserManager") &&
                    (passwordHash == null || passwordHash.equals(""))){
                throw new Exception("Not allowed to change user preferences");
            } else if (!Turbine.getConfiguration().getString("webswamp.testserver").equals("false")) {
                throw new Exception("Not allowed to change user preferences");
            }
    	}


    	if (data.getParameters().get("pw") != null &&
    			!data.getParameters().get("pw").equals("")) {
    		if (!data.getParameters().get("pw").equals(data.getParameters().get("pw2"))){
    			throw new Exception(
    				i18n.tr("The passwords you entered do not match.", user));
    		}
    		new SecurityAPI().getAuthenticatedUser(uname, data.getParameters().get("currentpw"));
    		user.setPassword(data.getParameters().get("pw"));
    		message += i18n.tr("Changed password.", user) + "\n";
    	}


    	if (data.getParameters().get("firstname") != null &&
    			!data.getParameters().get("firstname").equals(user.getFirstName())) {
    		user.setFirstName(data.getParameters().get("firstname"));
    		message += i18n.tr("Changed firstname.", user) + "\n";
    	}
    	if (data.getParameters().get("lastname") != null &&
    			!data.getParameters().get("lastname").equals(user.getLastName())) {
    		user.setLastName(data.getParameters().get("lastname"));
    		message += i18n.tr("Changed lastname.", user) + "\n";
    	}
    	if (data.getParameters().get("email") != null &&
    			!data.getParameters().get("email").equals(user.getEmail())) {
    		user.setEmail(data.getParameters().get("email"));
    		message += i18n.tr("Changed email.", user) + "\n";
    	}

    	if (!message.equals("")){
    		new SecurityAPI().storeUser(user);
	        context.put("statusheader", "Success");
	        context.put("statusclass", "success");
	        context.put("icon", "ok");
	        context.put("statusmessage", message);
    	}
    }


}
