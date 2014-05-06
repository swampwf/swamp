/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Cornelius Schumacher <cschum [at] suse.de>
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

package de.suse.swamp.util;

/**
 * This class represents a context help object.
 *
 */
public class ContextHelp {

    private String context;
    private String text;
    private String title;

    public ContextHelp( String context, String text, String title ) {
      this.context = context;
      this.text = text;
      this.title = title;
    }

    public void setContext( String context ) {
      this.context = context;
    }

    public String getContext() {
      return context;
    }

    public String getText() {
      return text;
    }

    public String getTitle() {
      return title;
    }

    public boolean equals(ContextHelp contextHelp) {
    	if (contextHelp.getTitle().equals(this.title)) {
    		return true;
    	}
    	return false;
    }
}
