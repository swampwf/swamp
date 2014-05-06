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

package de.suse.swamp.core.conditions;

import java.util.*;

/**
 * Template for SubsFinishedCondition
 *
 * This Condition will wait for the finish 
 * of all Subworkflows of the given name
 *
 */


public class SubsFinishedConditionTemplate extends ConditionTemplate {

    // name of the subworkflowtype to wait for
    private String subname;
    private String subversion;
	
	
    public SubsFinishedConditionTemplate(String subname, String subversion) {
        this.subname = subname;
        this.subversion = subversion;
    }

    public SubsFinishedCondition getSubsFinishedCondition() {
        return new SubsFinishedCondition(subname, subversion, false);
    }

    public Condition getCondition() {
        return getSubsFinishedCondition();
    }

    
    public ArrayList addAllConditionTemplates(ArrayList list){
    	list.add(this);
    	return list;
    }

    public String getSubname() {
        return subname;
    }

    public String getSubversion() {
        return subversion;
    }
    
}
