/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt <tschmidt@suse.de>
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

package de.suse.swamp.modules.actions;

import java.lang.reflect.*;
import java.util.*;

import org.apache.turbine.modules.actions.*;
import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.modules.screens.*;
import de.suse.swamp.util.*;


/**
 * @author tschmidt (tschmidt@suse.de)
 */
public class SecureAction extends VelocitySecureAction {
    
    /** Constant needed for Reflection */
    private static final Class [] methodParams
            = new Class [] { RunData.class, Context.class };
    
    /**
     * Implement this to add information to the context.
     *
     * @param data Turbine information.
     * @param context Context for web pages.
     * @exception Exception, a generic exception.
     */
    public void doPerform( RunData data,Context context )
        throws Exception {
    }

   
    /** 
     * Using the same method as screens
     */
    protected boolean isAuthorized(RunData data) throws Exception {
		return new SecureScreen().isAuthorized(data);
	}
    
    
    /**
     * Overwriting Turbine's default action invocation here 
     * to add error handling functionality and display the error 
     * page on Exceptions.
     *
     * @param data A Turbine RunData object.
     * @param context Velocity context information.
     * @exception Exception a generic exception.
     */
    public void executeEvents(RunData data, Context context) throws Exception {
        // Name of the button.
        String theButton = null;
        // ParameterParser.
        org.apache.turbine.util.parser.ParameterParser pp = data.getParameters();
        String button = pp.convert(BUTTON);
        String key = null;

        // Loop through and find the button.
        for (Iterator it = pp.keySet().iterator(); it.hasNext(); ) {
            key = (String) it.next();
            if (key.startsWith(button)) {
                if (considerKey(key, pp)) {
                    theButton = formatString(key);
                    break;
                }
            }
        }

        if (theButton == null) {
            throw new NoSuchMethodException("ActionEvent: The button was null");
        }

        Method method = null;
        try {
            method = getClass().getMethod(theButton, methodParams);
            Object[] methodArgs = new Object[] { data, context };
            Logger.DEBUG("Invoking " + method);
            method.invoke(this, methodArgs);
        } catch (NoSuchMethodException nsme) {
            data.getResponse().setStatus(400);
            Logger.ERROR("Method " + theButton + " not found in " + getClass().getName());
            context.put("errormsg", "Method " + theButton + " not found in " + getClass().getName());
            setTemplate(data, "Error.vm");
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            data.getResponse().setStatus(400);
            Logger.ERROR("Error invoking " + method + ": " + t.getMessage());
            t.printStackTrace();
            context.put("errormsg", t.getMessage());
            setTemplate(data, "Error.vm");
        }
        finally {
            pp.remove(key);
        }
    }
    
    
}
