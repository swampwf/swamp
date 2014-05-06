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

/**
 * @author tschmidt
 * 
 * This is a new Login-Action (Replacement for Turbine-internal Login Method)
 * for doing the login stuff in SWAMP. it does the authentification, and setting
 * of ACL.
 */

import java.util.*;

import javax.servlet.http.*;

import org.apache.commons.configuration.*;
import org.apache.commons.lang.*;
import org.apache.commons.lang.StringUtils;
import org.apache.turbine.*;
import org.apache.turbine.modules.actions.*;
import org.apache.turbine.om.security.*;
import org.apache.turbine.services.security.*;
import org.apache.turbine.util.*;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.turbine.util.security.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;





public class LoginActions extends VelocityAction {

    public void doPerform(RunData data, Context context) throws Exception {
    }

    /**
     * Checks the if the login data is correct, 
     * and provides Turbine with a Turbineuser afterwards.
     * 
     * @author Thomas Schmidt
     * @param data - Turbine information.
     * @exception Exception, a generic exception.
     */
    public void doLoginuser(RunData data, Context context) throws Exception {

        ParameterParser pp = data.getParameters();
        String username = pp.getString("username", "").toLowerCase();
        String password = pp.getString("password", "");
       // cause of login-error 
        String cause = null;

        if (StringUtils.isEmpty(username)) { return; }
        
        try {

            User user = TurbineSecurity.getAuthenticatedUser(username, password);
            // Store the user object.
            data.setUser(user);
            // Mark the user as being logged in.
            user.setHasLoggedIn(new Boolean(true));
            // Save the User object into the session.
            data.save();
            Logger.LOG(username + " has successfully logged in.");
            
            // if we have a "query", it's a redirect from the login page:
            // if we want restrict to do direct logins to special actions, we have to restrict it here
            if (pp.containsKey("query") && !pp.get("query").equals("") 
                    && !(pp.get("query").indexOf("doLogoutuser") > 0)){
                Logger.DEBUG("Found a query, redirecting to " + pp.get("query"));
                data.declareDirectResponse();
                data.setRedirectURI(pp.get("query"));
            }
        } catch (Exception e) {
            Logger.ERROR("Login Error: " + e.getMessage());
            // Retrieve an anonymous user.
            data.setUser(TurbineSecurity.getAnonymousUser());
            data.setScreen(Turbine.getConfiguration().getString("screen.login"));
            data.setScreenTemplate(Turbine.getConfiguration().getString("template.login"));
            data.setLayoutTemplate("DefaultLayout.vm");

            // set the right error-message:
            if (e instanceof StorageException){
				cause = "Error in communicating with authentication server: " + e.getMessage();
            } else if (e instanceof PasswordMismatchException) {
				cause = "Wrong password entered for username: " + username;
            } else if (e instanceof UnknownEntityException) {
				cause = "Unknown username: " + username;
            } else if (e instanceof NoSuchElementException) {
				cause = "Unknown username: " + username;
            } else if (e instanceof DataBackendException) {
				cause = "Could not connect to user database: " + e.getMessage();
            } else {
                cause = "Fatal Error : " + e.getMessage();
            }
			data.setMessage("Login failed. Cause: " + StringEscapeUtils.escapeHtml(cause));
            
        }
        
        // Check for XML-Output for external scripts
        if (data.getParameters().containsKey("xmlresponse") && 
                data.getParameters().get("xmlresponse").equals("true")) {
            if (cause != null && !cause.equals("")) {
                 // FIXME: Mapping ERROR to Errornumber must happen here
                ExternalActions.doSendXMLOutput(data, "1", StringEscapeUtils.escapeHtml(cause));
            } else {
                ExternalActions.doSendXMLOutput(data, "0", "Your are logged in");  
            }
        }
    }

        
        
    public void doLogoutuser(RunData data, Context context) throws Exception {
        User user = data.getUser();

        if (!TurbineSecurity.isAnonymousUser(user)) {
            // Make sure that the user has really logged in...
            if (!user.hasLoggedIn()) {
                Logger.ERROR("Trying to logout a not-logged-in User! (" + user.getName() + ")");
                return;
            }
            user.setHasLoggedIn(Boolean.FALSE);
        }

        Configuration conf = Turbine.getConfiguration();
        data.setMessage(conf.getString(TurbineConstants.LOGOUT_MESSAGE));
		
		// This will cause the acl to be removed from the session in the Turbine servlet code.
        data.setACL(null);

        // Retrieve an anonymous user.
        data.setUser(TurbineSecurity.getAnonymousUser());

        // In the event that the current screen or related navigations
        // require acl info, we cannot wait for Turbine to handle
        // regenerating acl.
        data.getSession().removeAttribute(AccessControlList.SESSION_KEY);
        data.save();
        
        HttpSession session = data.getSession();
        session.invalidate();
        data.setUser(TurbineSecurity.getAnonymousUser());
        String loginScreen = Turbine.getConfiguration().getString("template.login");
        data.setScreenTemplate(loginScreen);
        Logger.LOG(user.getName() + " has logged out.");
        
        // Check for XML-Output for external scripts
        if (data.getParameters().containsKey("xmlresponse") && 
                data.getParameters().get("xmlresponse").equals("true")) {
                ExternalActions.doSendXMLOutput(data, "0", "Your are logged out"); 
        }

    }

}