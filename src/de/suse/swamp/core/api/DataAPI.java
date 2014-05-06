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

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;
import de.suse.swamp.core.workflow.*;

/**

 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 */

public class DataAPI {

	/**
     * private method for getting a databit, no security checks for the databit itself
     * // FIXME: check if he is allowed to read
	 */
	public Databit doGetDataBit(int wfid, String path, String username) 
		throws UnknownElementException, SecurityException {
		WorkflowAPI wfapi = new WorkflowAPI();
		Workflow wf = wfapi.getWorkflow(wfid, username);
		if (wf == null) {
			throw new UnknownElementException("Workflow #" + wfid + " not found.");
		}
		Databit dbit = wf.getDatabit(path);
		if (dbit == null) {
			throw new UnknownElementException("Databitpath: " + path + " not found " + 
					"for workflow #" + wfid);
		} else
			return dbit;
	}


	public boolean doUpdateDataBitValue(int wfid, String path, String newVal, boolean force, 
			String username) throws Exception, SecurityException {
        return doUpdateDataBitValue(wfid, path, newVal, force, username, new ResultList());
	}
    
    // FIXME: check if he is allowed to write
    public boolean doUpdateDataBitValue(int wfid, String path, String newVal, boolean force, 
            String username, ResultList results) throws Exception, SecurityException {
        Databit dbit = doGetDataBit(wfid, path,username);
        return dbit.setValue(newVal, force, username, results);
    }
    
    public void doAddDatabit(int wfid, String dsetname, String dbitname, String newVal, 
            String username, String datatype, String datashortdesc, String datadesc) throws Exception, SecurityException {
        
        WorkflowAPI wfapi = new WorkflowAPI();
        Workflow wf = wfapi.getWorkflowForWriting(wfid, username);
        if (wf == null) {
            throw new UnknownElementException("Workflow #" + wfid + " not found.");
        }
        if (wf.containsDatabit(dsetname + "." + dbitname)) {
            throw new Exception("Databit: " + dsetname + "." + dbitname + " already exists in Workflow #" + wfid);
        }
        Dataset dset = wf.getDataset(dsetname);
        Databit dbit = DataManager.createDatabit(dbitname, datadesc, datatype, "", Data.READWRITE);
        dbit.setDSetId(dset.getId());
        dbit.checkDataType(newVal);
        dset.addDatabit(dbit);
        dbit.setValue(newVal, username);
   }  
    
    
    public void doEmptyDatacache(String uname) throws StorageException, UnknownElementException, SecurityException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(uname), "swampadmins")) {
            throw new SecurityException("Not allowed to truncate data-cache.");
        }
        DataManager.clearCache();
    }

    public int doGetDatacacheSize(String uname) throws StorageException, UnknownElementException, SecurityException {
        if (!SecurityManager.isGroupMember(SecurityManager.getUser(uname), "swampadmins")) {
            throw new SecurityException("Not allowed to read data-cache.");
        }
        return DatasetCache.getInstance().getCacheSize();
    }
    
}