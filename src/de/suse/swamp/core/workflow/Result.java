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

import de.suse.swamp.core.security.*;

 /**
  * The result from any action.
  *
  * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
  *
  */

public class Result {
	
	int workflowId;
    // the user who generated this result
    String uname = SWAMPUser.SYSTEMUSERNAME;
	
    // FIXME: get user in here
	public Result(int wfid){
		this.workflowId = wfid;
	}

	public int getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(int workflowId) {
		this.workflowId = workflowId;
	}

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }
	
}
