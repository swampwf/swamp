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

import java.lang.reflect.*;
import java.util.*;

import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * ActionTemplate for custom actions, that means actions 
 * that specify their own Java Class to be instantiated. 
 * This action is normally used for calling external or 
 * doing things that are unsupported by the shipped action classes
 * 
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *  
 */


public class CustomActionTemplate extends SystemActionTemplate {

	private String classname;
	private String function;
	// the type of event that is sent when this custom-action is done
	private String eventtype;	
	
	public CustomActionTemplate(String name, NodeTemplate nodeTemplate, String eventtype, 
			String targetClass, String function) {
		super (name, nodeTemplate);
		this.eventtype = eventtype;
		this.classname = targetClass;
		this.function = function;
	}


    public String getType() {
        return "custom";
    }

    public ArrayList validate(Result result) {
        ArrayList errors = new ArrayList();
        // TODO: SystemTasks aren't validated 
        return errors;
    }

    
    public void act(Result result) {
        act(result, new ResultList());
    }
    
    
    public void act(Result result, ResultList history) {
        String histResult = "CustomAction: \"" + getDescription() + "\" ";

            Object custominstance = null;
            Boolean functionresult = Boolean.FALSE;
            Logger.LOG("Instanciating Class: " + this.getClassname() + 
                    " Method: " + this.getFunction());
            try {
                
                Integer arg1 = new Integer(result.getWorkflowId());
                String uname = result.getUname();
                
                Class[] parameterType = new Class[] { arg1.getClass(), uname.getClass()};
                Class customclass = Class.forName(this.getClassname());
                custominstance = customclass.newInstance();

                Method method = customclass.getMethod(this.function, parameterType);
                functionresult = (Boolean) method.invoke(custominstance, 
                        new Object[] {arg1, uname});

            } catch (InstantiationException e2) {
                e2.printStackTrace();
                Logger.ERROR("Could not instantiate Class: " + e2.getMessage());
                histResult += "\nError: " + e2.getMessage();
            } catch (NoSuchMethodException e2) {
                e2.printStackTrace();
                Logger.ERROR("Method not found: " + e2.getMessage());
                histResult += "\nError: " + e2.getMessage();
            } catch (Exception e2) {
                if (e2.getCause() != null){
                    e2.getCause().printStackTrace();
	                Logger.ERROR("CustomAction failed: " + e2.getCause().getMessage());
	                histResult += "\nError: " + e2.getCause().getMessage();
                } else {
                    e2.printStackTrace();
                	Logger.ERROR("CustomAction failed: " + e2.getMessage());
	                histResult += "\nError: " + e2.getMessage();
                }
            }

            Logger.DEBUG("Answer of class: " + functionresult);
            if (functionresult.booleanValue()){
                history.addResult(ResultList.MESSAGE, histResult + " done.");
            } else {
                history.addResult(ResultList.ERROR, histResult);
            }
    }

	
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        events.add(new Event(eventtype, result.getWorkflowId(), result.getWorkflowId()));
        return events;
    }

	
    /**
     * @return Returns the classname.
     */
    public String getClassname() {
        return classname;
    }

    /**
     * @return Returns the function.
     */
    public String getFunction() {
        return function;
    }

    /**
     * @return Returns the eventtype.
     */
    public String getEventtype() {
        return eventtype;
    }	
	
}