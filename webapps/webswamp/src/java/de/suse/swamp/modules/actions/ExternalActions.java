/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Thomas Schmidt
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
import java.util.*;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.container.SecurityManager;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.modules.screens.*;
import de.suse.swamp.util.*;
import de.suse.swamp.webswamp.*;

/**
 * This action provides a XML interface to SWAMP. 
 * Please use the SOAP interface which is far more powerful.
 */
public class ExternalActions extends SecureAction {
    
    org.apache.log4j.Logger log = org.apache.log4j.Logger
        .getLogger(ExternalActions.class.getName());
    
    
    public void doPerform(RunData data, Context context) throws Exception {
    }
    
    
    public void doSendevent(RunData data, Context context) throws Exception {
    	try {
			String error = "";
			String username = data.getUser().getName();

			String eventtype = data.getParameters().get("etype");
			int wfid = data.getParameters().getInt("wfid");
			Logger.DEBUG("Received External Event :"
					+ data.getParameters().toString(), log);

			if (eventtype == null || eventtype.equals("")) {
				error += "\n- Please provide a valid Eventtype.";
			}

			// getting optional userid:
			username = data.getParameters().getString("username", username);
            username = data.getParameters().getString("user", username);

			WorkflowAPI wfapi = new WorkflowAPI();
			EventAPI eventapi = new EventAPI();
			Workflow wf = wfapi.getWorkflow(wfid, username);
			context.put("workflow", wf);
			if (wf == null) {
				error += "\n- Please provide a valid Workflow-ID.";
			}

			if (username != null && username.length() > 0) {
				try {
					SecurityManager.getUser(username);
				} catch (Exception e) {
					error += "Invalid username " + username;
				}
			}

			if (error.equals("")) {
				try {
					Logger.DEBUG("Received external Event " + eventtype
							+ " for Workflow #" + wfid, log);
					Event ev = new Event(eventtype, 0, wfid);
					ArrayList events = new ArrayList();
					events.add(ev);
					eventapi.sendEvents(events, username, new ResultList());
					context.put("workflowid", new Integer(wfid));
				} catch (Exception e) {
					error += "\n -" + e.getMessage();
				}
			}

			if (!error.equals("")) {
				// FIXME: Mapping ERROR to Errornumber must happen here
				doSendXMLOutput(data, "1", error);
			} else {
				doSendXMLOutput(data, "0", "Your Event was sent");
			}
		} catch (Exception e) {
            Logger.ERROR("Internal Error: " + e.getMessage());
            e.printStackTrace();
            doSendXMLOutput(data, "1", "Internal Error: " + e.getMessage());
		}
	}
    
    
    /**
	 * Sends a XML Page to the external System
	 * 
	 * @param data
	 * @param error
	 * @throws Exception
	 */
    public static void doSendXMLOutput(RunData data, String code, String msg) 
        throws Exception {
        String hoststring = WebSWAMP.appLink;
        
        if (!code.equals("0")){
            Logger.ERROR("External action got error: " + msg);
        }
        
        // avoid processing of ScreenTemplate with permission checks etc.
        data.declareDirectResponse();
        data.setLayout("DirectResponseLayout");
        PrintWriter out = data.getResponse().getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
        out.println("<!DOCTYPE workflow SYSTEM \"" + hoststring
                + "/conf/dtds/externalaction.dtd\">");
        out.println("<swampxmlresponse>");
        out.println("	<errorcode>" + code + "</errorcode>");
        out.println("	<msg>" + msg + "</msg>");
        out.println("</swampxmlresponse>");
        out.flush();
        out.close();
    }
        
    
    /** Writing data directly to Databits that are given in 
     *  HTTP Parameters.
     * @param data
     * @param context
     * @throws Exception
     */
    public void doSenddata(RunData data, Context context) throws Exception {

		Logger.DEBUG("Received External Data :"
				+ data.getParameters().toString(), log);
		try {
			DatapackActions dAction = new DatapackActions();
			dAction.doSavedatapack(data, context);

			if (context.get("statusclass").equals("error")) {
				doSendXMLOutput(data, "1", (String) context
						.get("statusmessage"));
			} else {
				doSendXMLOutput(data, "0", (String) context
						.get("statusmessage"));
			}
		} catch (Exception e) {
            Logger.ERROR("Internal Error: " + e.getMessage());
            e.printStackTrace();
            doSendXMLOutput(data, "1", "Internal Error: " + e.getMessage());
		}
	}
    
    
    /** Reading the subworkflow-ids of a workflow
     */
    public void doGetchildids(RunData data, Context context) throws Exception {
        log.debug("Received External Data :" + data.getParameters().toString());
        try {
            String username = data.getUser().getName();
            WorkflowAPI wfapi = new WorkflowAPI();
            int wfid = data.getParameters().getInt("workflowid");
            Workflow wf = wfapi.getWorkflow(wfid, username);
            SWAMPHashSet wfs = null;
            if (data.getParameters().containsKey("isRunning")) {
                boolean running = data.getParameters().getBoolean("isRunning");
                wfs = new SWAMPHashSet(wf.getSubWorkflows(running));
            } else {
                wfs = new SWAMPHashSet(wf.getSubWorkflows());
            }
            if (wfs.size() > 0) {
                StringBuffer ids = new StringBuffer();
                int count = 0;
                for (Iterator it = wfs.iterator(); it.hasNext();) {
                    if (count > 0)
                        ids.append(",");
                    ids.append(((Workflow) it.next()).getId());
                    count++;
                }
                doSendXMLOutput(data, "0", ids.toString());
            } else {
                doSendXMLOutput(data, "1", "Workflow #" + wfid + " has no subworkflows attached!");
            }
        } catch (Exception e) {
            doSendXMLOutput(data, "1", "Internal Error: " + e.getMessage());
        }
    }
    
    
    /** Reading the parent Workflow id of a workflow
     */
    public void doGetparentid(RunData data, Context context) throws Exception {
		log.debug("Received External Data :" + data.getParameters().toString());
		try {
			String username = data.getUser().getName();
			WorkflowAPI wfapi = new WorkflowAPI();
			int wfid = data.getParameters().getInt("workflowid");
			Workflow wf = wfapi.getWorkflow(wfid, username);
			int id = wf.getParentwfid();
			if (id == 0){
				doSendXMLOutput(data, "1", "Workflow #" + wfid + " has no parent workflow!");
			} else {
				doSendXMLOutput(data, "0", String.valueOf(id));
			}
		} catch (Exception e) {
			doSendXMLOutput(data, "1", "Internal Error: " + e.getMessage());
		}
	}
    
    
    /**
     * get a list of workflow ids, filtered 
     * by the provied strings
     */
    public void doGetworkflowidlist(RunData data, Context context)
        throws Exception {
        Logger.DEBUG("XML request for wfid-list:" + data.getParameters().toString());
        try {
            String uname = data.getUser().getName();
            WorkflowAPI wfApi = new WorkflowAPI(); 
            HashMap filters = Workflows.parameterParserToHashMap(data.getParameters());
            ArrayList ids = new ArrayList(wfApi.getWorkflowIds(filters, null, uname));
            String idString = "";
            for (Iterator it = ids.iterator(); it.hasNext(); ){
                if (!idString.equals("")) idString += ", ";
                idString += (Integer) it.next();
            }
            doSendXMLOutput(data, "0", idString);
        } catch (Exception e) {
            doSendXMLOutput(data, "1", "Internal Error: " + e.getMessage());
        }
    } 
    
        
     /**
	 * Reading data directly from Dataset HTTP Parameters.
	 * @param data
	 * @param context
	 * @throws Exception
	 */
    public void doGetdata(RunData data, Context context) throws Exception {

        Logger.DEBUG("External Data-read:" + data.getParameters().toString().replaceAll("\n", " "),
				log);

		try {
			if (data.getParameters().containsKey("workflowid")
					&& data.getParameters().containsKey("databitname")) {

				String username = data.getUser().getName();
				WorkflowAPI wfapi = new WorkflowAPI();
				Workflow wf = wfapi.getWorkflow(data.getParameters().getInt(
						"workflowid"), username);
				if (wf != null) {
					Databit dbit = null;
					try {
						dbit = wf.getDatabit(data.getParameters().get(
								"databitname"));
					} catch (Exception e) {
						Logger.ERROR("External Request for Databit: "
								+ data.getParameters().get("databitname")
								+ ". No such Databit.", log);
					}
					if (dbit != null) {
						doSendXMLOutput(data, "0", dbit.getValue());
					} else {
						doSendXMLOutput(data, "1", "Please provide a valid "
								+ " var databitname.");
					}
				} else {
					doSendXMLOutput(data, "1", "Please provide a valid "
							+ " var workflowid.");
				}
			} else {
				doSendXMLOutput(data, "1", "Please provide the vars "
						+ "workflowid and databitname.");
			}
		} catch (Exception e) {
			doSendXMLOutput(data, "1", "Internal Error: " + e.getMessage());
		}
	}
}
