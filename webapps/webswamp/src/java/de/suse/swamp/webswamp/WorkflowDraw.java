/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Doris Baum <dbaum@suse.de>
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

import de.suse.swamp.core.workflow.*;
import de.suse.swamp.core.container.*;
import de.suse.swamp.core.actions.*;
import de.suse.swamp.util.*;

import java.io.*;
import java.util.*;

/**
 * A Singleton to draw workflows and cases. The WorkflowDraw uses the AT&T dot
 * format to output graph descriptions and then draws the graphs using dot.
 * Note: This class generates a dependency to the programs
 * "dot" and "convert" which can be found in the packages
 * "graphviz" and "imagemagick" of SUSE LINUX.
 *
 * @author  Doris Baum &lt;dbaum@suse.de&gt;
 * @author  tschmidt
 * @version $Id$
 */

public class WorkflowDraw {

    /**
     * Constructor for WorkflowDraw.
     * Initializes the graphicsPath with the value provided by
     * SWAMP.getGraphTempLocation(), adds a trailing slash if
     * necessary
     */
    private WorkflowDraw() {
        SWAMP swamp = SWAMP.getInstance();
        String path = swamp.getGraphLocation();
        path = path.replaceAll("/$", "");
        graphicsPath = path + "/";
    }

    /**
     * Returns the global WorkflowDraw object. Use this to
     * access the WorkflowDraw;
     * @return reference to the global WorkflowDraw object.
     */
    public static WorkflowDraw getWorkflowDraw() {
        if (workflowDraw == null) {
            workflowDraw = new WorkflowDraw();
        }
        return workflowDraw;
    }


    /**
     * Draws a diagram of the workflow workflow.
     * Returns a string with the path and filename of the
     * diagram drawn.
     * @param  workflow   the workflow to be drawn
     * @return the path and filename of the diagram file
     */
    public synchronized String drawWorkflow(Workflow workflow, String size)
        throws Exception {

        //debug
        int wfid = workflow.getId();
        String wfname = workflow.getTemplateName();

        // set filename for png file to workflowname-workflowid.png
        // and filename for dot file to workflowname-workflowid.dot
        String filePrefix = (wfname + wfid);
        String graphicsFilename = null;

        if (size == null){
            graphicsFilename = graphicsPath + filePrefix + ".png";
        } else {
            graphicsFilename = graphicsPath + filePrefix + "-" +
            new String(size).replaceAll(" ", "_").replaceAll(",", "_") + ".png";
        }

        String dotFilename = graphicsPath + filePrefix + ".dot";
        // open the dot file to write dot definition of workflow
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(dotFilename), "UTF-8" );
        os.write( getDotSource(workflow, size, filePrefix, false).toString() );
        os.close();

        // call dot to draw the graph
        callDot(graphicsFilename, dotFilename);
        return graphicsFilename;
    }






    private synchronized StringBuffer getDotSource(Workflow workflow, String size, String name,
            boolean isSubGraph) throws Exception {

        //debug
        int wfid = workflow.getId();

        // fetch the nodes and the active nodes from the workflow
        // and prepare an iterator for the nodes
        ArrayList nodes = workflow.getAllNodes();
        Iterator nodeIterator = nodes.iterator();

       //replace "-" with "_" due to graphviz
        name = name.replace('-', '_');

        StringBuffer dotScript = new StringBuffer();

        // name the graph like the file
        if (!isSubGraph){
            dotScript.append("digraph " + name + " {\n");
            //dotScript.append("concentrate=true;");
            dotScript.append("graph [rankdir=TB, pack=false, nodesep=\".3333\", ranksep=\".3333\"];");
            dotScript.append("node[shape=box, color=bisque4, style=\"setlinewidth(2),rounded\", fontname=\"Arial\", fontsize=12, fontcolor=grey10];");
            // rounded boxes for states
            dotScript.append("edge[color=antiquewhite4, style=\"setlinewidth(2)\", fontname=\"Courier\", fontcolor=grey10, fontsize=10];");

        } else {
            dotScript.append("subgraph cluster_" + name +
                    " {\n label = \"" + name + "\"; \n");
            dotScript.append("concentrate=false; \n");
            dotScript.append("concentrate=false; \n");
            dotScript.append("overlap=false; \n");
        }



        if (size != null && !isSubGraph){
            dotScript.append("size=\"" + size + "\";\n");
        }

       /*
        fileWriter.write("agendabox [shape=box, label="
        + "\"Graph Agenda: \\n- Black parts of the graph are currently not "
        + "active\\n- Active Nodes are colored green.\\n- Edges that are "
        + "waiting for its conditions to be fulfilled are colored green.\\n"
        + "- Conditions that are still waiting for a certain event are "
        + "colored red.\", labeljust=l];");
        */
        // walk over all nodes and write their edges to the dot file
        while (nodeIterator.hasNext()) {
            Node node = (Node) nodeIterator.next();
            ArrayList edges = node.getEdges();

            if (edges != null) {
                for (Iterator edgeIt = edges.iterator(); edgeIt.hasNext();) {
                    Edge edge = (Edge) edgeIt.next();
                    dotScript.append("\t" + name + "_" + edge.getFrom().getName().replace('-', '_')
                            + " -> " + name + "_" + edge.getToNode().getName().replace('-', '_'));
                    String label = (edge.getCondition().getEventString().equalsIgnoreCase("none") ?
                            "" : edge.getCondition().getEventString().replaceAll("\\\\n","\\\\l"));
                    dotScript.append(" [label=\"" + label + "\\l\"");

                    // if the node is active, colourize its edges
                    if (node.isActive()) {
                        dotScript.append(",color=olivedrab2,style=bold");

                        // should never be the case...
                        if (edge.getCondition().evaluate()) {
                            dotScript.append(",fontcolor=olivedrab2");
                        } else {
                            dotScript.append(",fontcolor=maroon4");
                        }
                    }
                    dotScript.append("];\n");
                }
            }

            // print replacedDesc if running Wf, normalDesc if Template
            String nodeDesc;
            if (wfid > 0){
                nodeDesc = node.getReplacedDescription().replaceAll("\\\"", "");
            } else {
                nodeDesc = node.getTemplate().getDescription().replaceAll("\\\"", "");
            }




            // print Description for all nodes:
            dotScript.append("\t" + name + "_" + node.getName().replace('-', '_') +
                        " [label=\"");

            // mark as Milestone node
            if (node.getMileStone() != null){
                dotScript.append("Milestone: \\n" +
                        node.getMileStone().getDescription());
            } else {
                dotScript.append(nodeDesc);
            }
            dotScript.append("\"");


            if (node.getActionsTemplates() == null) {
                dotScript.append(", label=\"!!!!Node template missing!!!!\", color=red, style=bold");
                dotScript.append("];\n");
                continue;
            }

           // mark bold if UserActions inside:
            for (Iterator it = node.getActionsTemplates().iterator(); it.hasNext(); ){
                ActionTemplate action = (ActionTemplate) it.next();
                if (action instanceof UserActionTemplate) {
                    dotScript.append(", style=bold");
                    break;
                }
            }

            // if the node is active, colourize the node itself
            if (node.isActive()) {
                dotScript.append(", color=olivedrab2");
            }
            if (node.isStartNode() || node.isEndNode()) {
                dotScript.append(", color=darkslategrey");
            }
            dotScript.append("];\n");


            for (Iterator actionIt = node.getActionsTemplates().iterator();
                actionIt.hasNext(); ){
                ActionTemplate temp = (ActionTemplate) actionIt.next();
                if (temp.getType().equals("StartSubWorkflowAction")){
                    StartSubworkflowActionTemplate action  =
                            (StartSubworkflowActionTemplate) temp;
                    WorkflowTemplate subwftemp = WorkflowManager.getInstance().
                        getWorkflowTemplate(action.getSubname());
                    Workflow subWf = subwftemp.getWorkflow();
                    // avoid circular subworkflow graphs:
                    if (!subWf.getName().equals(workflow.getName())){
                    dotScript.append(getDotSource(subWf, null,
                            subwftemp.getName(), true));
                    // edge to startnode:
                    dotScript.append("\t" + name + "_" + node.getName().replace('-', '_')
                            + " -> " + (subwftemp.getName() + "_" + subWf.getStartNode().getName()).replace('-', '_'));
                    dotScript.append(" [label=\"\"]; \n");
                    }
                }
             }
        }

        // close the graph
        dotScript.append("}\n");
        return dotScript;
    }

    /**
     * This method calls the dot program to draw the graph specified
     * in the file dotFilename. dot must be accessible on the host
     * running the swamp application.
     * @param  graphicsFilename the name of the file where the graph
     *                          will be put
     *         dotFilename the name of the dot file where the graph
     *                     is specified
     * @return the path and filename of the diagram file
     */
    private void callDot(String graphicsFilename, String dotFilename)
            throws Exception {
        Logger.LOG("Drawing graph to " + graphicsFilename);
        // Set command to execute dot
        String command[] = { "dot", "-Tpng", "-o", graphicsFilename,
                dotFilename };
        // Execute command
		try {
			Runtime.getRuntime().exec(command).waitFor();
		} catch (IOException e) {
			throw new IOException("Exception: " + e.getMessage() + ". Please " +
					"make sure to have the program \"dot\" from the graphviz package installed.");
		}
    }

    private static String graphicsPath;
    private static WorkflowDraw workflowDraw;
}
