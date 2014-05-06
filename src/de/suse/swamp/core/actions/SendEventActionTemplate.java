/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt (tschmidt@suse.de)
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

import java.io.*;
import java.text.*;
import java.util.*;

import org.apache.velocity.*;
import org.apache.velocity.app.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class SendEventActionTemplate extends SystemActionTemplate {

    private String eventType;
    private String triggerDatabit;
    private String triggerOffset;
    private boolean onlyWeekdays;
    private String targetWfs;
    
    public SendEventActionTemplate(String name, NodeTemplate nodeTemplate, String eventType) {
		super(name, nodeTemplate);
        this.eventType = eventType;
    }


    public void act(Result result) {
        act(result, new ResultList());
    }
    
    
    /**
     * This action will initially be called from the Workflow 
     * and then it will be called regulary by the scheduler as long 
     * as this action stays active.
     */
    public void act(Result genericResult, ResultList history) {
        SendEventActionResult result = (SendEventActionResult) genericResult;
        Workflow wf = WorkflowManager.getInstance().getWorkflow(result.getWorkflowId());

        // do we have a target date?
        Date triggerDate = new Date();
        try {
            if (result.isDone()) {
                Logger.DEBUG("Skipping already done SendEventAction.");
            } else if (triggerDatabit == null) {
                // send event immediately:
                history.addResults(sendEvent(wf, result.getUname()));
                result.setDone(true);
            } else if (triggerOffset != null && !wf.getDatabitValue(triggerDatabit).equals("")) {
                Calendar now = Calendar.getInstance();
                int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
                if (this.onlyWeekdays && (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) {
                    Logger.DEBUG("SendEventAction " + getName() + " not triggered! (Weekend)");
                    return;
                }
                triggerDate = this.getTriggerDate(wf.getId());
                if (triggerDate.equals(new Date()) || triggerDate.before(new Date())) {
                    Logger.DEBUG("SendEventAction " + getName() + " triggered!");
                    // send to all wfs from the list:
                    history.addResults(sendEvent(wf, result.getUname()));
                    // action has been done, set in result.
                    result.setDone(true);
                } else {
                    Logger.DEBUG("SendEventAction " + getName() + " not triggered! (not yet)");
                }
            } else {
                Logger.DEBUG("SendEventAction " + getName() + " not triggered! (no date yet)");
            }
        } catch (Exception e) {
            Logger.ERROR("SendEventAction Error: " + e.getMessage());
            return;
        }
    }

    
    /**
     * @return an ArrayList with error strings, empty if everything went fine
     */
    public ArrayList validate(Result result) {
        // TODO: SystemTasks aren't validated 
        return new ArrayList();
    }


    
    /**
     * Sendeventaction always sends events in act()
     */
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        return events;
    }

    
    public String getType() {
        return "SendEventAction";
    }


    /**
     * Returns the triggerdate for this Action
     */
    public Date getTriggerDate(int wfid) throws Exception {
        Date triggerDate = null;
        Workflow wf = WorkflowManager.getInstance().getWorkflow(wfid);
        // sendeventactions without trigger-databit are valid. they send immediately on activation
        if (this.triggerDatabit != null && !this.triggerDatabit.equals("")){
            Databit dbit = wf.getDatabit(this.triggerDatabit);
            if (dbit == null){
                Logger.ERROR("SendEventAction: Unable to get Databit: " + 
                        this.triggerDatabit + " for Wf: " + wfid);
                throw new Exception("SendEventAction: Unable to get Databit: " + 
                    this.triggerDatabit + " for Wf: " + wfid);
            } else if (dbit.getValue().equals("")) {
                return null;            
            } else {
                Date startDate = ((dateDatabit) dbit).getValueAsDate();
                //Logger.DEBUG("Trigger-BaseDate is: " + startDate);
                triggerDate = new Date(startDate.getTime() + this.getTriggerOffsetMs());
                Logger.DEBUG("TriggerDate is: " + triggerDate);
            }
        } else {
            triggerDate = new Date();
        }
        return triggerDate;
    }
    
    /**
     * Returns the triggerdate for this Action as a formatted string
     */
    public String getTriggerDateAsString(int wfid) throws Exception {
        DateFormat df = new SimpleDateFormat(datetimeDatabit.dateTimeFormat);
        return df.format(getTriggerDate(wfid));
    }
    
    
    private long getTriggerOffsetMs(){
        long offset = 0; 
        if (triggerOffset.startsWith("+") && 
            (triggerOffset.endsWith("d") || 
                    triggerOffset.endsWith("h") || 
                    triggerOffset.endsWith("m")) && 
            (triggerOffset.length() > 2)){
            int offsetNumber = Integer.parseInt(triggerOffset.substring(1, 
                    triggerOffset.length() - 1));
            //Logger.DEBUG("Extracted offsetNumber " + offsetNumber);
            if (triggerOffset.endsWith("h")){
                offset = offsetNumber * 1000l * 60l * 60l;
            } else if (triggerOffset.endsWith("d")){
                offset = offsetNumber * 1000l * 60l * 60l * 24l;
            } else if (triggerOffset.endsWith("m")){
                offset = offsetNumber * 1000l * 60l;
            }
            //Logger.DEBUG("Will set triggerOffset to " + offset);
        } else {
            Logger.ERROR(this.getName() + 
                    " TriggerOffset must have the format: +<number>[d|h] (" + 
                    triggerOffset + ")");
        }       
        return offset;
    }


    public void setOnlyWeekdays(boolean onlyWeekdays) {
        this.onlyWeekdays = onlyWeekdays;
    }


    public void setTargetWfs(String targetWfs) {
        this.targetWfs = targetWfs;
    }


    public void setTriggerDatabit(String triggerDatabit) {
        this.triggerDatabit = triggerDatabit;
    }


    public void setTriggerOffset(String triggerOffset) {
        this.triggerOffset = triggerOffset;
    }


	public String getTriggerDatabit() {
		return triggerDatabit;
	}


	public String getTriggerOffset() {
		return triggerOffset;
	}
    
    
    /**
     * Evaluate the target wfs and send them the event
     */
    private ResultList sendEvent(Workflow wf, String uname){
        SWAMPHashSet targetList = new SWAMPHashSet();
        ResultList results = new ResultList();
        // do we have target wfs?
        if (targetWfs != null) {
            VelocityContext context = new VelocityContext();
            context.put("wf", wf);
            String ident = "ScriptAction";
            StringWriter w = new StringWriter();
            try {
                Velocity.evaluate(context, w, ident, targetWfs);
            } catch (Exception e) {
                Logger.ERROR("Error in evaluating velocity String: " + targetWfs + ". Message: "
                        + e.getMessage());
                return results;
            }
            targetList.add(w.toString(), ",");
            Logger.DEBUG("extracted target workflows: " + targetList.toString(","));
        } else {
            // if no targetwfs given, send to the wf itself
            targetList.add(String.valueOf(wf.getId()));
        }
        
        for (Iterator it = targetList.iterator(); it.hasNext();) {
            int target = new Integer(((String) it.next())).intValue();
            try {
                EventManager.handleWorkflowEvent(new Event(this.eventType, wf.getId(),
                        target), uname, results);
            } catch (StorageException e) {
                Logger.ERROR("SendEventAction Error: " + e.getMessage());
                return results;
            }
        }
        return results;
    }


    public boolean isOnlyWeekdays() {
        return onlyWeekdays;
    }
    
    
}