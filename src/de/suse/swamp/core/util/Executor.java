/*
 * SWAMP Workflow Administration and Management Platform
 *
 * Copyright (c) 2006 Thomas Schmidt <tschmidt [at] suse.de>
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


package de.suse.swamp.core.util;

import java.io.*;
import java.util.*;

import de.suse.swamp.util.*;

/**
 * Class for executing external executables and 
 * providing their exit code and stdout and stderr Strings 
 * 
 * @author tschmidt
 */
public class Executor {
    
    private String executable;
    private ArrayList arguments = new ArrayList(); 

    private StringBuffer executionError;
    private StringBuffer stderr; 
    private StringBuffer stdout;
    private int exitVal;
    private boolean exceptionOnError = true;
    private int timeout = 15000;
    private int interval = 50;
    
    
    // extra logger for storage stuff
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(
            "de.suse.swamp.core.util.Executor");
    
    public Executor(String executable){
        this.executable = executable;
    }
    
    public Executor(){
    }
    
    public void reset() {
        executable = null;
        arguments = new ArrayList();
        executionError = null;
        stderr = null;
        stdout = null;
        exitVal = 0;
        exceptionOnError = true;
        timeout = 15000;
        interval = 50;
    }
    
    /**
     * Execute the set program.
     * @return - the exitvalue of the process. 0 indicates success; 
     * any other number is an error-code. 
     * @throws Exception
     */
    public String execute() throws Exception {
        if (this.executable == null)
            throw new Exception("No executable set!");
        exitVal = -1;
        stdout = new StringBuffer();
        stderr = new StringBuffer();
        String[] command = new String[arguments.size() + 1];
        command[0] = executable;
        long time = System.currentTimeMillis();
        for (int i = 0; i < arguments.size(); i++) {
            command[i + 1] = (String) arguments.get(i);
        }
        Logger.DEBUG("executing command: " + getCommandString(), log);

        try {
            Process proc = Runtime.getRuntime().exec(command);

            InputStream inputStream = proc.getInputStream();
            InputStream errorStream = proc.getErrorStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            BufferedInputStream bufferedErrorStream = new BufferedInputStream(errorStream);

            long time_waiting = 0;
            boolean process_finished = false;

            while (time_waiting < timeout && !process_finished) {
                try {
                    while (bufferedInputStream.available() > 0) {
                        stdout.append((char) bufferedInputStream.read());
                    }
                    while (bufferedErrorStream.available() > 0) {
                        stderr.append((char) bufferedErrorStream.read());
                    }
                    exitVal = proc.exitValue();
                    process_finished = true;
                } catch (IllegalThreadStateException e) {
                    // process hasn't finished yet
                    process_finished = false;
                    Thread.sleep(interval);
                    time_waiting += interval;
                }
            }

            if (!process_finished) {
                proc.destroy();
                throw new IOException("Process timed out!");
            }
        } catch (Exception e) {
            executionError = new StringBuffer(e.getMessage());
            log.error("Executor got an exception: " + e.getMessage());
            log.error("Command was: " + getCommandString());
            if (isExceptionOnError()) {
                throw e;
            }
        } finally {
            log.debug("Execution took: " + (System.currentTimeMillis() - time) + "ms.");
        }

        if (exitVal == 0) {
            Logger.LOG("Execution result is: " + exitVal + ", Stdout: " +
                    stdout.toString() + "\nStderr: " + stderr.toString(), log);
        } else {
            StringBuffer result = new StringBuffer();
            result.append("Execution of external command failed./n");
            result.append("Command was: " + getCommandString() + "\n");
            if (executionError == null)
                result.append("Execution result is: " + exitVal + "/n");
            else
                result.append("Executor got an exception: " + executionError);
            if (!stderr.toString().equals(""))
                result.append("Stderr: " + stderr.toString() + "\n");
            if (!stdout.toString().equals(""))
                result.append("Stdout: " + stdout.toString() + "\n");
            if (isExceptionOnError())
                throw new Exception(result.toString());
            else
                log.error(result);
        }
        return String.valueOf(exitVal);
    }
    
    
    public void addArgument(String argument){
        this.arguments.add(argument.trim());
    }

    public ArrayList getArguments() {
        return arguments;
    }

    public String getExecutable() {
        return executable;
    }

    public String getExitVal() {
        if (executionError == null)
            return String.valueOf(exitVal);
        else
            return "";
    }

    public String getExecutionError() {
        if (executionError == null)
            return "";
        else
            return executionError.toString();
    }

    public String getStderr() {
        if (stderr == null)
            return "";
        else
            return stderr.toString();
    }

    public String getStdout() {
        if (stdout == null)
            return "";
        else
            return stdout.toString();
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public boolean isExceptionOnError() {
        return exceptionOnError;
    }

    public void setExceptionOnError(boolean exceptionOnError) {
        this.exceptionOnError = exceptionOnError;
    }
    
    public String getCommandString(){
        StringBuffer command = new StringBuffer();
        command.append(executable);
        for (int i = 0; i < this.arguments.size(); i++){
            command.append(" \"").append(arguments.get(i)).append("\"");
        }
        return command.toString();
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Set timeout for the started process in ms.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    

}
