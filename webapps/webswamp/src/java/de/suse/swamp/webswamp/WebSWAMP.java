/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Klaas Freitag<freitag@suse.de>
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

import org.apache.turbine.*;
import org.apache.turbine.services.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.util.*;

public class WebSWAMP extends TurbineBaseService implements WebSWAMPService {

    public static String relativeAppLink;
    public static String appLink;
    public static String secureAppLink;
	
    public void init() throws InitializationException {
        doSWAMPInit();
        setInit(true);
    }

    public void shutdown() {
        relativeAppLink = null;
        appLink = null;
        secureAppLink = null;
        setInit(false);
    }

    private void doSWAMPInit() {
        System.setProperty("swamp.home", Turbine.getConfiguration().getString("webswamp.home"));
        Logger.DEBUG("Set swamp.home to: " + Turbine.getConfiguration().getString("webswamp.home"));
        System.setProperty("swamp.conf", Turbine.getConfiguration().getString("swamp.conf"));
        Logger.DEBUG("Set swamp.conf to: " + Turbine.getConfiguration().getString("swamp.conf"));

        // This makes sure that SWAMP is started by the webapp to read the right config files
        SWAMP.getInstance();
        // preload workflow templates
        WorkflowManager.getInstance();

        // set the Link values:
        if (getSecureAppLink() != null) {
            secureAppLink = getSecureAppLink();
            Logger.DEBUG("Set WebSWAMP.secureAppLink to: " + secureAppLink);
            SWAMP.getInstance().setProperty("secureAppLink", secureAppLink);
        }
        if (getAppLink() != null) {
            appLink = getAppLink();
            Logger.DEBUG("Set WebSWAMP.appLink to: " + appLink);
            SWAMP.getInstance().setProperty("appLink", appLink);
            // fall back to non-secure link
            if (secureAppLink == null) {
                secureAppLink = appLink;
                Logger.DEBUG("Set WebSWAMP.secureAppLink to: " + secureAppLink);
                SWAMP.getInstance().setProperty("secureAppLink", secureAppLink);
            }
        }

        if (getRelativeAppLink() != null) {
            relativeAppLink = getRelativeAppLink();
            Logger.DEBUG("Set WebSWAMP.relativeAppLink to: " + relativeAppLink);
            SWAMP.getInstance().setProperty("relativeAppLink", relativeAppLink);
        }
    }

    
    private String getSecureAppLink() {
        // don't rely on Turbines cached values, use those of
        // the current configuration:
        String link = "https://";
        String hostname = Turbine.getConfiguration().getString("webswamp.hostname");
        if (hostname != null && !hostname.equals("")) {
            link += hostname;
        } else {
            link += Turbine.getServerName();
            Logger.ERROR("Please specify hostname in your configuration! "
                    + "Falling back to " + Turbine.getServerName());
        }
        String sslPort = Turbine.getConfiguration().getString("webswamp.sslport");
        if (sslPort != null && sslPort.length() > 0) {
            if (!sslPort.equals("443")){
            link += ":" + sslPort;
            }
        } else {
            Logger.WARN("Please specify webswamp.sslport in your configuration " + 
            		"if you want to generate HTTPS links!");
        	    return null;   
        }
        link += getRelativeAppLink();
        return link;
    }
    
    
    private String getAppLink() {
        // don't rely on Turbines cached values, use those of
        // the current configuration:
        String link = "http://";
        String hostname = Turbine.getConfiguration().getString("webswamp.hostname");
        if (hostname != null && !hostname.equals("")) {
            link += hostname;
        } else {
            link += Turbine.getServerName();
            Logger.ERROR("Please specify hostname in your configuration! "
                    + "Falling back to " + Turbine.getServerName());
        }
        String port = Turbine.getConfiguration().getString("webswamp.port");
        if (port != null && port.length() > 0) {
            if (!port.equals("80")){
            link += ":" + port;
            }
        } else {
            Logger.ERROR("Please specify webswamp.port in your configuration!");
            return null;   
        }
        link += getRelativeAppLink();
        return link;
    }
    
    
    
    private String getRelativeAppLink() {
        String path = Turbine.getConfiguration().getString("webswamp.path");
        if (path == null || path.equals("")) {
            path = Turbine.getContextPath();
            Logger.ERROR("Please specify webswamp.path in your configuration! "
                    + "Falling back to " + Turbine.getContextPath());
        }
     return path;
    }
    
}