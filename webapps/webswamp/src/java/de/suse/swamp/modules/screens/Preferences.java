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

import org.apache.turbine.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.turbine.services.security.*;
import de.suse.swamp.util.*;



public class Preferences extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);

        //TODO: how to get a list of available locales automatically?
        HashMap locales = new HashMap();
        locales.put("de", "");
        locales.put("cs", "");
        locales.put("en", "");
        locales.put("es", "");

        SWAMPUser user = ((SWAMPTurbineUser) data.getUser()).getSWAMPUser();
        String locale = user.getPerm("lang", SWAMP.getInstance().getProperty("LOCALE"));

        if (!locales.containsKey(locale)){
        	Logger.ERROR("Language " + locale + " not available, setting default.");
        	locale = SWAMP.getInstance().getProperty("LOCALE");
        }

        I18n i18n = new I18n(getClass(), "de.suse.swamp.webswamp.i18n.Webswamp");
        locales.put("de", i18n.tr("German", user));
        locales.put("cs", i18n.tr("Czech", user));
        locales.put("en", i18n.tr("English", user));
        locales.put("es", i18n.tr("Spanish", user));

        context.put("userlocale", locale);
		context.put("locales", locales);
		context.put("taskpage", user.getPerm("taskpage", "results"));
		String uname = user.getUserName();

		WorkflowAPI wfapi = new WorkflowAPI();
		List wfNames = wfapi.getAllWorkflowTemplateNames(uname);
		context.put("wfnames", wfNames);
		Map reminders = new HashMap();
		for (Iterator it = wfNames.iterator(); it.hasNext(); ) {
		    String wfName = (String) it.next();
		    reminders.put(wfName, user.getPerm(wfName + "_reminder", "none"));
		}
		context.put("reminders", reminders);

		String passwordHash = user.getPasswordHash();
		// a non-empty passwordhash indicates a local user
		if (new SWAMPAPI().doGetProperty("AUTH_CLASS", uname)
				.equals("de.suse.swamp.core.security.SWAMPLDAPUserManager") &&
				(passwordHash == null || passwordHash.equals(""))){
			context.put("locked", i18n.tr("Fields are automatically synced from %1", user,
					new SWAMPAPI().doGetProperty("LDAP_BIND_URL", uname)));
		} else if (!Turbine.getConfiguration().getString("webswamp.testserver").equals("false")) {
			context.put("locked", i18n.tr("Fields are locked because this is a test system.", user,
					new SWAMPAPI().doGetProperty("LDAP_BIND_URL", uname)));
		}

    }
}