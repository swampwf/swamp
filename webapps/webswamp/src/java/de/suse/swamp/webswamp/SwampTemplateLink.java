/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt
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

package de.suse.swamp.webswamp;

import java.util.*;

import org.apache.turbine.services.pull.*;
import org.apache.turbine.services.pull.tools.*;
import org.apache.turbine.util.*;

import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * Extended Turbine TemplateLink Pull-Tool.
 */
public class SwampTemplateLink extends TemplateLink implements ApplicationTool {

    private org.apache.turbine.util.parser.ParameterParser parameters;
    /**
     * Overwriting init-Method of TemplateLink 
     * to get activated Systemfilters and optionally add them 
     * to Links.
     *
     * @param data assumed to be a RunData object
     */
    public void init(Object data){
        super.init(data);
        RunData runData = (RunData) data;
        this.parameters = runData.getParameters();
    }
    
    
    public String getRssLink(){
        String url = "/rss?action=query";
        for (Iterator it = parameters.keySet().iterator(); it.hasNext(); ) {
            String parameter = it.next().toString();
            if (parameter.startsWith("systemfilter")){
                if (!parameters.getString(parameter).equals("null")
                        && !parameters.getString(parameter).equals("")){
                    String value = parameters.getString(parameter);
                    // Logger.DEBUG("Adding " + parameter + ":" + value + " to link.");
                    url += "&" + parameter + "=" + value;
                }
            }
        }
        return url;
    }
    
    
    /** Adds all "Systemfilter_XX" params to the new Link. 
     *  must be called before! any addPathInfo() Methods. 
     * @return - the new Link
     */
    public String addSystemFilterParams(){
        // iterate over parameters and attach every Systemfilter param, 
        // identified by a leading "systemfilter_" 
        for (Iterator it = parameters.keySet().iterator(); it.hasNext(); ) {
            String parameter = it.next().toString();
            if (parameter.startsWith("systemfilter")){
                if (!parameters.getString(parameter).equals("null")
                        && !parameters.getString(parameter).equals("")){
                    String value = parameters.getString(parameter);
                    // Logger.DEBUG("Adding " + parameter + ":" + value + " to link.");
                    this.addPathInfo(parameter, value);
                }
            }
        }
        return toString();
    }
   
    /**
     * Query the current URL for a parameter
     */
    public String getCurrentParam(String param){
    	return parameters.get(param);
    }
    
    
    /**
     * Adds a name=value pair to the path_info string.
     * Overwriting Turbine metod here to catch null assignments.
     */
    public TemplateLink addPathInfo(String name, Object value) {
        if (name == null || value == null ){
            Logger.ERROR("Cannot add URL parameter " + name + ":" + value);
        } else {
            super.addPathInfo(name, value);
        }
        return this;
    }
    
    
    public String toString(){
        // Turbine sets "null" on empty values which we do not want
        return super.toString().replaceAll("null", "");
    }
    
    
    /**
     * @return true if the $link Object currently has a secure target.
     */
    public boolean isSecure(){
        if (this.getAbsoluteLink().substring(0,5).equalsIgnoreCase("https")){
            return true;
        } else {
            return false;
        }
    }

    
    /**
     * @return URL for the secure Login Page
     */
    public String getSecureLoginLink() {
		return WebSWAMP.secureAppLink;
	}
    
    public TemplateLink addPathInfo(org.apache.turbine.util.parser.ParameterParser pp){
        Logger.DEBUG("Adding " + pp.toString() + " to Link");
        return super.addPathInfo(pp);
    }
    
}