/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2003 Sonja Krause-Harder <skh@suse.de>
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

/**
 * WorkflowReader - read in xml and give back workflow-template objects. 
 * If you change the DTD, change the WorkflowHandler (an inner
 * class defined below) in this file.
 *
 * The WorkflowReader constructs a compound WorkflowTemplate object
 * (consisting of nodes, edges, conditions, actions and datapacks) for every
 * workflow description it reads. These templates are managed by
 * de.suse.swamp.core.container.WorkflowManager and create an actual Workflow
 * object to track a workflow.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 *
 */

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import de.suse.swamp.core.actions.*;
import de.suse.swamp.core.conditions.*;
import de.suse.swamp.core.data.*;
import de.suse.swamp.core.security.roles.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;
import de.suse.swamp.util.*;

class WorkflowXMLReader {

    private XMLReader parser;
	private WorkflowTemplate workflowTempl;

    /* Edges, Nodes and Actions can't be nested */
    private EdgeTemplate pendingEdgeTempl = null;
    private NodeTemplate pendingNodeTempl = null;
    private MileStoneTemplate pendingMileStoneTempl = null;
    private ActionTemplate pendingActionTempl = null;
    private FieldTemplate pendingFieldTempl = null;
    
    /* We can only assign the 'to' nodes to the edges when we have read in
     * all the nodes. So we keep temporary data structures until we have read in
     * the complete description. Final assembly of the WorkflowTemplate object
     * happens in the endDocument() method of the WorkflowHandler class */

    /* Conditions can be nested, thus we need a stack to remember them. */
    private Stack condStack = new Stack();

    // PCDATA is collected in a stack as well...
    private Stack pcdataStack = new Stack();

    // Stack to keep the current element (we like to call setDescription() on
    // all of them...
    private Stack componentStack = new Stack();

    /* A Workflow contains one "root"-Dataset with one or more 
     * Datasets and -Bits*/
    private DatasetTemplate rootDatasetTempl = null;
    // Stack for Datasets
    private Stack currDatasetTempl = new Stack();

    private DatabitTemplate currDatabitTempl = null;
    // role stuff
    private WorkflowRole currRole = null;
    
    // for enum values of a databit
    private ArrayList enumvalues = new ArrayList();	
    
    // extra logger for xml parsing
    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            WorkflowXMLReader.class.getName());
	
	
    WorkflowXMLReader() throws Exception {
        try {
            Logger.DEBUG("Creating new WorkflowXMLReader", log);
            parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser"); 
			// Validating XML Workflows with DTD File
            parser.setFeature("http://xml.org/sax/features/validation", true); 
            parser.setEntityResolver(new resolver());
        } catch (Exception e) {
            Logger.ERROR("WorkflowReader.java: couldn't create parser:\n" + e);
            throw new Exception("WorkflowReader.java: couldn't create parser:\n" + e);
        }
    }

    
    synchronized WorkflowTemplate readWorkflowDef(File file) throws FileNotFoundException, Exception {
        Logger.DEBUG("Start reading workflow def from " + file.getAbsolutePath(), log);
        try {
            WorkflowHandler handler = new WorkflowHandler();
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);
            parser.parse(file.getAbsolutePath()); 
        } catch (SAXParseException err) { 
            String errmsg = "** Parsing error" + ", line " + err.getLineNumber() + 
                ", uri " + err.getSystemId() + " " + err.getMessage();
            Logger.ERROR(errmsg);
            throw new Exception(errmsg);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(e.getMessage());
        } catch (Exception e) {
            String errmsg = "*** WorkflowReader.java: parser could not parse " + 
                    "file: " + file + "\n" + e;
            Logger.ERROR(errmsg);
            e.printStackTrace();
            throw e;
        }
        return workflowTempl;
    }

    
             
    /* make that an inner class so that we can access all these private fields
     * defined above to keep track of the parsing process */
    class WorkflowHandler extends DefaultHandler {

        /** 
         * Is called when the parser finds PCDATA within an element. This method
         * takes care that a nicely trimmed pcdata stack element is available
         * when endElement is called.
         */
        public void characters(char[] ch, int start, int length) throws SAXException {
            String pcdata = new String(ch, start, length);
            if (length > 0 && pcdata.trim().length() > 0){
                Logger.DEBUG("Reading characters: " + pcdata, log);
                /* Take pcdata of the same tag from stack and append pcdata from this
                   call to characters() */
                if (!pcdataStack.empty()) {
                    pcdata = (String) pcdataStack.pop() + pcdata;
                }
                /* Put new value for pcdata on stack */
                pcdataStack.push(pcdata);
                Logger.DEBUG("PCDATA is now: " + pcdata, log);
            }
        }

        public void startElement(String uri, String localName, String qName, 
                Attributes attributes) throws SAXException {

            pcdataStack.push("");
            Logger.DEBUG("Startelement: <" + qName + ">", log);

            // Workflow
            if (qName.equals("workflow")) {
                String name = "";
                String wfVersion = "";
                String leastSwampVersion = "";
                String parentWfVersion = null;
                String parentWf = null;
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("version")) {
                        wfVersion = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("leastSWAMPVersion")) {
                        leastSwampVersion = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("parentwf")) {
                        parentWf = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("parentwfversion")) {
                        parentWfVersion = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute " + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
            	workflowTempl = new WorkflowTemplate(name, wfVersion);
            	workflowTempl.setRequiredSWAMPVersion(leastSwampVersion);
                workflowTempl.setParentWfName(parentWf);
                workflowTempl.setParentWfVersion(parentWfVersion);
            	componentStack.push(workflowTempl);

            // All types of nodes
            } else if (qName.equals("node")) {

                String name = "";
                String type = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("type")) {
                        type = attributes.getValue(i);
                    }
                }
                
                pendingNodeTempl = new NodeTemplate(name, type);

                workflowTempl.addNodeTemplate(pendingNodeTempl);
                componentStack.push(pendingNodeTempl);

            // Add MileStones to nodes
            } else if (qName.equals("milestone")) {

                String name = "";
                int weight = 0;
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("weight")) {
                        weight = Integer.parseInt(attributes.getValue(i));
                    }
                }

              pendingMileStoneTempl = new MileStoneTemplate(name, weight); 
              pendingNodeTempl.setMileStoneTemplate(pendingMileStoneTempl);
              componentStack.push(pendingMileStoneTempl);
            } else if ( qName.equals("duedate") ) {
            	String databit = null;
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("databit")) {
                        databit = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                if ( databit != null )
                	pendingNodeTempl.setDueDateReference(databit);
                
                // Edges
            } else if (qName.equals("edge")) {
                String to = "";
                String event = "";
                String from = pendingNodeTempl.getName();
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("to")) {
                        to = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("event")) {
                        event = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }

                if (!event.equals("")) {
                    EventConditionTemplate condTempl = new EventConditionTemplate(new EventTemplate(event));
                    workflowTempl.addWaitingForEvents(event);
                    condStack.push(condTempl);
                    pendingEdgeTempl = new EdgeTemplate(from, to);
                } else {
                    pendingEdgeTempl = new EdgeTemplate(from, to);
                }

           // data conditions
            } else if (qName.equals("data")) { 
                String field = "";
                String check = "";
                String value = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("field")) {
                        field = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("check")) {
                        check = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("value")) {
                        value = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                DataConditionTemplate condTempl = new DataConditionTemplate(field, check, value);
                workflowTempl.addWaitingForEvents(Event.DATACHANGED);
                String fieldName = field; 
                if (fieldName.lastIndexOf(".") > 0)
                    fieldName = field.substring(field.lastIndexOf(".") + 1);
                workflowTempl.addListenerPath(fieldName);
                condStack.push(condTempl);
                
           // wait for Subworkflows finish onditions
            } else if (qName.equals("subsfinished")) { 
                String subname = "";
                String subversion = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("subname")) {
                        subname = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("subversion")) {
                            subversion = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                SubsFinishedConditionTemplate condTempl = new SubsFinishedConditionTemplate(subname, subversion);
                workflowTempl.addWaitingForEvents(Event.SUBWORKFLOW_FINISHED);
                condStack.push(condTempl);
                
           // Notifications
            } else if (qName.equals("notification")) {

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("msgtemplate")) {
                        msgtemplate = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("name")) {
                        notificationname = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("msgtext")) {
                        msgtext = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("x-reason")) {
                        xreason = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("subject")) {
                        subject = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }


                // recipients
            } else if (qName.equals("recipient") ) {      
                // Now create an Action for each recipient 
                String recipientname = "";
                String recipientemail = "";
                String dbit = "";
                String recipientrole = "";

                for (int i = 0; i < attributes.getLength(); i++) {
                    //if (attributes.getQName(i).equals("type")) {
                    //    type = attributes.getValue(i);
                    if (attributes.getQName(i).equals("recipientname")) {
                        recipientname = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("recipientemail")) {
                        recipientemail = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("dbit")) {
                        dbit = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("recipientrole")) {
                        recipientrole = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                
                pendingActionTempl = new NotifyActionTemplate(notificationname, msgtemplate, msgtext, 
                        xreason, recipientname, recipientemail, dbit, recipientrole, subject, 
                        pendingNodeTempl);
                // and now save into the nodeTempl
                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);
                
                
            // Event conditions
            } else if (qName.equals("event")) {
                String type = "";

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("type")) {
                        type = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                condStack.push(new EventConditionTemplate(new EventTemplate(type)));
                workflowTempl.addWaitingForEvents(type);


                // Actions: manualtask
            } else if (qName.equals("manualtask")) {
                String name = "";
                String role = null;
                String eventtype = "";
                String notificationTemplate = "";
                boolean mandatory = true;
                boolean restricted = false;

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("role")) {
                        role = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("eventtype")) {
                        eventtype = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals(
                            "notificationtemplate")) {
                        notificationTemplate = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("mandatory")) {
                        mandatory = Boolean.valueOf(attributes.getValue(i)).booleanValue();
                    } else if (attributes.getQName(i).equals("restricted")) {
                        restricted = Boolean.valueOf(attributes.getValue(i)).booleanValue();
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                pendingActionTempl = new ManualtaskActionTemplate(name, role, 
                        eventtype, pendingNodeTempl, notificationTemplate, mandatory, restricted);

                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);
                
                // Actions: customtask
            } else if (qName.equals("customtask")) {
                String name = "";
                String eventtype = "";
                String klasse = "";
                String function = "";

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("eventtype")) {
                        eventtype = attributes.getValue(i);
                    }  else if (attributes.getQName(i).equals("class")) {
                        klasse = attributes.getValue(i);
                    }  else if (attributes.getQName(i).equals("function")) {
                        function = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                pendingActionTempl = new CustomActionTemplate(name, pendingNodeTempl, eventtype, 
						klasse, function);

                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);

           // Action: StartSubworkflowAction
           } else if (qName.equals("startsubworkflow")) {
                String subname = "";
                String subversion = "";
                String name = "";

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("subname")) {
                        subname = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("subversion")) {
                        subversion = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                pendingActionTempl = new StartSubworkflowActionTemplate(name, 
                        subname, subversion, pendingNodeTempl);
                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);
                                
                // Actions: decision
            } else if (qName.equals("decision")) {
                String name = "";
                String role = null;
                String notificationTemplate = "";
                boolean mandatory = true;
                boolean restricted = false;

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("role")) {
                        role = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals(
                            "notificationtemplate")) {
                        notificationTemplate = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("restricted")) {
                        restricted = Boolean.valueOf(attributes.getValue(i)).booleanValue();
                    } else if (attributes.getQName(i).equals("mandatory")) {
                            mandatory = Boolean.valueOf(attributes.getValue(i)).booleanValue();
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                pendingActionTempl = new DecisionActionTemplate(name, role, pendingNodeTempl, 
                        notificationTemplate, mandatory, restricted);

                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);

                // Actions: answers to decision action
            } else if (qName.equals("answer")) {
                String eventtype = "";

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("eventtype")) {
                        eventtype = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                AnswerTemplate answerTempl = new AnswerTemplate(eventtype);
                ((DecisionActionTemplate) pendingActionTempl).addAnswerTemplate(answerTempl);

                componentStack.push(answerTempl);

                // Action: dataedit
            } else if (qName.equals("dataedit")) {
                String name = "";
                String role = null;
                String eventtype = "";
                String notificationTemplate = "";
                boolean mandatory = true;
                boolean restricted = false;

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("role")) {
                        role = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("eventtype")) {
                        eventtype = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals(
                    "notificationtemplate")) {
                        notificationTemplate = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("restricted")) {
                        restricted = Boolean.valueOf(attributes.getValue(i)).booleanValue();
                    } else if (attributes.getQName(i).equals("mandatory")) {
                        mandatory = Boolean.valueOf(attributes.getValue(i)).booleanValue();
                    }else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }

                pendingActionTempl = new DataeditActionTemplate(name, role, 
                        eventtype, pendingNodeTempl, notificationTemplate, mandatory, restricted);
                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);

                
            } else if (qName.equals("sendevent")) {
                String eventtype = "";  
                String name = "";
                
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("eventtype")) {
                        eventtype = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("name")) {
                            name = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                    pendingActionTempl = new SendEventActionTemplate(name, 
                            pendingNodeTempl, eventtype);
                    pendingNodeTempl.addActionTempl(pendingActionTempl);
                    componentStack.push(pendingActionTempl);
            
            } else if (qName.equals("triggerdate")) {
                String databit = "";
                String offset = "";
                boolean onlyWeekdays = false;
                
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("databit")) {
                        databit = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("offset")) {
                        offset = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("onlyweekdays")) {
                        if (attributes.getValue(i).equals("true")){
                            onlyWeekdays = true;
                        }
                    }
                }
                ((SendEventActionTemplate) pendingActionTempl).setOnlyWeekdays(onlyWeekdays);
                ((SendEventActionTemplate) pendingActionTempl).setTriggerDatabit(databit);
                ((SendEventActionTemplate) pendingActionTempl).setTriggerOffset(offset);               
                
            } else if (qName.equals("scriptaction")) {
                String name = "";
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    }
                }

                pendingActionTempl = new ScriptActionTemplate(name, pendingNodeTempl);
                pendingNodeTempl.addActionTempl(pendingActionTempl);
                componentStack.push(pendingActionTempl);
                
                // Action: fields for dataedit action
            } else if (qName.equals("field")) {
                String path = "";
                boolean mandatory = false;

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("path")) {
                        path = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("mandatory")) {
                        if (attributes.getValue(i).equals("true") || attributes.getValue(i).equals("yes")) {
                            mandatory = true;
                        }
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }

                pendingFieldTempl = new FieldTemplate(path, mandatory);
                // avoid classcastexception: dtd-check is done after sax events...
                if (pendingActionTempl instanceof DataeditActionTemplate) 
                    ((DataeditActionTemplate) pendingActionTempl).addFieldTemplate(pendingFieldTempl);

            } else if (qName.equals("script")) {
                ScriptTemplate script = new ScriptTemplate();
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("language")) {
                        script.setLanguage(attributes.getValue(i));
                    }
                }
                ((Scriptable) componentStack.peek()).setScript(script);
                
            } else if (qName.equals("dataset")) {
                /* This is a dataset. The dataset contains a collection of
                 * other datasets and databits. */
                String name = "";
                String desc = "";
                int state = Data.READWRITE;

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                        if (name.indexOf(".") > 0){
                            Logger.ERROR("Don't use \".\" in Dataset Names");
                            throw new SAXException("Don't use \".\" in Dataset Names");
                        }
                    } else if (attributes.getQName(i).equals("description")) {
                        desc = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("state")) {
                        state = Data.toState(attributes.getValue(i));
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                // as we are parsing from top to down, the first dataset is our
                // root dataset
                if (rootDatasetTempl == null) {
                    rootDatasetTempl = new DatasetTemplate(name, desc, state);
                    currDatasetTempl.push(rootDatasetTempl);
               // adding normal child-dataset:
                } else if (currDatasetTempl.size() > 0) {
                    DatasetTemplate childSet = new DatasetTemplate(name, desc, state);
                    DatasetTemplate dsetTempl = (DatasetTemplate) currDatasetTempl.peek();
                    dsetTempl.addDatasetTempl(childSet);
                    currDatasetTempl.push(childSet);
                }

            } else if (qName.equals("databit")) {
                String name = "";
                String desc = null;
                String type = "";
                String shortDesc = null;
                int state = Data.READWRITE;
		
                /* retrieve the databit data */
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                        if (name.indexOf(".") > 0){
                        Logger.ERROR("Don't use \".\" in Dataset Names");
                        throw new SAXException("Don't use \".\" in Databit Names");
                        }
                    } else if (attributes.getQName(i).equals("description")) {
                        desc = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("type")) {
                        type = attributes.getValue(i).toLowerCase(); 
                    } else if (attributes.getQName(i).equals("state")) {
                            state = Data.toState(attributes.getValue(i));
                    } else if (attributes.getQName(i).equals("shortdesc")) {
                        shortDesc = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }

                currDatabitTempl = new DatabitTemplate(name, desc, type, state);
                componentStack.push(currDatabitTempl);
                
                if (shortDesc != null){
                    currDatabitTempl.setShortDescription(shortDesc);
                }

                if (currDatasetTempl.peek() != null) { // this shouldn't be null
                    ((DatasetTemplate) currDatasetTempl.peek()).
                        addDatabitTempl(currDatabitTempl);
                } else {
                    Logger.ERROR("Cannot add Databit without dataset"); 
                }
                
            } else if (qName.equals("dbedit")) {
                /* The edit information of a databit */
                int xsize = 10;
                int ysize = 0;
                String type = "text";

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("type")) {
                        type = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("xsize")) {
                        xsize = Integer.valueOf(attributes.getValue(i)).intValue();
                    } else if (attributes.getQName(i).equals("ysize")) {
                        ysize = Integer.valueOf(attributes.getValue(i)).intValue();
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                
                DataEditInfoTemplate inf = new DataEditInfoTemplate(type, xsize, ysize);
                currDatabitTempl.setEditInfoTemplate(inf);
                
            } else if (qName.equals("role")) {
                String name = "";
                Boolean restricted = Boolean.TRUE;
                String type = "value";

                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals("name")) {
                        name = attributes.getValue(i);
                    } else if (attributes.getQName(i).equals("restricted")) {
                        restricted = Boolean.valueOf(attributes.getValue(i));
                    } else if (attributes.getQName(i).equals("type")) {
                        type = attributes.getValue(i);
                    } else {
                        Logger.ERROR("Unimplemented attribute" + 
                                attributes.getQName(i) + " in " + qName);
                    }
                }
                if (type.equals("databit")){
                    currRole = new DatabitRole(name, restricted.booleanValue());
                } else if (type.equals("value")) {
                    currRole = new ValueRole(name, restricted.booleanValue());
                } else if (type.equals("reference")) {
                    currRole = new ReferencesRole(name, restricted.booleanValue());
                } else if (type.equals("dbreference")) {
                    currRole = new DbReferencesRole(name, restricted.booleanValue());
                }
                componentStack.push(currRole);
                
                
           // skipping CDATA only and unused elements
           } else if (qName.equals("metadata") || 
                   qName.equals("templatedescription") 
                   || qName.equals("description")
                    || qName.equals("creator") 
                    || qName.equals("history") 
                    || qName.equals("change")
                    || qName.equals("question") 
                    || qName.equals("and") 
                    || qName.equals("or") 
                    || qName.equals("not")
                    || qName.equals("helpcontext") 
                    || qName.equals("longdesc") 
                    || qName.equals("value")
                    || qName.equals("defaultvalue")
                    || qName.equals("roles")
                    || qName.equals("targetwfs")
                    || qName.equals("rolevalue")) {
                
            } else {
                Logger.ERROR("Unsupported Element Type: " + qName);
            } /* end of very long list of opening xml elements (tags) */
        }

        
        /* Add here what you want to happen when an element (tag) is closed. */
        public void endElement(String uri, String localName, String qName) 
        			throws SAXException {

            Logger.DEBUG("Endelement: </" + qName + ">", log);
            String pcdata = "";
            if (!pcdataStack.empty()) {
                pcdata = (String) pcdataStack.pop();
            }
                
            // now pcdata contains the PCDATA text within the element that 
            // was just closed

            if (qName.equals("workflow")) {
                componentStack.pop();
            } else if (qName.equals("node") || qName.equals("startnode") || 
                    qName.equals("endnode")) {
                componentStack.pop();
            } else if (qName.equals("manualtask")) {
                componentStack.pop();
            } else if (qName.equals("decision")) {
                componentStack.pop();
            } else if (qName.equals("dataedit")) {
                componentStack.pop();
            } else if (qName.equals("startsubworkflow")) {
                componentStack.pop();
            } else if (qName.equals("notification")) {
                 msgtemplate = "";
                 msgtext = "";
                 xreason = "";
                 subject = "";
            } else if (qName.equals("edge")) {
                if (condStack.empty()){
                    throw new SAXException("No condition defined for edge from " + 
                            pendingNodeTempl.getName() + " to " + pendingEdgeTempl.getToId());
                }
                pendingEdgeTempl.setCondTempl((ConditionTemplate) condStack.pop());
                pendingNodeTempl.addEdgeTempl(pendingEdgeTempl);
            } else if (qName.equals("and")) {
                ArrayList conds = new ArrayList();
                conds.add(condStack.pop()); conds.add(condStack.pop());
                condStack.push(new ANDConditionTemplate(conds));
            } else if (qName.equals("or")) {
                condStack.push(new ORConditionTemplate((ConditionTemplate) 
                        condStack.pop(), (ConditionTemplate) condStack.pop()));
            } else if (qName.equals("not")) {
                condStack.push(new NOTConditionTemplate((ConditionTemplate) 
                        condStack.pop()));
            } else if (qName.equals("node")) {
                pendingNodeTempl = null;
            } else if (qName.equals("dataset")) {
                // removing current dataset from stack
                currDatasetTempl.pop();
            } else if (qName.equals("databit")) {
                componentStack.pop();
                // don't allow empty values for enums and booleans
                String type = currDatabitTempl.getType();
                if (type.equals("boolean") && currDatabitTempl.getDefaultValue().equals("")){
                    Logger.WARN(currDatabitTempl.getName() + " has empty default value, setting to 'false'");
                    currDatabitTempl.setDefaultValue("false");
                } else if (type.equals("enum") && currDatabitTempl.getDefaultValue().equals("") && 
                        !enumvalues.contains("")) {
                    // default to the first enum val
                    Logger.WARN(currDatabitTempl.getName() + " has empty default value, but no blank field. Adding it.");
                    enumvalues.add("");
                }
                currDatabitTempl.setEnumvalues(enumvalues);
                currDatabitTempl = null;
                enumvalues = new ArrayList();
            } else if (qName.equals("defaultvalue")) {
                currDatabitTempl.setDefaultValue(pcdata.trim().replaceAll("[\\s]+", " "));
            } else if (qName.equals("description")) {
            	pcdata = pcdata.trim().replaceAll("[\\s]+", " ");
                ((Describable) componentStack.peek()).setDescription(pcdata);
            } else if (qName.equals("templatedescription")) {
            	pcdata = pcdata.trim().replaceAll("[\\s]+", " ");
                WorkflowTemplate wft = ((WorkflowTemplate) componentStack.peek());
                wft.setTemplateDescription(pcdata);
            } else if (qName.equals("longdesc")) {
            	pcdata = pcdata.trim().replaceAll("[\\s]+", " ");
                ExtDescribable element = (ExtDescribable) componentStack.peek();
                element.setLongDescription(pcdata);
            } else if (qName.equals("helpcontext")) {
                ((ExtDescribable) componentStack.peek()).setHelpContext(pcdata);
            } else if (qName.equals("question")) {
                ((DecisionActionTemplate) componentStack.peek()).setQuestion(pcdata);
            } else if (qName.equals("answer")) {
                ((AnswerTemplate) componentStack.peek()).setText(pcdata);
                componentStack.pop();
            } else if (qName.equals("value")) {
                  enumvalues.add(pcdata.trim().replaceAll("[\\s]+", " "));
            } else if (qName.equals("role")) {
                workflowTempl.addRole(currRole);
                currRole = null;
            } else if (qName.equals("rolevalue")) {
                currRole.addValue(pcdata);
            } else if (qName.equals("script")) {
                ((Scriptable) componentStack.peek()).getScript().setScript(pcdata);
            } else if (qName.equals("targetwfs")) {
                ((SendEventActionTemplate) pendingActionTempl).setTargetWfs(pcdata);

            } /* end of list of closing xml elements (tags) */

        }

        /**
         * things to do after the document was parsed.
         */
        public void endDocument() throws SAXException {
            Logger.DEBUG("Finished reading workflow definition.", log);
            if (workflowTempl != null && rootDatasetTempl != null) {
                Logger.DEBUG("Adding root-dataset template " + 
                		rootDatasetTempl.getName(), log);
                workflowTempl.addDatasetTemplate(rootDatasetTempl);
                rootDatasetTempl = null;
            }
            Logger.DEBUG("Workflow template parsed.", log);
        }

        public void error(SAXParseException e) throws SAXParseException {
            throw e;
        }

        // Need this vars for knowing notification details when parsing 
        // recipients
        String msgtemplate = "";
        String notificationname = "";
        String msgtext = "";
        String xreason = "";
        String subject = "";  
        
    } // Here ends the inner class "WorkflowHandler"
    
    
    
    /**
     * Map the systemId of the DTD to a static path, to enable validation 
     * of xml files from any path
     */
    class resolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId){
            InputSource source = new InputSource(new ByteArrayInputStream(new byte[0]));
            if (systemId.indexOf("workflow.dtd") > 0){
                try {
                    String path = SWAMP.getInstance().getDTDLocation();
                    source = new InputSource(new FileInputStream(path));
                } catch (FileNotFoundException e) {
                    Logger.ERROR("Could not find workflow.dtd file.");
                }
            }
            return source;
        }
    }
    

} /* Here ends the ("outer") class "WorkflowReader" */
