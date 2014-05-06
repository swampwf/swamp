/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.util;

import java.util.*;

import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * @author cschum
 * @author tschmidt
 *
 * This class represents the result of reading a workflow definition file. 
 * If the validation did not find fatal errors, the workflow template 
 * object is available via getWorkflowTemplate()
 */
public class WorkflowReadResult {
    
    private String wfName;
    private String wfVersion;
    private WorkflowTemplate template = null;
    private ArrayList warnings = new ArrayList();
    private ArrayList errors = new ArrayList();
    
	
    public WorkflowReadResult() {
	}

	
	public void addError( String msg ) {
		// only add errors once
		if (!errors.contains(msg)) errors.add(msg);
	}

    public void addWarning( String msg ) {
    	// only add warnings once
		if (!warnings.contains(msg)) warnings.add(msg);
    }

	/**
	 * @return Returns if reading the workflow succeeded.
	 */
	public boolean hasErrors() {
		return errors.size() > 0 ? true : false;
	}
    
    public boolean hasWarnings() {
        return warnings.size() > 0 ? true : false;
    }
    

    public String toString(){
        StringBuffer result = new StringBuffer();
        for (Iterator it = warnings.iterator(); it.hasNext(); ){
            String warning = (String) it.next();
            result.append("** Warning: " + warning + "\n");
        }
        for (Iterator it = errors.iterator(); it.hasNext(); ){
            String error = (String) it.next();
            result.append("**** Error: " + error + "\n");
        }
        if (!hasErrors()) {
            result.append("Workflow " + this.wfName + "-" + this.wfVersion + " read successfuly");
        }
        return result.toString();
    }


    public ArrayList getErrors() {
        return errors;
    }


    public ArrayList getWarnings() {
        return warnings;
    }


    public String getWfName() {
        return wfName;
    }


    public String getWfVersion() {
        return wfVersion;
    }


    public WorkflowTemplate getTemplate() {
        if (hasErrors() || template == null){
            Logger.ERROR("Requested invalid template " + this.wfName + " " + this.wfVersion);
        }
        return template;
    }


    public void setTemplate(WorkflowTemplate template) {
        this.template = template;
    }


    public void setWfName(String wfName) {
        this.wfName = wfName;
    }


    public void setWfVersion(String wfVersion) {
        this.wfVersion = wfVersion;
    }
    

}
