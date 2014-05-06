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

package de.suse.swamp.core.container;

import java.io.*;
import java.util.*;

import org.apache.torque.*;
import org.apache.velocity.*;
import org.apache.velocity.app.*;

import de.suse.swamp.util.*;


/**
 * This is the main SWAMP Object (a Singleton) to ask for system-wide stuff like
 * properties or other objects floating around. This affects the location
 * of external resources, like configuration files, DTDs, workflow definitions
 * etc.
 * 
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 */

public class SWAMP {
    
    private static SWAMP swamp = null;
    private static Properties SWAMPProperties; 
    static String fs = System.getProperty("file.separator");
    
    private SWAMP(Properties p) {
        SWAMPProperties = p;
        this.init();       
    }
    

    /**
     * @return - the SWAMP instance
     */
    public static synchronized SWAMP getInstance() {
        if (swamp == null) {
            swamp = new SWAMP(readProperties());
        }
        return swamp;
    }
    
    
    /**
     * swamp.home is read from the System-Object which takes the property
     * from whereever, but not from the defaults file. You may specify the
     * Paramaters swamp.home + swamp.conf by Commandline (java -D), or 
     * on webapp startup, or however SWAMP was started up. 
     * @return SWAMP Singleton Object
     */
    private static Properties readProperties() {
        Properties properties = new Properties();
        String SWAMPHome = System.getProperty("swamp.home");
        if (SWAMPHome == null || SWAMPHome.equals("")) {
            System.out.println("Was not able to determine swamp path, exiting. " + 
            "(Check your hostname.properties file, or specify by " +
            "-Dswamp.home=...)");
        }
        System.out.println("swamp.home set to " + SWAMPHome);
        properties.setProperty("swamp.home", SWAMPHome);
        String SWAMPConf = System.getProperty("swamp.conf");
        
        if (SWAMPConf == null || SWAMPConf.equals("")) {
            System.out.println("Was not able to determine swamp config path, exiting. " + 
                    "(Check your hostname.properties file, or specify by " +
                    "-Dswamp.conf=...)");
        }

        SWAMPConf = SWAMPHome + fs + SWAMPConf;
        System.out.println("Loading property file " + SWAMPConf);
        try {
            InputStream propertyStream = new FileInputStream(SWAMPConf);
            properties.load(propertyStream);
        } catch (Exception e) {
            System.err.println("Could not find property file: " + e);
            System.exit(1);
        }
        return properties;
    }
    
    
    /**
     * Initializes Services that are needed by SWAMP: 
     * Torque, Velocity
     */
    private void init() {
        if (!Torque.isInit()) {
            try {
                Torque.init(this.getTorqueProperties());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        addVelocityMacro();
        SWAMPProperties.put("bootDate", new Date().toString());
    }
    
    /**
     * @return The correct DTD location for use in parsing XML files. 
     */
    public String getDTDLocation() {
        return getSWAMPHome() + fs + "conf" + fs + "dtds" + fs + "workflow.dtd";
    }
    
    /**
     * @return The location where temporary dot and image files for
     * displaying graphs should be put. Set according to GRAPH_DIR in the 
     * config file.
     */
    public String getGraphLocation() {
        return getProperty("GRAPH_DIR");
    }
    
    public String getWorkflowLocation() {
        return getProperty("WORKFLOW_LOCATION");
    }
    
    
    public String getTorqueProperties() {
        return getProperty("TORQUE_PROP_FILE");
    }
    
    public String getProperty(String key) {
        String property = SWAMPProperties.getProperty(key);
        if (property != null) {
            property = property.replaceAll
			("\\$SWAMP_HOME", SWAMPProperties.getProperty("swamp.home")).trim();           
        } else {
            Logger.ERROR("Property " + key + " not found in defaults file");
        }
        return property;
    }
    
    public void setProperty(String key, String value){
        SWAMPProperties.put(key, value);        
    }
    
    public String getProperty(String key, String defaultval) {
    	String val = getProperty(key);
    	if (val == null) val = defaultval;
    	return val;
    }

    public String getSWAMPHome() {
        return getProperty("swamp.home");
    }
    
    
    /**
     * Adding general purpose macros to velocity
     */
    private void addVelocityMacro() {
        // include notification macros: 
        File macrosFile = new File(this.getProperty("TEMPLATES_LOCATION") + 
                System.getProperty("file.separator") + "macros.vm");
        String macros = "";
        try {
            macros = FileUtils.getText(macrosFile);
            VelocityContext context = new VelocityContext();
            StringWriter w = new StringWriter();
            Velocity.evaluate(context, w, "MacroFile", macros);
        } catch (Exception e1) {
            Logger.ERROR("Unable to load notification macros from " + macrosFile.getPath());
        }
    }
    
    
}
