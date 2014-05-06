/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt
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


/**
 * Providing URL shortcuts for webswamp: 
 * - <host>/webswamp/task/xx
 * - <host>/webswamp/wf/xx
 * - <host>/webswamp/workflow/xx
 *
 * @author Thomas Schmidt
 * @version $Id$
 *
 */
import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import de.suse.swamp.util.*;

public class UrlShortcut extends HttpServlet {

    
    public void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        // check the url where the user wants to go: 
        String path = request.getRequestURI();
        Logger.DEBUG("Analyzing URL: " + path);
        StringTokenizer tok = new StringTokenizer(path, "/");
        String targetPage = null;
        int targetId = 0;
        
        /* target urls look like this: 
         * http://127.0.0.1:8080/webswamp/swamp/template/DisplayTask.vm/taskid/1
         * http://127.0.0.1:8080/webswamp/swamp/template/DisplayWorkflow.vm/workflowid/1
         */
        
        while(tok.hasMoreElements()){
            String token = tok.nextToken();
            if (token.equals("task")){
                targetPage = "DisplayTask.vm/taskid";
            } else if (token.equals("wf") || token.equals("workflow")){
                targetPage = "DisplayWorkflow.vm/workflowid";
            } else {
                try {
                    targetId = new Integer(token).intValue();
                } catch (Exception e) {
                    //Logger.ERROR("Cannot create shortcut from Token: " + token);
                }
            }
        }
        
        if (targetPage != null && targetId > 0){
            response.sendRedirect("/webswamp/swamp/template/" + targetPage + "/" + targetId);
        } else {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("Cannot create shortcut URL from: " + path);
            out.println("Syntax is (xx is the ID): ");
            out.println("<host>/webswamp/task/xx or ");
            out.println("<host>/webswamp/wf/xx or ");
            out.println("<host>/webswamp/workflow/xx or ");
        }
    
    }
    
    
}
