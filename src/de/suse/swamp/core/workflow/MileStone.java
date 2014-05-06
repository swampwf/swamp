/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2005 Thomas Schmidt (tschmidt [at] suse.de)
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

package de.suse.swamp.core.workflow;

/**
 * A MileStone marks an important point in a Workflow
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * 
 * @version $Id: Node.java 4841 2005-01-27 16:29:34Z tschmidt $
 *
 */

import de.suse.swamp.core.util.*;

public class MileStone extends Persistant {

    private boolean displayed; 
    private MileStoneTemplate mile;
    private Node node;
    
    public MileStone(Node node, boolean displayed, MileStoneTemplate mile) {
        this.node = node;
        this.mile = mile;
        this.displayed = displayed;
        setModified(true);
    }

    /**
     * @return Returns the reached.
     */
    public boolean isReached() {
        return this.node.isActive();
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return getTemplate().getName();
    }
    
    public String getDescription() {
        return getTemplate().getDescription();
    }
    
    public int getWeight() {
        return getTemplate().getWeight();
    }
    
    public MileStoneTemplate getTemplate(){
        return mile;
    }

    public boolean isDisplayed() {
        return displayed;
    }

    public void setDisplayed(boolean active) {
        if (displayed != active) {
            this.displayed = active;
            setModified(true);
        }
    }
    
}