/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt@suse.de> 
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

package de.suse.swamp.core.container;

import java.util.*;

import org.apache.torque.*;
import org.apache.torque.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.om.*;
import de.suse.swamp.util.*;

/**
 * Storing statistical data to the dbStatistics table
 * 
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *
 * @version $Id$
 */

public final class StatisticStorage {

    // extra logger for storage stuff
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.container.Storage");
    
    /**
     * Store a statistic entry in the db
     * @param avg - avg age in seconds
     * @param running
     * @param type
     * @throws StorageException
     */
    public static void storeStats(long avg, int running, String type) throws StorageException {

        Dbstatistics dbstats = new Dbstatistics();
        
        try {
            dbstats.setNew(true);
            dbstats.setDate(new Date());
            dbstats.setAvgage(avg);
            dbstats.setRunningcount(running);
            dbstats.setWftype(type);
            dbstats.save();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.ERROR("Error in storing Stats: " + e.getMessage(), log);
            throw new StorageException("Could not store Statistics", e);
        }
    }


    public static List loadStats(String templateName, Date startDate, Date endDate) {
        Criteria crit = new Criteria();
        if (startDate != null)
            crit.add(DbstatisticsPeer.DATE, startDate, Criteria.GREATER_EQUAL);
        if (endDate != null) {
            crit.and(DbstatisticsPeer.DATE, endDate, Criteria.LESS_EQUAL);
        }
        crit.add(DbstatisticsPeer.WFTYPE, templateName);
        List storedStatistics = new ArrayList();
        try {
            Logger.DEBUG("Statistic query: " + crit.toString());
            storedStatistics = DbstatisticsPeer.doSelect(crit);
        } catch (TorqueException e) {
            Logger.DEBUG("Error loading statistics: " + e.getMessage(), log);
        }
        return storedStatistics;
    }
    
    
    
}
