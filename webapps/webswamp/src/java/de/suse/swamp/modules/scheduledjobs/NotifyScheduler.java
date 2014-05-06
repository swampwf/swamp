/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de>
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

package de.suse.swamp.modules.scheduledjobs;

import java.util.*;

import org.apache.turbine.services.schedule.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;


public class NotifyScheduler extends SWAMPScheduledJob {

    public NotifyScheduler() {
    }

    /**
     * Run the Jobentry from the scheduler queue. From ScheduledJob.
     * 
     * @param job - The job to run.
     */
    public void run(JobEntry job) throws Exception {
        results.reset();
    	Date start = new Date(System.currentTimeMillis());
        NotificationManager.sendNotifies();
        
        results.addResult(ResultList.MESSAGE, "Went through notification queue.");
        Logger.DEBUG("Scheduled job sendnotifies() task: " + job.getTask()
                + " ran @: " + start);
    }
}

