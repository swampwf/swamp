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
 * The template for the action to edit data
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author Thomas Schmidt
 *
 */

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;


public class DataeditActionTemplate extends UserActionTemplate {

    private ArrayList fieldTempls = new ArrayList();
    private String eventtype;
	

    public DataeditActionTemplate(String name, String role, String eventtype, NodeTemplate nodeTemplate, 
            String notificationtemplate, boolean mandatory, boolean restricted) {
		super(name, role, nodeTemplate, notificationtemplate, mandatory, restricted);
        this.eventtype = eventtype;
    }


    public String getType() {
        return "dataedit";
    }

    
    /**
     * returns all fields of this action
     */
    public LinkedHashMap getAllFields(int workflowId) throws Exception {
		LinkedHashMap hm = new LinkedHashMap();
		for (Iterator it = fieldTempls.iterator(); it.hasNext();) {
			Field f = ((FieldTemplate) it.next()).getField();
			Workflow wf = WorkflowManager.getInstance().getWorkflow(workflowId);
			Databit dbit = wf.getDatabit(f.getPath());
			if (dbit != null) {
				String setname = f.getSetName();
				// if it is a comment or thread type we want to display all fields 
				if (dbit.getType().equals("comment") || dbit.getType().equals("thread")){
					List fields = ((commentDatabit) dbit).getThreadFields(f.getSetName(), f.isMandatory());
					if (hm.containsKey(setname)) {
						((ArrayList) hm.get(setname)).addAll(fields);
					} else {
						hm.put(setname, fields);
					}
				} else {
					Field field = dbit.getField(f.getSetName());
					field.setMandatory(f.isMandatory());
					field.setState(Data.READWRITE);
					if (hm.containsKey(setname)) {
						((ArrayList) hm.get(setname)).add(field);
					} else {
						ArrayList list = new ArrayList();
						list.add(field);
						hm.put(setname, list);
					}
				}
				
			} else if (f.isMandatory()){
			    throw new Exception("Mandatory field: " + f.getPath() + " not available for wf #" + workflowId);
			} else {
				Logger.WARN("Optional field: " + f.getPath() + " not available for wf #" + workflowId);
			}
		}
		return hm;
	}

    
    public ArrayList validate(Result result) {
        ArrayList errors = new ArrayList();
        DataeditResult deResult = (DataeditResult) result;
        Workflow wf = WorkflowManager.getInstance().getWorkflow(result.getWorkflowId());

        // iterate through all fields
        for (Iterator iter = fieldTempls.iterator(); iter.hasNext();) {
            Field field = ((FieldTemplate) iter.next()).getField();
            Logger.DEBUG("Verifying field: " + field.getPath());
            String path = field.getPath();
            field.setState(Data.READWRITE);
            Databit dbit = wf.getDatabit(path);
            // set to reply field on comment
            if (dbit != null && dbit.getType().equals("comment")){
            	List fields = ((commentDatabit) dbit).
            		getThreadFields(field.getSetName(), field.isMandatory());
            	field = (Field) fields.get(fields.size()-1);
            	path = field.getPath();
            }
            // check if mandatory fields are set
            if (!deResult.hasValue(path) && field.isMandatory()) {
            	String desc;
            	if (dbit == null) 
            		desc = path;
            	else {
            		desc = dbit.getDescription();
                	if (dbit.getDescription().length() > 50) 
                		desc = dbit.getDescription().substring(0,50) + "...";  
            	}
                errors.add("Mandatory field \"<i>" + desc + "</i>\" not set.");
                deResult.addErrorField(path, "");
            }

            String tocheck = deResult.getValue(path);
            // if a field is not mandatory and empty don't do integrity checks, no use to check fileref fields
            if (tocheck != null && (!tocheck.equals("") || field.isMandatory()) && !dbit.getType().equals("fileref")) {
                // We need the databit object for checking the correct type of the value
                try {
                    dbit.checkDataType(tocheck);                    
                } catch (Exception e) {
					if (!dbit.getType().equals("bugzilla")){
						deResult.addErrorField(path, tocheck);
						errors.add(e.getMessage());
					} else {
						// Bugzilla errors such as not available, or not permitted
						// must not block the workflow	
						if (e.getMessage().indexOf("NotFound") > 0 ){
							deResult.addErrorField(path, tocheck);
							errors.add("Bug with ID: " + tocheck + " could not be found in Bugzilla.");
						} else {
                            deResult.addErrorField(path, tocheck);
                            errors.add(e.getMessage());
                        }
					}
                }
            }
        }
        runScript(deResult, errors);
        return errors;
    }

    
    
    public void act(Result result, ResultList history) {
        DataeditResult deResult = (DataeditResult) result;
        HashMap values = deResult.getValues();
        Workflow wf = WorkflowManager.getInstance().getWorkflow(result.getWorkflowId());
        for (Iterator iter = values.keySet().iterator(); iter.hasNext(); ) {
            String path = (String) iter.next();
            String value = (String) values.get(path);
            try {
                wf.getDatabit(path).setValue(value, false, result.getUname(), history);
            } catch (Exception e){
                Logger.BUG("Tried to write unverified Databitcontent! " + 
                        " original Message was: " + e.getMessage());
            }
        }
    }

    
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        // Special Event for this DataEditAction
        events.add(new Event(eventtype, result.getWorkflowId(), result.getWorkflowId()));
        return events;
    }

    public String toXML() {
        StringBuffer sb = new StringBuffer();
        sb.append("<DataeditAction>\n");
        sb.append("</DataeditAction>");
        return new String(sb);
    }


    /**
     * @return Returns the eventtype.
     */
    public String getEventtype() {
        return eventtype;
    }
    
    /**
     * @param eventtype The eventtype to set.
     */
    public void setEventtype(String eventtype) {
        this.eventtype = eventtype;
    }
 
    public void addFieldTemplate(FieldTemplate fieldTempl) {
        fieldTempls.add(fieldTempl);
    }


    public ArrayList getFieldTempls() {
        return fieldTempls;
    }
	
}