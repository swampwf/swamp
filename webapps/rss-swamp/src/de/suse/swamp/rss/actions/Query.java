/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt
 * Copyright (c) 2007 Novell Inc.
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
package de.suse.swamp.rss.actions;

import java.util.*;

import javax.servlet.http.*;

import com.sun.syndication.feed.synd.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.tasks.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


public class Query implements ActionIface {
	
    public List getItems(HttpServletRequest request) throws Exception {
        
        List items = new ArrayList();    
        WorkflowAPI wfapi = new WorkflowAPI();
        String uname = request.getRemoteUser();
        SWAMP swamp = SWAMP.getInstance();
        String swampLink = swamp.getProperty("appLink");
        de.suse.swamp.util.Logger.DEBUG("Creating RSS for query");
        SWAMPHashMap parameters = new SWAMPHashMap();
        for (Iterator it = request.getParameterMap().keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            parameters.put(key, request.getParameter(key));
        }
        List wfs = wfapi.getWorkflows(parameters, null, uname);
        
        for (Iterator it = wfs.iterator(); it.hasNext();) {
            Workflow wf = (Workflow) it.next();
            String desc = wf.getStateDescription();
            for (Iterator it2 = wf.getActiveTasks(true).iterator(); it2.hasNext();) {
                WorkflowTask task = (WorkflowTask) it2.next();
                desc += "<br />Task: <a href=\"" + swampLink + "/task/" + task.getId() + "\">" + task.getReplacedDescription() + "</a>";
            }
            SyndEntry entry = new SyndEntryImpl();
            SyndContentImpl description = new SyndContentImpl();
            description.setType("text/plain");
            desc = desc.replaceAll("\r", "").replaceAll("\\\\n", "<br />").replaceAll("\\\n", "<br />")
            .replaceAll("\\n", "<br />").replaceAll("\n", "<br />");
            description.setValue(desc);
            entry.setDescription(description);
            entry.setLink(swampLink + "/wf/" + wf.getId());
            Databit date = wf.getDatabit("System." + wf.getStartNode().getName() + ".enterDate");
            entry.setPublishedDate(((datetimeDatabit) date).getValueAsDate());
            entry.setTitle(wf.getReplacedDescription());
            items.add(entry);
        }
        
        return items;
	}
    
}

	