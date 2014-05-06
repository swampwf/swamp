/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Thomas Schmidt <tschmidt@suse.de>
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
 * Display Debug Info
 *
 * @author Thomas Schmidt
 *
 */

import org.apache.commons.collections.map.*;
import org.apache.turbine.services.schedule.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;

public class Debug extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        
		// Memory Stuff 
		    long totalMem = Runtime.getRuntime().totalMemory()/1024l/1024l;
		    int cpu = Runtime.getRuntime().availableProcessors();
		    long freeMem = Runtime.getRuntime().freeMemory()/1024l/1024l;
		    long usedMem = totalMem - freeMem;
		    String user = data.getUser().getName();
		    SWAMPAPI swamp = new SWAMPAPI();
		    WorkflowAPI wfApi = new WorkflowAPI();
		    TaskAPI taskApi = new TaskAPI();
		    SecurityAPI secApi = new SecurityAPI();
		    DataAPI dataApi = new DataAPI();
		    
		    context.put("bootdate", swamp.doGetProperty("bootDate", user));
		    context.put("standardlogo", "true");
		    
		    context.put("totalMem", String.valueOf(totalMem));
		    context.put("cpu", String.valueOf(cpu));
		    context.put("freeMem", String.valueOf(freeMem));
		    context.put("usedMem", String.valueOf(usedMem));
		    
		    context.put("buildtime", swamp.doGetProperty("BUILDTIME", user));
		    context.put("buildhost", swamp.doGetProperty("BUILDHOST", user));
 
		    
		    // cache information: 
		    context.put("taskcachesize", String.valueOf(taskApi.doGetTaskCacheSize(user)));
		    LRUMap cache = wfApi.doGetWorkflowCache(user);
		    context.put("cacheMaxsize", String.valueOf(cache.maxSize()));
		    context.put("cachesize", String.valueOf(cache.size()));
		    context.put("usercachesize", String.valueOf(secApi.doGetUserCacheSize(user)));
		    context.put("datacachesize", String.valueOf(dataApi.doGetDatacacheSize(user)));
            
            // job scheduler (TurbineSchedulerService)
            context.put("jobs", TurbineScheduler.listJobs());
        
    }
	

}