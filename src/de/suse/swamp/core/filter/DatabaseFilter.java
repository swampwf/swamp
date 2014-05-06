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

package de.suse.swamp.core.filter;

import java.util.*;

import org.apache.commons.lang.*;


/**
 * @author tschmidt
 * Interface for different database-filters
 */
public abstract class DatabaseFilter extends WorkflowFilter {

    String orderColumn;
    int limit;

    public DatabaseFilter() {
        super();
    }


    public abstract String getSQL();

    
    public String buildSQLString(ArrayList columns, 
            ArrayList tables, ArrayList conditions) {
        StringBuffer sqlString = new StringBuffer("SELECT DISTINCT ");
        for (Iterator it=columns.iterator(); it.hasNext(); ){
            sqlString.append(StringEscapeUtils.escapeSql((String) it.next())).append(", ");
        }
        sqlString.delete(sqlString.length()-2, sqlString.length());
        
        sqlString.append(" FROM ");
        for (Iterator it=tables.iterator(); it.hasNext(); ){
            sqlString.append(StringEscapeUtils.escapeSql((String) it.next())).append(", ");
        }
        sqlString.delete(sqlString.length()-2, sqlString.length());
        
        if (conditions != null && conditions.size() >= 1) {
            sqlString.append(" WHERE ");
            for (Iterator it = conditions.iterator(); it.hasNext();) {
                sqlString.append((String) it.next()).append(" AND ");
            }
            sqlString.delete(sqlString.length() - 5, sqlString.length());
        }
        
        // does this filter set an ordering?
        if (descending != null && orderColumn != null){
            sqlString.append(" ORDER BY " + orderColumn);
            if (descending.equals(Boolean.TRUE))
                sqlString.append(" DESC");
            else 
                sqlString.append(" ASC");
            sqlString.append(", dbWorkflows.wfid ASC");
            
        }
        if (limit > 0) {
            sqlString.append(" LIMIT " + limit);
        }
        
       sqlString.append(";");
        return sqlString.toString();
    }

    /**
     * @param orderColumn The orderColumn to set.
     */
    void setOrderColumn(String orderColumn) {
        this.orderColumn = orderColumn;
    }


    public int getLimit() {
        return limit;
    }


    public void setLimit(int limit) {
        this.limit = limit;
    }
}