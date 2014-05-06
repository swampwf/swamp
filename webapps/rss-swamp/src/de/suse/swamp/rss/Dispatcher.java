/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2007 Thomas Schmidt
 * Copyright (c) 2007 Novell Inc.
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
package de.suse.swamp.rss;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.rss.actions.*;
import de.suse.swamp.util.*;


public class Dispatcher extends HttpServlet {
	
	public void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
	            
        SWAMP swamp = SWAMP.getInstance();
        String swampLink = swamp.getProperty("appLink");
        
        //check requested output
        String format = request.getParameter("format");
        
        // check requested action: 
        String action = request.getParameter("action");
        
        try {
            if (action == null) {
                response.sendRedirect("README");
            } else if (action.equalsIgnoreCase("mytasks")) {
                ActionIface actionClass = new MyTasks();
                String title = "SWAMP MyTasks";
                OutputFormatter.format(response, actionClass.getItems(request), title, swampLink, title,format); 
            } else if (action.equalsIgnoreCase("query")) {
                ActionIface actionClass = new Query();
                String title = "SWAMP Query";
                OutputFormatter.format(response, actionClass.getItems(request), title, swampLink, title,format);
            } else {
                response.sendRedirect("README");
            }
        } catch (Exception e) {
            Logger.ERROR("Error in RSS generation: " + e.getMessage());
            e.printStackTrace();
            OutputFormatter.format(response, e);
        }
        
	}
}

	