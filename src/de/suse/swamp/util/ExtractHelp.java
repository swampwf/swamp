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

import de.suse.swamp.core.container.*;

import java.util.*;
import java.io.*;

public class ExtractHelp
{
  public static void main ( String[] args )
  {
    if ( args.length != 1 ) {
      System.err.println( "Usage: ExtractHelp <output directory>" );
      return;
    }

    File outputDir = new File( args[ 0 ] );
    if ( !outputDir.exists() ) {
      System.err.println( "Directory '" + args[ 0 ] + "' does not exist." );
      return;
    }
    if ( !outputDir.isDirectory() ) {
      System.err.println( "'" + args[ 0 ] + "' is not a directory." );
      return;
    }

    SWAMP swamp = SWAMP.getInstance();

    try {
     
      ArrayList helps = WorkflowManager.loadAllContextHelp();

      for (Iterator it = helps.iterator(); it.hasNext(); ) {
        ContextHelp help = (ContextHelp) it.next();

        String fileName = outputDir.getPath() + "/" + help.getContext().
            replaceAll("\\.", "/");

        try {            
          System.out.println( "Extracting to file: " + fileName );

          Writer f = new BufferedWriter( new FileWriter( fileName ) );
          f.write( help.getTitle() + "\n" + help.getText() );
          f.close();
        } catch (IOException e) {
          System.out.println( "Error while writing file '" + fileName + "'." );
        }
      }
    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }
}
