/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh [at] suse.de>
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

package de.suse.swamp.util;

 /**
  * Basic logging functionality
  *
  * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
  * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
  * @version $Id$
  *
  */


public class Logger {

    public static org.apache.log4j.Logger log = org.apache.log4j.Logger
        .getLogger(Logger.class.getName());

    
    /**
     * Use BUG to indicate "this line should never be executed, programming
     * error somewhere"
     */
    public static void BUG(String message) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(Logger.class.getName());
        Logger.BUG(message, log);
    }
    
    
    public static void BUG(String message, org.apache.log4j.Logger log) {
        log.fatal("************************************************");
        log.fatal(message);
        log.fatal("************************************************");
    }

    /**
     * Use this for standard error messages
     */
    public static void ERROR(String message) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(Logger.class.getName());
        Logger.ERROR(message, log);
    }
    
    public static void ERROR(String message, org.apache.log4j.Logger log) {
        log.error("************************************************");
        log.error(message);
        log.error("************************************************");
    }

    
    
    /**
     * Use this for warnings
     */
    public static void WARN(String message) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Logger.class.getName());
        Logger.WARN(message, log);
    }
    
    public static void WARN(String message, org.apache.log4j.Logger log) {
        log.warn("#!!# " + message);
    }
    
    
    
    /**
     * Use this for events that really should be logged for posterity
     */
    public static void LOG(String message) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Logger.class.getName());
        Logger.LOG(message, log);
    }
    
    public static void LOG(String message, org.apache.log4j.Logger log) {
         log.info(message);
    }

    /**
     * Use this for debugging
     */
    public static void DEBUG(String message) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger(Logger.class.getName());
        Logger.DEBUG(message, log);
    }


    public static void DEBUG(String message, org.apache.log4j.Logger log) {
        log.debug(message);
    }
    
    
    
    
    
}
