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
import java.util.*;

import javax.servlet.http.*;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;


public class OutputFormatter {
	
    private static String defaultFormat = "rss_2.0";
    private static int maxAmount = 150;
    
    public static void format(HttpServletResponse response, List items, String title, 
            String link, String desc, String type) throws Exception {

        // build response
        response.setContentType("application/xml; charset=UTF-8");
        // check for valid type: 
        if (type == null) {
            type = defaultFormat;
        } else if (type.indexOf("rss") >= 0) {
            type = "rss_2.0";
        } else if (type.indexOf("atom") >= 0) {
            type = "atom_0.3";
        } else {
            type = defaultFormat;
        }
        
        if (items.size() > maxAmount)
            items = items.subList(0, maxAmount - 1);
        
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(type);

        feed.setTitle(title);
        feed.setLink(link);
        feed.setDescription(desc);

        feed.setEntries(items);
        SyndFeedOutput output = new SyndFeedOutput();
        output.output(feed,response.getWriter());
    }
    
    
    
    public static void format(HttpServletResponse response, Exception e) {
        try {
            response.setContentType("text/plain; charset=UTF-8");
            PrintWriter output = response.getWriter();
            output.println("RSS generation error: " + e.getMessage() + "\n");
            e.printStackTrace(output);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
}

	