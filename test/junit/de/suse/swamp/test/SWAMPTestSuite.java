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

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.util.*;
import junit.framework.*;

/**
 * @author tschmidt
 * This Class is called by <i>ant junit</i> and calls  
 */
public class SWAMPTestSuite extends TestCase {

	public static Test suite() {		
		// Initialize SWAMP
	    Logger.log.setLevel(org.apache.log4j.Level.INFO);
	    Date start = new Date();
	    Logger.LOG("Starting SWAMP Init");
		SWAMP swamp = SWAMP.getInstance();
		long diff = System.currentTimeMillis() - start.getTime();
		Logger.LOG("SWAMP-init took " + (diff/1000l) + " seconds");
		
		TestSuite suite= new TestSuite("All SWAMP JUnit Tests");
		//suite.addTest(new TestSuite(TestCustomMaintenance.class));
		//suite.addTest(new TestSuite(TestWorkflowManager.class));
		//suite.addTest(new TestSuite(TestTaskManager.class));
		//suite.addTest(new TestSuite(TestStatisticsGraph.class));
		//suite.addTest(new TestSuite(TestBugzilla.class));
        suite.addTest(new TestSuite(TestWorkflowRemove.class));
        
	    return suite;
	}
	
}
