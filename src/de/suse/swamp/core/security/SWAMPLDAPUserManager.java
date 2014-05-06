/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de>
 * Copyright (c) 2006 Juergen Pabel <jpabel@akkaya.de>
 *
 * Copyright (c) 2006 Novell Inc.
 * Copyright (c) 2006 Akkaya Consulting GmbH
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

package de.suse.swamp.core.security;

/**
 * The LDAPUsermanager authenticates and fetches User
 * from a LDAP server and stores them in the SWAMP DB.
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *
 */
import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

public class SWAMPLDAPUserManager implements UserManagerIface {

    private String LDAP_BIND_URL;
    private String LDAP_BIND_USER;
    private String LDAP_BIND_PASS;

    private String LDAP_USER_BASEDN;
    private String LDAP_USER_SEARCH;

    // LDAP environment
    Properties env;


    public SWAMPLDAPUserManager() {
        LDAP_BIND_URL  = SWAMP.getInstance().getProperty("LDAP_BIND_URL");
        LDAP_BIND_USER = SWAMP.getInstance().getProperty("LDAP_BIND_USER");
        LDAP_BIND_PASS = SWAMP.getInstance().getProperty("LDAP_BIND_PASS");

        LDAP_USER_BASEDN   = SWAMP.getInstance().getProperty("LDAP_USER_BASEDN");
        LDAP_USER_SEARCH   = SWAMP.getInstance().getProperty("LDAP_USER_SEARCH");

        env = new Properties();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");
        env.put( Context.PROVIDER_URL, LDAP_BIND_URL );
        env.put(Context.STATE_FACTORIES, "PersonStateFactory");
        env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");

        // use a connection pool
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.pool.maxsize", "20");
        env.put("com.sun.jndi.ldap.connect.pool.prefsize", "10");
        env.put("com.sun.jndi.ldap.connect.pool.timeout", "30000");
        env.put("com.sun.jndi.connect.timeout", "4000");
        // TCP timeout connecting to server
        env.put("com.sun.jndi.ldap.connect.timeout", "4000");
        // set pool logging:
        //System.setProperty("com.sun.jndi.ldap.connect.pool.debug", "fine");
    }


    /**
     * Loads the user from db if already there, or tries to fetch from
     * LDAP and store into the db then.
     */
    public SWAMPUser loadUser(String userName) throws StorageException, UnknownElementException {
        // load from db:
        if (userName == null || userName.equals("")){
            throw new NoSuchElementException("Tried to load user with empty name!");
        }
        SWAMPUser user = null;
        if (userName.equalsIgnoreCase("anonymous")){
            user = new SWAMPUser();
            user.setUserName(userName);
        } else {
            user = SecurityStorage.loadUserFromDB(userName);
            if (user == null) {
                user = loadUserFromLDAP(userName);
                if (user != null) {
                    SecurityStorage.storeUser(user);
                } else {
                    throw new UnknownElementException("User " + userName +
                            "not found!");
                }
            }
        }
        return user;
    }


    private SWAMPUser loadUserFromLDAP(String userName)
        throws StorageException, UnknownElementException {
        SWAMPUser user = null;
        Logger.DEBUG("Fetching user from ldap: " + userName);

        if (!userName.matches("[a-zA-Z-_0-9\\.]+")) {
            throw new StorageException("Username contains illegal characters.");
        }

        try {
            env.put( Context.SECURITY_PRINCIPAL, LDAP_BIND_USER );
            env.put( Context.SECURITY_CREDENTIALS, LDAP_BIND_PASS );
            DirContext ctx = new InitialDirContext( env );
            String filter = LDAP_USER_SEARCH.replaceAll("%s", userName);
            String[] attrIDs = {"givenName","sn","mail"};

            SearchControls constraints = new SearchControls();
            constraints.setReturningAttributes(attrIDs);
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer = ctx.search( LDAP_USER_BASEDN, filter, constraints);

            if (answer.hasMore()) {
                SearchResult sr = (SearchResult)answer.next();
                Attributes attrs = sr.getAttributes();
                user = new SWAMPUser();
                user.setUserName(userName);
                user.setFirstName( extractLDAPValue(attrs,"givenName") );
                user.setLastName( extractLDAPValue(attrs,"sn") );
                user.setEmail( extractLDAPValue(attrs,"mail") );
                SecurityManager.storeUser(user);
            }
            ctx.close();
        } catch ( Exception e ) {
        	e.printStackTrace();
            throw new StorageException(e.getMessage());
        }
        if (user == null){
            throw new UnknownElementException("No such user in LDAP: " + userName);
        }
        return user;
    }


    /**
     * authenticates the user against an LDAP server.
     * special users that have a password in the database get authenticated
     * directly, so that we can add special users here if the
     * LDAP server is not at our control.
     */
    public void authenticateUser(String loginName, String password)
        throws StorageException, UnknownElementException, PasswordException {
        Logger.DEBUG("Trying to authenticate user: " + loginName);

        if (!loginName.matches("[a-zA-Z-_0-9\\.]+")) {
            throw new StorageException("Username contains illegal characters.");
        }

        // catch empty passwords:
        if (password == null || loginName == null || password.equals("")){
            throw new PasswordException("Cannot authenticate with empty pw or username!");
        }

        SWAMPUser user = loadUser(loginName);
        if (user.getPasswordHash() != null && user.getPasswordHash().length() > 0) {
            Logger.DEBUG("Switching to DB authentication for user: " + loginName);
            SWAMPDBUserManager userManager = new SWAMPDBUserManager();
            userManager.authenticateUser(loginName, password);
            return;
        } else {
            String userDn;
            try {
                env.put(Context.SECURITY_PRINCIPAL, LDAP_BIND_USER);
                env.put(Context.SECURITY_CREDENTIALS, LDAP_BIND_PASS);
                DirContext ctx = new InitialDirContext(env);
                String filter = LDAP_USER_SEARCH.replaceAll("%s", loginName);
                String[] attrIDs = {};

                SearchControls constraints = new SearchControls();
                constraints.setReturningAttributes(attrIDs);
                constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
                NamingEnumeration answer = ctx.search(LDAP_USER_BASEDN, filter, constraints);
                ctx.close();
                if (answer.hasMore()) {
                    SearchResult sr = (SearchResult) answer.next();
                    userDn = sr.getName() + ',' + LDAP_USER_BASEDN;
                } else {
                    throw new Exception("User dn not found with: " + filter);
                }
            } catch (Exception e) {
                throw new UnknownElementException(e.getMessage());
            }
            //Logger.DEBUG("Found dn for " + loginName + ": " + userDn);
            env.put(Context.SECURITY_PRINCIPAL, userDn);
            env.put(Context.SECURITY_CREDENTIALS, password);
            try {
                DirContext userctx = new InitialDirContext(env);
                userctx.close();
            } catch (CommunicationException e) {
                throw new StorageException("Error in communicating with LDAP server: " + e.getMessage());
            } catch (Exception e) {
                throw new PasswordException(e.getMessage());
            }
        }
    }

    /**
     * Get the LDAP value as string
     */
    private String extractLDAPValue(Attributes attributes, String name)
			throws NamingException {
    	String val = (String) attributes.get(name).get();

    	// TODO: do base64 decoding if needed
    	/*if( Base64.isBase64( val ) ) {
            val = new String( Base64.decode( val.getBytes() ) );
        }*/

    	return val;
	}


}
