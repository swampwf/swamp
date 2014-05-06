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


package de.suse.swamp.core.actions;

/**
 * An action to represent a human making a decision.
 *
 * @author Sonja Krause-Harder &lt;skh@suse.de&gt;
 * @version $Id$
 *
 */

import java.util.*;

import de.suse.swamp.core.notification.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.core.workflow.*;

public class DecisionActionTemplate extends UserActionTemplate {


    private ArrayList answerTempls = new ArrayList();
    private String question;  
    
    public DecisionActionTemplate(String name, String role, NodeTemplate nodeTemplate, 
			String notificationTemplate, boolean mandatory, boolean restricted) {
		super (name, role, nodeTemplate, notificationTemplate, mandatory, restricted); 
    }


    public void setQuestion(String question) {
        this.question = question;
    }

    public void addAnswerTemplate(AnswerTemplate answerTempl) {
        answerTempls.add(answerTempl);
    }  
	

    public String getQuestion () {
        return question;
    }
    
    public String getReplacedQuestion (Workflow wf) {
        return NotificationTools.workflowDataReplace(question, wf);
    }

    public ArrayList getAnswers() {
        return answerTempls;
    }
    
    public ArrayList getReplacedAnswers (Workflow wf) {
        ArrayList replacedAnswers = new ArrayList();
        for (Iterator it = answerTempls.iterator(); it.hasNext(); ){
            AnswerTemplate t = (AnswerTemplate) it.next();
            AnswerTemplate replaced = new AnswerTemplate(t.getEventtype());
            replaced.setText(NotificationTools.workflowDataReplace(t.getText(), wf));
            replacedAnswers.add(replaced);
        }
        return replacedAnswers;
    }

    // default description fits better in my use case (zoz 2010-12-23)
    // public String getDescription() {
    //     return "Decision: " + getQuestion();
    // }

    public String getType () {
        return "decision";
    }
    
    /**
     * @return an ArrayList with error strings, empty if everything went fine
     */
    public ArrayList validate (Result result) {
		ArrayList errors = new ArrayList();
		int selection = ((DecisionResult) result).getSelection();
		if (selection < 0) {
		    errors.add ("Nothing selected, please make a decision.");
		} else if (selection >= answerTempls.size()) {
		    errors.add ("Not a possible answer: " + selection);
		}
		runScript(result, errors);
		return errors;
    }

    
    /**
     * This action only sends events, so nothing needs to be done in act ()
     */
    public void act (Result result, ResultList hist) {}

    
    public ArrayList getEvents(Result result) {
        ArrayList events = new ArrayList();
        int selection = ((DecisionResult) result).getSelection();
        if (selection >= 0 && selection < answerTempls.size()) {
            AnswerTemplate answer = (AnswerTemplate) answerTempls.get(selection);
            events.add(new Event(answer.getEventtype(), result.getWorkflowId(), result.getWorkflowId()));
        }
        return events;
    }



}