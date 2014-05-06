/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2008 Thomas Schmidt <tschmidt [at] suse.de>
 * Copyright (c) 2008 Novell Inc.
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

import groovy.lang.*;

import java.io.*;
import java.util.*;

import org.apache.velocity.*;
import org.apache.velocity.app.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.util.*;

/**
 * This class represents a script that can be evaluated with 
 * a supported (velocity, groovy) interpreter 
 * @author tschmidt
 *
 */
public class ScriptTemplate {

    private String language = "velocity";
    private String script;
    private String description;
    private HashMap parameters;
    private static GroovyShell groovyShell = getGroovyShell();

    public ScriptTemplate() {
    }
    
    public ScriptTemplate(String script) {
        this.script = script;
    }
    
    public boolean equals(ScriptTemplate script2) {
        if (this.script.equals(script2.getScript()) && this.language.equals(script2.getLanguage())) {
            return true;
        }
        return false;
    }
    
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Evaluates the script with the given engine and returns the result
     * @throws Exception - when running the script failed
     */
    public String evaluate() throws Exception {
        String result = "";
        if (language.equals("velocity")) {
            VelocityContext context = new VelocityContext();
            for (Iterator it = parameters.keySet().iterator(); it.hasNext();) {
                String paramKey = (String) it.next();
                context.put(paramKey, parameters.get(paramKey));
            }
            StringWriter w = new StringWriter();
            Velocity.evaluate(context, w, "ScriptAction", script);
            result = w.toString().trim();
        } else if (language.equals("groovy")) {
            for (Iterator it = parameters.keySet().iterator(); it.hasNext();) {
                String paramKey = (String) it.next();
                groovyShell.setVariable(paramKey, parameters.get(paramKey));
            }
            Object value = groovyShell.evaluate(script);
            if (value != null && value instanceof String) {
                result = (String) value;
            }
        } else {
            throw new Exception("Unsupported scripting language " + language);
        }
        Logger.DEBUG("Script result: " + result);
        return result;
    }

    public HashMap getParameters() {
        return parameters;
    }

    public void setParameters(HashMap parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Check if the script has a valid syntax. 
     * The groovySafeMode can be switched from the config. 
     * TODO: the safe mode certainly has to be improved to be safe
     */
    public void checkScript() throws Exception {
        if (language.equals("velocity")) {
            VelocityContext context = new VelocityContext();
            StringWriter w = new StringWriter();
            Velocity.evaluate(context, w, "velocityscript", script);
        } else if (language.equals("groovy")) {
            boolean groovySafeMode = Boolean.valueOf(SWAMP.getInstance().getProperty("GROOVY_SECURE_MODE", "false")).booleanValue();
            if (groovySafeMode) {
                if (script.indexOf("import ") >= 0 || script.indexOf("de.suse.swamp") > 0)
                    throw new Exception("Found illegal import statement in script.");
            }
            groovyShell.parse(script);
        } else {
            throw new Exception("Unsupported scripting language " + language);
        }  
    }
    

    private static GroovyShell getGroovyShell() {
        if (groovyShell == null) {
            Binding binding = new Binding();
            groovyShell = new GroovyShell(binding);
            Logger.DEBUG("Initiated new groovy shell.");
        }
        return groovyShell;
    }
    
}
