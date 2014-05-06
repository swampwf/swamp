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

package de.suse.swamp.core.actions;

/**
 * Template for a manual task action in workflow context
 * 
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * 
 */

import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;

public class ManualtaskActionTemplate extends UserActionTemplate {

    // the type of event that is sent when this manual action is done
    private String eventtype;

    public ManualtaskActionTemplate(String name, String role, String eventtype, NodeTemplate nodeTemplate,
            String notificationtemplate, boolean mandatory, boolean restricted) {
        super(name, role, nodeTemplate, notificationtemplate, mandatory, restricted);
        this.eventtype = eventtype;
    }

    public String getType() {
        return "manualtask";
    }

    public ArrayList validate(Result result) {
        ArrayList errors = new ArrayList();
        ManualtaskResult mtResult = (ManualtaskResult) result;
        runScript(mtResult, errors);
        if (mtResult.getDone() == false) {
            errors.add("Task not done. Try again.");
        }
        return errors;
    }

    /**
     * this would contain any actions that have to be done beside sending the
     * resulting events. This action type, however, doesn't do anything else, so
     * this is left empty. This method gets called by the Task (triggered by the
     * task manager)
     */
    public void act(Result result, ResultList history) {
    }

    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        ManualtaskResult mtResult = (ManualtaskResult) result;
        if (mtResult.getDone() == true) {
            events.add(new Event(eventtype, result.getWorkflowId(), result.getWorkflowId()));
        }
        return events;
    }

    public String getEventtype() {
        return eventtype;
    }

}