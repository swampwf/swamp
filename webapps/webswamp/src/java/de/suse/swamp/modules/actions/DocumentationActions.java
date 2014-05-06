/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Klaas Freitag <freitag@suse.de>
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

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.container.*;
import de.suse.swamp.util.*;

/**
 * Actions for documentation.
 */
public class DocumentationActions extends SecureAction {

    /**
     *  IMPORTANT: doXxx methods may only have ONE upper case letter X
     *  after the do, anything else must be lower case.
     */

    /**
      Saves documentation to database.
    */
    public void doSavedocumentation( RunData data, Context context )
        throws Exception
    {
        String helpContext = data.getParameters().get("editHelpContext");
        String helpText = data.getParameters().get("editHelpText");
        String helpTitle = data.getParameters().get("editHelpTitle");

        DocumentationManager.getInstance().storeContextHelp (
              new ContextHelp( helpContext, helpText, helpTitle ) );
        
        // send mail about new Documentation.
        NotificationManager.newNotification("newdoku", data.getUser().getName(), SWAMP.getInstance().getProperty("POSTMASTER"));
        
    }

}
