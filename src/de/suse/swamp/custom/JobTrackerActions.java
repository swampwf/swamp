/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt <tschmidt@suse.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 */

package de.suse.swamp.custom;

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.security.*;
import de.suse.swamp.core.workflow.*;

/**
 * @author tschmidt
 * Methods for handling JobTracker specific stuff
 */

public class JobTrackerActions {
	
    WorkflowManager wfMan = WorkflowManager.getInstance();
    
	/** 
     * Remove unused rating subworkflows
	 */
	public Boolean removeEmptyRatings(Integer wfid, String userId) throws Exception {
        // get involved developers: 
        Workflow wf = wfMan.getWorkflow(wfid.intValue());
        for (Iterator it = wf.getAllSubWfIds().iterator(); it.hasNext(); ){
            Workflow subWf = wfMan.getWorkflow(((Integer) it.next()).intValue());            
            if (subWf.containsDatabit("bewertungsdata.bewerterdata.comment") && 
                    subWf.getDatabitValue("bewertungsdata.bewerterdata.comment").equals("") && 
                    subWf.containsDatabit("bewertungsdata.bewerterdata.result") &&
                    subWf.getDatabitValue("bewertungsdata.bewerterdata.result").equals("")) {
                wfMan.removeWorkflow(subWf, SWAMPUser.SYSTEMUSERNAME);
            }
        }
        return Boolean.TRUE;
	}
       
}