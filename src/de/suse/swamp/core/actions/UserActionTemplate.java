/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt (tschmidt@suse.de)
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

import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;

public abstract class UserActionTemplate extends ActionTemplate implements Scriptable {   

    protected String notificationTemplate = "";
	// marks action as important for the advance of the workflow. 
	// non-mandatory actions will be displayed at another place in the GUI.
	protected boolean mandatory = true;
	protected boolean restricted = false;
    protected ScriptTemplate script;
    
	
	public UserActionTemplate(String name, String role, NodeTemplate nodeTemplate, 
	        String notificationTemplate, boolean mandatory, boolean restricted){
		super(name, role, nodeTemplate);
		this.notificationTemplate = notificationTemplate;
		this.mandatory = mandatory;
		this.restricted = restricted;
	}
	
	
	/**
     * @return Returns the notificationTemplate.
     */
    public String getNotificationTemplate() {
        return this.notificationTemplate;
    }
    /**
     * @return Returns the mandatory.
     */
    public boolean isMandatory() {
        return mandatory;
    }
    /**
     * @param mandatory The mandatory to set.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }


	public boolean isRestricted() {
		return restricted;
	}

    public void setScript(ScriptTemplate script) {
        this.script = script;
    }

    public ScriptTemplate getScript() {
        return this.script;
    }
    
    public void runScript(Result result, List errors) {
        if (script != null) {
            HashMap parameters = new HashMap();
            parameters.put("result", result);
            parameters.put("errors", errors);
            script.setParameters(parameters);
            try {
                script.evaluate();
            } catch (Exception e) {
                errors.add("Unable to run usertask script: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    

}
