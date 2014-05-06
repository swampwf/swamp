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

package de.suse.swamp.core.container;

import java.util.*;

import org.apache.torque.*;
import org.apache.torque.om.*;
import org.apache.torque.util.*;

import com.workingdogs.village.*;

import de.suse.swamp.core.conditions.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

/**
 * This is the Storage-Manager for everything that needs
 * to be stored from the WorkflowManager.
 *
 * Note for the accessibility of this Methods:
 * "If none of the access modifiers public, protected, or private are specified,
 * a class member or constructor is accessible throughout the package that
 * contains the declaration of the class in which the class member is declared,
 * but the class member or constructor is not accessible in any other package.
 *
 * so they may only be instantiated by the Manager-Classes.
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

final class WorkflowStorage {

    // extra logger for storage stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.container.Storage");

    /**
     * Workflow storage method. It can be called with new and formerly stored
     * workflows, because it checks, if a workflow is new and needs to be
     * stored initially or if it already exists in the database and only
     * needs to be updated.
     *
     * @param wf - the workflow object to store
     * @return the id of the workflow
     */
    static int storeWorkflow(Workflow wf) throws StorageException {
        int wfID = wf.getId();
        if (wf.isModified()) {
            Criteria crit = new Criteria();
            crit.add(DbworkflowsPeer.TEMPLATENAME, wf.getTemplateName());
            crit.add(DbworkflowsPeer.PARENTWFID, wf.getParentwfid());
            crit.add(DbworkflowsPeer.VERSION, wf.getVersion());

            if (wfID > 0) {
                // do an update
                crit.add(DbworkflowsPeer.WFID, wfID);
                try {
                    DbworkflowsPeer.doUpdate(crit);
                    log.debug("updating workflow: " + crit.toString());
                } catch (TorqueException e) {
                    log.error("Can not update Dbworkflows with wf: " + wf.getId());
                    throw new StorageException(e.getMessage());
                }
            } else {
                // do an insert since we do not have a valid id yet.
                try {
                    NumberKey key = (NumberKey) DbworkflowsPeer.doInsert(crit);
                    wfID = key.intValue();
                    log.debug("insert workflow: " + crit.toString());
                    wf.setId(wfID);
                } catch (TorqueException e) {
                    log.error("Can not insert in Dbworkflows: " + e);
                    throw new StorageException(e.getMessage());
                }
            }
            wf.setModified(false);
        }
        storeNodes(wf.getAllNodes(), wfID);
        return wfID;
    }


    /**
     * Deletes a Workflow from the entire System, including history, nodes, edges,
     * tasks, simply everything that has references to it
     * @param wf
     */
    static void removeWorkflow(Workflow wf) throws Exception {
        log.info("Deleting workflow: " + wf.getName());
        removeSubworkflows(wf.getId());
        for (Iterator it = wf.getAllNodes().iterator(); it.hasNext(); ){
            Node node = (Node) it.next();
            removeEdges(node);
            removeMilestone(node);
            Criteria crit = new Criteria();
            crit.add(DbnodesPeer.NODEID, node.getId());
            log.debug("removing nodes: " + crit.toString());
            DbnodesPeer.doDelete(crit);
        }
        removeNotifications (wf);
        TaskStorage.removeTasks(wf);
        TaskManager.removeFromActiveTaskCache(wf.getId());
        removeHistory(wf);
        StorageManager.removeDatasetsFromWorkflow(wf);
        // remove from workflowtable
        Criteria crit = new Criteria();
        crit.add(DbworkflowsPeer.WFID, wf.getId());
        log.debug("removing workflow: " + crit.toString());
        DbworkflowsPeer.doDelete(crit);
    }


    static private void removeSubworkflows(int wfId) throws Exception {
        Criteria crit = new Criteria();
        WorkflowManager wfMan = WorkflowManager.getInstance();
        crit.add(DbworkflowsPeer.PARENTWFID, wfId);
        List storedSubwfs = DbworkflowsPeer.doSelect(crit);
        for (Iterator it = storedSubwfs.iterator(); it.hasNext(); ){
            Dbworkflows dbwf = (Dbworkflows) it.next();
            removeWorkflow(wfMan.getWorkflow(dbwf.getWfid()));
        }
    }

    static private void removeMilestone (Node node) throws TorqueException {
        if (node.getMileStone() != null){
            Criteria crit = new Criteria();
            crit.add(DbmilestonesPeer.MILESTONEID, node.getMileStone().getId());
            log.debug("removing milestone: " + crit.toString());
            DbmilestonesPeer.doDelete(crit);
        }
    }

    static private void removeEdges (Node node) throws TorqueException {
        for (Iterator it = node.getEdges().iterator(); it.hasNext(); ){
            Edge edge = (Edge) it.next();
            removeCondition(edge.getCondition());
            Criteria crit = new Criteria();
            crit.add(DbedgesPeer.EDGEID, edge.getId());
            log.debug("removing edges: " + crit.toString());
            DbedgesPeer.doDelete(crit);
        }
    }

    static private void removeCondition(Condition cond) throws TorqueException {
        removeChildConditions (cond);
        String type = cond.getConditionType();
        Criteria crit = new Criteria();
        // SUB DATA EVENT have an additional entry in db*conditions
        if (type.equals("SUB")){
            crit.add(DbsubsfinishedconditionsPeer.CONDID, cond.getId());
            DbsubsfinishedconditionsPeer.doDelete(crit);
        } else if (type.equals("DATA")){
            crit.add(DbdataconditionsPeer.CONDID, cond.getId());
            DbdataconditionsPeer.doDelete(crit);
        } else if (type.equals("EVENT")){
            crit.add(DbeventconditionsPeer.CONDID, cond.getId());
            DbeventconditionsPeer.doDelete(crit);
        } else if (!type.equals("AND") && !type.equals("OR") && !type.equals("NOT")){
            Logger.ERROR("Cannot remove condition of unknown type: " + type);
        }
        crit = new Criteria();
        crit.add(DbconditionsPeer.CONDID, cond.getId());
        log.debug("removing conditions: " + crit.toString());
        DbconditionsPeer.doDelete(crit);
    }

    /** recursively delete all childconditions */
    static private void removeChildConditions (Condition condition) throws TorqueException {
        for (Iterator it = condition.getChildConditions().iterator(); it.hasNext(); ){
            Condition cond = (Condition) it.next();
            removeCondition(cond);
        }
    }


    static private void removeNotifications (Workflow wf) throws TorqueException {
        Criteria crit = new Criteria();
        crit.add(DbnotificationsPeer.WORKFLOWID, wf.getId());
        log.debug("removing notifications: " + crit.toString());
        DbnotificationsPeer.doDelete(crit);
    }


    static private void removeHistory (Workflow wf) throws TorqueException {
        Criteria crit = new Criteria();
        crit.add(DbhistoryPeer.WORKFLOWID, wf.getId());
        DbhistoryPeer.doDelete(crit);
        crit = new Criteria();
        crit.add(DbeventhistoryPeer.SOURCEWFID, wf.getId());
        crit.add(DbeventhistoryPeer.TARGETWFID, wf.getId());
        log.debug("removing history: " + crit.toString());
        DbeventhistoryPeer.doDelete(crit);
        crit.add(DbeventhistoryPeer.SOURCEWFID, 0);
        DbeventhistoryPeer.doDelete(crit);
    }

    /**
     *  Stores the nodes of a workflow to database.
     *
     * @param nodeList the list of nodes
     * @param wfID the id of the workflow to store.
     * @return res to indicate if storage failed or not.
     */
    private static boolean storeNodes(ArrayList nodeList, int wfID) throws StorageException {
        boolean res = true;
        for (Iterator nodeIt = nodeList.iterator(); nodeIt.hasNext();) {
            Node newNode = (Node) nodeIt.next();
           if (newNode.isModified()) {
                Criteria crit = new Criteria();
                crit.add(DbnodesPeer.WORKFLOWID, wfID);
                crit.add(DbnodesPeer.NAME, newNode.getName());
                crit.add(DbnodesPeer.ACTIVITY, newNode.isActive() ? 1 : 0);
                crit.add(DbnodesPeer.ISENDNODE, newNode.isEndNode() ? 1 : 0);
                /*
                 * Check if the node to save has already a non-zero id. If it
                 * has, it should already exist in the database.
                 */
                if (newNode.getId() > 0) {
                    crit.add(DbnodesPeer.NODEID, newNode.getId());
                    try {
                        DbnodesPeer.doUpdate(crit);
                    } catch (TorqueException e) {
                        Logger.ERROR("ERR: Can not update Dbnodes:" + e);
                        throw new StorageException("ERR: Can not update Dbnodes", e);
                    }
                    Logger.DEBUG("Updated Node with id: " + newNode.getId(), log);
                } else {
                    int nodeID;
                    try {
                        NumberKey key = (NumberKey) DbnodesPeer.doInsert(crit);
                        nodeID = key.intValue();
                    } catch (TorqueException e) {
                        Logger.ERROR("Unable to doInsert in Dbnodes: " + e);
                        throw new StorageException("Unable to doInsert in Dbnodes: "
                        		+ e.getMessage(), e);
                    }
                    newNode.setId(nodeID);
                    Logger.DEBUG("Inserted Node with id: " + nodeID, log);
                }
            }
           newNode.setWorkflowId(wfID);
           newNode.setModified(false);
        }
        /* Now all Nodes are stored. Again, loop over the nodes and store the
         * edges of them.  it is important to save all nodes first in order to
         * have proper ids for every node to
         * set the edge-to link correctly.
         */
        try {
			storeNodeEdges(nodeList);
		} catch (Exception e) {
			Logger.ERROR("Storing of Node-Edges failed: " + e.getMessage());
			throw new StorageException("Storing of Node Edges failed: " + e.getMessage());
		}
        res &= storeNodeMileStones(nodeList);
        if (res == false) {
        	Logger.ERROR("Storing of Node-MileStone failed");
        	throw new StorageException("Storing of Node-MileStone failed");
        }
        return res;
    }

    /**
	 * store the edges to database.
	 *
	 * @param nodes - the list of all nodes.
	 * @return true or false to indicate success.
	 */
    private static void storeNodeEdges(ArrayList nodes) throws Exception {
        // Iterate over all nodes of a workflow
    	for (Iterator it = nodes.iterator(); it.hasNext();) {
    		Node node = (Node) it.next();
    		// Iterate over the nodes edges
    		for (Iterator edgeIt = node.getEdges().iterator(); edgeIt.hasNext();) {
    			Edge edge = (Edge) edgeIt.next();
    			if (edge.isModified()) {
    				Criteria crit = new Criteria();
    				int edgeId = edge.getId();
    				crit.add(DbedgesPeer.NODEID, node.getId());
    				int toNodeId = edge.getToNode().getId();
    				if (toNodeId > 0) {
    					crit.add(DbedgesPeer.NODETO, toNodeId);
    					if (edgeId > 0) {
    						crit.add(DbedgesPeer.EDGEID, edgeId);
    						DbedgesPeer.doUpdate(crit);
    						Logger.DEBUG("Updated Edge #" + edgeId, log);
    					} else {
    						NumberKey key = (NumberKey) DbedgesPeer.doInsert(crit);
    						edgeId = key.intValue();
    						edge.setId(edgeId);
    						Logger.DEBUG("Inserted Edge #" + edgeId, log);
    					}
    					edge.setModified(false);
    				}
    			}

                /* Now save the conditions for the edge. */
                edge.setWorkflowId(node.getWorkflowId());
                if (edge.isConditionLoaded()) {
					storeCondition(0, edge, edge.getCondition());
				}
                edge.setModified(false);
            }
        }
    }



    /**
     * store a nodes MileStone
     * @param nodes
     * @return
     */
    private static boolean storeNodeMileStones(ArrayList nodes) throws StorageException {

        boolean res = true;
        // Iterate over all nodes of a workflow
        for (Iterator it = nodes.iterator(); res && it.hasNext();) {

            Node node = (Node) it.next();
            MileStone mile = node.getMileStone();

            if (mile != null && mile.isModified()) {
                Criteria crit = new Criteria();
                crit.add(DbmilestonesPeer.NODEID, node.getId());
                crit.add(DbmilestonesPeer.DISPLAYED, mile.isDisplayed() ? 1 : 0);

                try {
                    if (mile.getId() > 0) {
                        crit.add(DbmilestonesPeer.MILESTONEID, mile.getId());
                        DbmilestonesPeer.doUpdate(crit);
                    } else {
                        NumberKey key = (NumberKey) DbmilestonesPeer.doInsert(crit);
                        mile.setId(key.intValue());
                    }
                } catch (TorqueException e2) {
                    e2.printStackTrace();
                }
            }
        }
        return res;
    }








    /**
     * stores the condition tree of an edge. The parentID needs to be zero for
     * the condition, that is directly stored in the edge. If the conditions are
     * nested, the parent ID contains the id of the parent condition.
     *
     * @param parentId -
     *           the id of the condition this condition is nested in or zero if
     *           the condition is the edge condition.
     * @param cond -
     *           the condition to save.
     * @return - true for successfull saving, else false.
     */
    private static boolean storeCondition(int parentId, Edge edge, Condition cond) {

        boolean res = true;
        int condId = cond.getId();
        if (cond.isModified()) {
            /* Update or insert into the Conditiontable */
            Criteria crit = new Criteria();
            crit.add(DbconditionsPeer.EDGEID, edge.getId());
            crit.add(DbconditionsPeer.TYPE, cond.getConditionType());
            crit.add(DbconditionsPeer.PARENT, parentId);

            // need an update
            if (condId > 0) {
                crit.add(DbconditionsPeer.CONDID, condId);
                try {
                    DbconditionsPeer.doUpdate(crit);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not update Condition. detail: " + e);
                    res = false;
                }
            } else {
                // need to create new database entry
                try {
                    NumberKey key = (NumberKey) DbconditionsPeer.doInsert(crit);
                    condId = key.intValue();
                    cond.setId(condId);
                    Logger.DEBUG("Inserted Condition #" + condId
                            + " (" + cond.getConditionType() + ")", log);
                } catch (TorqueException e) {
                    res = false;
                    Logger.ERROR("Could not insert Condition. detail: " + e);
                }
            }
        }

        // store Event conditions
        if (res && cond.getConditionType().equalsIgnoreCase("EVENT")) {
            res &= storeEventCondition((EventCondition) cond);
            // store Data conditions
        } else if (res && cond.getConditionType().equalsIgnoreCase("DATA")) {
            res &= storeDataCondition((DataCondition) cond);
       } else if (res && cond.getConditionType().equalsIgnoreCase("SUB")) {
            res &= storeSubsFinishedCondition((SubsFinishedCondition) cond);
        // AND + OR Conditions don't need an extra Table.
        } else if (res && !cond.getConditionType().equalsIgnoreCase("AND") &&
                !cond.getConditionType().equalsIgnoreCase("OR") &&
                !cond.getConditionType().equalsIgnoreCase("NOT")) {
            Logger.ERROR("Unsupported Condition Type: " + cond.getConditionType(), log);
        }

        /* Loop over all nested conditions and save them */
        if (res) {
            List condList = cond.getChildConditions();
            for (Iterator childIt = condList.iterator(); childIt.hasNext();) {
                // Save the child tree.
                Condition childCond = (Condition) childIt.next();
                res &= storeCondition(condId, edge, childCond);
                /* Now the condition ID of the child is valid. */
            }
        }
        cond.setWorkflowId(edge.getWorkflowId());
        // set them to unmodified
        cond.setModified(false);
        return res;
    }


    /**
     * Method to store an EventCondition
     * @param cond - the Eventcondition to store
     * @return boolean result
     */
    static boolean storeEventCondition(EventCondition cond) {

       if (cond.isModified()) {
            // Check if already in DB:
            Criteria detailCrit = new Criteria();
            detailCrit.add(DbeventconditionsPeer.CONDID, cond.getId());
            List storedconds;
            try {
                storedconds = DbeventconditionsPeer.doSelect(detailCrit);
            } catch (TorqueException e1) {
                e1.printStackTrace();
                Logger.ERROR("Couldn't select from Eventconditions Table!", log);
                return false;
            }
            detailCrit = new Criteria();
            detailCrit.add(DbeventconditionsPeer.CONDID, cond.getId());
            detailCrit.add(DbeventconditionsPeer.EVENTTYPE, cond
                    .getEventString());
            detailCrit.add(DbeventconditionsPeer.STATE, cond.evaluate() ? 1 : 0);
            // Do update
            if (storedconds.size() > 0) {
                try {
                    DbeventconditionsPeer.doUpdate(detailCrit);
                    Logger.DEBUG("Updated EventCondition #" + cond.getId(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not update EventCondition. detail: " + e, log);
                    return false;
                }
                // do insert
            } else {
                try {
                    DbeventconditionsPeer.doInsert(detailCrit);
                    Logger.DEBUG("Inserted EventCondition #" + cond.getId(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not insert EventCondition. detail: " + e, log);
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Method to store a DataCondition
     * @param cond - the Datacondition to store
     * @return boolean result
     */
    static boolean storeSubsFinishedCondition(SubsFinishedCondition subscond) {
        if (subscond.isModified()) {
            // Check if already in DB:
            Criteria detailCrit = new Criteria();
            detailCrit.add(DbsubsfinishedconditionsPeer.CONDID, subscond.getId());
            List storedconds;
            try {
                storedconds = DbsubsfinishedconditionsPeer.doSelect(detailCrit);
            } catch (TorqueException e1) {
                e1.printStackTrace();
                Logger.ERROR("Couldn't select from Dbsubsfinishedconditions Table!", log);
                return false;
            }

            detailCrit = new Criteria();
            detailCrit.add(DbsubsfinishedconditionsPeer.CONDID, subscond.getId());
            detailCrit.add(DbsubsfinishedconditionsPeer.SUBNAME, subscond.getSubname());
			detailCrit.add(DbsubsfinishedconditionsPeer.SUBVERSION, subscond.getSubversion());
            detailCrit.add(DbdataconditionsPeer.STATE, subscond.evaluate() ? 1 : 0);

            // Do update
            if (storedconds.size() == 1) {
                try {
                    DbsubsfinishedconditionsPeer.doUpdate(detailCrit);
                    Logger.DEBUG("Updated Dbsubsfinishedconditions #" + subscond.getId(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not update subsfinished-condition detail: "+ e, log);
                    return false;
                }
                // do insert
            } else if (storedconds.size() == 0) {
                try {
                    DbsubsfinishedconditionsPeer.doInsert(detailCrit);
                    Logger.DEBUG("Inserted subsfinishedCondition #" + subscond.getId(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not insert subsfinished-condition detail: " + e, log);
                    return false;
                }
            } else {
                Logger.ERROR("Error in saving subsfinishedCondition!", log);
                return false;
            }
        }
        return true;
    }





    /**
     * Method to store a DataCondition
     * @param cond - the Datacondition to store
     * @return boolean result
     */
    static boolean storeDataCondition(DataCondition datacond) {
        if (datacond.isModified()) {
            // Check if already in DB:
            Criteria detailCrit = new Criteria();
            detailCrit.add(DbdataconditionsPeer.CONDID, datacond.getId());
            List storedconds;
            try {
                storedconds = DbdataconditionsPeer.doSelect(detailCrit);
            } catch (TorqueException e1) {
                e1.printStackTrace();
                Logger.ERROR("Couldn't select from Dataconditions Table!", log);
                return false;
            }

            detailCrit = new Criteria();
            detailCrit.add(DbdataconditionsPeer.CONDID, datacond.getId());
            detailCrit.add(DbdataconditionsPeer.FIELD, datacond.getField());
            detailCrit.add(DbdataconditionsPeer.CONDCHECK, datacond.getCheck());
            detailCrit.add(DbdataconditionsPeer.CONDVALUE, datacond.getValue());
            // don't call evaluate() to get the state because the dataset may
            // not be attached yet
            detailCrit.add(DbdataconditionsPeer.STATE, datacond.getState() ? 1 : 0);

            // Do update
            if (storedconds.size() == 1) {
                try {
                    DbdataconditionsPeer.doUpdate(detailCrit);
                    Logger.DEBUG("Updated DataCondition #" + datacond.getId(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not update Data-condition detail (id=" + datacond.getId() + 
                            " value=" + datacond.getValue() + "): "+ e, log);
                    return false;
                }
                // do insert
            } else if (storedconds.size() == 0) {
                try {
                    DbdataconditionsPeer.doInsert(detailCrit);
                    Logger.DEBUG("Inserted DataCondition #" + datacond.getId(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not insert Data-condition detail: "
                                    + e);
                    return false;
                }
            } else {
                Logger.ERROR("Error in saving DataCondition!", log);
                return false;
            }
        }
        return true;
    }




    /** Returns a filtered Workflow-id list sorted by Workflow-ID ASCENDING
     * @param filters
     * @return
     */
    static List getWorkflowIds(List filters){
        return getWorkflowIds(filters, null);
    }




    /**
     * @param filters
     * @param order
     * @return
     */
    static List getWorkflowIds(List filters, DatabaseFilter order) {

        long time = System.currentTimeMillis();

        if ((filters == null || filters.size() == 0) && order == null) {
            Logger.WARN("Empty filterlist for loading Workflows!", log);
            return new ArrayList();
        } else {

            List idList = null;

            // add the order filter on the first place, to have the
            // correctly ordered id list. Then the idlist will be retained
            // with the workflow-ids that matched the filters
            List myFilters = new ArrayList();
            if (order != null) {
				myFilters.add(order);
			}
            myFilters.addAll(filters);

            boolean hasDatabaseFilter = false;
            // iterate over database filters
            for (Iterator it = myFilters.iterator(); it.hasNext(); ) {
                List dbIdList = null;
                List tempIdList = new ArrayList();
                Object filter = it.next();
                if (filter instanceof DatabaseFilter){
                    DatabaseFilter sqlfilter = (DatabaseFilter) filter;
                    hasDatabaseFilter = true;
                    String sql = sqlfilter.getSQL();
                    Logger.DEBUG("executing: " + sql, log);
                    try {
                        dbIdList = DbworkflowsPeer.executeQuery(sql);
                        int size = dbIdList.size();
                        for (int i = 0; i < size; i++){
                            tempIdList.add(((Record) dbIdList.get(i)).getValue(1).asIntegerObj());
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (idList == null) {
                        idList = new ArrayList(tempIdList);
                    } else {
                        idList.retainAll(tempIdList);
                    }
                }
            }

            // if there were no database filters, get all wf-ids:
            if (!hasDatabaseFilter){
                if (idList == null) {
					idList = new ArrayList();
				}
                List dbIdList = null;
                String sql = "SELECT wfid from dbWorkflows;";
                try {
                    dbIdList = DbworkflowsPeer.executeQuery(sql);
                    int size = dbIdList.size();
                    for (int i = 0; i < size; i++){
                        idList.add(((Record) dbIdList.get(i)).getValue(1).asIntegerObj());
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            // iterate over memory filters, pipe idlist in
            for (Iterator it = myFilters.iterator(); it.hasNext(); ) {
                Object filter = it.next();
                if (filter instanceof MemoryFilter){
                    MemoryFilter memfilter = (MemoryFilter) filter;
                    idList = memfilter.getFilteredList(idList);
                }
            }

            Logger.DEBUG("building idlist took us: " +
                    String.valueOf(System.currentTimeMillis() - time) + " ms", log);
            return idList;
        }
    }



    static HashMap loadWorkflows(List ids) throws StorageException  {
    	Criteria crit = new Criteria();
    	crit.addIn(DbworkflowsPeer.WFID, ids);
    	log.info("bulk-loading workflows: " + ids);
    	return loadWorkflows(crit);
    }



    /**
     * loads the workflow with @param wfId from the storage backend
     * @throws NoSuchElementException - if the id does not exist, or
     * the corresponding workflow-template is not available
     */
    static Workflow loadWorkflow(int wfId) throws NoSuchElementException, StorageException {
        Criteria crit = new Criteria();
        Logger.LOG("Loading Workflow " + wfId, log);
        crit.add(DbworkflowsPeer.WFID, wfId);
        HashMap wfs = loadWorkflows(crit);
        if (wfs.size() != 1){
            throw new NoSuchElementException("Workflow #" + wfId + " not found in db.");
        }
        return (Workflow) wfs.get(new Integer(wfId));
    }


    static private HashMap loadWorkflows(Criteria crit) throws StorageException {
        List dbwfs = null;
        HashMap workflows = new HashMap();
        try {
            dbwfs = DbworkflowsPeer.doSelect(crit);
        } catch (TorqueException e) {
            throw new StorageException(e.getMessage());
        }
        for (Iterator it = dbwfs.iterator(); it.hasNext();) {
            // create the workflow storage object
            Dbworkflows Dbworkflows = (Dbworkflows) it.next();
            String wfName = Dbworkflows.getTemplatename();
            String wfVersion = Dbworkflows.getVersion();
            if (WorkflowManager.getInstance().getWorkflowTemplate(wfName, wfVersion) == null) {
                Logger.ERROR("Workflowtemplate " + wfName + " v" + wfVersion
                        + " not available for stored wf #" + Dbworkflows.getWfid());
            } else {
                Workflow wf = new Workflow(wfName, wfVersion);
                wf.setId(Dbworkflows.getWfid());
                wf.setParentwfid(Dbworkflows.getParentwfid());
                workflows.put(new Integer(wf.getId()), wf);
            }
        }
        return workflows;
    }

    /**
     * load the nodes for a stored Workflow.
     *
     * @param wfID    - the database ID of the workflow to load
     */
    static ArrayList loadNodes(int wfID) {
        Criteria crit = new Criteria();
        crit.add(DbnodesPeer.WORKFLOWID, wfID);
        crit.addAscendingOrderByColumn(DbnodesPeer.NODEID);
        ArrayList nodes = new ArrayList();
        try {
            List storedNodes = DbnodesPeer.doSelect(crit);
            for (Iterator it = storedNodes.iterator(); it.hasNext();) {
                Dbnodes Dbnodes = (Dbnodes) it.next();
                Node node = new Node(Dbnodes.getNodeid(), Dbnodes.getName(),
                        Dbnodes.getActivity() == 1 ? true : false);
                node.setWorkflowId(wfID);
				node.setModified(false);
                nodes.add(node);
            }
        } catch (TorqueException e) {
            Logger.ERROR("ERR: Can not load node list! " + e.getMessage(), log);
            nodes.clear();
        }
        return nodes;
    }



    static ArrayList loadEdges(Node node) {
        ArrayList edges = new ArrayList();
        Criteria c = new Criteria();
        c.add(DbedgesPeer.NODEID, node.getId());
        c.addAscendingOrderByColumn(DbedgesPeer.EDGEID);
        try {
            List storedEdges = DbedgesPeer.doSelect(c);
            for (Iterator edgeIt = storedEdges.iterator(); edgeIt.hasNext();) {
                // create an edge from this Dbedges
                Dbedges Dbedges = (Dbedges) edgeIt.next();
                Logger.DEBUG("Loading Edge (id=" + Dbedges.getEdgeid() +
                        ",from=" + Dbedges.getNodeid() + ",to="
                        + Dbedges.getNodeto(), log);
                Workflow wf = WorkflowManager.getInstance().getWorkflow(node.getWorkflowId());
                Node nodeTo = wf.getNode(Dbedges.getNodeto());
                Edge edge = new Edge(Dbedges.getEdgeid(), node, nodeTo);
                edge.setWorkflowId(node.getWorkflowId());
                edge.setModified(false);
                edges.add(edge);
            }
        } catch (TorqueException e) {
            Logger.ERROR("ERR: Can not load edge list!" + e.getMessage(), log);
            edges.clear();
        }
        return edges;
    }



    static Condition loadCondition(Edge edge) {
        List condList = loadChildConditions(edge, 0);
        /* The edge only has one condtion */
        if (condList.size() != 1) {
            Logger.ERROR("The list of conditions of an edge has != 1 entry", log);
        }
        Condition cond = (Condition) condList.get(0);
        if (edge.getWorkflowId() != 0){
            cond.setWorkflowId(edge.getWorkflowId());
        } else {
            Logger.ERROR("Trying to set null-Workflow for Condition in Edge "
                    + edge.getId(), log);
        }
        return cond;
    }




    /**Loads the MileStone of a Node
     * @param nodeID
     * @return
     */
    static MileStone loadNodeMileStone(Node node){
        MileStone stone = null;
        Criteria crit = new Criteria();
        crit.add(DbmilestonesPeer.NODEID, node.getId());
        List storedStones = null;
        try {
            storedStones = DbmilestonesPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.ERROR("ERR: Can not select from milestones with nodeID="
                    + node.getId(), log);
            e.printStackTrace();
        }
        if (storedStones != null && storedStones.size() > 0){
            Dbmilestones dbstone = (Dbmilestones) storedStones.get(0);
            stone = new MileStone(node, dbstone.getDisplayed() == 1 ? true : false,
                    node.getTemplate().getMileStoneTemplate());
            stone.setId(dbstone.getMilestoneid());
        }
        return stone;
    }




    /**
     * @param edgeID
     * @param parentID
     * @return
     */
    private static List loadChildConditions(Edge edge, int parentCondID) {

        List condList = new ArrayList();
        Criteria crit = new Criteria();
        crit.add(DbconditionsPeer.EDGEID, edge.getId());
        crit.add(DbconditionsPeer.PARENT, parentCondID);

        /*
         * First, query the conditions for the top level conditions. It has a
         * parent id = 0.
         */

        try {
            List storedCondList = DbconditionsPeer.doSelect(crit);
            for (Iterator it = storedCondList.iterator(); it.hasNext();) {
                Dbconditions storedCond = (Dbconditions) it.next();
                String type = storedCond.getType();
                Logger.DEBUG("Loading Condition type " + type, log);
                int condID = storedCond.getCondid();
                /* load the child conditions here */
                List childConditions = loadChildConditions(edge, storedCond
                        .getCondid());
                Condition cond = null;
                if (type.equalsIgnoreCase("AND")) {
                        cond = new ANDCondition(childConditions);
                } else if (type.equalsIgnoreCase("OR")) {
                    /* OR needs two child conditions */
                    if (childConditions.size() == 2) {
                        cond = new ORCondition((Condition) childConditions
                                .get(0), (Condition) childConditions.get(1));
                    } else {
                        Logger.ERROR("OR Condition load error: Need two child conditions", log);
                    }
                } else if (type.equalsIgnoreCase("NOT")) {
                    /* OR needs two child conditions */
                    if (childConditions.size() == 1) {
                        cond = new NOTCondition((Condition) childConditions
                                .get(0));
                    } else {
                        Logger.ERROR("NOT Condition load error: Need two child conditions", log);
                    }
                } else if (type.equalsIgnoreCase("EVENT")) {
                    cond = loadEventCondition(condID);
                } else if (type.equalsIgnoreCase("DATA")) {
                    cond = loadDataCondition(condID);
               } else if (type.equalsIgnoreCase("SUB")) {
                    cond = loadSubsFinishedCondition(condID);
                } else {
                    Logger.ERROR("Condition " + storedCond.getCondid() +
                            type + " could not be loaded from Database", log);
                }

                if (cond != null && condID > 0) {
                    cond.setId(condID);
                    cond.setWorkflowId(edge.getWorkflowId());
                    cond.setModified(false);
                    condList.add(cond);
                }
            }
        } catch (TorqueException e) {
            Logger.ERROR("ERR: Can not load conditions!", log);
        }
        return condList;
    }


    /**
     * @param condID - ConditionID of the EventCondition
     * @return - the loaded Eventcondition
     */
    private static EventCondition loadEventCondition(int condID) {

        Event e = null;
        EventCondition cond = null;
        Criteria crit = new Criteria();
        crit.add(DbeventconditionsPeer.CONDID, condID);
        try {
            List storedEventConds = DbeventconditionsPeer.doSelect(crit);
            if (storedEventConds.size() == 1) {
                Dbeventconditions evcond = (Dbeventconditions) storedEventConds.get(0);
                e = new Event(evcond.getEventtype(), 0, 0);
                cond = new EventCondition(e, evcond.getState() == 1 ? true : false);
            } else {
                Logger.ERROR("ERR: No Event Detail for Event Condition!", log);
            }
        } catch (TorqueException exc) {
            Logger.ERROR("ERR: Can not load conditions: " + e, log);
        }
        return cond;
    }


    /**
     * Loads a Datacondition for a given conditionID
     * @param condID
     * @return the loaded Datacondition
     */
    private static DataCondition loadDataCondition(int condID) {
        Event e = null;
        DataCondition cond = null;
        Criteria crit = new Criteria();
        crit.add(DbdataconditionsPeer.CONDID, condID);
        try {
            List storedDataConds = DbdataconditionsPeer.doSelect(crit);
            if (storedDataConds.size() == 1) {
                Dbdataconditions datacond = (Dbdataconditions) storedDataConds.get(0);
                cond = new DataCondition(datacond.getField(),
                        datacond.getCondcheck(), datacond.getCondvalue(),
                        datacond.getState() == 1 ? true : false);
            } else {
                Logger.ERROR("ERR: No data Detail for data Condition!", log);
            }
        } catch (TorqueException exc) {
            Logger.ERROR("ERR: Can not load data-condition: " + e, log);
        }
        return cond;
    }


    /**
     * Loads a SubsFinishedCondition for a given conditionID
     * @param condID
     * @return the loaded SubsFinishedCondition
     */
    private static SubsFinishedCondition loadSubsFinishedCondition(int condID) {
    	Event e = null;
    	SubsFinishedCondition cond = null;
    	Criteria crit = new Criteria();
    	crit.add(DbsubsfinishedconditionsPeer.CONDID, condID);
    	try {
    		List storedConds = DbsubsfinishedconditionsPeer.doSelect(crit);
    		if (storedConds.size() == 1) {
    			Dbsubsfinishedconditions dbcond = (Dbsubsfinishedconditions)
				storedConds.get(0);
    			cond = new SubsFinishedCondition(dbcond.getSubname(),
    					dbcond.getSubversion(), dbcond.getState() == 1 ? true : false);
    		} else {
    			Logger.ERROR("Cannot load SubsFinishedCondition " + condID, log);
    		}
    	} catch (TorqueException exc) {
    		Logger.ERROR("Cannot load SubsFinishedCondition " + condID + e, log);
    	}
    	return cond;
    }


}
