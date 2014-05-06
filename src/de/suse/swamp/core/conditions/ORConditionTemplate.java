/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh@suse.de>
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
 * Class description to end up in javadoc.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

public class ORConditionTemplate extends ConditionTemplate {

    private ConditionTemplate cond1Templ;
    private ConditionTemplate cond2Templ;    
	
	public ORConditionTemplate() {
        super();
    }

    public ORConditionTemplate(ConditionTemplate cond1Templ, 
            ConditionTemplate cond2Templ) {
        this.cond1Templ = cond1Templ;
        this.cond2Templ = cond2Templ;
    }

    public ORCondition getORCondition() {
        return new ORCondition(cond1Templ.getCondition(), 
                cond2Templ.getCondition());
    }

    public Condition getCondition() {
        return getORCondition();
    }
    
    public ArrayList addAllConditionTemplates(ArrayList list){
    	list.add(this);
    	list = cond1Templ.addAllConditionTemplates(list);
    	list = cond2Templ.addAllConditionTemplates(list);
    	return list;
    }
    
}