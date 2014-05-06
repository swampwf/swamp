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
 * A MileStone marks an important point in an Workflow
 *
 * @author Thomas Schmidt &lt;tschmidt@suse.de&gt;
 * 
 * @version $Id: Node.java 4841 2005-01-27 16:29:34Z tschmidt $
 *
 */



public class MileStoneTemplate implements Describable {

	private String name; 
    private String description;
    private int weight;

    public MileStoneTemplate(String name, int weight) {
        this.name = name;
        this.weight = weight;
    }

    
    /**
     * @return a milestone Object
     */
    MileStone getMileStone(Node node){
    	MileStone mileStone = new MileStone(node, true, this);
    	return mileStone;
    }
    
    
    /**
     * @return Sets the description.
     */
    public void setDescription(String desc) {
        this.description = desc;
    }
    
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }
    /**
     * @return Returns the weight.
     */
    public int getWeight() {
        return weight;
    }
}