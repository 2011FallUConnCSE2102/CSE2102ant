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
import java.util.*;

/**
 * This Task makes it easy to generate javadocs for a collection of source code.
 *
 * Current known limitations are:
 *  - multiple source path breaks operation
 *  - patterns must be of the form "xxx.*", every other pattern doesn't work.
 *  - the java comment-stripper reader is horribly slow
 *  - there is no control on arguments sanity since they are left
 *    to the javadoc implementation.
 *  - argument J in javadoc1 is not supported (what is that for anyway?)
 *
 * Note: This task is run on another VM because stupid Javadoc calls
 * System.exit() that would break Ant functionality.
 *
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author Stefano Mazzocchi <a href="mailto:stefano@pache.org">stefano@apache.org</a>
 */

public class Javadoc extends Exec {

    private static final String JAVADOC1 = "sun.tools.javadoc.Main";
    private static final String JAVADOC2 = "com.sun.tools.javadoc.Main";
    
    private String sourcePath = null;
    private File destDir = null;
    private File overviewFile = null;
    private String sourceFiles = null;
    private String packageNames = null;
    private boolean pub = false;
    private boolean prot = false;
    private boolean pack = false;
    private boolean priv = false;
    private boolean author = true;
    private boolean version = true;
    private String doclet = null;
    private File docletpath = null;
    private boolean old = false;
    private String classpath = null;
    private String bootclasspath = null;
    private String extdirs = null;
    private boolean verbose = false;
    private String locale = null;
    private String encoding = null;
    private boolean use = false;
    private boolean splitindex = false;
    private String windowtitle = null;
    private String doctitle = null;
    private String header = null;
    private String footer = null;
    private String bottom = null;
    private String link = null;
    private String linkoffline = null;
    private String group = null;
    private boolean nodeprecated = false;
    private boolean nodeprecatedlist = false;
    private boolean notree = false;
    private boolean noindex = false;
    private boolean nohelp = false;
    private boolean nonavbar = false;
    private File stylesheetfile = null;
    private File helpfile = null;
    private String docencoding = null;
    private Vector compileList = new Vector(10);

    public void setSourcepath(String src) {
        sourcePath = project.translatePath(src);
    }
    public void setDestdir(String src) {
        destDir = project.resolveFile(src);
    }
    public void setSourcefiles(String src) {
        sourceFiles = src;
    }
    public void setPackagenames(String src) {
        packageNames = src;
    }
    public void setOverview(String src) {
        overviewFile = project.resolveFile(src);
    }
    public void setPublic(String src) {
        pub = Project.toBoolean(src);
    }
    public void setProtected(String src) {
        prot = Project.toBoolean(src);
    }
    public void setPackage(String src) {
        pack = Project.toBoolean(src);
    }
    public void setPrivate(String src) {
        priv = Project.toBoolean(src);
    }
    public void setDoclet(String src) {
        doclet = src;
    }
    public void setDocletPath(String src) {
        docletpath = project.resolveFile(src);
    }
    public void setOld(String src) {
        old = Project.toBoolean(src);
    }
    public void setClasspath(String src) {
        classpath = Project.translatePath(src);
    }
    public void setBootclasspath(String src) {
        bootclasspath = Project.translatePath(src);
    }
    public void setExtdirs(String src) {
        extdirs = src;
    }
    public void setVerbose(String src) {
        verbose = Project.toBoolean(src);
    }
    public void setLocale(String src) {
        locale = src;
    }
    public void setEncoding(String src) {
        encoding = src;
    }
    public void setVersion(String src) {
        version = Project.toBoolean(src);
    }
    public void setUse(String src) {
        use = Project.toBoolean(src);
    }
    public void setAuthor(String src) {
        author = Project.toBoolean(src);
    }
    public void setSplitindex(String src) {
        splitindex = Project.toBoolean(src);
    }
    public void setWindowtitle(String src) {
        windowtitle = src;
    }
    public void setDoctitle(String src) {
        doctitle = src;
    }
    public void setHeader(String src) {
        header = src;
    }
    public void setFooter(String src) {
        footer = src;
    }
    public void setBottom(String src) {
        bottom = src;
    }
    public void setLink(String src) {
        link = src;
    }
    public void setLinkoffline(String src) {
        linkoffline = src;
    }
    public void setGroup(String src) {
        group = src;
    }
    public void setNodeprecated(String src) {
        nodeprecated = Project.toBoolean(src);
    }
    public void setNodeprecatedlist(String src) {
        nodeprecatedlist = Project.toBoolean(src);
    }
    public void setNotree(String src) {
        notree = Project.toBoolean(src);
    }
    public void setNoindex(String src) {
        noindex = Project.toBoolean(src);
    }
    public void setNohelp(String src) {
        nohelp = Project.toBoolean(src);
    }
    public void setNonavbar(String src) {
        nonavbar = Project.toBoolean(src);
    }
    public void setStylesheetfile(String src) {
        stylesheetfile = project.resolveFile(src);
    }
    public void setDocencoding(String src) {
        docencoding = src;
    }

    public void execute() throws BuildException {
        if (sourcePath == null && destDir == null ) {
            String msg = "sourcePath and destDir attributes must be set!";
            throw new BuildException(msg);
        }

        boolean javadoc1 = (Project.getJavaVersion() == Project.JAVA_1_1);

        project.log("Generating Javadoc", project.MSG_INFO);

        Vector argList = new Vector();

// ------------------------------------------------ general javadoc arguments
        if (classpath == null)
            classpath = System.getProperty("java.class.path");

        if ( (!javadoc1) || (sourcePath == null) ) {
            argList.addElement("-classpath");
            argList.addElement(classpath);
            if (sourcePath != null) {
                argList.addElement("-sourcepath");
                argList.addElement(sourcePath);
            }
        } else { 
            argList.addElement("-classpath");
            argList.addElement(sourcePath + 
                System.getProperty("path.separator") + classpath);
        }

        if (destDir != null) {
            argList.addElement("-d");
            argList.addElement(destDir.getAbsolutePath());
        }
        if (version)
            argList.addElement ("-version");
        if (nodeprecated)
            argList.addElement ("-nodeprecated");
        if (author)
            argList.addElement ("-author");
        if (noindex)
            argList.addElement ("-noindex");
        if (notree)
            argList.addElement ("-notree");
        if (pub)
            argList.addElement ("-public");
        if (prot)
            argList.addElement ("-protected");
        if (pack)
            argList.addElement ("-package");
        if (priv)
            argList.addElement ("-private");
        if (encoding != null) {
            argList.addElement("-encoding");
            argList.addElement(encoding);
        }
        if (docencoding != null) {
            argList.addElement("-docencoding");
            argList.addElement(docencoding);
        }

// --------------------------------- javadoc2 arguments for default doclet

// XXX: how do we handle a custom doclet?

        if (!javadoc1) {
            if (overviewFile != null) {
                argList.addElement("-overview");
                argList.addElement(overviewFile.getAbsolutePath());
            }
            if (old)
                argList.addElement("-1.1");
            if (verbose)
                argList.addElement("-verbose");
            if (use)
                argList.addElement("-use");
            if (splitindex)
                argList.addElement("-splitindex");
            if (nodeprecatedlist)
                argList.addElement("-nodeprecatedlist");
            if (nohelp)
                argList.addElement("-nohelp");
            if (nonavbar)
                argList.addElement("-nonavbar");
            if (doclet != null) {
                argList.addElement("-doclet");
                argList.addElement(doclet);
            }
            if (bootclasspath != null) {
                argList.addElement("-bootclasspath");
                argList.addElement(bootclasspath);
            }
            if (extdirs != null) {
                argList.addElement("-extdirs");
                argList.addElement(extdirs);
            }
            if (locale != null) {
                argList.addElement("-locale");
                argList.addElement(locale);
            }
            if (encoding != null) {
                argList.addElement("-encoding");
                argList.addElement(encoding);
            }
            if (windowtitle != null) {
                argList.addElement("-windowtitle");
                argList.addElement(windowtitle);
            }
            if (doctitle != null) {
                argList.addElement("-doctitle");
                argList.addElement(doctitle);
            }
            if (header != null) {
                argList.addElement("-header");
                argList.addElement(header);
            }
            if (footer != null) {
                argList.addElement("-footer");
                argList.addElement(footer);
            }
            if (bottom != null) {
                argList.addElement("-bottom");
                argList.addElement(bottom);
            }
            if (link != null) {
                argList.addElement("-link");
                argList.addElement(link);
            }
            if (linkoffline != null) {
                argList.addElement("-linkoffline");
                argList.addElement(linkoffline);
            }
            if (group != null) {
                argList.addElement("-group");
                argList.addElement(group);
            }
            if (stylesheetfile != null) {
                argList.addElement("-stylesheetfile");
                argList.addElement(stylesheetfile.getAbsolutePath());
            }
            if (helpfile != null) {
                argList.addElement("-helpfile");
                argList.addElement(helpfile.getAbsolutePath());
            }
        }

        if ((packageNames != null) && (packageNames.length() > 0)) {
            Vector packages = new Vector();
            StringTokenizer tok = new StringTokenizer(packageNames, ",", false);
            while (tok.hasMoreTokens()) {
                String name = tok.nextToken().trim();
                if (name.endsWith(".*")) {
                    packages.addElement(name);
                } else {
                    argList.addElement(name);
                }
            }
            if (packages.size() > 0) {
                evaluatePackages(sourcePath, packages, argList);
            }
        }

        if ((sourceFiles != null) && (sourceFiles.length() > 0)) {
            StringTokenizer tok = new StringTokenizer(sourceFiles, ",", false);
            while (tok.hasMoreTokens()) {
                argList.addElement(tok.nextToken().trim());
            }
        }

        project.log("Javadoc args: " + argList.toString(), "javadoc", project.MSG_VERBOSE);

        project.log("Javadoc execution", project.MSG_INFO);

        StringBuffer b = new StringBuffer();
        b.append("java ");
        if (javadoc1) {
            b.append(JAVADOC1);
        } else {
            b.append(JAVADOC2);
        }
        b.append(" ");
        
        Enumeration e = argList.elements();
        while (e.hasMoreElements()) {
            String arg = (String) e.nextElement();
            if (!arg.startsWith("-")) {
                b.append("\"");
                b.append(arg);
                b.append("\"");
            } else {
                b.append(arg);
            }
            if (e.hasMoreElements()) b.append(" ");
        }
        
        run(b.toString());
    }

    /**
     * Given a source path, a list of package patterns, fill the given list
     * with the packages found in that path subdirs matching one of the given
     * patterns.
     */
    private void evaluatePackages(String source, Vector packages, Vector argList) {
        project.log("Parsing source files for packages", project.MSG_INFO);
        project.log("Source dir = " + source, project.MSG_VERBOSE);
        project.log("Packages = " + packages, project.MSG_VERBOSE);

        Hashtable map = mapClasses(new File(source));

        Enumeration e = map.keys();
        while (e.hasMoreElements()) {
            String pack = (String) e.nextElement();
            for (int i = 0; i < packages.size(); i++) {
                if (matches(pack, (String) packages.elementAt(i))) {
                    argList.addElement(pack);
                    break;
                }
            }
        }
    }

    /**
     * Implements the pattern matching. For now it's only able to
     * guarantee that "aaa.bbb.ccc" matches "aaa.*" and "aaa.bbb.*"
     * FIXME: this code needs much improvement.
     */
    private boolean matches(String string, String pattern) {
        return string.startsWith(pattern.substring(0, pattern.length() - 2));
    }

    /**
     * Returns an hashtable of packages linked to the last parsed
     * file in that package. This map is use to return a list of unique
     * packages as map keys.
     */
    private Hashtable mapClasses(File path) {
        Hashtable map = new Hashtable();

        Vector files = new Vector();
        getFiles(path, files);

        Enumeration e = files.elements();
        while (e.hasMoreElements()) {
            File file = (File) e.nextElement();
            String packageName = getPackageName(file);
            if (packageName != null) map.put(packageName, file);
        }

        return map;
    }

    /**
     * Fills the given vector with files under the given path filtered
     * by the given file filter.
     */
    private void getFiles(File path, Vector list) {
        if (!path.exists()) {
            throw new BuildException("Path " + path + " does not exist.");
        }

        String[] files = path.list();
        String cwd = path.getPath() + System.getProperty("file.separator");

        if (files != null) {
            int count = 0;
            for (int i = 0; i < files.length; i++) {
                File file = new File(cwd + files[i]);
                if (file.isDirectory()) {
                    getFiles(file, list);
                } else if (files[i].endsWith(".java")) {
                    count++;
                    list.addElement(file);
                }
            }
            if (count > 0) {
                project.log("found " + count + " source files in " + path, "javadoc", project.MSG_VERBOSE);
            }
        } else {
            throw new BuildException("Error occurred during " + path + " evaluation.");
        }
    }

    /**
     * Return the package name of the given java source file.
     * This method performs valid java parsing to figure out the package.
     */
    private String getPackageName(File file) {
        String name = null;

        try {
            // do not remove the double buffered reader, this is a _major_ speed up in this special case!
            BufferedReader reader = new BufferedReader(new JavaReader(new BufferedReader(new FileReader(file))));
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    project.log("Could not evaluate package for " + file, "javadoc", project.MSG_WARN);
                    return null;
                }
                if (line.trim().startsWith("package ")) {
                    name = line.substring(8, line.indexOf(";")).trim();
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            project.log("Exception " + e + " parsing " + file, "javadoc", project.MSG_WARN);
            return null;
        }

        project.log(file + " --> " + name, "javadoc", project.MSG_VERBOSE);

        return name;
    }

    /**
     * This is a java comment and string stripper reader that filters 
     * these lexical tokens out for purposes of simple Java parsing.
     * (if you have more complex Java parsing needs, use a real lexer).
     * Since this class heavily relies on the single char read function, 
     * you are reccomended to make it work on top of a buffered reader.
     */
    class JavaReader extends FilterReader {

        public JavaReader(Reader in) {
            super(in);
        }

        public int read() throws IOException {
            int c = in.read();
            if (c == '/') {
                c = in.read();
                if (c == '/') {
                    while (c != '\n') c = in.read();
                } else if (c == '*') {
                    while (true) {
                        c = in.read();
                        if (c == '*') {
                            c = in.read();
                            if (c == '/') {
                                c = read();
                                break;
                            }
                        }
                    }
                }
            }
            if (c == '"') {
                while (true) {
                    c = in.read();
                    if (c == '\\') c = in.read();
                    if (c == '"') {
                        c = read();
                        break;
                    }
                }
            }
            if (c == '\'') {
                c = in.read();
                if (c == '\\') c = in.read();
                c = in.read();
                c = read();
            }
            return c;
        }

        public int read(char cbuf[], int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                int c = read();
                if (c == -1) {
                    if (i == 0) {
                        return -1;
                    } else {
                        return i;
                    }
                }
                cbuf[off + i] = (char) c;
            }
            return len;
        }

        public long skip(long n) throws IOException {
            for (long i = 0; i < n; i++) {
                if (in.read() == -1) return i;
            }
            return n;
        }
    }
}
