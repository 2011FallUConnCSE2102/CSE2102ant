/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.*;
import java.io.*;

/**
 *
 *
 * @author duncan@x180.com
 */

public class Exec extends Task {
    private String os;
    private String out;
    private String dir;
    private String command;

    public void execute() throws BuildException {
        // test if os match
        String myos = System.getProperty("os.name");
        project.log("Myos = " + myos, Project.MSG_VERBOSE);
        if ((os != null) && (os.indexOf(myos) < 0)){
            // this command will be executed only on the specified OS
            project.log("Not found in " + os, Project.MSG_VERBOSE);
            return;
        }
        
        String ant = project.getProperty("ant.home");
        if (ant == null) throw new BuildException("Property 'ant.home' not found");

        String antRun = project.resolveFile(ant + "/bin/antRun").toString();
        if (myos.toLowerCase().indexOf("windows") >= 0) antRun = antRun + ".bat";
        command = antRun + " " + project.resolveFile(dir) + " " + command;
        
        run(command);
    }

    protected void run(String command) throws BuildException {
        
        try {
            // show the command
            project.log(command, "exec", Project.MSG_VERBOSE);

            // exec command on system runtime
            Process proc = Runtime.getRuntime().exec(command);

            PrintWriter fos=null;
            if( out!=null )  {
                fos=new PrintWriter( new FileWriter( out ) );
                project.log("Output redirected to " + out, Project.MSG_VERBOSE);
            }
            pipeOutput(proc.getInputStream(), "exec", fos);
            pipeOutput(proc.getErrorStream(), "error", fos);
            if (null != fos)
        	fos.close();

            proc.waitFor();
            
            // close the output file if required 
            if (fos != null) fos.close();

            // check its exit value
            int err = proc.exitValue();
            if (err != 0) {
                project.log("Result: " + err, "exec", Project.MSG_ERR);
            }
        } catch (IOException ioe) {
            throw new BuildException("Error exec: " + command );
        } catch (InterruptedException ex) {
        }
    }

    public void setDir(String d) {
        this.dir = d;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setOutput(String out) {
        this.out = out;
    }

    private void pipeOutput(InputStream is, String name, PrintWriter fos) 
        throws IOException 
    {
project.log("pipeOutput", name, Project.MSG_INFO);
        InputStreamReader isr=new InputStreamReader(is);
        BufferedReader din = new BufferedReader(isr);

        // pipe output to STDOUT
        String line;
        while((line = din.readLine()) != null) {
        if( fos==null)
            project.log(line, name, Project.MSG_INFO);
        else
            fos.println(line);
        }
    }
}
