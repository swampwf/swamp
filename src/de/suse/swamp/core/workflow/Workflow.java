/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh [at] suse.de>
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

package de.suse.swamp.core.workflow;

import java.io.*;
import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

/**
 * Represents an actual, existing workflow instance. This one has a data pack,
 * is started, lives, times out, receives events, and dies in the end. The
 * <b>Workflow</b> class contains the actual state machine implementation. It
 * is initialized with all the information it needs based on the
 * <b>WorkflowTemplate</b> class. The workflow keeps its own local copy of the
 * information that is specific to this instance, that is, datapacks|bits|sets
 * and Edges (the conditions contained in the edges save their state).
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class Workflow extends Persistant {


    // Workflow is based on this Version. Helps to get data that is common.
    private String version;
    // cache ids of attached datasets
    private HashSet dataSetIdCache = new HashSet();
    // cache ids of subworkflows
    private TreeSet subIdCache = null;
    // cache for already loaded nodes (if not null all nodes are loaded)
    private ArrayList nodeCache;
    // The name of the workflow as defined in the workflow description file.
    // In database context, this is the templateID
    private String templName;
    private int parentwfid;


    /**
     * Constructor, constructs a new Workflow, and sets its state to GENERATING
     * You have to set the target state by yourself.
     */
    public Workflow(String templName, String version) {
        this.templName = templName;
        this.version = version;
        setModified(true);
    }


    /**
     * @return "The name of the workflow template this workflow instance is
     * derived from" - "the workflow id".
     */
    public String getName() {
        return getTemplateName() + "-" + getId();
    }

    public String getTemplateName() {
        return templName;
    }


    /**
     * @return ArrayList with with Workflows that are Subworkflows of this workflow
     */
    public List getSubWorkflows() {
        getSubWfIds();
        List workflowList = new ArrayList();
        for (Iterator it = subIdCache.iterator(); it.hasNext(); ){
            Workflow wf = WorkflowManager.getInstance().
            	getWorkflow(((Integer) it.next()).intValue());
            workflowList.add(wf);
        }
        return workflowList;
    }


    /**
     * @return ArrayList with with Workflows that are Subworkflows of this workflow
     * and in the given state
     */
    public List getSubWorkflows(boolean running) {
        getSubWfIds();
        List workflowList = new ArrayList();
        for (Iterator it = subIdCache.iterator(); it.hasNext(); ){
            Workflow wf = WorkflowManager.getInstance().
            	getWorkflow(((Integer) it.next()).intValue());
            if (wf.isRunning() == running){
            	workflowList.add(wf);
            }
        }
        return workflowList;
    }



    /**
     * initializes the subwfId cache, and returns it.
     */
    public Set getSubWfIds() {
        if (this.subIdCache == null){
            PropertyFilter filter = new PropertyFilter();
            filter.setParentWfId(this.getId());
            subIdCache = new TreeSet();
            subIdCache.addAll(WorkflowManager.getInstance().getWorkflowIds(filter, null));
        }
        return this.subIdCache;
    }


    /**
     * @return - the whole tree of attached subworkflow ids
     */
    public HashSet getAllSubWfIds() {
        return getAllSubWfIds(new HashSet());
    }


    HashSet getAllSubWfIds(HashSet ids) {
        ids.addAll(getSubWfIds());
        for (Iterator it = getSubWorkflows().iterator(); it.hasNext(); ){
            Workflow subwf = (Workflow) it.next();
            ids.addAll(subwf.getAllSubWfIds(ids));
        }
        return ids;
    }

    /**
     * Returns a list of active nodes of the workflow object
     *
     * @return a list of active nodes.
     */
    public ArrayList getActiveNodes() {
        ArrayList activeNodes = new ArrayList();
        for (Iterator iter = getAllNodes().iterator(); iter.hasNext(); ) {
            Node node = (Node) iter.next();
            if (node.isActive()) {
                activeNodes.add(node);
            }
        }
        return activeNodes;
    }

    public void deactivateAllNodes() throws Exception {
        this.getActiveNodes();
        for (Iterator it = this.getActiveNodes().iterator(); it.hasNext();) {
            ((Node) it.next()).deactivate();
        }
    }

    /**
     * Deactivate all nodes that don't have a milestone
     * and that are not endnodes.
     * They need to stay active to be able to see in which state the workflow ended.
     * Gets called on workflow deactivation. (entering endnode)
     */
    public void deactivateNonMileStoneNodes() throws Exception {
        this.getActiveNodes();
        for (Iterator it = this.getActiveNodes().iterator(); it.hasNext();) {
            Node n = ((Node) it.next());
            if (n.getMileStone() == null && !n.isEndNode()){
                n.deactivate();
            }
        }
    }


    /**
     * Returns a list of active Tasks of the workflow object
     * @return a list of active Tasks.
     */
    public List getActiveTasks() throws Exception {
        List temp = TaskManager.getActiveTasksForWorkflow(getId());
        return temp;
    }


    public ArrayList getActiveTasks(boolean mandatory) throws Exception {
        ArrayList temp = TaskManager.getActiveUserTasks(getId(), mandatory);
        return temp;
    }

    public WorkflowTask getActiveTask(String actionName) throws Exception {
        ArrayList temp = TaskManager.getAllActiveTasks(getId(), actionName);
        for (Iterator it = temp.iterator(); it.hasNext(); ){
        	WorkflowTask task = (WorkflowTask) it.next();
        	if (task.getWorkflowId() == this.getId()){
        		return task;
        	}
        }
        Logger.ERROR("No active task for: " + actionName + " found in wf: " + getId());
        return null;
    }

    /**
     * get the list of all nodes of a workflow, not only the active ones.
     * @return the complete list of nodes
     */
    public ArrayList getAllNodes() {
        // initialize the cache
        if (nodeCache == null){
            nodeCache = WorkflowManager.loadNodes(this.getId());
        }
        return nodeCache;
    }

    /**
     * check if there is a node with a given Id in the workflow and return it in
     * case it was found.
     *
     * @param id - the database id to search for.
     * @return a node or null if no node was found.
     */
    public Node getNode(int id) {
        for (Iterator it = getAllNodes().iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            if (node.getId() == id) {
                return node;
            }
        }
        Logger.ERROR("Node with ID " + id + " was not found in Workflow " + getName());
        return null;
    }

    /**
     * check if there is a node with a given name in the
     * workflow and return it in case it was found.
     *
     * @param name - the nodes name.
     * @return a node or null if no node was found.
     */
    public Node getNode(String name) {
        for (Iterator it = getAllNodes().iterator(); it.hasNext(); ) {
            Node node = (Node) it.next();
            if (node.getName().equals(name)) {
                return node;
            }
        }
        Logger.ERROR("Node with name " + name + " was not found in Workflow " + getId());
        return null;
    }


    public boolean containsNode(String name) {
        boolean found = false;
        for (Iterator it = getAllNodes().iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            if (node.getName().equals(name)) {
                found = true;
                break;
            }
        }
        return found;
    }
    
    
    /**
     * Central event handling routine. The method queries all nodes and their
     * edges and provides the incoming Event to every edge with help of method
     * handleEvent in {@Edge}s.
     * <p>
     * Note that this method changes the databases: If the edges are satisfied
     * for a node, it is left to go to its successor.
     *
     * @param e the event that will be handled by the workflow object
     */
    public synchronized void handleEvent(Event e, String userName, ResultList history)
        throws StorageException{
        // the loop below changes activeNodes and activeEdges,
        // so we need a copy
        Logger.DEBUG("Wf-" + this.getId() + ": Handling event " + e.getType());
        ArrayList activeNodes = getActiveNodes();
        for (Iterator iter = activeNodes.iterator(); iter.hasNext();) {
            Node activeNode = (Node) iter.next();
            ArrayList nextNodes = activeNode.handleEvent(e);

            if (nextNodes.size() > 0) {
                leaveNode(activeNode, userName);
                for (Iterator nextIter = nextNodes.iterator(); nextIter.hasNext();) {
                    Node nextnode = (Node) nextIter.next();
                    try {
                        enterNode(nextnode, userName, history);
                    } catch (Exception ex) {
                        Logger.ERROR("Could not enter Node "
                                + nextnode.getName() + " Reason: " + ex.getMessage());
                        history.addResult(ResultList.ERROR, "Could not enter Node "
                                + nextnode.getName() + " Reason: " + ex.getMessage());
                        // FIXME: Don't stop the whole Workflow if one
                        // node is running wild
                        // throw new Exception(ex);
                    }
                }
            }
        }
        Logger.DEBUG("Wf-" + this.getId() + ":Finished Handling event " + e.getType());
    }

    /**
     * Enter a node. This utility method is usually called from handleEvent and
     * enters a particular node if the conditions of its incoming edges are all satisfied.
     *
     * @param node - The node that will be entered.
     */

    public void enterNode(Node node, String userName, ResultList history) throws Exception {
        if (node == null){ throw new IllegalArgumentException("Node to enter is NULL!"); }

        Logger.DEBUG("Entering node: " + node.getName());
        node.activate(userName, history);

        // do nothing when entering a deadend node
        if (node.getEdges() != null && !node.getEdges().isEmpty()) {
            // If empty edges are found, the node is left immediately
            ArrayList nextNodes = node.handleEvent(new Event("none", getId(), getId()));
            if (nextNodes.size() > 0) {
                leaveNode(node, userName);
                for (Iterator iter = nextNodes.iterator(); iter.hasNext(); ) {
                    enterNode((Node) iter.next(), userName, history);
                }
            }
        }
    }

    /**
     * Leave a node. This method removes the given node from the list of actives
     * nodes of this case. It resets existing edges.
     *
     * @param node - the node to leave
     */
    public void leaveNode(Node node, String userName) throws StorageException {
        Logger.DEBUG("Leaving node: " + node.getName());
        HistoryManager.create("NODE_LEAVE", node.getId(), getId(), userName, null);
        node.deactivate();
    }



    /**
     * @return the Paths of all Databits in all attached Datasets
     */
    public ArrayList getAllDatabitPaths() {
        ArrayList bitPaths = new ArrayList();
        for (Iterator it = this.getDatasets().iterator(); it.hasNext(); ){
           Dataset dset = (Dataset) it.next();
           for (Iterator it2 = dset.getAllDatabitPaths().iterator(); it2.hasNext(); ){
               String val = (String) it2.next();
               bitPaths.add(dset.getName() + "." + val);
           }
        }
        return bitPaths;
    }



    /** Get the Databit in the given path
     * Notation: datasetname.[datasetname.[databitname]]
     *
     * This function also provides an API to access Workflow
     * Properties like descriptions, due dates ...
     * Access to this values is done by a leading "System."
     *
     * @param path
     * @return requested Databit if found, null if not.
     */
    public Databit getDatabit(String path) {
        // Logger.DEBUG("getDatabit: " + path );
    	Databit dbit = null;
        if (path.startsWith("System.")){
            path = path.substring(7);
            dbit = DataToWfPropertyMapper.getValueForWorkflow(this, path);
        } else {
            StringTokenizer st = new StringTokenizer(path, ".");
            if (st.countTokens() == 0) {
                Logger.BUG("Empty pathname for Databit in Wf #" + this.getId());
            } else if (st.countTokens() == 1) {
                String databitName = st.nextToken();
                Logger.ERROR("Warning: Please use qualified Databit-Pathname "
                        + " for Dbit " + databitName);
            } else if (st.countTokens() > 1) {
                // assuming that the first item is the datasetname
                String datasetName = st.nextToken();
                Dataset dataset = getDataset(datasetName);
                if (dataset != null) {
                    String newField = path.substring(datasetName.length() + 1);
                    dbit = dataset.getDatabit(newField);
                }
            }
        }
        return dbit;
    }


    /**
     * Search for a databit with that id, returns null if not found
     */
    public Databit getDatabit(int id) {
        Databit dbit = null;
        for (Iterator it = getAllDatabitPaths().iterator(); it.hasNext(); ){
            String path = (String) it.next();
            Databit checkdbit = getDatabit(path);
            if (checkdbit.getId() == id){
                dbit = checkdbit;
                break;
            }
        }
        return dbit;
    }

    /**
     * Convenience Method to get a Databitvalue as String directly
     *
     * @param path
     * @return
     */
    public String getDatabitValue(String path) {
        Databit dbit = getDatabit(path);
        if (dbit != null) {
            return dbit.getValue();
        } else {
            Logger.ERROR("Request non-existant databit-value " + path + " in wf: " + getName());
            return "";
        }
    }


     /**
      * Evaluate if this Workflow has a Databit attached at the provided path
      *
      * @param path
      * @return
      */
    public boolean containsDatabit(String path) {
       return getDatabit(path) == null ? false : true;
    }



    /**
	 * Will fetch all Data from the <i>from </i>-Workflows Default Dataset into
	 * this Workflows Default Dataset
	 *
	 * @param from
	 * @return
	 */
    public int fetchContentFrom(Workflow from, String uname) {
        int count = 0;
        List dataPaths = from.getDataset(from.getTemplate().getDefaultDsetName()).
            getAllDatabitPaths();
        for (Iterator it = dataPaths.iterator(); it.hasNext();) {
            String targetPath = (String) it.next();
            String dataPath = from.getTemplate().getDefaultDsetName() + "." + targetPath;
            Databit fromBit = from.getDatabit(dataPath);
            String fromContent = fromBit.getValue();
           if (this.containsDatabit(this.getTemplate().getDefaultDsetName() + "." + targetPath)) {
                Databit targetBit = this.getDatabit(this.getTemplate().getDefaultDsetName() + "."
                        + targetPath);
                try {
                    if (!fromBit.getType().equals(targetBit.getType())) {
                        Logger.WARN("Incompatible Databits in path: " + dataPath +
                        		" " + fromBit.getType() + "/" + targetBit.getType());
                    }
                    if (!fromBit.getValue().equals(targetBit.getValue())) {
                        if (fromBit.getType().equals("fileref")) {
                            Logger.DEBUG("Going to copy file: " + fromBit.getValue());
                            String fileDir = SWAMP.getInstance().getProperty("ATTACHMENT_DIR");
                            String fs = System.getProperty("file.separator");
                            File fromFile = new File(fileDir + fs + fromBit.getId() + "-"
                                    + fromContent);
                            File toFile = new File(fileDir + fs + targetBit.getId() + "-"
                                    + fromContent);
                            FileUtils.copyFile(fromFile, toFile);
                        }
                        targetBit.setValue(fromContent, uname);
                        count++;
                        Logger.DEBUG("Copied Databit " + dataPath + "(" + fromContent + ") to Wf-"
                                + this.getId());
                    }
                } catch (Exception e) {
                    Logger.ERROR("Databit copy failed: " + e.getMessage());
                }
            }
        }
        return count;
    }



    /**
     * @param displayed 
     * @return - a list of Milestones included in the nodes
     * the returned list will already be ordered by the milestones
     * weight ascending to represent the correct order.
     */
    public LinkedList getMileStones(boolean displayed) {
        LinkedList stones = new LinkedList();
        for (Iterator it = getAllMileStones().iterator(); it.hasNext();) {
            MileStone stone = (MileStone) it.next();
            if (stone.isDisplayed() == displayed) {
                stones.add(stone);
            }
        }
        return stones;
    }


    /**
     * @param displayed 
     * @return - a list of Milestones included in the nodes
     * the returned list will already be ordered by the milestones
     * weight ascending to represent the correct order.
     */
    public LinkedList getAllMileStones() {
        LinkedList stones = new LinkedList();
        for (Iterator it = getAllNodes().iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            MileStone stone = node.getMileStone();
            if (stone != null) {
                stones.add(stone);
            }
        }
        // order by weight:
        Comparator comp = new Comparator() {
            public int compare(Object stone1, Object stone2) {
                Integer weight1 = new Integer(((MileStone) stone1).getTemplate().getWeight());
                Integer weight2 = new Integer(((MileStone) stone2).getTemplate().getWeight());
                return weight1.compareTo(weight2);
            }
        };
        Collections.sort(stones, comp);
        return stones;  
    }



    /**
     * Returns a string representing the workflow object
     *
     * @return a multiline string showing workflow data.
     */
    public String toString() {
        // general info
        StringBuffer sb = new StringBuffer("[Workflow: " + getName()
                + " unique id: " + getId() + "\n" + " version: "
                + getVersion() + "]\n");
        // add info from nodes
        ArrayList nodes = getAllNodes();

        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            sb.append("\n");
            sb.append(((Node) iter.next()).toString());
        }
        // add infos from datapack
        for (Iterator it = getDatasets().iterator(); it.hasNext(); ){
            Dataset dset = (Dataset) it.next();
            sb.append("\n" + dset.toString());
        }
        return sb.toString();
    }

    /**
     * Returns a string representing the workflow object
     *
     * @return a multiline string showing workflow data.
     */
    public String toXML() {
        // general info
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" standalone=\"no\" ?>\n");
        sb.append("<Workflow id=\"" + getId() + "\" name=\"" + getName()
                + " version=\"" + getVersion() + "\">\n");

        // add info from nodes
        ArrayList nodes = getAllNodes();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            sb.append(((Node) iter.next()).toXML() + "\n");
        }

        // add infos from datapack
        for (Iterator it = getDatasets().iterator(); it.hasNext(); ){
            Dataset dset = (Dataset) it.next();
            sb.append("\n" + dset.toXML());
        }
         sb.append("\n</Workflow>\n");
        return new String(sb);
    }


    public boolean equals(Workflow wf) {
        return (getId() == wf.getId());
    }


    /**
     * Gets a dataset from the workflow.
     * Check with the local DatasetIdCache
     * if this Dataset is already in the global DatasetCache, if not
     * it is loaded from DB and added.
     * @return attached dataset of the given name
     */
    public Dataset getDataset(String datasetName) {
        Dataset dSet = null;
        StringTokenizer st = new StringTokenizer(datasetName, ".");
        if (datasetName.startsWith("System.")){
        	dSet = new Dataset("System", "System values", Data.READWRITE);
        } else {
            if (st.countTokens() == 0) {
                Logger.BUG("Requesting empty dsetname in wf: " + this.getId());
            } else if (st.countTokens() == 1) {
                dSet = DatasetCache.getInstance().getByName(this.dataSetIdCache, datasetName);
                if (dSet == null) {
                    // Set not yet in idcache or global cache, load + store it
                    dSet = DataManager.loadDatasetforWorkflow(getId(), datasetName);
                    addToIdCache(dSet);
                }
            } else if (st.countTokens() > 1) {
                String targetSet = st.nextToken();
                // redirecting request to the next dataset
                dSet = this.getDataset(targetSet);
                if (dSet != null){
                    String newSetName = datasetName.substring(targetSet.length() + 1);
                    dSet = dSet.getDataset(newSetName);
                }
            }
        }
       return dSet;
    }



    /**
     * Loads all attached Datasets from DB bypassing cache,
     * but adding them to the cache afterwards.
     * Bypass the cache because we do not know if all
     * attached Datasets are already cached.
     * @return List of attached Datasets
     */
    public List getDatasets() {
        List dsets = DataManager.loadDatasetsforWorkflow(getId());
        addToIdCache(dsets);
        return dsets;
    }


    /**
     * gets the current workflow state.
     */

    public String getState() {
        if (isRunning()) {
			return "running";
		} else {
			return "inactive";
		}
    }

    /**
     * Check if a workflow is running, ie has no active endnodes
     */
    public boolean isRunning(){
        boolean running = true;
        List activeNodes = getActiveNodes();
        for (Iterator it = activeNodes.iterator(); it.hasNext(); ){
            Node n = (Node) it.next();
            NodeTemplate nt = n.getTemplate();
            if (nt != null && nt.getType().equals("end")){
                running = false;
                break;
            }
        }
        // check if it has active nodes at all:
        if (running && activeNodes.size() == 0) {
			running = false;
		}
        return running;
    }


    /**
     * @return Returns the startNode.
     */
    public Node getStartNode() {
        for (Iterator it = getAllNodes().iterator(); it.hasNext(); ){
            Node n = (Node) it.next();
            if (n.getTemplate().getType().equals("start")){
                return n;
            }
        }
        Logger.ERROR("No startnode found for Wf-" + this.getId());
        return null;
    }

	/**
	 * @return Returns the parentwfid.
	 */
	public int getParentwfid() {
	    return parentwfid;
	}


    /**
     * @return Returns workflow-ID from the workflow that is
     * on top of the subworkflow stack, or its own id if there is no
     * parent workflow
     */
    public int getMasterParentWfId() {
        if (getParentwfid() > 0) {
            return WorkflowManager.getInstance().
                getWorkflow(getParentwfid()).getMasterParentWfId();
        } else {
            return getId();
        }
    }

    public Workflow getMasterParentWf() {
            return WorkflowManager.getInstance().
                getWorkflow(getMasterParentWfId());
    }


    /**
     * @return - HashSet with all Workflow ids of that workflow stack.
     * (All ids of parent and sub-workflows)
     * Useful to send an event to all workflows that belong together.
     */
    public HashSet getAllWfStackIds(){
        WorkflowManager wfman = WorkflowManager.getInstance();
        Workflow top = wfman.getWorkflow(getMasterParentWfId());
        HashSet stackIds = top.getAllSubWfIds(new HashSet());
        stackIds.add(new Integer(top.getId()));
        Logger.DEBUG("Got wfStackIds: " + stackIds);
        return stackIds;
    }



    public Workflow getParentWf() {
        return WorkflowManager.getInstance().getWorkflow(getParentwfid());
    }

	public boolean isSubWorkflow() {
		return (parentwfid > 0);
	}


	/**
	 * @param parentwfid The parentwfid to set.
	 */
	public void setParentwfid(int parentwfid) throws NoSuchElementException {
		if (parentwfid != 0){
			// check if id and type are valid:
			Workflow parent = WorkflowManager.getInstance().getWorkflow(parentwfid);
			if (!this.getTemplate().getParentWfName().equals(parent.getTemplateName())){
				throw new NoSuchElementException("New parents name (" +
						parent.getTemplateName() + ") and allowed name (" +
						this.getTemplate().getParentWfName() + ") mismatch!");
			}
		}
		this.parentwfid = parentwfid;
		setModified(true);
	}

    /**
     * @return Returns the version.
     */
    public String getVersion() {
        return this.version;
    }

   /**
     * @return The description text for this workflow including replaced vars
     */
    public String getReplacedDescription() {
    	StringBuffer desc = new StringBuffer();
    	if (getTemplate() != null){
    		desc.append(NotificationTools.workflowDataReplace(getTemplate().
    				getDescription(), this).trim());
    	}
    	return desc.toString().replaceAll("[ \t]+", " ").replaceAll("\\( ", "(").replaceAll(" \\)", ")");
    }


    /**
     * @return Explaining text in which status this workflow currently is.
     * Consisting of the longdescription elements of active nodes. (not milestone nodes)
     */
    public String getStateDescription(){
    	StringBuffer desc = new StringBuffer();
        if (this.isRunning()){
            for (Iterator it = this.getActiveNodes().iterator(); it.hasNext(); ){
                Node node = (Node) it.next();
                if (node.getMileStone() == null &&
                    node.getTemplate().getLongDescription() != null &&
                            !node.getTemplate().getLongDescription().trim().equals("")){
                	desc.append(node.getTemplate().
                    getReplacedLongDescription(this.getId())).append('\n');
                }
            }
        } else {
            for (Iterator it = this.getActiveNodes().iterator(); it.hasNext(); ){
                Node node = (Node) it.next();
                if (node.isEndNode()){
                    if (node.getTemplate().getLongDescription() != null &&
                            !node.getTemplate().getReplacedLongDescription(this.getId()).trim().equals("")){
                    	desc.append(node.getTemplate().
                        getReplacedLongDescription(this.getId())).append('\n');
                    } else if (node.getTemplate().getDescription() != null &&
                            !node.getTemplate().getReplacedDescription(getId()).trim().equals("")){
                    	desc.append(node.getTemplate().
                    	getReplacedDescription(this.getId())).append('\n');
                    }
                }
            }
        }
        return desc.toString();
    }


    /**
     * @return the correspondig WorkflowTemplate for fetching common data
     */
    public WorkflowTemplate getTemplate() {
       WorkflowManager wfMan = WorkflowManager.getInstance();
        return wfMan.getWorkflowTemplate(this.templName, this.version);
    }


    /**
     * Should only be called by WorkflowTemplate for initial setting of nodes.
     * @param nodes The nodes attached to this workflow.
     */
    void setNodes(ArrayList nodeCache) {
        this.nodeCache = nodeCache;
    }

    public void resetSubIdCache (){
        this.subIdCache = null;
    }


    private void addToIdCache(List dsets){
    	if (dsets != null){
    		for (Iterator it = dsets.iterator(); it.hasNext(); ){
    			addToIdCache((Dataset) it.next());
    		}
    	}
    }


    private void addToIdCache(Dataset dset){
    	if (dset != null){
    		dataSetIdCache.add(new Integer(dset.getId()));
    	}
    }


    /**
    * Checks if the User has the given role in this Workflowinstance.
    * (Permissions between Template and instance may differ in the future)
    * This is configured in the Workflow definition.
    * Example roles are "admin" - "starter" - "user"
    * or a Databit-Path that contains the usernames
    *
    * swampadmins as defined in the SWAMP usermanagement have all roles
    *
    */
    public boolean hasRole(String username, String role){
        boolean hasRole = false;
        WorkflowTemplate wfTemp = getTemplate();
        WorkflowRole wfrole = wfTemp.getWorkflowRole(role);
        try {
			if (wfrole.hasRole(username, this)){
			    hasRole = true;
			} else {
			        SWAMPUser user = null;
			        try {
			            user = SecurityManager.getUser(username);
			        } catch (StorageException e) {
			            Logger.ERROR("Error fetching user for role-check: " + username);
			        }
			    // SWAMPAdmins, Workflow-Admins and "Owner"
			    // always have all permissions in a workflow
			    if (SecurityManager.isGroupMember(user, "swampadmins") ||
			            wfTemp.getWorkflowRole(WorkflowRole.ADMIN).hasRole(username, this) ||
			            wfTemp.getWorkflowRole("owner").hasRole(username, this)){
			        hasRole = true;
			    }
			}
		} catch (Exception e) {
			Logger.ERROR("Error in checking role " + role +
					" in " + getName() + " Msg: " + e.getMessage());
		}
        return hasRole;
    }


    public WorkflowRole getRole(String role){
        return getTemplate().getWorkflowRole(role);
    }


    /**
     * get the databittemplate for a databit.
     * Can also be fetched from the parent workflow if this is a subworkflow,
     * or be null if it was not created based on a template.
     */
    public DatabitTemplate getDatabitTemplate(String path){
        DatabitTemplate template = null;
        if (this.getTemplate().getDatabitTemplate(path) != null) {
			template = this.getTemplate().getDatabitTemplate(path);
		}
        // TODO check parent workflows if template not found
        return template;
    }


    public Workflow getWorkflowForRole(String roleName) throws Exception {
    	Workflow wf = this;
    	if (roleName.startsWith("parent.") && parentwfid > 0){
    		wf = getParentWf().getWorkflowForRole(roleName.substring(7));
    	} else if (roleName.startsWith("parent.")) {
    	    throw new Exception("No parent workflow available for role: " + roleName);
    	}
    	return wf;
    }


    /**
     * Change the parent workflowid, means attach this workflow to another parent.
     * This also means to remove the datasets of old parent and attach those of the new one.
     * Setting parent id to 0 means detach completely
     */
    public boolean changeParentId(int newId, String uname) throws
    	NoSuchElementException, StorageException  {
    	boolean changed = false;
    	if (newId != this.getParentwfid()){
	    	WorkflowManager wfMan = WorkflowManager.getInstance();
	    	Workflow newParent = null;
	    	Workflow oldParent = null;
	    	List oldParentDsets = null;
	    	List newParentDsets = null;
	    	if (newId > 0) {
	    		newParent =	wfMan.getWorkflow(newId);
	    		newParentDsets = newParent.getDatasets();
	    	}
	    	if (this.getParentwfid() > 0) {
	    		oldParent = wfMan.getWorkflow(this.getParentwfid());
	    		oldParentDsets = oldParent.getDatasets();
	    	}
	    	setParentwfid(newId);
	    	// remove old dsetids / add new dsetids from/to itself and all childs:
	    	HashSet stackIds = this.getAllSubWfIds();
	    	stackIds.add(new Integer(this.getId()));
	    	for (Iterator it = stackIds.iterator(); it.hasNext(); ){
	    		Workflow wf = wfMan.getWorkflow(((Integer) it.next()).intValue());
	    		if (oldParent != null){
		    		for (Iterator oldIt = oldParentDsets.iterator(); oldIt.hasNext(); ){
		    			WorkflowManager.detachDatasetFromWorkflow(((Dataset)
		    					oldIt.next()).getId(), wf.getId());
		    		}
	    		}
	    		if (newParent != null) {
		    		for (Iterator newIt = newParentDsets.iterator(); newIt.hasNext(); ){
		    			DataManager.addDatasetToWorkflow(((Dataset)
		    					newIt.next()).getId(), wf.getId());
		    		}
	    		}
	    		wfMan.removeFromCache(wf.getId());
	    	}
	    	// change parent id:
	    	wfMan.removeFromCache(newId);
	    	if (oldParent != null) {
				wfMan.removeFromCache(oldParent.getId());
			}
	    	WorkflowManager.storeWorkflow(this);
	    	changed = true;
    	}
    	return changed;
    }


    /**
     * Start this workflow
     */
    public void start(String userName, ResultList history) throws Exception {
        if (this.isRunning()) {
			throw new Exception("Don't call start() on already running wf instances: " + getName());
		}

        enterNode(getStartNode(), userName, history);
        WorkflowManager.storeWorkflow(this);
    }


}
