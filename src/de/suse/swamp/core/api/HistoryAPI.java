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
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.util.SecurityException;

/**

 * @author  Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * @version $Id$
 */

public class HistoryAPI {
	
	public ArrayList getHistoryEntries(int wfid, int itemId, String filter, String uname) throws SecurityException,
			UnknownElementException {
		ArrayList results = null;

		// check permissions for workflow read access
		WorkflowAPI wfapi = new WorkflowAPI();
		wfapi.getWorkflow(wfid, uname);
		results = HistoryManager.getHistoryEntries(wfid, itemId, filter);
		return results;
	}

	public ArrayList getHistoryEntries(int wfid, String filter, String uname) throws SecurityException,
			UnknownElementException {
		ArrayList results = null;

		// check permissions for workflow read access
		WorkflowAPI wfapi = new WorkflowAPI();
		wfapi.getWorkflow(wfid, uname);
		results = HistoryManager.getHistoryEntries(wfid, filter);
		return results;
	}

    
    public ArrayList getHistoryEntries(ArrayList affectedIds, String filter, String uname)
            throws SecurityException, UnknownElementException {
        ArrayList results = null;
        results = HistoryManager.getHistoryEntries(affectedIds, filter);
        return results;
    }
    
    
    
	public ArrayList getHistoryEntries(int wfid, String uname) throws SecurityException, UnknownElementException {
		ArrayList results = null;

		// check permissions for workflow read access
		WorkflowAPI wfapi = new WorkflowAPI();
		wfapi.getWorkflow(wfid, uname);
		results = HistoryManager.getWorkflowHistory(wfid);
		return results;
	}
	
	   
}