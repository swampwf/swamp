/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt
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

package de.suse.swamp.modules.screens;

/**
 * Pages inherited from this Class can be seen by any not logged in user.
 * 
 * @author Thomas Schmidt
 * @version $Id$
 */

import org.apache.turbine.services.velocity.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

public class UnsecureScreen extends SWAMPScreen {

    protected void doBuildTemplate(RunData data) throws Exception {
        doBuildTemplate(data, TurbineVelocity.getContext(data));
    }

    
	public void doBuildTemplate(RunData data, Context context) throws Exception {
		super.doBuildTemplate(data, context);
	}

    
	protected boolean isAuthorized(RunData data) throws Exception {
		boolean isAuthorized = true;
		return isAuthorized;
	}

}
