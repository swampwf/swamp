/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2004 Cornelius Schumacher <cschum@suse.de>
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

package de.suse.swamp.modules.screens;

import org.apache.turbine.util.*;
import org.apache.velocity.context.*;

import de.suse.swamp.core.api.*;
import de.suse.swamp.core.util.*;
import de.suse.swamp.util.*;

/**
  Provides context help.
*/
public class EditDocumentation extends SecureScreen {

    public void doBuildTemplate(RunData data, Context context) throws Exception {
        super.doBuildTemplate(data, context);
        String contextString = data.getParameters().get("helpcontext");
        try {
            ContextHelp help = new DocumentationAPI().
                getContextHelp(contextString, data.getUser().getName());
            context.put("editContextHelp", help);
        } catch (StorageException e) {
            setErrorScreen(data, context, e.getMessage());
        }
        context.put("standardlogo", "true");
    }

}
