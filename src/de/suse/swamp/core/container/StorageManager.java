/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Klaas Freitag<freitag@suse.de>
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

/**
 * This is the storage manager of the SWAMP project. It offers save
 * and restore methods for various objects.
 *
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

import java.io.*;
import java.util.*;

import org.apache.torque.*;
import org.apache.torque.om.*;
import org.apache.torque.util.*;

import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.history.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

final class StorageManager {

    // extra logger for storage stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.container.Storage");

   static Databit loadDatabit(int dSetid, String name) {
        Criteria bitCrit = new Criteria();
        bitCrit.add(DbdatabitsPeer.DATASETID, dSetid);
        bitCrit.add(DbdatabitsPeer.NAME, name);
        bitCrit.addAscendingOrderByColumn(DbdatabitsPeer.DATABITID);
        Databit dbit = loadDatabit(bitCrit);
        if (dbit != null) {
            dbit.setDSetId(dSetid);
            dbit.setModified(false);
        }
        return dbit;
    }



   private static Databit loadDatabit(Criteria bitCrit) {
        Databit dbit = null;
        List storedBits = null;
        try {
            storedBits = DbdatabitsPeer.doSelect(bitCrit);
        } catch (TorqueException e1) {
            Logger.ERROR("Could not select databits: " + e1, log);
        }
        if (storedBits.size() == 1) {
            Dbdatabits dbbit = (Dbdatabits) storedBits.get(0);
            dbit = DataManager.createDatabit(dbbit.getName(), dbbit.getDescription(), dbbit
                    .getDatatype(), dbbit.getValue(), dbbit.getState());
            dbit.setId(dbbit.getDatabitid());
            loadDbEdit(dbit);
            loadEnumVals(dbit);
            loadComment(dbit);
            // dbit restore finished, adding.
            dbit.setModified(false);
        }
        return dbit;
    }


   private static void loadComment(Databit dbit) {
		if (dbit.getType().equals("comment") || dbit.getType().equals("thread")) {
			commentDatabit commentDbit = (commentDatabit) dbit;
			Criteria crit = new Criteria();
			crit.add(DbcommentsPeer.DATABITID, commentDbit.getId());
			List storedcomments = new ArrayList();
			try {
				storedcomments = DbcommentsPeer.doSelect(crit);
			} catch (TorqueException e) {
				Logger.ERROR("Could not load Dbcomments: " + e.getMessage(),
						log);
			}
			if (storedcomments.size() == 1) {
				Dbcomments dbcomment = (Dbcomments) storedcomments.get(0);
				commentDbit.setCommentAuthor(dbcomment.getUsername());
				commentDbit.setCommentDate(dbcomment.getDate());
			}
		}
	}


    private static void loadEnumVals(Databit dbit) {
		// If enumeration type, fetch enum values:
		if (dbit.getType().equalsIgnoreCase("enum")
				|| dbit.getType().equalsIgnoreCase("multienum")) {
			enumDatabit enumDatabit = (enumDatabit) dbit;
			Criteria crit = new Criteria();
			crit.add(DbdatabitenumsPeer.DATABITID, enumDatabit.getId());
			List storedenums = new ArrayList();
			try {
				storedenums = DbdatabitenumsPeer.doSelect(crit);
			} catch (TorqueException e2) {
				Logger.ERROR("Could not load DbitEnums: " + e2.getMessage(), log);
			}
			// get the values
			for (Iterator enumit = storedenums.iterator(); enumit.hasNext();) {
				Dbdatabitenums dbenum = (Dbdatabitenums) enumit.next();
				enumDatabit.addEnumvalue(dbenum.getValue());
			}
		}
	}

    private static void loadDbEdit(Databit dbit) {
        // Load edit info if required
        if (dbit.getId() > 0) {
            Criteria crit = new Criteria();
            crit.add(DbeditinfosPeer.DATABITID, dbit.getId());
            List storedEditinfos = new ArrayList();
            try {
                storedEditinfos = DbeditinfosPeer.doSelect(crit);
            } catch (TorqueException e) {
                Logger.ERROR("Cannot load editinfo for databit #" + dbit.getId(), log);
            }
            if (storedEditinfos.size() > 0) {
                Dbeditinfos dbeinfo = (Dbeditinfos) storedEditinfos.get(0);
                DataEditInfo info = new DataEditInfo(dbeinfo.getType(),
                        dbeinfo.getXsize(), dbeinfo.getYsize());
                dbit.setEditInfo(info);
                info.setModified(false);
            }
        } else {
            Logger.ERROR("Cannot restore DBEdits for Databit not in DB!", log);
        }
    }


    /** Load all Databits that are included in the given Set
     * @param dSetid
     * @return
     */
    static List loadAllDatabits(int dSetid) {
        List bits = new ArrayList();
        if (dSetid > 0) {
            Criteria bitCrit = new Criteria();
            bitCrit.add(DbdatabitsPeer.DATASETID, dSetid);
            bitCrit.addAscendingOrderByColumn(DbdatabitsPeer.DATABITID);
            List storedBits = new ArrayList();
            try {
                storedBits = DbdatabitsPeer.doSelect(bitCrit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select databits for set #" + dSetid + " " + e1, log);
            }
            for (Iterator it = storedBits.iterator(); it.hasNext();) {
                Dbdatabits dbbit = (Dbdatabits) it.next();
                Databit dbit = DataManager.createDatabit(dbbit.getName(), dbbit
                        .getDescription(), dbbit.getDatatype(), dbbit.getValue(), dbbit.getState());
                dbit.setId(dbbit.getDatabitid());
                dbit.setDSetId(dSetid);
                loadEnumVals(dbit);
                loadComment(dbit);
                loadDbEdit(dbit);
                // dbit restore finished, adding.
                dbit.setModified(false);
                bits.add(dbit);
            }
        } else {
            Logger.ERROR("Cannot load Databits for set #" + dSetid, log);
        }
        return bits;
    }



    static void addDatasetToWorkflow(int dSetId, int WfId) {
        if (dSetId > 0 && WfId > 0){
        Criteria crit = new Criteria();
        crit.add(DbdatasetWorkflowPeer.WORKFLOWID, WfId);
        crit.add(DbdatasetWorkflowPeer.DATASETID, dSetId);
        try {
            DbdatasetWorkflowPeer.doInsert(crit);
            Logger.LOG("Attached DSet #" + dSetId + " to Workflow #" + WfId, log);
        } catch (TorqueException e1) {
            Logger.ERROR("Could not add dataset " + dSetId +
                    " to Workflow " + WfId + ". Reason: " + e1, log);
        }
        } else {
            Logger.ERROR("Could not add Dataset #" +  dSetId + " to " +
                    " Workflow #" + WfId, log);
        }
    }


    /**
     * Removes this (toplevel-)dataset from the workflow. Does not delete it.
     */
    static void detachDatasetFromWorkflow(int dSetId, int wfId) {
		if (dSetId > 0 && wfId > 0) {
			Criteria crit = new Criteria();
			crit.add(DbdatasetWorkflowPeer.WORKFLOWID, wfId);
			crit.add(DbdatasetWorkflowPeer.DATASETID, dSetId);
			try {
				DbdatasetWorkflowPeer.doDelete(crit);
				Logger.LOG("Detached DSet #" + dSetId + " from Workflow #"
						+ wfId, log);
			} catch (TorqueException e1) {
				Logger.ERROR("Could not detach dataset " + dSetId
						+ " from Workflow " + wfId + ". Reason: " + e1, log);
			}
		} else {
			Logger.ERROR("Could not detach dataset " + dSetId
					+ " from Workflow " + wfId, log);
		}
	}


    static void addDatasetToDataset(int dSetId, int childSetId) {
        if (dSetId > 0 && childSetId > 0) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetDatasetPeer.ROOTDATASETID, dSetId);
            crit.add(DbdatasetDatasetPeer.DATASETID, childSetId);
            try {
                DbdatasetDatasetPeer.doInsert(crit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not add child dataset " + childSetId
                        + " to set " + dSetId + ". Reason: " + e1, log);
            }
        } else {
            Logger.ERROR("Could add Cild-Dataset #" + childSetId + " to "
                    + dSetId, log);
        }
    }


     /**
      * Loading all Root Datasets for a Workflow from DB
      *
      * @param WfId
      * @return
      */
    static List loadDatasetsforWorkflow(int WfId) {
        List sets = new ArrayList();
        List setNames = new ArrayList();
        if (WfId > 0) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetWorkflowPeer.WORKFLOWID, WfId);
            crit.addJoin(DbdatasetWorkflowPeer.DATASETID,
                    DbdatasetsPeer.DATASETID);
            List storedSets = new ArrayList();
            try {
                storedSets = DbdatasetsPeer.doSelect(crit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select datasets for wf #" + WfId + e1, log);
            }
            if (storedSets.size() == 0) {
                Logger.ERROR("No Datasets found for Wf-ID " + WfId, log);
            }
            for (Iterator it = storedSets.iterator(); it.hasNext();) {
                Dbdatasets dbset = (Dbdatasets) it.next();
                Dataset dSet = new Dataset(dbset.getName(), dbset
                        .getDescription(), dbset.getState());
                dSet.setId(dbset.getDatasetid());
                if (setNames.contains(dSet.getName())){
                	Logger.ERROR("Workflow #" + WfId + " contains multiple " +
                			"children with name: " + dSet.getName());
                } else {
                	setNames.add(dSet.getName());
                	sets.add(dSet);
                }
            }
        } else {
            Logger.ERROR("Cannot load a Dataset for Workflowid 0!", log);
        }
        return sets;
    }



    static Dataset loadDatasetforWorkflow(int WfId, String setName) {
        Dataset dSet = null;
        if (WfId > 0 && !setName.equals("")) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetWorkflowPeer.WORKFLOWID, WfId);
            crit.addJoin(DbdatasetWorkflowPeer.DATASETID,
                    DbdatasetsPeer.DATASETID);
            crit.add(DbdatasetsPeer.NAME, setName);
            List storedSets = new ArrayList();
            try {
                storedSets = DbdatasetsPeer.doSelect(crit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select dataset for wf #" + WfId + e1, log);
            }
            if (storedSets.size() == 1) {
                Dbdatasets dbset = (Dbdatasets)storedSets.get(0);
                dSet = new Dataset(dbset.getName(), dbset
                        .getDescription(), dbset.getState());
                dSet.setId(dbset.getDatasetid());
            } else if (storedSets.size() > 1) {
            	Logger.ERROR("Workflow #" + WfId + " has " + storedSets.size() +
            			" attached datasets of name: " + setName, log);
            }
        } else {
            Logger.ERROR("Cannot load a Dataset for Workflowid 0 or without a name!", log);
        }
        return dSet;
    }




    static List loadChildDatasetsforDataset(int datasetId) {
        List sets = new ArrayList();
        List setNames = new ArrayList();
        if (datasetId > 0) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetDatasetPeer.ROOTDATASETID, datasetId);
            crit.addJoin(DbdatasetDatasetPeer.DATASETID, DbdatasetsPeer.DATASETID);
            List storedSets = new ArrayList();
            try {
                storedSets = DbdatasetsPeer.doSelect(crit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select child-datasets for ds #"
                        + datasetId + e1, log);
            }
            for (Iterator it = storedSets.iterator(); it.hasNext();) {
                Dbdatasets dbset = (Dbdatasets) it.next();
                Dataset dSet = new Dataset(dbset.getName(), dbset
                        .getDescription(), dbset.getState());
                dSet.setId(dbset.getDatasetid());
                if (setNames.contains(dSet.getName())){
                	Logger.ERROR("Dataset #" + datasetId + " contains multiple " +
                			"children with name: " + dSet.getName());
                } else {
                	setNames.add(dSet.getName());
                	sets.add(dSet);
                }
            }
        } else {
            Logger.ERROR("Cannot load a Dataset for parent-set #0!", log);
        }
        return sets;
    }


    /**
     * load all workflows that attach this dataset at any level.
     */
   static List loadWorkflowIdsforDatasetTree(int datasetId, List wfIds) {
       if (datasetId > 0) {
           wfIds.addAll(loadWorkflowIdsforDataset(datasetId));
           for (Iterator it = loadParentDatasets(datasetId).iterator(); it.hasNext(); ){
               int setId = ((Integer) it.next()).intValue();
               loadWorkflowIdsforDatasetTree(setId, wfIds);
           }
       } else {
           Logger.ERROR("Cannot load parent Datasets for set #0!", log);
       }
       return wfIds;
   }


     /**
      * load all workflows that attach this dataset at root level.
      */
    static private List loadWorkflowIdsforDataset(int datasetId) {
        List wfIds = new ArrayList();
        Criteria crit = new Criteria();
        crit.add(DbdatasetWorkflowPeer.DATASETID, datasetId);
        List storedWfs = new ArrayList();
        try {
            storedWfs = DbdatasetWorkflowPeer.doSelect(crit);
            for (int i = 0; i<storedWfs.size(); i++ ){
                wfIds.add(new Integer(((DbdatasetWorkflow) storedWfs.get(i)).getWorkflowid()));
            }
        } catch (Exception e1) {
            Logger.ERROR("Could not select workflows for ds #" + datasetId + e1, log);
        }
        return wfIds;
    }


    static private List loadParentDatasets(int dsetId) {
        ArrayList parentIds = new ArrayList();
            Criteria crit = new Criteria();
            crit.add(DbdatasetDatasetPeer.DATASETID, dsetId);
            List storedSets = new ArrayList();
            try {
                storedSets = DbdatasetDatasetPeer.doSelect(crit);
                for (int i = 0; i<storedSets.size(); i++ ){
                    parentIds.add(new Integer(((DbdatasetDataset) storedSets.get(i)).getRootdatasetid()));
                }
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select parent-datasets for ds #" + dsetId + e1, log);
            }
        return parentIds;
    }



    static Dataset loadChildDataset(int parentId, String name) {
        Dataset dSet = null;
        if (parentId > 0) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetDatasetPeer.ROOTDATASETID, parentId);
            crit.addJoin(DbdatasetDatasetPeer.DATASETID,
                    DbdatasetsPeer.DATASETID);
            crit.add(DbdatasetsPeer.NAME, name);
            List storedSets = new ArrayList();
            try {
                storedSets = DbdatasetsPeer.doSelect(crit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select child-datasets for ds #"
                        + parentId + e1, log);
            }
            if (storedSets.size() > 0) {
                Dbdatasets dbset = (Dbdatasets) storedSets.get(0);
                dSet = new Dataset(dbset.getName(), dbset.getDescription(),
                        dbset.getState());
                dSet.setId(dbset.getDatasetid());
            }
        } else {
            Logger.ERROR("Cannot load a Dataset for parent-set #0!", log);
        }
        return dSet;
    }


   static Dataset loadDataset(int dSetID) {
        Dataset dSet = null;
        if (dSetID > 0) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetsPeer.DATASETID, dSetID);
            List storedSets = new ArrayList();
            try {
                storedSets = DbdatasetsPeer.doSelect(crit);
            } catch (TorqueException e1) {
                Logger.ERROR("Could not select dataset for #" + dSetID + e1, log);
            }
            if (storedSets.size() == 1) {
                Dbdatasets dbset = (Dbdatasets) storedSets.get(0);
                dSet = new Dataset(dbset.getName(), dbset.getDescription(),
                        dbset.getState());
                dSet.setId(dbset.getDatasetid());
            } else {
                Logger.ERROR("dataset for #" + dSetID + " not found in DB", log);
            }
        } else {
            Logger.ERROR("Cannot load a Dataset with id #0!", log);
        }
        return dSet;
    }


    /**
     * storage of a dataset.
     * @param dPackID - the database id of the of the datapack.
     * @param dSet - the dataset object to store
     */
    static void storeDataSet(Dataset dSet) {

       if (dSet.isModified()) {
            Criteria crit = new Criteria();
            crit.add(DbdatasetsPeer.NAME, dSet.getName());
            crit.add(DbdatasetsPeer.DESCRIPTION, dSet.getDescription());
            crit.add(DbdatasetsPeer.STATE, dSet.getState());
            if (!(dSet.getId() > 0)) {
                // The id is still 0 so we have to insert a new dataset.
                try {
                    NumberKey key = (NumberKey) DbdatasetsPeer.doInsert(crit);
                    dSet.setId(key.intValue());
                    Logger.DEBUG("Inserted Dataset " + dSet.getName(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not insert dataset " + dSet.getName(), log);
                }
            } else {
                crit.add(DbdatasetsPeer.DATASETID, dSet.getId());
                try {
                    DbdatasetsPeer.doUpdate(crit);
                    Logger.DEBUG("Updated Dataset " + dSet.getName(), log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not update dataset " + dSet.getName(), log);
                }
            }
            dSet.setModified(false);
        }
    }



    /**
     * Store (update/insert) databit to db
     */
    static void storeDataBit(Databit dbit) {
    	// branching to Workflow if its a Workflow Property
    	if (dbit instanceof SystemDatabit) {
    		DataToWfPropertyMapper.setValueForWorkflow((SystemDatabit) dbit);
    		try {
    			WorkflowStorage.storeWorkflow(((SystemDatabit) dbit).getWf());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	} else if (dbit.getDSetId() > 0) {
    		// shortcut for not modified databits:
    		if (dbit.isModified()) {
    			Criteria crit = new Criteria();
    			crit.add(DbdatabitsPeer.DATASETID, dbit.getDSetId());
    			crit.add(DbdatabitsPeer.DATATYPE, dbit.getType());
    			crit.add(DbdatabitsPeer.DESCRIPTION, dbit.getDescription());
    			crit.add(DbdatabitsPeer.VALUE, dbit.getValue());
    			crit.add(DbdatabitsPeer.STATE, dbit.getState());
    			crit.add(DbdatabitsPeer.NAME, dbit.getName());

    			if (dbit.getId() > 0) {
    				// update required
    				try {
    					crit.add(DbdatabitsPeer.DATABITID, dbit.getId());
    					DbdatabitsPeer.doUpdate(crit);
    					Logger.DEBUG("Updated databit " + dbit.getName() + " in db.", log);
    				} catch (TorqueException e) {
    					Logger.ERROR("Could not update databit "
    							+ dbit.getName() + " " +  e.getMessage(), log);
    				}
    				if (dbit.getType().equals("comment") || dbit.getType().equals("thread")) {
    					crit = new Criteria();
    					crit.add(DbcommentsPeer.DATABITID, dbit.getId());
    					crit.add(DbcommentsPeer.DATE, ((commentDatabit) dbit).getCommentDate());
    					crit.add(DbcommentsPeer.USERNAME, ((commentDatabit) dbit).getCommentAuthor());
    					try {
    						DbcommentsPeer.doUpdate(crit);
						} catch (TorqueException e) {
							Logger.ERROR("Could not update comment values. " + e.getMessage() , log);
						}
    				} else if (dbit.getType().equalsIgnoreCase("enum") || dbit.getType().equalsIgnoreCase("multienum")) {
    				    storeDatabitEnum((enumDatabit) dbit);
    				}
    			} else {
    				// We need to insert.
    				try {
    					NumberKey key = (NumberKey) DbdatabitsPeer.doInsert(crit);
    					dbit.setId(key.intValue());
    					Logger.DEBUG("Inserted databit " + dbit.getName(), log);
    				} catch (TorqueException e) {
    					Logger.ERROR("Could not insert databits" + e, log);
    				}

    				if (dbit.getType().equalsIgnoreCase("comment")){
    					crit = new Criteria();
    					crit.add(DbcommentsPeer.DATABITID, dbit.getId());
    					crit.add(DbcommentsPeer.DATE, new Date());
    					crit.add(DbcommentsPeer.USERNAME, SWAMPUser.SYSTEMUSERNAME);
    					try {
    						DbcommentsPeer.doInsert(crit);
						} catch (TorqueException e) {
							Logger.ERROR("Could not insert comment values. " + e.getMessage() , log);
						}
    				// if we have an enumeration Type -> extra insert
    				} else if (dbit.getType().equalsIgnoreCase("enum")
    						|| dbit.getType().equalsIgnoreCase("multienum")) {
    				    storeDatabitEnum((enumDatabit) dbit);
    				}
    			}
    			dbit.setModified(false);
    		}

    		if (!(dbit.getId() > 0)) {
    			Logger.ERROR("Could not set proper id for databit "
    					+ dbit.getName(), log);
    		} else {
    			// Store the edit-info if required
    			DataEditInfo eInfo = dbit.getEditInfo();
    			if (eInfo != null) {
    				storeDatabitEditInfo(eInfo, dbit.getId());
    			}
    		}
    	} else {
    		Logger.ERROR("Cannot store Databit without DSetId!", log);
    	}
    }

    /**
     * Store databitenum values
     */
    static private void storeDatabitEnum(enumDatabit dbit) {
        // delete all enumvalues, we don't know if one changed
        Criteria crit = new Criteria();
        crit.add(DbdatabitenumsPeer.DATABITID, dbit.getId());

        try {
            DbdatabitenumsPeer.doDelete(crit);
            crit = new Criteria();
            crit.add(DbdatabitenumsPeer.DATABITID, dbit.getId());
            for (Iterator it = dbit.getEnumvalues().iterator(); it.hasNext();) {
                String value = it.next().toString();
                crit.add(DbdatabitenumsPeer.VALUE, value);
                DbdatabitenumsPeer.doInsert(crit);
            }
        } catch (TorqueException e) {
            Logger.ERROR("Could not insert databit-enumvalues. " + e.getMessage(), log);
        }        
    }
    
    

    static private void storeDatabitEditInfo(DataEditInfo eInfo, int databitID) {
        if (eInfo == null) {
            Logger.ERROR("Can not store null data edit info", log);
            return;
        }
        if (eInfo.isModified()) {
            Criteria crit = new Criteria();
            crit.add(DbeditinfosPeer.DATABITID, databitID);
            crit.add(DbeditinfosPeer.TYPE, eInfo.getType());
            crit.add(DbeditinfosPeer.XSIZE, eInfo.getXsize());
            crit.add(DbeditinfosPeer.YSIZE, eInfo.getYsize());
            int editInfoID = eInfo.getId();
            if (editInfoID > 0) {
                // need an update
                crit.add(DbeditinfosPeer.EDITINFOID, editInfoID);
                try {
                    DbeditinfosPeer.doUpdate(crit);
                    Logger.DEBUG("Updated db-editinfo " + editInfoID, log);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not update db-editinfo " + editInfoID, log);
                }
            } else {
                // need to insert.
                try {
                    NumberKey key = (NumberKey) DbeditinfosPeer.doInsert(crit);
                    int newID = key.intValue();
                    eInfo.setId(newID);
                } catch (TorqueException e) {
                    Logger.ERROR("Could not insert db-editinfo: " + e, log);
                }
            }
            eInfo.setModified(false);
        }
    }


    /**
     * removes all atached datasets + bits that aren't attached anywhere else
     */
    static protected void removeDatasetsFromWorkflow(Workflow wf) throws TorqueException {
        for (Iterator it = wf.getDatasets().iterator(); it.hasNext(); ){
            Dataset dset = (Dataset) it.next();
            removeDatasetTree(dset);
            // remove from the connection table
            Criteria crit = new Criteria();
            crit.add(DbdatasetWorkflowPeer.WORKFLOWID, wf.getId());
            crit.add(DbdatasetWorkflowPeer.DATASETID, dset.getId());
            DbdatasetWorkflowPeer.doDelete(crit);
            Logger.DEBUG("DbdatasetWorkflow: removed " + dset.getName() + " - " + wf.getName(), log);
        }
    }


    /**
     * remove this dataset, all children that have no workflows,
     * and dataset connections
     */
    static private void removeDatasetTree(Dataset dset) throws TorqueException {
        // remove children
        List datasets = loadChildDatasetsforDataset(dset.getId());
        Logger.DEBUG("Found " + datasets.size() + " children of set: " + dset.getName(), log);
        for (Iterator it = datasets.iterator(); it.hasNext(); ){
            removeDatasetTree((Dataset) it.next());
        }
        Logger.DEBUG("Going to remove dset: " + dset.getName(), log);
        // do other workflows link to this dataset?
        ArrayList wfs = new ArrayList();
        List workflows = getWorkflowsForDataset(dset, wfs);
        if (workflows.size() == 1){
            Logger.DEBUG("No other wfs attach dset: " + dset.getName() + " - removing.", log);
            // remove all databits
            removeDatabits(dset.getId());
            // remove dataset - datase connection
            Criteria crit = new Criteria();
            crit.add(DbdatasetDatasetPeer.DATASETID, dset.getId());
            DbdatasetDatasetPeer.doDelete(crit);
            // remove dataset:
            crit = new Criteria();
            crit.add(DbdatasetsPeer.DATASETID, dset.getId());
            DbdatasetsPeer.doDelete(crit);
            // remove from cache:
            DataManager.removeFromCache(dset.getId());
        } else {
            Logger.DEBUG("Dset: " + dset.getName() + " is attached by " +
                    workflows.size() + " workflows - keeping.", log);
        }
    }

    private static void removeDatabits(int dsetId) throws TorqueException {
        for (Iterator it = loadAllDatabits(dsetId).iterator(); it.hasNext(); ){
            Criteria crit = new Criteria();
            Databit dbit = (Databit) it.next();
            int dbitid = dbit.getId();
            // remove attached files
            if (dbit.getType().equals("fileref")){
                try {
                    dbit.setValue("", false, SWAMPUser.SYSTEMUSERNAME, new ResultList());
                } catch (Exception e) {
                    Logger.ERROR("Deleting file for " + dbit.getName() + " failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            // remove dbeditinfos
            crit.add(DbeditinfosPeer.DATABITID, dbitid);
            DbeditinfosPeer.doDelete(crit);
            // remove dbComments
            crit.add(DbcommentsPeer.DATABITID, dbitid);
            DbcommentsPeer.doDelete(crit);
            // remove databit:
            crit.add(DbdatabitenumsPeer.DATABITID, dbitid);
            DbdatabitenumsPeer.doDelete(crit);
            // remove "DATA_CHANGED" entries from history:
            crit = new Criteria();
            crit.add(DbhistoryPeer.TYPE, Event.DATACHANGED);
            crit.add(DbhistoryPeer.ITEMID, dbitid);
            DbhistoryPeer.doDelete(crit);
        }
        Criteria crit = new Criteria();
        crit.add(DbdatabitsPeer.DATASETID, dsetId);
        DbdatabitsPeer.doDelete(crit);
    }


    /**
     * get list of wfs that atttach either this dataset or any parent dataset of it
     */
    static private List getWorkflowsForDataset(Dataset dset, List wfs) throws TorqueException {
        WorkflowManager wfman = WorkflowManager.getInstance();
        // get workflows that directly attach the dataset:
        Criteria crit = new Criteria();
        crit.add(DbdatasetWorkflowPeer.DATASETID, dset.getId());
        List dbDatasetWorkflows = DbdatasetWorkflowPeer.doSelect(crit);
        for (Iterator it = dbDatasetWorkflows.iterator(); it.hasNext(); ){
            wfs.add(wfman.getWorkflow(((DbdatasetWorkflow) it.next()).getWorkflowid()));
        }
        // do we have a parent dataset?
        crit = new Criteria();
        crit.add(DbdatasetDatasetPeer.DATASETID, dset.getId());
        List dbdatasetDatasets = DbdatasetDatasetPeer.doSelect(crit);
        for (Iterator it = dbdatasetDatasets.iterator(); it.hasNext(); ){
            Dataset rootset = DataManager.loadDataset(((DbdatasetDataset) it.next()).getRootdatasetid());
            wfs = getWorkflowsForDataset(rootset, wfs);
        }
        return wfs;
    }



    /**
     * in here are the pending events that are actually in the system.
     * delete them when the workflow is finished, or they are handled.
     * @param ev
     * @return
     * @throws Exception
     */
    static int storeEvent(Event ev) throws StorageException {
        int eventHistID = 0;
        Criteria crit = new Criteria();
        crit.add(DbeventhistoryPeer.EVENTTYPE, ev.getType());
        crit.add(DbeventhistoryPeer.SOURCEWFID, ev.getSenderWfId());
        crit.add(DbeventhistoryPeer.TARGETWFID, ev.getTargetWfId());
        crit.add(DbeventhistoryPeer.DATETIME, new Date());
        try {
            NumberKey key = (NumberKey) DbeventhistoryPeer.doInsert(crit);
            eventHistID = key.intValue();
            ev.setId(eventHistID);
        } catch (TorqueException e) {
            Logger.BUG("Could not write to Event History Table: " + e, log);
            Logger.BUG("Criteria was: " + crit);
            throw new StorageException("Unable to store Event", e);
        }
        return eventHistID;
    }




    /**
     * Loads the history of a workflow. Again, do not use this method directly
     * but use the @link History to access a workflows history.
     *
     * @param wfID - the ID of the workflow
     */
    static ArrayList loadWorkflowHistory(int wfID) {

        ArrayList resList = new ArrayList();
        Criteria crit = new Criteria();
        crit.add(DbhistoryPeer.WORKFLOWID, wfID);
        crit.addAscendingOrderByColumn(DbhistoryPeer.HISTORYID);
        List dbHist = null;
        try {
            dbHist = DbhistoryPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.ERROR("Can not select from history table: " + e, log);
        }
        if (dbHist != null) {
            Iterator it = dbHist.iterator();
            while (it.hasNext()) {
                Dbhistory hist = (Dbhistory) it.next();
                resList.add(new HistoryEntry(hist.getHistoryid(),
                        hist.getItemid(), hist.getType(),
                        hist.getUsername(), hist.getDatetime(), hist.getData()));
            }
        }
        return resList;
    }




    /**
     * Loads history-entrys based on the given parameters, the latest at last
     */
    static ArrayList loadHistoryEntries(int wfID, int affectedId, String typeStart) {
        Criteria crit = new Criteria();
        crit.add(DbhistoryPeer.WORKFLOWID, wfID);
        crit.add(DbhistoryPeer.ITEMID, affectedId);
        crit.add(DbhistoryPeer.TYPE, (Object) (typeStart + "%"), Criteria.LIKE);
        crit.addAscendingOrderByColumn(DbhistoryPeer.HISTORYID);
        return loadHistoryEntries(crit);
    }


    static ArrayList loadHistoryEntries(int wfID, String typeStart) {
        Criteria crit = new Criteria();
        crit.add(DbhistoryPeer.WORKFLOWID, wfID);
        crit.add(DbhistoryPeer.TYPE, (Object) (typeStart + "%"), Criteria.LIKE);
        crit.addAscendingOrderByColumn(DbhistoryPeer.HISTORYID);
        return loadHistoryEntries(crit);
    }


    /**
     * Loads history-entrys based on the given parameters.
     */
    static ArrayList loadHistoryEntries(ArrayList affectedIds, String typeStart) {
        Criteria crit = new Criteria();
        crit.add(DbhistoryPeer.ITEMID, affectedIds, Criteria.IN);
        crit.add(DbhistoryPeer.TYPE, (Object) (typeStart + "%"), Criteria.LIKE);
        crit.addAscendingOrderByColumn(DbhistoryPeer.HISTORYID);
        return loadHistoryEntries(crit);
    }


    private static ArrayList loadHistoryEntries(Criteria crit) {
        ArrayList resList = new ArrayList();
        List dbHist = null;
        try {
            dbHist = DbhistoryPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.ERROR("Can not select from history table: " + e, log);
        }
        if (dbHist != null) {
            for (Iterator it = dbHist.iterator(); it.hasNext(); ) {
                Dbhistory hist = (Dbhistory) it.next();
                resList.add(new HistoryEntry(hist.getHistoryid(),
                        hist.getItemid(), hist.getType(),
                        hist.getUsername(), hist.getDatetime(), hist.getData()));
            }
        }
        return resList;
    }





    /**
     * create a history entry. Do not use this mehtod directly.
     * Please @see HistoryEntry class for details.
     *
     * @param type - the type of history entry
     * @param affectedID - the corresponding foreign key.
     * @param userID - the user id who did the step
     */
    static void createHistoryEntry(String type, int workflowID,
            int affectedID, String userName, String data) {
        Criteria crit = new Criteria();
        crit.add(DbhistoryPeer.TYPE, type);
        crit.add(DbhistoryPeer.ITEMID, affectedID);
        if (workflowID > 0) {
			crit.add(DbhistoryPeer.WORKFLOWID, workflowID);
		}
        crit.add(DbhistoryPeer.USERNAME, userName);
        if (data != null && data.length() > 255) {
			data = data.substring(0, 254);
		}
        crit.add(DbhistoryPeer.DATA, data);
        try {
            DbhistoryPeer.doInsert(crit);
        } catch (TorqueException e) {
            Logger.BUG("Can not insert to History-Table: " + e, log);
        }
    }



    static Event loadEventFromHistory(int eventid) {
        Criteria crit = new Criteria();
        crit.add(DbeventhistoryPeer.EVENTHISTID, eventid);
        List events = null;
        try {
            events = DbeventhistoryPeer.doSelect(crit);
        } catch (Exception e) {
            Logger.BUG("Could not select from EventHistory Table", log);
        }
        if (events.size() != 1) {
            Logger.ERROR("!=1 Event in EventhistoryTable with EventID=" + eventid, log);
            return null;
        }
        Dbeventhistory dbevent = (Dbeventhistory) events.get(0);
        return new Event(dbevent.getEventtype(), dbevent.getSourcewfid(),
                dbevent.getTargetwfid());
    }



    static void storeContextHelp( ContextHelp h )
        throws StorageException {

        Criteria crit = new Criteria();
        crit.add( DbcontexthelpPeer.CONTEXT, h.getContext() );

        Dbcontexthelp help = new Dbcontexthelp();

        try {
          List helps = DbcontexthelpPeer.doSelect( crit );
          if ( helps.size() == 1) {
              help = (Dbcontexthelp) helps.get( 0 );
          } else if ( helps.size() > 1 ) {
              throw new StorageException( "Context ambigous" );
          }
        } catch ( TorqueException e ) {
          throw new StorageException( "Unable to find context help", e );
        }

        try {
          help.setContext( h.getContext() );
          help.setTitle( h.getTitle() );
          help.setText( h.getText() );
          help.save();
        } catch ( Exception e ) {
          throw new StorageException( "Unable to save context help", e );
        }
    }

    static ContextHelp loadContextHelp (String context) throws StorageException {

       if (context != null && context.length() > 0){

        Criteria crit = new Criteria();
        crit.add( DbcontexthelpPeer.CONTEXT, context );
        List helps;

        try {
          helps = DbcontexthelpPeer.doSelect( crit );
        } catch ( TorqueException e ) {
          throw new StorageException( "Unable to load context help", e );
        }
          if ( helps.size() == 1) {
              Dbcontexthelp help = (Dbcontexthelp) helps.get( 0 );
             ContextHelp h = new ContextHelp( context, help.getText(),
                     help.getTitle() );
             return h;
          } else if ( helps.size() > 1 ) {
              throw new StorageException( "Context ambigous" );
          } else {
              // not found in DB -> try filesystem:
             return loadContextHelpFromFile(context);
          }
       } else {
           Logger.ERROR("Please provide a valid context for loading help.", log);
           return null;
       }
    }


    private static ContextHelp loadContextHelpFromFile (String context){
        String helpPath = DocumentationManager.getInstance().getDocuLocation();

        // fix for windows separators in regexps
        String fs = System.getProperty("file.separator");
        if (fs.equals("\\")) {
			fs = "\\\\";
		}

        String contextPath = context.replaceAll("\\.", fs);
        File helpfile = new File(helpPath + System.getProperty("file.separator") + contextPath);
        //Logger.DEBUG("Trying to add helpfile: " + helpfile.getAbsolutePath());
        ContextHelp contextHelp = null;

        if (helpfile.exists() && helpfile.isFile()) {
                try {
                    String lineSep = System.getProperty("line.separator");
                    BufferedReader br = new BufferedReader(new FileReader(
                            helpfile));
                    String nextLine = "";
                    StringBuffer sb = new StringBuffer();

                    String title = br.readLine();
                    while ((nextLine = br.readLine()) != null) {
                        sb.append(nextLine);
                        sb.append(lineSep);
                    }
                    contextHelp = new ContextHelp(context, sb.toString(), title);
                } catch (Exception e) {
                    Logger.ERROR("Exception while reading help file " + helpfile, log);
                    e.printStackTrace();
                }
            } else {
                //Logger.ERROR("Cannot find Helpfile: " + helpfile.getAbsolutePath());
            }
        return contextHelp;
    }





    public static ArrayList loadAllContextHelp() throws StorageException {

        Criteria crit = new Criteria();
        try {
            ArrayList result = new ArrayList();

            List helps = DbcontexthelpPeer.doSelect(crit);
            for (int i = 0; i < helps.size(); ++i) {
                Dbcontexthelp help = (Dbcontexthelp) helps.get(i);
                result.add(new ContextHelp(help.getContext(), help.getText(),
                        help.getTitle()));
            }

            return result;
        } catch (TorqueException e) {
            throw new StorageException("Unable to load context help", e);
        }
    }

}
