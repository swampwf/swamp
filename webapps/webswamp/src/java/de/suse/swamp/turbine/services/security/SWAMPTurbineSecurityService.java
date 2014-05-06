/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt
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

package de.suse.swamp.turbine.services.security;

 /**
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  * @version $Id$
  */

import org.apache.turbine.om.security.*;
import org.apache.turbine.services.security.db.*;
import org.apache.turbine.util.security.*;

import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.api.*;

public class SWAMPTurbineSecurityService extends DBSecurityService {

    public SWAMPTurbineSecurityService () {
	super();

    }

    /**  
     * Get an authenticated user or an Exception.
     */
    public User getAuthenticatedUser(String username, String password) 
        throws DataBackendException, UnknownEntityException,
        PasswordMismatchException {
        
        SWAMPUser SwampUser = null;
		try {
			SwampUser = new SecurityAPI().getAuthenticatedUser(username, password);
		// mapping SWAMP exceptions to Turbine Exceptions to stay compatible 
        // with the Turbine Interface
        } catch (StorageException e) {
			throw new DataBackendException(e.getMessage());
		} catch (UnknownElementException e) {
            throw new UnknownEntityException(e.getMessage());
		} catch (PasswordException e) {
			throw new PasswordMismatchException(e.getMessage());
		}
        
        // if we got a valid user this way, the pw is correct and we can 
        // hand over a Turbine user to the webapp.
        User user = new SWAMPTurbineUser(SwampUser);
        return user;
    }
    
    
    /** We do not use Turbines ACL system. 
     *  We just need to know if the user is authenticated. 
     *  Permissions will be checked against the username 
     *  on calling methods from the manager classes.
     */
    public AccessControlList getACL(User user){
    	return null;   	
    }

    /** Just overwriting this Method because Turbine would fail in 
     * storing the TurbineUser. 
     * We do write the Users Permdata directly to the DB and not on 
     * Session end.
     **/
    public void saveOnSessionUnbind(User user) 
        throws UnknownEntityException, DataBackendException {
        user.setTempStorage(null);
        user = null;
    }



}
