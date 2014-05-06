/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Klaas Freitag <freitag@suse.de>
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

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author Klaas Freitag &lt;freitag@suse.de&gt;
 *
 * The SWAMP event manager helps with the following points:
 *  - receiving Events
 *  - storing their appearance for logging purposes
 *  - distribution to all interested points.
 *  
 *  All methods are static as the Eventmanager has no 
 *  member variables
 */

public final class EventManager {

    private EventManager() {
    }

    /**
     * Handle events for a specific workflow.
     * 
     * @param events - a list of events to handle.
     */
    public static void handleWorkflowEvents(ArrayList events, 
            String userName, ResultList history) throws StorageException {
        for (Iterator iter = events.iterator(); iter.hasNext();) {
            handleWorkflowEvent((Event) iter.next(), userName, history);
        }
    }

    /**
     * Handle a single event for a specific workflow.
     * 
     */
    public static void handleWorkflowEvent(Event ev, String userName, ResultList history) 
        throws StorageException {
        // Events are Stored in Eventhistory
        StorageManager.storeEvent(ev);
        WorkflowManager wfman = WorkflowManager.getInstance();
        Workflow workflow = null;
		try {
			workflow = wfman.getWorkflow(ev.getTargetWfId());
		} catch (NoSuchElementException e) {
			throw new StorageException("Tried to send event " + ev.getType() + 
					" to invalid workflow #" + ev.getTargetWfId());
		}
		HistoryManager.create("EVENT_RECEIVE", ev.getId(), ev.getTargetWfId(), userName, null);
        if (!workflow.isRunning()) {
            Logger.DEBUG("Skipping event for inactive workflow #" + workflow.getName());
            history.addResult(ResultList.INFO, "Skipping event for inactive workflow #" + 
                    workflow.getName());
        } else {
            WorkflowTemplate wfTemp = workflow.getTemplate(); 
            if (wfTemp.getWaitingForEvents().contains(ev.getType()) ){
                if (ev instanceof DataChangedEvent && 
                        !wfTemp.hasListenerPath(((DataChangedEvent) ev).getFieldPath())) {
                    Logger.DEBUG("Skipped event handling of " + ev.getType() + " for wf#" + 
                            workflow.getName()
                            + " (not listening on field " + 
                            ((DataChangedEvent) ev).getFieldPath() + ")");
                    history.addResult(ResultList.INFO, "Skipped event handling of " + ev.getType() + " for wf#" + 
                            workflow.getName() + " (not listening on field " + 
                            ((DataChangedEvent) ev).getFieldPath() + ")");
                } else {
                    int histSize = history.getResults().size();
                    workflow.handleEvent(ev, userName, history);
                    HistoryManager.create("EVENT_HANDLED", ev.getId(), ev.getTargetWfId(), userName, null);
                    WorkflowStorage.storeWorkflow(workflow);
                    if (histSize == history.getResults().size()) {
                        history.addResult(ResultList.INFO, "Event " + ev.getType() + " was received by workflow #" 
                                + workflow.getName() + " but did not change its state.");
                    }
                }
            } else {
                Logger.DEBUG("Skipped event handling of " + ev.getType() + " for wf#" + 
                        workflow.getName()
                        + " (not in waitingForEvents)");
                history.addResult(ResultList.INFO, "Skipped event handling of " + ev.getType() + " for wf#" + 
                        workflow.getName() + " (not in waitingForEvents)");
            }
        }
    }

    /**
     * Loads an Event from Storage
     * @param eventID
     * @return
     */
    public static Event loadEventFromHistory(int eventID) {
        return StorageManager.loadEventFromHistory(eventID);
    }
    
}
