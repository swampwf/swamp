/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh@suse.de>
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

package de.suse.swamp.core.container;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

import org.apache.commons.collections.map.*;

import de.suse.swamp.core.conditions.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * A Singleton to manage workflow definitions and cases. The WorkflowManager
 * maintains a list of available workflow templates and methods for
 * starting workflows out of them. It also known workflow definitions and all currently active
 * cases. It also provides methods for getting workflows and does caching of
 * the most popular workflow instances.
 *
 * @author  Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class WorkflowManager {

    // contains a HashMap of valid workflow templates.
    // key: name of WfTemplate,
    // value: TreeMap with key: VersionString, value: WorkflowTemplate-Object
    private HashMap workflowTempls = new HashMap();
    //private ArrayList workflows = new ArrayList();
    private static WorkflowManager workflowManager = null;
    // cache for holding already loaded Wfs in memory.
    // key: wfid as Integer
    // value: wf-object
    private LRUMap workflowCache;


    private WorkflowManager() {
        try {
            if (SWAMP.getInstance().getProperty("WF_CACHE_SIZE") != null){
                workflowCache = new LRUMap(Integer.
                        valueOf(SWAMP.getInstance().getProperty("WF_CACHE_SIZE")).intValue());
            } else {
                workflowCache = new LRUMap(500);
            }
            reloadAllWorkflowDefinitions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Read in all available workflow definitions.
     * @return - List with all WorkflowReadResults containing warnings and errors.
     */
    public synchronized List reloadAllWorkflowDefinitions() throws Exception {
        SWAMP swamp = SWAMP.getInstance();
        String workflowLoc = swamp.getWorkflowLocation();
        File workflowDir = new File(workflowLoc);
        ArrayList results;
        synchronized (workflowTempls) {
            workflowTempls.clear();
            WorkflowReader reader = new WorkflowReader(workflowDir);
            results = reader.readAllWorkflows();
            installValidTemplates(results);
        }
        return results;
    }

    /**
     * Read in all available workflow definitions of the workflowtype "name".
     * @return - List with all WorkflowReadResults containing warnings and errors.
     */
    public synchronized List reloadWorkflowDefinitions(String name) throws Exception {
        SWAMP swamp = SWAMP.getInstance();
        String workflowLoc = swamp.getWorkflowLocation();
        File workflowDir = new File(workflowLoc);
        List results = new ArrayList();
        synchronized (workflowTempls) {
            workflowTempls.remove(name);
            WorkflowReader reader = new WorkflowReader(workflowDir);
            reader.readWorkflowType(name, results);
            installValidTemplates(results);
        }
        return results;
    }



    public synchronized List reloadWorkflowDefinition(String name, String version) throws Exception {
        SWAMP swamp = SWAMP.getInstance();
        String workflowLoc = swamp.getWorkflowLocation();
        File workflowDir = new File(workflowLoc);
        TreeMap versions = (TreeMap) workflowTempls.get(name);
        if (versions != null) {
            versions.remove(version);
        }
        WorkflowReader reader = new WorkflowReader(workflowDir);
        List results = new ArrayList();
        results.add(reader.readWorkflow(name, version, results));
        installValidTemplates(results);
        return results;
    }



    /**
     * Workflowtemplates from results without errors
     * will get added to the list of available templates, and their files
     * doc + image files will get installed.
     */
    public void installValidTemplates(List results){
        for (Iterator it = results.iterator(); it.hasNext(); ){
            WorkflowReadResult result= (WorkflowReadResult) it.next();
            if (result.getErrors().size() == 0) {
                WorkflowTemplate wfTemp = result.getTemplate();
                TreeMap templateVersions = new TreeMap();
                if (workflowTempls.keySet().contains(wfTemp.getName())) {
                    templateVersions = (TreeMap) workflowTempls.get(wfTemp.getName());
                } else {
                    workflowTempls.put(wfTemp.getName(), templateVersions);
                }
                // only install files from the latest version:
                if (templateVersions.isEmpty() ||
                		((String) templateVersions.lastKey()).compareTo(result.getWfVersion()) < 0){
	                try {
	                    installWorkflowFiles(wfTemp.getName(), wfTemp.getVersion());
	                } catch (Exception e) {
	                    Logger.ERROR("Installing files from template: " + wfTemp.getName() +
	                            " failed. " + e.getMessage());
	                }
                }
                templateVersions.put(result.getWfVersion(), wfTemp);
                clearWorkflowCache(result.getWfName(), result.getWfVersion());
                Logger.DEBUG("Successfully added " + result.getWfName() + "-" +
                        result.getWfVersion() + " to TemplateList");
            }
        }
    }



    /**
     * Copy files from workflow directory to doc + image location
     */
    private void installWorkflowFiles(String templateName, String templateVersion) throws Exception {
        String fs = System.getProperty("file.separator");
        String wfSourceDir = SWAMP.getInstance().getWorkflowLocation() + fs + templateName + fs + templateVersion;
        // copy images:
        String imageDir = SWAMP.getInstance().getSWAMPHome() +  fs + ".." + fs + "resources" + fs + "workflows" + fs + templateName + fs + templateVersion + fs + "images";
        if (new File(wfSourceDir + fs + "images").exists()){
            copyDirContent(wfSourceDir + fs + "images", imageDir);
        }
        // copy documentation
        String docDir = SWAMP.getInstance().getSWAMPHome() +  fs + ".." + fs + "templates" + fs + "app" + fs + "docs" + fs + "workflows" + fs + templateName;
        if (new File(wfSourceDir + fs + "docs").exists()){
            copyDirContent(wfSourceDir + fs + "docs", docDir);
        }
        // copy other page templates
        String templateDir = SWAMP.getInstance().getSWAMPHome() +  fs + ".." + fs + "templates" + fs + "app" + fs + "screens" + fs + "workflows" + fs + templateName;
        if (new File(wfSourceDir + fs + "templates").exists()){
            copyDirContent(wfSourceDir + fs + "templates", templateDir);
        }
    }


    /**
     * Copy all files from "fromDir" to "toDir".
     * Will create the destination location if needed.
     * @throws Exception if source not existant or error occurs.
     */
    private void copyDirContent(String fromDir, String toDir) throws Exception {
        String fs = System.getProperty("file.separator");
        File[] files = new File(fromDir).listFiles();
        if (files == null) {
            throw new FileNotFoundException("Sourcepath: " + fromDir + " not found!");
        }
        for (int i = 0; i < files.length; i++) {
            File dir = new File(toDir);
            dir.mkdirs();
            if (files[i].isFile()){
                try {
                    // Create channel on the source
                    FileChannel srcChannel = new FileInputStream(files[i]).getChannel();
                    // Create channel on the destination
                    FileChannel dstChannel = new FileOutputStream(toDir + fs + files[i].getName())
                            .getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                } catch (Exception e) {
                    Logger.ERROR("Error during file copy: " + e.getMessage());
                    throw e;
                }
            }
        }
    }


    /**
     * Use this method to get access to the global WorkflowManager object.
     * @return A reference to the global WorkflowManager object
     */
    public static synchronized WorkflowManager getInstance() {
        if (workflowManager == null) {
            workflowManager = new WorkflowManager();
        }
        return workflowManager;
    }

    /**
     * Returns a Workflow Object
     * @param name - name of the Workflow template
     * @param userId
     * @param parentWfId - set to 0 if this is no subworkflow
     * @param dsets if set to <b>null</b> the default-sets from the
     * XML-Definitions will be attached.
     * @param state
     * @return
     * @throws SecurityException if the user is not allowed to create it
     * @throws StorageException if the storage failed
     */
    public Workflow createWorkflow(String name, String userName,
            int parentWfId, List dsets, String version, boolean started, ResultList history)
            throws SecurityException, StorageException, Exception {
        Logger.LOG("creating new workflow from template " + name + ", version: " + version);
        WorkflowTemplate tmpl;
        if (version == null) {
            tmpl = getWorkflowTemplate(name);
            Logger.DEBUG("No version given, taking latest: " + tmpl.getVersion());
        } else {
            tmpl = getWorkflowTemplate(name, version);
        }

        if (tmpl == null){
        	throw new Exception("Unable to load template: " + name + "-" + version);
        }

        Workflow wf = null;
        if (dsets != null) {
            wf = tmpl.createWorkflow(dsets);
        } else {
            wf = tmpl.createWorkflow();
        }

        // Workflow needs to be stored once before
        // starting, to have ids for everything.
        WorkflowStorage.storeWorkflow(wf);

        wf.setParentwfid(parentWfId);
        if (parentWfId > 0){
        	Workflow parentWf = getWorkflow(parentWfId);
            getWorkflow(parentWfId).resetSubIdCache();
            // copying content from parent
            wf.fetchContentFrom(parentWf, userName);
            // attaching parents Datasets
           for (Iterator it = parentWf.getDatasets().iterator(); it.hasNext(); ){
               DataManager.addDatasetToWorkflow(((Dataset) it.next()).getId(),
            		   wf.getId());
           }
           history.addResult(ResultList.MESSAGE, "Subworkflow " + wf.getReplacedDescription()
                   + " (" + wf.getName() + ") attached.");
           WorkflowStorage.storeWorkflow(wf);
        }
        workflowCache.put(new Integer(wf.getId()), wf);

        if (!userName.equals(SWAMPUser.SYSTEMUSERNAME) && wf.getRole("owner") instanceof DatabitRole){
            wf.getDatabit(((DatabitRole) wf.getRole("owner")).getRoleDatabit()).setValue(userName, userName);
        }
        HistoryManager.create("TASK_WORKFLOWSTART", wf.getId(), wf.getId(), userName, "");
        history.addResult(ResultList.MESSAGE, "Workflow " + name + " with version " + tmpl.getVersion() +
                " successfully started. (ID: " + wf.getId() + ")");

        // in some cases (subworkflows) we want a not started wf to
        // copy values into its dataset before entering the startnode.
        if (started) {
        	wf.start(userName, history);
        }
        return wf;
    }



    /**
     * Deletes a Workflow from the entire System, including history, nodes, edges,
     * tasks, simply everything that has references to it
     * @param wf
     */
    public void removeWorkflow(Workflow wf, String uname) throws Exception {
        synchronized (workflowCache) {
            Logger.LOG("User: " + uname + " is going to delete workflow: " + wf.getName());
            WorkflowStorage.removeWorkflow(wf);
            if (wf.isSubWorkflow()) {
				wf.getParentWf().resetSubIdCache();
			}
            workflowCache.remove(new Integer(wf.getId()));
            Logger.LOG("Workflow " + wf.getId() + " removed.");
        }
    }


    public ArrayList getWorkflowTemplateNames() {
        return new ArrayList(workflowTempls.keySet());
    }


    public ArrayList getWorkflowTemplateVersions(String templatename) {
        Map versionMap = (Map) workflowTempls.get(templatename);
        ArrayList versions = null;
        if (versionMap != null){
            versions = new ArrayList(versionMap.keySet());
        } else {
            Logger.ERROR("Could not find the requested Template:" + templatename);
        }
        return versions;
    }

    /**
     * contains a HashMap of valid workflow templates.
     * key: name of WfTemplate,
     * value: TreeMap with key: VersionString, value: WorkflowTemplate-Object
     */
    public HashMap getWorkflowTemplates() {
        return new HashMap(workflowTempls);
    }



    /** Get the Template out of the internal Template List
     * @param name
     * @return
     * @throws Exception
     */
    public WorkflowTemplate getWorkflowTemplate(String name, String version) {
        WorkflowTemplate wftemp = null;
        Map templateVersions = (Map) workflowTempls.get(name);
        if (templateVersions != null && templateVersions.get(version) != null){
            wftemp = (WorkflowTemplate) templateVersions.get(version);
        } else {
            Logger.ERROR("Could not find the requested Template:" + name + " Version: " + version);
        }
        return wftemp;
    }


    /** Get the probably newest Template of that name from the List
     * (alphabetically the last one)
     * @param name
     * @return
     * @throws Exception
     */
    public WorkflowTemplate getWorkflowTemplate(String name) {
        WorkflowTemplate wftemp = null;
        Map templateVersions = (Map) workflowTempls.get(name);
        if (templateVersions != null && templateVersions.keySet().size() > 0) {
            // order List alphabetically
            List versions = Arrays.asList(templateVersions.keySet().toArray());
            Collections.sort(versions);
            wftemp = (WorkflowTemplate) templateVersions.get(versions.get(versions.size() - 1));
        }
        if (wftemp == null){
            Logger.ERROR("Could not find the requested Template:" + name);
        }
        return wftemp;
    }


    public boolean templateExists(String name, String version) {
        boolean exists = false;
        Map templateVersions = (Map) workflowTempls.get(name);
        if (templateVersions != null && templateVersions.get(version) != null){
            exists = true;
        }
        return exists;
    }


    /**
     * @return List with Workflow-ids that matched the filter
     */
    public List getWorkflowIds(DatabaseFilter filter, DatabaseFilter order) {
        List filterList = new ArrayList();
		filterList.add(filter);
		return getWorkflowIds(filterList, order);
    }

    /**
     * @return List with Workflow-ids that matched the filterlist
     */
    public List getWorkflowIds(List filterList, DatabaseFilter order) {
        List idList = WorkflowStorage.getWorkflowIds(filterList, order);
        return idList;
    }

    /**
     * @return List with Workflow-objects that matched the filter
     */
    public List getWorkflows(WorkflowFilter filter, DatabaseFilter order) throws NoSuchElementException {
		ArrayList filterList = new ArrayList();
		filterList.add(filter);
		return this.getWorkflows(filterList, order);
    }

    /**
     * @return List with Workflow-objects that matched the filterlist
     */
    public List getWorkflows(List filterList, DatabaseFilter order) throws NoSuchElementException {
        List idList = WorkflowStorage.getWorkflowIds(filterList, order);
        List wfList = new ArrayList();
        int size = idList.size();
        for (int i = 0; i<size; i++){
            Workflow wf = getWorkflow(((Integer) idList.get(i)).intValue());
            wfList.add(wf);
        }
        return wfList;
    }


    /**
     * @return Workflow-object with the given (db-)id.
     * Will try to get it from the cache at first.
     * If not found, a NoSuchElementException is thrown.
     */
    public Workflow getWorkflow(int id) throws NoSuchElementException {
		Integer idObj = new Integer(id);
		synchronized (workflowCache) {
    		if (workflowCache.containsKey(idObj)){
    			return (Workflow) workflowCache.get(idObj);
    		} else {
    			Workflow wf;
				try {
					wf = WorkflowStorage.loadWorkflow(id);
				} catch (StorageException e) {
					throw new NoSuchElementException(e.getMessage());
				}
    			workflowCache.put(idObj, wf);
    			return wf;
    		}
		}
    }


    /**
     * Do a bulk load of workflow ids.
     * If one of the workflows cannot be loaded, an exception is thrown.
     */
    public List getWorkflows(HashSet ids) throws StorageException {
		List workflows = new ArrayList();
		List idsToLoad = new ArrayList();
		for (Iterator it = ids.iterator(); it.hasNext(); ){
			Integer idObj = (Integer) it.next();
    		if (workflowCache.containsKey(idObj)){
    			workflows.add(workflowCache.get(idObj));
    		} else {
    			idsToLoad.add(idObj);
    		}
		}
		if (idsToLoad.size() > 0) {
	    	for (Iterator it = WorkflowStorage.
	    			loadWorkflows(idsToLoad).values().iterator(); it.hasNext(); ) {
	    		Workflow wf = (Workflow) it.next();
	    		synchronized (workflowCache) {
	    			workflowCache.put(new Integer(wf.getId()), wf);
	    			workflows.add(wf);
	    		}
			}
		}
    	return workflows;
    }


    /**
     * @return Returns the workflowCache.
     */
    public LRUMap getWorkflowCache() {
        return workflowCache;
    }

    public void removeFromCache(int wfid) {
    	workflowCache.remove(new Integer(wfid));
    }


	public static Condition loadCondition(Edge edge) {
		return WorkflowStorage.loadCondition(edge);
	}


	public static ArrayList loadEdges(Node node) {
		return WorkflowStorage.loadEdges(node);
	}

   public static MileStone loadNodeMileStone(Node node) {
        return WorkflowStorage.loadNodeMileStone(node);
    }

	public static ArrayList loadNodes(int id) {
		return WorkflowStorage.loadNodes(id);
	}


	public static void storeWorkflow(Workflow wf) throws StorageException {
		WorkflowStorage.storeWorkflow(wf);
	}


	public static ArrayList loadAllContextHelp() throws Exception {
		return StorageManager.loadAllContextHelp();
	}

    public static boolean isInstantiated(){
        return workflowManager != null ? true : false;
    }

    public static void detachDatasetFromWorkflow(int dSetId, int wfId) {
    	StorageManager.detachDatasetFromWorkflow(dSetId, wfId);
    }


    /**
     * Convenience method which evaluates recursively which templates are subworkflows of the given template
     * @return - List of templateNames
     */
    public List getSubwfTypes(String wfTempName, List templates) {
        for (Iterator it = workflowTempls.values().iterator(); it.hasNext(); ){
            // iterate over ordered list with versions of a template
            TreeMap versions = (TreeMap) it.next();
            WorkflowTemplate template = (WorkflowTemplate) versions.get(versions.lastKey());
            if (template.getParentWfName() != null && template.getParentWfName().equals(wfTempName)) {
                templates.add(template.getName());
                getSubwfTypes(template.getName(), templates);
            }
        }
        return templates;
    }

    
    
    public void clearWorkflowCache(String templateName, String templateVersion) {
        List idsToRemove = new ArrayList();
        for (Iterator it = workflowCache.values().iterator(); it.hasNext(); ) {
            Workflow wf = (Workflow) it.next();
            if (wf.getTemplateName().equals(templateName) && 
                    wf.getVersion().equals(templateVersion)) {
                idsToRemove.add(new Integer(wf.getId()));
            }
        }
        for (Iterator it = idsToRemove.iterator(); it.hasNext(); ) {
            workflowCache.remove(it.next());
        }
        if (idsToRemove.size() > 0) Logger.DEBUG("Removed from wfcache: " + idsToRemove);
    }

}