/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt
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

import java.io.*;

import javax.servlet.http.*;

import net.sf.jmimemagic.*;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.util.*;

/**
 * @author tschmidt
 *
 * Servlet acts as a proxy for downloads for attched files 
 * to prevent direct access to the file directory and 
 * do permission checks.
 */

public class FileDownload extends SecureAction {
    
    
    public void doPerform(RunData data, Context context) throws Exception {
        super.doPerform(data, context);

        DataAPI dataApi = new DataAPI();
        
        int wfid = data.getParameters().getInt("wfid");
        String path = data.getParameters().get("path");
        String username = data.getUser().getName();
        
        Databit dbit = dataApi.doGetDataBit(wfid, path, username);
        String fileDir = new SWAMPAPI().doGetProperty("ATTACHMENT_DIR", username);
        String fs = System.getProperty("file.separator");
        String filename = fileDir + fs + dbit.getId() + "-" + dbit.getValue(); 
        
        if (!dbit.getType().equalsIgnoreCase("fileref")){
            throw new Exception("Databit: " + path + " does not contain a file.");
        }
        
        File file = new File(filename);
        if (!file.exists()){
            Logger.ERROR("File: " + filename + " not found.");
            throw new Exception("File: " + dbit.getValue() + " not found.");
        }
        
        // avoid processing of ScreenTemplate with permission checks etc.
        data.declareDirectResponse();
        data.setLayout("DirectResponseLayout");
        HttpServletResponse response = data.getResponse();
        
        String contentType = "application/binary";
        try {
            // Set the headers, evaluate the mimetype.
            MagicMatch match = Magic.getMagicMatch(file, true);
            contentType = match.getMimeType();
        } catch (Exception e) {
            log.debug("Wasn't able to get contettype from " + file.getName() + ", using default application/binary");
        }
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=" + dbit.getValue());

        // Send the file.
        OutputStream out = response.getOutputStream();
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[4 * 1024]; // 4K buffer
        int bytesRead;
        while ((bytesRead = is.read(buf)) != -1) {
            out.write(buf, 0, bytesRead);
        }
        is.close();
        out.flush();
        out.close();
    }
    
    
}
