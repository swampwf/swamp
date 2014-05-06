/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt (tschmidt@suse.de)
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

package de.suse.swamp.core.api;

import java.beans.*;
import java.util.*;

import org.apache.commons.collections.map.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class WorkflowAPI {

    
  
	public LRUMap doGetWorkflowCache(String username) throws SecurityException, 
		UnknownElementException, StorageException {
		if (!SecurityManager.isGroupMember(SecurityManager.getUser(username), "swampadmins")){
			throw new SecurityException("No permission to read Workflowcache!");
		}
		return WorkflowManager.getInstance().getWorkflowCache();
	}

    
    public Workflow getWorkflow(int wfid, String username) throws SecurityException,
            UnknownElementException {
        Workflow wf = WorkflowManager.getInstance().getWorkflow(wfid);
        if (wf == null) {
            throw new UnknownElementException("Workflow #" + wfid + " not found");
        } else if (!wf.hasRole(username, WorkflowRole.USER)) {
            throw new SecurityException("No permission to read Workflow #" 
            		+ wfid + " for " + username + ". (Not in role USER)");
        }
        return wf;
    }
    
    
    public Workflow getWorkflowForWriting(int wfid, String username) throws SecurityException, UnknownElementException {
        Workflow wf = this.getWorkflow(wfid, username);
        if (!wf.hasRole(username, WorkflowRole.ADMIN) && 
             !wf.hasRole(username, WorkflowRole.OWNER)){
            throw new SecurityException("No permission to write to Workflow #" + wfid + " for " + username);
        }
        return wf;
    }
    
    
    public boolean hasWorkflowTemplate(String template, String username) {
        WorkflowTemplate wftemp = WorkflowManager.getInstance().getWorkflowTemplate(template);
        boolean hasWf = true;
        if (wftemp == null || !wftemp.hasRole(username, WorkflowRole.USER) && 
                !wftemp.hasRole(username, WorkflowRole.STARTER)){
            hasWf = false;
        }
        return hasWf;
    }
    
    public WorkflowTemplate getWorkflowTemplate(String template, String username) 
        throws SecurityException, UnknownElementException {
        WorkflowTemplate wftemp = WorkflowManager.getInstance().getWorkflowTemplate(template);
        if (wftemp == null){
            throw new UnknownElementException("Template: " + template + " not found");
        } else if (!wftemp.hasRole(username, WorkflowRole.USER) && !wftemp.hasRole(username, WorkflowRole.STARTER)){
            throw new SecurityException("No permission to read template: " + template + " for " + username);
        }
        return wftemp;
    }
    
    
    public WorkflowTemplate getWorkflowTemplate(String template, String version, String username)
            throws SecurityException, UnknownElementException {

        WorkflowTemplate wftemp = WorkflowManager.getInstance().getWorkflowTemplate(template, version);

        if (wftemp == null) {
            throw new UnknownElementException("Template: " + template + " not found");
        } else if (!wftemp.hasRole(username, WorkflowRole.USER) && !wftemp.hasRole(username, WorkflowRole.STARTER)) {
            throw new SecurityException("No permission to read template: " + template + " for " + username);
        }
        return wftemp;
    }

    
    
    public Collection getWorkflowRoles(String template, String version, String username)
        throws SecurityException, UnknownElementException {
        WorkflowTemplate wftemp = this.getWorkflowTemplate(template, version, username);
        return wftemp.getRoles();
    }
    
    
    
    /**
     * Returns only those template names the user is permitted to see
     * (is in role "users")
     */
    public ArrayList getWorkflowTemplateNames(String uname) 
        throws SecurityException, UnknownElementException {
        ArrayList names = new ArrayList();
        WorkflowManager wfman = WorkflowManager.getInstance();
        for (Iterator it = wfman.getWorkflowTemplateNames().iterator(); it.hasNext();) {
            String name = (String) it.next();
            WorkflowTemplate t = wfman.getWorkflowTemplate(name);
            if (t != null && (t.hasRole(uname, WorkflowRole.USER) || t.hasRole(uname, WorkflowRole.STARTER))) {
                names.add(name);
            }
        }
        return names;
    }
    
    
    /**
     * Returns all template names 
     */
    public ArrayList getAllWorkflowTemplateNames(String uname) 
        throws SecurityException, UnknownElementException {
        WorkflowManager wfman = WorkflowManager.getInstance();
        return wfman.getWorkflowTemplateNames();
    }
    
    
    /**
     * Convenience method which evaluates recursively which templates are subworkflows of the given template
     * @return - List of templateNames
     */
    public List getSubworkflowTemplateNames(String wfTempName, String uname) {
        // no check, only returns names...
        return WorkflowManager.getInstance().getSubwfTypes(wfTempName, new ArrayList());
    }
    
    
    
    /**
     * Returns only those templates the user is permitted to see
     * (is in role "users")
     *  @returns: 
     *  a HashMap of valid workflow templates. 
     *  key: name of WfTemplate, 
     *  value: TreeMap with key: VersionString, value: WorkflowTemplate-Object
     */
    public HashMap getWorkflowTemplates(String uname) 
        throws SecurityException, UnknownElementException {
        HashMap templates = new HashMap();
        WorkflowManager wfman = WorkflowManager.getInstance();
        for (Iterator it = wfman.getWorkflowTemplates().keySet().iterator(); it.hasNext();) {
            String name = (String) it.next();
            WorkflowTemplate t = wfman.getWorkflowTemplate(name);
            if (t != null && (t.hasRole(uname, WorkflowRole.USER) || t.hasRole(uname, WorkflowRole.STARTER))) {
                templates.put(name, wfman.getWorkflowTemplates().get(name));
            }
        }
        return templates;
    }
    
    
    public List getWorkflowIds(List filterList, DatabaseFilter order, 
            String uname) throws SecurityException, UnknownElementException {
        // FIXME: cannot check permissions for plain id-list        
        List idList = WorkflowManager.getInstance().getWorkflowIds(filterList, order);
        return idList;
    }
    
    public List getWorkflowIds(Map filterStringMap, DatabaseFilter order, 
            String uname) throws SecurityException, IllegalArgumentException {
        // FIXME: cannot check permissions for plain id-list
        List filterList = new MapToFilterlist().getFilters(filterStringMap);
        List idList = WorkflowManager.getInstance().getWorkflowIds(filterList, order);
        return idList;
    }
    

    public List getWorkflows(Map filterStringMap, DatabaseFilter order, 
            String uname) throws SecurityException, IllegalArgumentException, UnknownElementException {
        List filterList = new MapToFilterlist().getFilters(filterStringMap);
        List idList = getWorkflows(filterList, order, uname);
        return idList;
    }
    
    
    public List getWorkflowIds(DatabaseFilter filter, DatabaseFilter order, 
            String uname) throws SecurityException, UnknownElementException {
        // FIXME: cannot check permissions for plain id-list        
        List idList = WorkflowManager.getInstance().getWorkflowIds(filter, order);
        return idList;
    } 
    
    
    public List getWorkflows(List filterList, DatabaseFilter order, 
            String username) throws SecurityException, UnknownElementException {
        List idList = this.getWorkflowIds(filterList, order, username);
        List wfList = new ArrayList();
        int size = idList.size();
        for (int i = 0; i < size; i++){
            try {
                Workflow wf = this.getWorkflow(((Integer) idList.get(i)).intValue(), username);
                wfList.add(wf);
            } catch (Exception e) {
                Logger.ERROR(e.getMessage());
            }
        }
        return wfList;
    }
    
    
    public List getWorkflows(DatabaseFilter filter, DatabaseFilter order, 
            String uname) throws SecurityException, UnknownElementException { 
        ArrayList filterList = new ArrayList();
        filterList.add(filter);
        return this.getWorkflows(filterList, order, uname);
    }
    
    
    public Workflow createWorkflow(String name, String userName, int parentWfId, 
            List dsets, String version, boolean started, ResultList hist) 
            throws StorageException, SecurityException, UnknownElementException, Exception {
        WorkflowManager wfman = WorkflowManager.getInstance();
        // check for role "starter"
        WorkflowTemplate wftemp = wfman.getWorkflowTemplate(name);
        Workflow wf = null;
        if (wftemp.hasRole(userName, WorkflowRole.STARTER)){
            wf = wfman.createWorkflow(name, userName, parentWfId, dsets, version, started, hist);
            if (wf == null){
                throw new StorageException("Could not create Wf for Template: " + 
                        name + ", version: " + version);
            }
        } else {
            throw new SecurityException("No permission to start instance of Workflow: " + name + version);
        }
        return wf;
    }
    
    
    public int createWorkflowId(String name, String userName, int parentWfId, 
            List dsets, String version, boolean started, ResultList hist) 
            throws StorageException, SecurityException, UnknownElementException, Exception {
        return createWorkflow(name, userName, parentWfId, dsets, version, started, hist).getId();
    }
    
    
    public void storeWorkflow(Workflow wf, String uname) 
        throws StorageException, SecurityException {
        if (!wf.hasRole(uname, WorkflowRole.USER)){
            throw new SecurityException("Not allowed to store Workflow: " + wf.getName());
        }
        WorkflowManager.storeWorkflow(wf);
    }
    
    
    public List reloadAllWorkflowDefinitions(String uname) 
        throws StorageException, SecurityException, Exception {
        if (!SecurityManager.isGroupMember(
                SecurityManager.getUser(uname), "swampadmins")){
            throw new SecurityException("Not allowed to reload the Workflow definitions.");
        }
        return WorkflowManager.getInstance().reloadAllWorkflowDefinitions();       
    }
        
    
    /**
     * Reload all versions of the workflow
     */
    public List reloadWorkflowDefinitions(String uname, String templateName) 
        throws StorageException, SecurityException, Exception {
        WorkflowTemplate wftemp = getWorkflowTemplate(templateName, uname);
        // check if we are admin of the latest version.
        if (!wftemp.hasRole(uname, WorkflowRole.ADMIN)){
            throw new SecurityException("Not allowed to reload the Workflow definition.");
        }
        return WorkflowManager.getInstance().reloadWorkflowDefinitions(templateName);       
    }
    

    public List reloadWorkflowDefinitions(String uname, String templateName, String version) 
        throws StorageException, SecurityException, Exception {
        WorkflowTemplate wftemp = getWorkflowTemplate(templateName, version, uname);
        WorkflowTemplate wftempLatest = getWorkflowTemplate(templateName, uname);
        // can reload if admin of this template or the latest template
        if (!wftemp.hasRole(uname, WorkflowRole.ADMIN) && !wftempLatest.hasRole(uname, WorkflowRole.ADMIN)){
            throw new SecurityException("Not allowed to reload the Workflow definition.");
        }
        return WorkflowManager.getInstance().reloadWorkflowDefinition(templateName, version);       
    }
    

    /** 
     * Removes a workflow and all objects referencing it from the system
     * Cannot be undone!
     */
    public void removeWorkflow(String uname, int wfId) 
        throws StorageException, SecurityException, Exception {
        WorkflowManager wfMan = WorkflowManager.getInstance();
        Workflow wf = wfMan.getWorkflow(wfId);
        WorkflowTemplate wfTmp = wfMan.getWorkflowTemplate(wf.getTemplateName());
        // can delete if admin of this wf
        if (!wf.hasRole(uname, WorkflowRole.ADMIN) && !wfTmp.hasRole(uname, WorkflowRole.ADMIN)){
            throw new SecurityException("Not allowed to remove workflow. Must be in role admin.");
        }
        wfMan.removeWorkflow(wf, uname);       
    }
    
    
    
    public long doFullgc(String uname) 
        throws StorageException, SecurityException, UnknownElementException {
        if (!SecurityManager.isGroupMember(
                SecurityManager.getUser(uname), "swampadmins")){
            throw new SecurityException("Not allowed to do a full GC().");
        }
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
        Introspector.flushCaches(); 
        long freeMem2 = Runtime.getRuntime().freeMemory()/1024l/1024l;
        return freeMem2;
        
    }
    
    public void doEmptywfcache(String uname) 
        throws StorageException, SecurityException, UnknownElementException {
        if (!SecurityManager.isGroupMember(
                SecurityManager.getUser(uname), "swampadmins")){
            throw new SecurityException("Not allowed to truncate Caches.");
        }
        WorkflowManager wfm = WorkflowManager.getInstance();
        LRUMap cache = wfm.getWorkflowCache();
        cache.clear();
    }
    
    /**
     * Returns a HashMap with 
     * key: path, value: DatabitTemplate 
     * of all DatabitTemplates
     */
    public HashMap getAllDatabitTemplates(String wftemplate, String username) 
        throws StorageException, SecurityException, UnknownElementException {
        WorkflowTemplate wftemp = this.getWorkflowTemplate(wftemplate, username);
        
        HashMap bits = new HashMap();
        ArrayList paths = wftemp.getAllDatabitPaths();
        for (Iterator it = paths.iterator(); it.hasNext(); ){
            // FIXME: add security checks for the databits here
            String path = (String) it.next();
            DatabitTemplate bit = wftemp.getDatabitTemplate(path);
            bits.put(path, bit);
        }
        return bits;
    }
        
    /**
     * Change the parent workflow
     */
    public boolean changeParentId(int wfId, int newParentId, String uname)
			throws UnknownElementException, SecurityException, StorageException {
		Workflow wf = new WorkflowAPI().getWorkflowForWriting(wfId, uname);
		return wf.changeParentId(newParentId, uname);
	}
}