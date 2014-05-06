/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt@suse.de>
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

package de.suse.swamp.soap;

import java.rmi.*;
import java.util.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.ResultList.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * This Class is a mapping from external SOAP Calls
 * to the public SWAMP API.
 * It is checking the users credentials and calling
 * the SWAMP API with a validated username afterwards.
 *
 * All upcoming Exceptions are mapped to java.rmi.RemoteException
 * so that all SOAP clients can decode them.
 *
 * All methods are based on simple datatypes so that all
 * SOAP clients can work with them.
 *
 * @author tschmidt
 *
 */
public class SoapSwamp {

	protected static DataAPI dataApi = new DataAPI();
	protected static WorkflowAPI wfApi = new WorkflowAPI();
	protected static EventAPI eventApi = new EventAPI();
	protected static SecurityAPI securityApi = new SecurityAPI();

	// documentation for SOAP methods
    protected static HashMap docs = new HashMap();
    static {
    	docs.put("doGetData", "Returns the value that is stored at this path " +
    			"of the workflow with the provided id.");
    	docs.put("doGetAllDataPaths", "Returns all data paths that are available " +
    			"in the workflow with the provided id.");
        docs.put("doGetAllData", "Returns all databitvalues of a workflow as a HashMap. " +
                "Key: dataPath, value: databitvalue");
    	docs.put("doSendData", "Stores the given value in the provided workflow path. Returns " +
    			"true if the value actually has been changed.");
    	docs.put("doSendEvent", "Sends the event to the workflow with the provided id. Returns an Array " +
    	        "with result messages.");
    	docs.put("doGetProperty", "Returns a global SWAMP property. For example \"SWAMP_VERSION\".");
    	docs.put("getWorkflowInfo", "Returns a HashMap with the workflows properties.");
        //FIXME: doc for filter syntax needed
        docs.put("getWorkflowIdList", "Returns a list of ids that match the provided filters. " +
                "For the filter syntax look at de.suse.swamp.core.util.MapToFilterlist " +
                "(documentation to be done...)");
    	docs.put("createWorkflow", "Creates a new workflow. If a valid parentWorkflowId " +
    			"is provided, the created workflow will be a subworkflow to this id.");
    	docs.put("doGetArray", "Testmethod for the array datatype. Returns the " +
    			"array that was sent in.");
    	docs.put("doGetHash", "Testmethod for the hashmap datatype. Returns the " +
				"hash that was sent in.");
    	docs.put("doSendObject", "Testmethod for any object type. Returns a String representation " +
    			"of the sent object.");
        docs.put("doSendObject", "Testmethod for any object type. Returns a String representation " +
            "of the sent object.");
        docs.put("addUserToWfRole", "Add a user to this role of the workflow.");
        docs.put("removeUserFromWfRole", "Remove a user from this role of the workflow.");
    	docs.put("getAllDocs", "Returns a Hashmap with all documentation strings.");
    	docs.put("getMethodDoc", "Returns the documentation String for the provided method.");
    }


    /**
     * Startup SWAMP if not initialized yet
     */
    public SoapSwamp(){
        SWAMP.getInstance();
        Logger.DEBUG("SOAPSWAMP instanciated");
    }


    /* Data API ****************************************************/

    public String doGetData(int wfid, String path, String username, String password)
            throws RemoteException {
        //Logger.DEBUG("SOAPSWAMP: dogetData(): " + wfid + ", " + path);
        try {
            authenticate(username, password);
            return this.dataApi.doGetDataBit(wfid, path, username).getValue();
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }


    public Object[] doGetAllDataPaths(int wfid, String username, String password)
    throws RemoteException {
    	//Logger.DEBUG("SOAPSWAMP: doGetAllDataPaths(): " + wfid);
    	try {
    		authenticate(username, password);
    		Workflow wf = wfApi.getWorkflow(wfid, username);
    		return wf.getAllDatabitPaths().toArray();
    	} catch (Exception e) {
    		throw new RemoteException(e.getMessage());
    	}
    }


    public HashMap doGetAllData(int wfid, String username, String password)
    throws RemoteException {
        //Logger.DEBUG("SOAPSWAMP: doGetAllData(): " + wfid);
        try {
            authenticate(username, password);
            Workflow wf = wfApi.getWorkflow(wfid, username);
            HashMap wfdata = new HashMap();
            for (Iterator it = wf.getAllDatabitPaths().iterator(); it.hasNext(); ){
                String path = (String) it.next();
                wfdata.put(path, wf.getDatabitValue(path));
            }
            return wfdata;
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }


    public boolean doSendData(int wfid, String path, Object value, String username, String password)
            throws RemoteException {
        //Logger.DEBUG("SOAPSWAMP: doSendData(): " + wfid + ", " + path + " " + value);
        try {
            authenticate(username, password);
            Workflow wf = wfApi.getWorkflow(wfid, username);
            synchronized (wf) {
                boolean changed = dataApi.doUpdateDataBitValue(wfid, path, value.toString(), false, username);
                return changed;
            }
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }


    /* Event API ****************************************************/

    public Object[] doSendEvent(int wfid, String event, String username,
			String password) throws RemoteException {
		//Logger.DEBUG("SOAPSWAMP: doSendEvent(): " + wfid + ", " + event);
        ResultList hist = new ResultList();
        ArrayList results = new ArrayList();
        try {
            authenticate(username, password);
            Workflow wf = wfApi.getWorkflow(wfid, username);
			Event e = new Event(event, 0, wfid);
			synchronized (wf) {
                eventApi.sendEvent(e, username, hist);
            }
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
		// serialize the results to strings:
		for (Iterator it = hist.getResults().iterator(); it.hasNext(); ){
		    ResultItem item = (ResultItem) it.next();
		    results.add(item.toString());
		}
		return results.toArray();
	}



    /* TODO: Documentation API ****************************************************/

    public HashMap getAllDocs() throws RemoteException {
    	return docs;
    }

    public String getMethodDoc(String methodName) throws RemoteException {
    	return (String) docs.get(methodName);
    }


    /* TODO: History API ****************************************************/

    /* Security API ****************************************************/

    public boolean addUserToWfRole(String targetUser, String wfName, String roleName, String username, String password) 
        throws RemoteException {
        //Logger.DEBUG("SOAPSWAMP: addUserToWfRole(): " + targetUser + " role: " + roleName + " wf: " + wfName);
        try {
            authenticate(username, password);
            return securityApi.addUserToWfRole(targetUser, wfName, roleName, username);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public boolean removeUserFromWfRole(String targetUser, String wfName, String roleName, String username, String password) 
        throws RemoteException {
        //Logger.DEBUG("SOAPSWAMP: removeUserFromWfRole(): " + targetUser + " role: " + roleName + " wf: " + wfName);
        try {
            authenticate(username, password);
            return securityApi.removeUserFromWfRole(targetUser, wfName, roleName, username);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }
    
    
    /* SWAMP API ****************************************************/

    public String doGetProperty(String propName, String username,
			String password) throws RemoteException {
		//Logger.DEBUG("SOAPSWAMP: doGetProperty(): " + propName);
		try {
			authenticate(username, password);
			return SWAMP.getInstance().getProperty(propName);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}



    /* TODO: Task API ****************************************************/



    /* Workflow API ****************************************************/

    public HashMap getWorkflowInfo(int wfid, String username,
			String password) throws Exception {
		//Logger.DEBUG("SOAPSWAMP: getWorkflowInfo(): " + wfid);
		HashMap info = new HashMap();
		try {
			authenticate(username, password);
			Workflow wf = wfApi.getWorkflow(wfid, username);
			info.put("isRunning", new Boolean(wf.isRunning()));
			info.put("version", wf.getVersion());
			info.put("templateName", wf.getTemplateName());
			info.put("description", wf.getReplacedDescription());
			info.put("parentWfId", new Integer(wf.getParentwfid()));
			info.put("status", wf.getStateDescription());
			Object[] subwfs = wf.getSubWorkflows().toArray();
			for (int i = 0; i<subwfs.length; i++){
				subwfs[i] = new Integer(((Workflow) subwfs[i]).getId());
			}
			info.put("subWfIds", subwfs);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
		return info;
	}


    //FIXME: add order capability to soap interface
    public Object[] getWorkflowIdList(HashMap filterStrings, String username, String password)
            throws Exception {
        //Logger.DEBUG("SOAPSWAMP: getWorkflowIdList()");
        try {
            authenticate(username, password);
            ArrayList ids = new ArrayList(wfApi.getWorkflowIds(filterStrings, null, username));
            return ids.toArray();
        } catch (Exception e) {
            Logger.ERROR("SOAP Error: " + e.getMessage());
            throw new RemoteException(e.getMessage());
        }
    }



    public int createWorkflow(int parentWfid, String templateName, String username,
			String password) throws RemoteException {
		//Logger.DEBUG("SOAPSWAMP: createWorkflow() ");
		try {
			authenticate(username, password);
			WorkflowTemplate templ = wfApi.getWorkflowTemplate(templateName, username);
			return wfApi.createWorkflowId(templateName, username, parentWfid,
					null, templ.getVersion(), true, new ResultList());
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}









    /* Test API ****************************************************/

    /**
     * Testmethods for checking compatibility of
     * different SOAP implementations of datatypes
     */
    public String[] doGetArray(String [] test) throws RemoteException {
		Logger.DEBUG("SOAPSWAMP: doGetArray(): ");
		for (int i = 0; i < test.length; i++){
			Logger.DEBUG("[" + i + "]: " + test[i]);
		}
		try {
			return test;
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}


    public Map doGetHash(Map map) throws RemoteException {
		Logger.DEBUG("SOAPSWAMP: doGetHash(): " );
		for (Iterator i = map.keySet().iterator(); i.hasNext(); ){
			String it = (String) i.next();
			Logger.DEBUG("[" + it + "]: " + map.get(it));
		}
		try {
			return map;
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}


    public String doSendObject(Object map) throws RemoteException {
		Logger.DEBUG("SOAPSWAMP: doSendObject(): " + map.toString());
		try {
			return "got: " + map.toString();
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}


    /**
     * authenticating the SOAP user
     */
    protected SWAMPUser authenticate (String username, String password)
        throws StorageException, UnknownElementException, PasswordException {
        //Logger.DEBUG("SOAPSWAMP: authenticating user: " + username);
        return SecurityManager.getAuthenticatedUser(username, password);
    }




}
