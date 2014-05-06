/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.security.roles;

 /**
  * Representation of a role in a Workflow. 
  * Takes a databitpath for referencing role members.
  *
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  */

import java.util.*;

import org.apache.jcs.access.exception.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class DatabitRole extends WorkflowRole {

    private String roleDatabit;
    
    
    public DatabitRole(String name, boolean restricted){
        super(name, restricted);
    }


    /**
     * @param dataPath The dataPath to set.
     */
    public void addValue(String dataPath) {
        this.roleDatabit = dataPath;
    }


    public SWAMPHashSet getMemberNames(Workflow wf) throws Exception {
        SWAMPHashSet values = new SWAMPHashSet();
        if (wf.containsDatabit(this.roleDatabit)){
            Databit bit = wf.getDatabit(this.roleDatabit);
            values.add(bit.getValue(), ",");
        } else {
            throw new NoSuchElementException("Databitrole: " + getName() + " cannot find path: " + 
                    this.roleDatabit + " in Workflow " + wf.getName() + " for role checking!");
        }
        return values;
    }
    

    public SWAMPHashSet getMemberNames(WorkflowTemplate wf) throws NoSuchElementException {
        SWAMPHashSet values = new SWAMPHashSet();
        if (wf.containsDatabitTemplate(this.roleDatabit)){
            DatabitTemplate bit = wf.getDatabitTemplate(this.roleDatabit);
            values.add(bit.getDefaultValue(), ",");
        } else {
            throw new NoSuchElementException("Cannot find path: " + 
                    this.roleDatabit + " in Template " + wf.getName() + " for role checking!");
        }
        return values;
    }
    
    
    public boolean isStaticRole(WorkflowTemplate wfTemp){
    	boolean isStatic = false; 
    	return isStatic;
    }


	public String getRoleDatabit() {
		return roleDatabit;
	}
	
	
    public void verify (WorkflowReadResult result, WorkflowTemplate wfTemp, WorkflowVerifier verifier, List results) {
        try {
            SWAMPHashSet values = new SWAMPHashSet(); 
            DatabitTemplate dbitTemplate = verifier.getDatabitTemplate(this.roleDatabit, wfTemp, result, results);
            if (dbitTemplate != null){
                values = new SWAMPHashSet(dbitTemplate.getDefaultValue(), ",");
                // check if the role members are of valid "person" datatype
                Databit dbit = DataManager.createDatabit(this.roleDatabit, "", "person", "", Data.READWRITE);
                dbit.checkDataType(values.toString(","));   
            } else {
                result.addError("Role-Databit " + this.roleDatabit + " not found.");
            }         
        } catch (InvalidArgumentException e) {
            result.addError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            result.addError(e.getMessage());
        }      
    }
     
}

