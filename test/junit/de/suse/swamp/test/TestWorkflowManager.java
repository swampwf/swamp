/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt
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

package de.suse.swamp.test;

import junit.framework.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.filter.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

import java.util.*;


public class TestWorkflowManager extends TestCase {

	private WorkflowManager wfman;
	private SWAMP swamp;
	
	
	protected void setUp() throws Exception {
		super.setUp();
		Logger.log.setLevel(org.apache.log4j.Level.DEBUG);
		wfman = WorkflowManager.getInstance();
		swamp = SWAMP.getInstance();
		Assert.assertTrue(wfman != null);
		Assert.assertTrue(swamp != null);
	}

	
	
	// start Workflows of each available Template
	public void notestStartWorkflows() {
		for (int i = 0; i<3; i++){
		    for (Iterator it = wfman.getWorkflowTemplateNames().iterator(); it.hasNext(); ){
		        String wfname = (String) it.next();
		        assertNotNull(wfname);
		        Workflow wf = null;
		        WorkflowTemplate wftemp = wfman.getWorkflowTemplate(wfname);
		        try {
                    wf = wfman.createWorkflow(wftemp.getName(), "swamp-sm", 0, 
                            null, wftemp.getVersion(), true, new ResultList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
		        assertNotNull(wf);
		    }
		}
    }
	
	

	
    
    
	public void testShutdown() {
    }
	
	
	private void logMessage(String msg){
		Logger.log.setLevel(org.apache.log4j.Level.DEBUG);
		Logger.DEBUG(msg);
		Logger.log.setLevel(org.apache.log4j.Level.OFF);
	}
	
}
