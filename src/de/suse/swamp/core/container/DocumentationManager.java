/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt@suse.de>
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


import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

/**
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *
 * The SWAMP Doku-manager helps with the following points:
 *  - get a doku item (from DB or Filesystem)
 *  - add new Items
 */

public final class DocumentationManager {

    private static DocumentationManager dokuManager = null;
    private String docuLocation;
    
    private DocumentationManager() {
        docuLocation = SWAMP.getInstance().getProperty("DOCU_LOCATION");
    }


    /**
     * @return - all available helpfiles.
     * HashMap helps: key = path to the folder of helpfiles (relative from configured DOCU_LOCATION)
     *                value = ArrayList of ContextHelp objects
     */
    public HashMap getAvailableHelp(){
        HashMap helps = this.getAvailableHelp("workflows", new HashMap());
        helps.putAll(this.getAvailableHelp("help", new HashMap()));
        return helps;
    }
    
    

      /**
     * @param helpPath - path relative to DOCU_LOCATION
     */
    private HashMap getAvailableHelp(String helpPath, HashMap helpMap) {
        // add files
        File helpPathFile = new File(getDocuLocation() + 
                System.getProperty("file.separator") + helpPath);
        if (helpPathFile.exists()) {
           File[] helpFilesArr = helpPathFile.listFiles();
           ArrayList helpfiles = new ArrayList(); 
            for (int i = 0; i < helpFilesArr.length; i++) {
                //Logger.DEBUG("Checking doc location: " + helpFilesArr[i].getAbsolutePath());
                if (helpFilesArr[i].getName().equals("head")){
                      helpMap.put(helpPath, helpfiles);
                      
                } else if (helpFilesArr[i].isFile() ) {
                    String filename = (helpPath + System.getProperty("file.separator") + 
                            helpFilesArr[i].getName());
                    
                    // fix for windows separators in regexps
                    String fs = System.getProperty("file.separator");
                    if (fs.equals("\\")) fs = "\\\\";
                    
                    try {
                        helpfiles.add(DocumentationManager.getInstance().
                                retrieveContextHelp(filename.replaceAll(fs,".")));
                    } catch (StorageException e) {
                        Logger.ERROR("Help for " + filename + " not found!");
                    }
                } else if (helpFilesArr[i].isDirectory()) {
                    //Logger.DEBUG("Scanning doc-dir: " + helpFilesArr[i].getAbsolutePath());
                    helpMap = getAvailableHelp(helpPath + System.getProperty("file.separator")
                            + (helpFilesArr[i]).getName(), helpMap);
                }
            }

        } else {
            Logger.ERROR("Invalid Doc-Path: " + helpPathFile.getAbsolutePath());
        }
        return helpMap;
    }
    
    
    

      /**
     * @param helpContext - path to the help file 
     * relative from SWAMPs DOCU_LOCATION, "/" replaced with "."
     */
    public ContextHelp retrieveContextHelp(String helpContext)
            throws StorageException {
        return StorageManager.loadContextHelp(helpContext);
    }
    
      
     public void storeContextHelp(ContextHelp help) throws StorageException {
         StorageManager.storeContextHelp(help);
     }
      
      
      
    /**
     * @return Returns the docuLocation.
     */
    public String getDocuLocation() {
        return this.docuLocation;
    }
    
    
    
    /**
     * static sub to get an DocumentationManager
     * @return the static DocumentationManager
     */
    public static DocumentationManager getInstance() {
        if (dokuManager == null) {
            dokuManager = new DocumentationManager();
        }
        return dokuManager;
    }
    

}
