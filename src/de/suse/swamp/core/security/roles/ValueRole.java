/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.security.roles;

 /**
  * Representation of a role in a Workflow. 
  * Takes a list of usernames as value.
  *
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  */

import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

public class ValueRole extends WorkflowRole {

     private SWAMPHashSet roleValue = new SWAMPHashSet();
    
     
    public ValueRole(String name, boolean restricted){
        super(name, restricted);
    }

        
    
    public SWAMPHashSet getMemberNames(Workflow wf) throws Exception {
        SWAMPHashSet values = new SWAMPHashSet();
       	// redirect static rolevalue roles to the template: 
       	values = getMemberNames(wf.getTemplate());
        return values;
    }
    
    
    public SWAMPHashSet getMemberNames(WorkflowTemplate wf) throws NoSuchElementException {
        SWAMPHashSet values = this.roleValue;
        return values;
    }
    

    
    public void addValue(String staticValue) {
        this.roleValue.add(staticValue, ",");
    }
    

    public boolean isStaticRole(WorkflowTemplate wfTemp){
    	return true;
    }


    public void verify (WorkflowReadResult result, WorkflowTemplate wfTemp, WorkflowVerifier verifier, List results) {
        try {
        // check if the role members are of valid "person" datatype
        Databit dbit = DataManager.createDatabit(getName(), "", "person", "", Data.READWRITE);
        dbit.checkDataType(roleValue.toString(","));
        } catch (Exception e) {
                result.addError(e.getMessage());
        }      
    }
    
}

