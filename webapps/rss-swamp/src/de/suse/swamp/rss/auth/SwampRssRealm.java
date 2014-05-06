/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt
 * Copyright (c) 2007 Novell Inc.
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
package de.suse.swamp.rss.auth;

import java.security.*;
import java.util.*;

import org.apache.catalina.realm.*;

import de.suse.swamp.util.*;


public class SwampRssRealm extends UserDatabaseRealm {
	
	protected static final String name = "SwampRssRealm";
	
    /**
     * authenticating the user with the ldap-auth lib against ldap server. 
     * every authenticated user is allowed to to everything atm.
     */
    public Principal authenticate(String username, String credentials) {
    	try {
			de.suse.swamp.core.container.SecurityManager.
				getAuthenticatedUser(username, credentials);
		} catch (Exception e) {
			Logger.WARN("RSS HTTP login failed: " + e.getMessage());
			return null;
		} catch (Error e) {
            Logger.WARN("RSS HTTP login failed: " + e.getMessage());
            return null;
        }
        List roles = new ArrayList();
        roles.add("tomcat");
    	return new GenericPrincipal(this, username, "", roles);
    }

    
    
    protected String getName() {
        return (name);
    }

    
}

	