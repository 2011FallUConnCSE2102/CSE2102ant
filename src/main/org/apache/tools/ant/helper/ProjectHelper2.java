/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Locale;

import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.FileUtils;

import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.UnknownElement;

import org.xml.sax.XMLReader;

/**
 * Sax2 based project reader
 *
 * @author duncan@x180.com
 * @author Costin Manolache
 */
public class ProjectHelper2 extends ProjectHelper {
    /* Stateless */

    // singletons - since all state is in the context
    private static AntHandler elementHandler = new ElementHandler();
    private static AntHandler targetHandler = new TargetHandler();
    private static AntHandler mainHandler = new MainHandler();
    private static AntHandler projectHandler = new ProjectHandler();

    /**
     * helper for path -> URI and URI -> path conversions.
     */
    private static FileUtils fu = FileUtils.newFileUtils();

    public void parse(Project project, Object source)
            throws BuildException {
        this.getImportStack().addElement(source);
        //System.out.println("Adding " + source);
        AntXMLContext context = null;
        context = (AntXMLContext) project.getReference("ant.parsing.context");
//        System.out.println("Parsing " + getImportStack().size() + " " +
//                context+ " " + getImportStack() );
        if (context == null) {
            context = new AntXMLContext(project);
            project.addReference("ant.parsing.context", context);
            project.addReference("ant.targets", context.getTargets());
        }

        if (this.getImportStack().size() > 1) {
            // we are in an imported file.
            context.setIgnoreProjectTag(true);
            parse(project, source, new RootHandler(context));
        } else {
            // top level file
            parse(project, source, new RootHandler(context));
            // Execute the top-level target
            context.getImplicitTarget().execute();
        }
    }

    /**
     * Parses the project file, configuring the project as it goes.
     *
     * @exception BuildException if the configuration is invalid or cannot
     *                           be read
     */
    public void parse(Project project, Object source, RootHandler handler)
            throws BuildException {

        AntXMLContext context = handler.context;

        File buildFile = null;

        if (source instanceof File) {
            buildFile = (File) source;
//         } else if (source instanceof InputStream ) {
//         } else if (source instanceof URL ) {
//         } else if (source instanceof InputSource ) {
        } else {
            throw new BuildException("Source " + source.getClass().getName() +
                                     " not supported by this plugin");
        }

        FileInputStream inputStream = null;
        InputSource inputSource = null;

        buildFile = new File(buildFile.getAbsolutePath());
        context.setBuildFile(buildFile);

        try {
            /**
             * SAX 2 style parser used to parse the given file.
             */
            XMLReader parser = JAXPUtils.getNamespaceXMLReader();

            String uri = fu.toURI(buildFile.getAbsolutePath());

            inputStream = new FileInputStream(buildFile);
            inputSource = new InputSource(inputStream);
            inputSource.setSystemId(uri);
            project.log("parsing buildfile " + buildFile
                + " with URI = " + uri, Project.MSG_VERBOSE);

            DefaultHandler hb = handler;

            parser.setContentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location = new Location(exc.getSystemId(),
                exc.getLineNumber(), exc.getColumnNumber());

            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }

            throw new BuildException(exc.getMessage(), t, location);
        } catch (SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        } catch (FileNotFoundException exc) {
            throw new BuildException(exc);
        } catch (UnsupportedEncodingException exc) {
              throw new BuildException("Encoding of project file is invalid.",
                exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file: "
                + exc.getMessage(), exc);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                    // ignore this
                }
            }
        }
    }

    /**
     * The common superclass for all SAX event handlers used to parse
     * the configuration file.
     *
     * The context will hold all state information. At each time
     * there is one active handler for the current element. It can
     * use onStartChild() to set an alternate handler for the child.
     */
    public static class AntHandler  {
        /**
         * Handles the start of an element. This base implementation does
         * nothing.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXMLContext context)
            throws SAXParseException {
        }

        /**
         * Handles the start of an element. This base implementation just
         * throws an exception - you must override this method if you expect
         * child elements.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXMLContext context)
            throws SAXParseException {
            throw new SAXParseException("Unexpected element \"" + qname
                + " \"", context.getLocator());
        }

        public void onEndChild(String uri, String tag, String qname,
                                     AntXMLContext context)
            throws SAXParseException {
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled (i.e. at the </end_tag_of_the_element> ).
         */
        public void onEndElement(String uri, String tag,
                                 AntXMLContext context) {
        }

        /**
         * Handles text within an element. This base implementation just
         * throws an exception, you must override it if you expect content.
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @exception SAXParseException if this method is not overridden, or in
         *                              case of error in an overridden version
         */
        public void characters(char[] buf, int start, int count, AntXMLContext context)
            throws SAXParseException {
            String s = new String(buf, start, count).trim();

            if (s.length() > 0) {
                throw new SAXParseException("Unexpected text \"" + s
                    + "\"", context.getLocator());
            }
        }

        /**
         * Will be called every time a namespace is reached.
         * It'll verify if the ns was processed, and if not load the task
         * definitions.
         */
        protected void checkNamespace(String uri) {

        }
    }

    /**
     * Handler for ant processing. Uses a stack of AntHandlers to
     * implement each element ( the original parser used a recursive behavior,
     * with the implicit execution stack )
     */
    public static class RootHandler extends DefaultHandler {
        private Stack antHandlers = new Stack();
        private AntHandler currentHandler = null;
        private AntXMLContext context;

        public RootHandler(AntXMLContext context) {
            currentHandler = ProjectHelper2.mainHandler;
            antHandlers.push(currentHandler);
            this.context = context;
        }

        /**
         * Resolves file: URIs relative to the build file.
         *
         * @param publicId The public identifer, or <code>null</code>
         *                 if none is available. Ignored in this
         *                 implementation.
         * @param systemId The system identifier provided in the XML
         *                 document. Will not be <code>null</code>.
         */
        public InputSource resolveEntity(String publicId,
                                         String systemId) {

            context.getProject().log("resolving systemId: " +
                    systemId, Project.MSG_VERBOSE);

            if (systemId.startsWith("file:")) {
                String path = fu.fromURI(systemId);

                File file = new File(path);
                if (!file.isAbsolute()) {
                    file = fu.resolveFile(context.getBuildFileParent(), path);
                }
                try {
                    InputSource inputSource =
                            new InputSource(new FileInputStream(file));
                    inputSource.setSystemId(fu.toURI(file.getAbsolutePath()));
                    return inputSource;
                } catch (FileNotFoundException fne) {
                    context.getProject().log(file.getAbsolutePath() +
                            " could not be found", Project.MSG_WARN);
                }

            }
            // use default if not file or file not found
            return null;
        }

        /**
         * Handles the start of a project element. A project handler is created
         * and initialised with the element name and attributes.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception org.xml.sax.SAXParseException if the tag given is not
         *                              <code>"project"</code>
         */
        public void startElement(String uri, String tag, String qname, Attributes attrs)
            throws SAXParseException {
            AntHandler next
                = currentHandler.onStartChild(uri, tag, qname, attrs, context);
            antHandlers.push(currentHandler);
            currentHandler = next;
            currentHandler.onStartElement(uri, tag, qname, attrs, context);
        }

        /**
         * Sets the locator in the project helper for future reference.
         *
         * @param locator The locator used by the parser.
         *                Will not be <code>null</code>.
         */
        public void setDocumentLocator(Locator locator) {
            context.setLocator(locator);
        }

        /**
         * Handles the end of an element. Any required clean-up is performed
         * by the onEndElement() method and then the original handler
         * is restored to the parser.
         *
         * @param name The name of the element which is ending.
         *             Will not be <code>null</code>.
         *
         * @exception SAXException in case of error (not thrown in
         *                         this implementation)
         *
         */
        public void endElement(String uri, String name, String qName) throws SAXException {
            currentHandler.onEndElement(uri, name, context);
            AntHandler prev = (AntHandler) antHandlers.pop();
            currentHandler = prev;
            if (currentHandler != null) {
                currentHandler.onEndChild(uri, name, qName, context);
            }
        }

        public void characters(char[] buf, int start, int count)
            throws SAXParseException {
            currentHandler.characters(buf, start, count, context);
        }
    }

    public static class MainHandler extends AntHandler {

        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXMLContext context)
            throws SAXParseException {
            if (qname.equals("project")) {
                return ProjectHelper2.projectHandler;
            } else {
//                 if (context.importlevel > 0 ) {
//                     // we are in an imported file. Allow top-level <target>.
//                     if (qname.equals( "target" ) )
//                         return ProjectHelper2.targetHandler;
//                 }
                throw new SAXParseException("Unexpected element \"" + qname
                    + "\" " + name, context.getLocator());
            }
        }
    }

    /**
     * Handler for the top level "project" element.
     */
    public static class ProjectHandler extends AntHandler {

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"default"</code>,
         * <code>"name"</code>, <code>"id"</code> and <code>"basedir"</code>.
         *
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         *
         * @exception SAXParseException if an unexpected attribute is
         *            encountered or if the <code>"default"</code> attribute
         *            is missing.
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXMLContext context)
            throws SAXParseException {
            String id = null;
            String baseDir = null;

            Project project = context.getProject();

            /** XXX I really don't like this - the XML processor is still
             * too 'involved' in the processing. A better solution (IMO)
             * would be to create UE for Project and Target too, and
             * then process the tree and have Project/Target deal with
             * its attributes ( similar with Description ).
             *
             * If we eventually switch to ( or add support for ) DOM,
             * things will work smoothly - UE can be avoided almost completely
             * ( it could still be created on demand, for backward compat )
             */

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (key.equals("default")) {
                    if (value != null && !value.equals("")) {
                        if (!context.isIgnoringProjectTag()) {
                            project.setDefaultTarget(value);
                        }
                    }
                } else if (key.equals("name")) {
                    if (value != null) {
                        context.setCurrentProjectName(value);

                        if (!context.isIgnoringProjectTag()) {
                            project.setName(value);
                            project.addReference(value, project);
                        }
                    }
                } else if (key.equals("id")) {
                    if (value != null) {
                        // What's the difference between id and name ?
                        if (!context.isIgnoringProjectTag()) {
                            project.addReference(value, project);
                        }
                    }
                } else if (key.equals("basedir")) {
                    if (!context.isIgnoringProjectTag()) {
                        baseDir = value;
                    }
                } else {
                    // XXX ignore attributes in a different NS ( maybe store them ? )
                    throw new SAXParseException("Unexpected attribute \""
                        + attrs.getQName(i) + "\"", context.getLocator());
                }
            }

            // XXX Move to Project ( so it is shared by all helpers )
            String antFileProp = "ant.file." + context.getCurrentProjectName();
            String dup = project.getProperty(antFileProp);
            if (dup != null) {
                File dupFile = new File(dup);
                if (context.isIgnoringProjectTag() &&
                    !dupFile.equals(context.getBuildFile())) {
                    project.log("Duplicated project name in import. Project " +
                        context.getCurrentProjectName() + " defined first in " +
                        dup + " and again in " + context.getBuildFile(),
                        Project.MSG_WARN);
                }
            }

            if (context.getBuildFile() != null) {
                project.setUserProperty("ant.file."
                    + context.getCurrentProjectName(),
                    context.getBuildFile().toString());
            }

            if (context.isIgnoringProjectTag()) {
                // no further processing
                return;
            }
            // set explicitely before starting ?
            if (project.getProperty("basedir") != null) {
                project.setBasedir(project.getProperty("basedir"));
            } else {
                // Default for baseDir is the location of the build file.
                if (baseDir == null) {
                    project.setBasedir(context.getBuildFileParent().getAbsolutePath());
                } else {
                    // check whether the user has specified an absolute path
                    if ((new File(baseDir)).isAbsolute()) {
                        project.setBasedir(baseDir);
                    } else {
                        project.setBaseDir(project.resolveFile(baseDir,
                                                               context.getBuildFileParent()));
                    }
                }
            }

            project.addTarget("", context.getImplicitTarget());
            context.setCurrentTarget(context.getImplicitTarget());
        }

        /**
         * Handles the start of a top-level element within the project. An
         * appropriate handler is created and initialised with the details
         * of the element.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception org.xml.sax.SAXParseException if the tag given is not
         *            <code>"taskdef"</code>, <code>"typedef"</code>,
         *            <code>"property"</code>, <code>"target"</code>
         *            or a data type definition
         */
        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXMLContext context)
            throws SAXParseException {
            if (qname.equals("target")) {
                return ProjectHelper2.targetHandler;
            } else {
                return ProjectHelper2.elementHandler;
            }
        }

    }

    /**
     * Handler for "target" elements.
     */
    public static class TargetHandler extends AntHandler {

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. The attributes which
         * this handler can deal with are: <code>"name"</code>,
         * <code>"depends"</code>, <code>"if"</code>,
         * <code>"unless"</code>, <code>"id"</code> and
         * <code>"description"</code>.
         *
         * @param tag Name of the element which caused this handler
         *            to be created. Should not be <code>null</code>.
         *            Ignored in this implementation.
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         *
         * @exception SAXParseException if an unexpected attribute is encountered
         *            or if the <code>"name"</code> attribute is missing.
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXMLContext context)
            throws SAXParseException {
            String name = null;
            String depends = "";

            Project project = context.getProject();
            Target target = new Target();
            context.addTarget(target);

            for (int i = 0; i < attrs.getLength(); i++) {
                String key = attrs.getQName(i);
                String value = attrs.getValue(i);

                if (key.equals("name")) {
                    name = value;
                    if ("".equals(name)) {
                        throw new BuildException("name attribute must "
                            + "not be empty");
                    }
                } else if (key.equals("depends")) {
                    depends = value;
                } else if (key.equals("if")) {
                    target.setIf(value);
                } else if (key.equals("unless")) {
                    target.setUnless(value);
                } else if (key.equals("id")) {
                    if (value != null && !value.equals("")) {
                        context.getProject().addReference(value, target);
                    }
                } else if (key.equals("description")) {
                    target.setDescription(value);
                } else {
                    throw new SAXParseException("Unexpected attribute \""
                        + key + "\"", context.getLocator());
                }
            }

            if (name == null) {
                throw new SAXParseException("target element appears without "
                    + "a name attribute", context.getLocator());
            }

            Hashtable currentTargets = project.getTargets();

            // If the name has already beend defined ( import for example )
            if (currentTargets.containsKey(name)) {
                // Alter the name.
                if (context.getCurrentProjectName() != null) {
                    String newName = context.getCurrentProjectName()
                        + "." + name;
                    project.log("Already defined in main or a previous import, "
                        + "define " + name + " as " + newName,
                                Project.MSG_VERBOSE);
                    name = newName;
                } else {
                    project.log("Already defined in main or a previous import, "
                        + "ignore " + name, Project.MSG_VERBOSE);
                    name = null;
                }
            }

            if (name != null) {
                target.setName(name);
                project.addOrReplaceTarget(name, target);
            }

            // take care of dependencies
            if (depends.length() > 0) {
                target.setDepends(depends);
            }
        }

        /**
         * Handles the start of an element within a target.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public AntHandler onStartChild(String uri, String name, String qname,
                                       Attributes attrs,
                                       AntXMLContext context)
            throws SAXParseException {
            return ProjectHelper2.elementHandler;
        }

        public void onEndElement(String uri, String tag, AntXMLContext context) {
            context.setCurrentTarget(context.getImplicitTarget());
        }
    }

    /**
     * Handler for all project elements ( tasks, data types )
     */
    public static class ElementHandler extends AntHandler {

        /**
         * Constructor.
         */
        public ElementHandler() {
        }

        /**
         * Initialisation routine called after handler creation
         * with the element name and attributes. This configures
         * the element with its attributes and sets it up with
         * its parent container (if any). Nested elements are then
         * added later as the parser encounters them.
         *
         * @param tag Name of the element which caused this handler
         *            to be created. Must not be <code>null</code>.
         *
         * @param attrs Attributes of the element which caused this
         *              handler to be created. Must not be <code>null</code>.
         *
         * @exception SAXParseException in case of error (not thrown in
         *                              this implementation)
         */
        public void onStartElement(String uri, String tag, String qname,
                                   Attributes attrs,
                                   AntXMLContext context)
            throws SAXParseException {
            RuntimeConfigurable parentWrapper = context.currentWrapper();
            Object parent = null;

            if (parentWrapper != null) {
                parent = parentWrapper.getProxy();
            }

            /* UnknownElement is used for tasks and data types - with
               delayed eval */
            UnknownElement task = new UnknownElement(qname);
            task.setProject(context.getProject());
            //XXX task.setTaskType(qname);

            task.setTaskName(qname);

            Location location = new Location(context.getLocator().getSystemId(),
                    context.getLocator().getLineNumber(),
                    context.getLocator().getColumnNumber());
            task.setLocation(location);
            task.setOwningTarget(context.getCurrentTarget());

            context.configureId(task, attrs);

            if (parent != null) {
                // Nested element
                ((UnknownElement) parent).addChild(task);
            }  else {
                // Task included in a target ( including the default one ).
                context.getCurrentTarget().addTask(task);
            }

            // container.addTask(task);
            // This is a nop in UE: task.init();

            RuntimeConfigurable wrapper 
                = new RuntimeConfigurable(task, task.getTaskName());

            for (int i = 0; i < attrs.getLength(); i++) {
                wrapper.setAttribute(attrs.getQName(i),
                        attrs.getValue(i));
            }

            if (parentWrapper != null) {
                parentWrapper.addChild(wrapper);
            }

            context.pushWrapper(wrapper);
        }

        /**
         * Adds text to the task, using the wrapper
         *
         * @param buf A character array of the text within the element.
         *            Will not be <code>null</code>.
         * @param start The start element in the array.
         * @param count The number of characters to read from the array.
         *
         * @exception SAXParseException if the element doesn't support text
         *
         * @see ProjectHelper#addText(Project,java.lang.Object,char[],int,int)
         */
        public void characters(char[] buf, int start, int count,
                               AntXMLContext context)
            throws SAXParseException {
            RuntimeConfigurable wrapper = context.currentWrapper();
            wrapper.addText(buf, start, count);
        }

        /**
         * Handles the start of an element within a target. Task containers
         * will always use another task handler, and all other tasks
         * will always use a nested element handler.
         *
         * @param tag The name of the element being started.
         *            Will not be <code>null</code>.
         * @param attrs Attributes of the element being started.
         *              Will not be <code>null</code>.
         *
         * @exception SAXParseException if an error occurs when initialising
         *                              the appropriate child handler
         */
        public AntHandler onStartChild(String uri, String tag, String qname,
                                       Attributes attrs,
                                       AntXMLContext context)
            throws SAXParseException {
            return ProjectHelper2.elementHandler;
        }

        public void onEndElement(String uri, String tag, AntXMLContext context) {
            context.popWrapper();
        }
    }
}
