/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt <tschmidt [at] suse.de>
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

package de.suse.swamp.core.util;

import java.text.*;
import java.util.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.data.datatypes.*;
import de.suse.swamp.core.history.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

/**
 * Helper Class for mapping Workflow properties into the Data Namespace of a
 * Workflow.
 * 
 * Implemented values: 	<i>nodename </i>.dueDate - the dueDate of that Node
 * 						<i>nodename </i>.enterDate - the date it was entered the last time)
 * 
 * TODO: - provide Datasets for this Databits. - show this sets also in Workflow
 * view
 *  
 */

public class DataToWfPropertyMapper {

    public static ArrayList getAllPropertyPaths(WorkflowTemplate wf) {
        ArrayList paths = new ArrayList();
        for (Iterator it = wf.getAllNodeTemplates().values().iterator(); it.hasNext();) {
            NodeTemplate node = (NodeTemplate) it.next();
            if ( node.hasDueDate() ) {
            	paths.add("System." + node.getName() + ".dueDate");
            }
            paths.add("System." + node.getName() + ".enterDate");
        }
        return paths;
    }

    
    public static String nodeForDueDate(String path) {
        String[] elements = path.split("\\.");
        if (elements.length != 3)
            return new String();
        String first = elements[0];
        String node = elements[1];
        String last = elements[2];
        if (!first.equals("System") || !last.equals("dueDate"))
            return new String();
        else
            return node;
    }
    
    
    public static Databit getValueForWorkflow(Workflow wf, String path) {
        Databit databit = null;

        StringTokenizer st = new StringTokenizer(path, ".");
        if (st.countTokens() == 0) {

            Logger.ERROR("No Data Property path given.");

        } else if (st.countTokens() == 1) {

            Logger.ERROR("No Data Property available for path " + path);

        } else if (st.countTokens() == 2) {

            String nodeName = st.nextToken();
            String property = st.nextToken();

            Node node = wf.getNode(nodeName);

            if (node != null) {
                if (property.equals("dueDate")) {
                	if (node.hasDueDate()) {
                		databit = wf.getDatabit( node.getTemplate().getDueDateReference() );
					}
                } else if (property.equals("enterDate")){
                    ArrayList list = HistoryManager.getHistoryEntries(wf.getId(), 
                            node.getId(), "NODE_ENTER");
                    databit = DataManager.createDatabit("nodeEnter", "node activation time", "datetime", 
                            "", Data.READONLY);
                    if (list != null && list.size() > 0){
                        HistoryEntry entry = (HistoryEntry) list.get(list.size()-1);
                            databit = DataManager.createDatabit("nodeEnter", "node activation time", "datetime", 
                                    entry.getWhenString(), Data.READONLY);
                    }
                }
            } else {
                Logger.ERROR("Node " + nodeName + " not found in Wf " + wf.getId());
            }
        }
        return databit;
    }

    
    
    
    
    
    public static DatabitTemplate getValueForWorkflow(WorkflowTemplate wf, String path) {
        DatabitTemplate databitTemplate = null;

        StringTokenizer st = new StringTokenizer(path, ".");
        if (st.countTokens() == 0) {

            Logger.ERROR("No Data Property path given.");

        } else if (st.countTokens() == 1) {

            Logger.ERROR("No Data Property available for path " + path);

        } else if (st.countTokens() == 2) {

            String nodeName = st.nextToken();
            String property = st.nextToken();

            NodeTemplate node = wf.getNodeTemplate(nodeName);

            if (node != null) {
                if (property.equals("dueDate")) {
                	if (node.hasDueDate()) {
                		databitTemplate = wf.getDatabitTemplate( node.getDueDateReference() );
                		if (databitTemplate.getShortDescription() == null){
                            databitTemplate.setShortDescription("dueDate for " + nodeName);
                        }
                	}
                } else if (property.equals("enterDate")) {
                    databitTemplate = new DatabitTemplate(property, "System databit for " + 
                    		property + " of node " + nodeName, "datetime", Data.READONLY);
                    databitTemplate.setShortDescription("enterDate of " + nodeName);
                }
            } else {
                Logger.ERROR("Node " + nodeName + " not found in WfTemplate " + wf.getName());
            }
        }

        return databitTemplate;
    }
    
    
    
    
    public static void setValueForWorkflow(SystemDatabit dbit) {
        String value = dbit.getValue();
        String path = dbit.getPath().substring(7);

        StringTokenizer st = new StringTokenizer(path, ".");
        if (st.countTokens() == 0) {

            Logger.ERROR("No Data Property path given.");

        } else if (st.countTokens() == 1) {

            Logger.ERROR("No Data Property available for path " + path);

        } else if (st.countTokens() == 2) {

            String nodeName = st.nextToken();
            String property = st.nextToken();

            Node node = dbit.getWf().getNode(nodeName);

            if (property.equals("dueDate")) {

                if (value == null || value.equals("")) {
                    node.setDueDate(null);
                } else {
                    DateFormat df = new SimpleDateFormat(dateDatabit.dateFormat);
                    try {
                        node.setDueDate(df.parse(value));
                    } catch (ParseException e) {
                        Logger.ERROR("Cannot parse dateString: " + value);
                    }
                }

            } else {

                Logger.ERROR("Unknown System path " + path);

            }

        }
    }

}