/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.custom;

import java.io.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.workflow.*;

/**
 * @author tschmidt
 * Example class for using a customtask action.
 */

public class CustomActionExample {
	

	
	/** This method gets called from the ExampleWorkflow
	 * @param wid - the workflow id
	 * @param userId - the users id
	 * @return - true for success, false for failure
	 */
	public Boolean customTest(Integer wid, String userId) throws Exception {
		Workflow wf = WorkflowManager.getInstance().getWorkflow(wid.intValue());
		
		// do whatever you want here, but please don't break anything ;-)
		
        
		return Boolean.TRUE;
	}
	
	
	/** 
	 * Example method on how to call an external program by using a customaction
	 * @param wid - the workflow id
	 * @param userId - the users id
	 * @return - true for success, false for failure
	 */
	public Boolean executeProgram(Integer wid, String userId) throws Exception {
		Workflow wf = WorkflowManager.getInstance().getWorkflow(wid.intValue());
		
        // building the command line
        String command[] = { "free", "-l", "-t"};
        Process proc = Runtime.getRuntime().exec(command);
        
        // read the stdout of the program: 
        InputStream stdout = proc.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdout);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer output = new StringBuffer();
        String line = null; 
        while ( (line = br.readLine()) != null)
        	output.append(line);

        // get the programs exit value: 
        int exitVal = proc.waitFor();
        
		return Boolean.TRUE;
	}
		
	
	
	
	
    
}