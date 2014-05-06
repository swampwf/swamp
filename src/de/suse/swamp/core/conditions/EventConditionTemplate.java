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

import de.suse.swamp.core.workflow.*;

/**
 * Represents a condition that resolves to true once a certain event has
 * happened.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

public class EventConditionTemplate extends ConditionTemplate {

    private EventTemplate eventTempl;
	
    public EventConditionTemplate(EventTemplate eventTempl) {
        this.eventTempl = eventTempl;
    }

    public EventCondition getEventCondition() {
        return new EventCondition(eventTempl.getEvent(), false);
    }

    public Condition getCondition() {
        return getEventCondition();
    }
    
    public ArrayList addAllConditionTemplates(ArrayList list){
    	list.add(this);
    	return list;
    }

}