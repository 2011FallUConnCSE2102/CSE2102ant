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
import java.util.zip.*;
/**
 * Unzip a file. 
 *
 * @author costin@dnt.ro
 * @author Stefan Bodewig <a href="mailto:stefan.bodewig@megabit.net">stefan.bodewig@megabit.net</a>
 */
public class Expand extends Task {
    private String dest; // req
    private String source; // req
    
    /**
     * Do the work.
     *
     * @exception BuildException Thrown in unrecoverable error.
     */
    // XXX move it to util or tools
    public void execute() throws BuildException {
        Touch touch = new Touch();
        touch.setProject(project);
        touch.setTarget(target);
                    
	try {
	    File srcF=project.resolveFile(source);
	    File dir=project.resolveFile(dest);
	    
	    project.log("Expanding: " + srcF + " into " + dir, Project.MSG_INFO);
	    // code from WarExpand
	    ZipInputStream zis = new ZipInputStream(new FileInputStream(srcF));
	    ZipEntry ze = null;
	    
	    while ((ze = zis.getNextEntry()) != null) {
		try {
		    File f = new File(dir, project.translatePath(ze.getName()));
		    project.log("expand-file " + ze.getName() , "expand", Project.MSG_VERBOSE );
		    // create intermediary directories - sometimes zip don't add them
		    File dirF=new File(f.getParent());
		    dirF.mkdirs();
		    
		    if (ze.isDirectory()) {
			f.mkdirs(); 
		    } else {
			byte[] buffer = new byte[1024];
			int length = 0;
			FileOutputStream fos = new FileOutputStream(f);
			
			while ((length = zis.read(buffer)) >= 0) {
			    fos.write(buffer, 0, length);
			}
			
			fos.close();
		    }

                    if (project.getJavaVersion() != Project.JAVA_1_1) {
                        touch.setFile(f.getAbsolutePath());
                        touch.setMillis(ze.getTime());
                        touch.touch();
                    }

		} catch( FileNotFoundException ex ) {
		    System.out.println("FileNotFoundException: " +  ze.getName()  );
		}
	    }
	    project.log("</log:expand>", Project.MSG_VERBOSE );
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }

    /**
     * Set the destination directory. File will be unzipped into the
     * destination directory.
     *
     * @param d Path to the directory.
     */
    public void setDest(String d) {
	this.dest=d;
    }

    /**
     * Set the path to zip-file.
     *
     * @param s Path to zip-file.
     */
    public void setSrc(String s) {
	this.source = s;
    }
}
