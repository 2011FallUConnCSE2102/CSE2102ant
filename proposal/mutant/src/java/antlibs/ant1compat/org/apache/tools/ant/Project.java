/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.AntLibFactory;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.ExecService;
import org.apache.ant.common.service.FileService;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.common.util.PropertyUtils;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.util.FileUtils;

/**
 * Project facade
 *
 * @author Conor MacNeill
 * @created 30 January 2002
 */
public class Project implements org.apache.ant.common.event.BuildListener {

    /** String which indicates Java version 1.0 */
    public static final String JAVA_1_0 = "1.0";
    /** String which indicates Java version 1.1 */
    public static final String JAVA_1_1 = "1.1";
    /** String which indicates Java version 1.2 */
    public static final String JAVA_1_2 = "1.2";
    /** String which indicates Java version 1.3 */
    public static final String JAVA_1_3 = "1.3";
    /** String which indicates Java version 1.4 */
    public static final String JAVA_1_4 = "1.4";

    /**
     * @see MessageLevel.MSG_ERR
     */
    public static final int MSG_ERR = MessageLevel.MSG_ERR;
    /**
     * @see MessageLevel.MSG_WARN
     */
    public static final int MSG_WARN = MessageLevel.MSG_WARN;
    /**
     * @see MessageLevel.MSG_INFO
     */
    public static final int MSG_INFO = MessageLevel.MSG_INFO;
    /**
     * @see MessageLevel.MSG_VERBOSE
     */
    public static final int MSG_VERBOSE = MessageLevel.MSG_VERBOSE;
    /**
     * @see MessageLevel.MSG_DEBUG
     */
    public static final int MSG_DEBUG = MessageLevel.MSG_DEBUG;

    /** The java version detected that Ant is running on */
    private static String javaVersion;

    /**
     * the factory which created this project instance. This is used to
     * define new types and tasks
     */
    private AntLibFactory factory;

    /** Collection of Ant1 type definitions */
    private Hashtable dataClassDefinitions = new Hashtable();
    /** Collection of Ant1 task definitions */
    private Hashtable taskClassDefinitions = new Hashtable();

    /** The project description */
    private String description;

    /** The global filters of this project */
    private FilterSet globalFilterSet = new FilterSet();

    /** The AntContext that is used to access core services */
    private AntContext context;

    /** The core's FileService instance */
    private FileService fileService;

    /** The core's DataService instance */
    private DataService dataService;

    /** Th ecore's execution service */
    private ExecService execService;
    
    /** The core's Component Service instance */
    private ComponentService componentService;

    /** Ant1 FileUtils instance for manipulating files */
    private FileUtils fileUtils;
    /** The collection of global filters */
    private FilterSetCollection globalFilters
         = new FilterSetCollection(globalFilterSet);

    /** This project's listeners */
    private Vector listeners = new Vector();

    /** the target's we have seen */
    private Stack targetStack = new Stack();

    static {

        // Determine the Java version by looking at available classes
        // java.lang.StrictMath was introduced in JDK 1.3
        // java.lang.ThreadLocal was introduced in JDK 1.2
        // java.lang.Void was introduced in JDK 1.1
        // Count up version until a NoClassDefFoundError ends the try

        try {
            javaVersion = JAVA_1_0;
            Class.forName("java.lang.Void");
            javaVersion = JAVA_1_1;
            Class.forName("java.lang.ThreadLocal");
            javaVersion = JAVA_1_2;
            Class.forName("java.lang.StrictMath");
            javaVersion = JAVA_1_3;
            Class.forName("java.lang.CharSequence");
            javaVersion = JAVA_1_4;
        } catch (ClassNotFoundException cnfe) {
            // swallow as we've hit the max class version that
            // we have
        }
    }

    /**
     * Create the project
     *
     * @param factory the factory object creating this project
     */
    public Project(AntLibFactory factory) {
        this.factory = factory;
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * The old constructor fopr Project instances - not used now.
     *
     * @deprecated
     */
    public Project() {
        throw new BuildException("Projects can not be constructed to " 
            + "invoke Ant");
    }
    
    /**
     * The old initialisation method for Projects. Not used now
     *
     * @deprecated
     * @exception BuildException if the default task list cannot be loaded
     */
    public void init() throws BuildException {
        throw new BuildException("Projects can not be initialized in this " 
            + "manner any longer.");
    }


    /**
     * Old method used to execute targets
     * 
     * @param targetNames A vector of target name strings to execute.
     *                    Must not be <code>null</code>.
     * 
     * @exception BuildException always
     * @deprecated
     */
    public void executeTargets(Vector targetNames) throws BuildException {
        throw new BuildException("Targets within the project cannot be " 
            + "executed with this method.");
    }
        
    /**
     * static query of the java version
     *
     * @return a string indicating the Java version
     */
    public static String getJavaVersion() {
        return javaVersion;
    }

    /**
     * returns the boolean equivalent of a string, which is considered true
     * if either "on", "true", or "yes" is found, ignoring case.
     *
     * @param s the string value to be interpreted at a boolean
     * @return the value of s as a boolean
     */
    public static boolean toBoolean(String s) {
        return PropertyUtils.toBoolean(s);
    }

    /**
     * Translate a path into its native (platform specific) format. <p>
     *
     * This method uses the PathTokenizer class to separate the input path
     * into its components. This handles DOS style paths in a relatively
     * sensible way. The file separators are then converted to their
     * platform specific versions.
     *
     * @param to_process the path to be converted
     * @return the native version of to_process or an empty string if
     *      to_process is null or empty
     */
    public static String translatePath(String to_process) {
        if (to_process == null || to_process.length() == 0) {
            return "";
        }

        StringBuffer path = new StringBuffer(to_process.length() + 50);
        PathTokenizer tokenizer = new PathTokenizer(to_process);
        while (tokenizer.hasMoreTokens()) {
            String pathComponent = tokenizer.nextToken();
            pathComponent = pathComponent.replace('/', File.separatorChar);
            pathComponent = pathComponent.replace('\\', File.separatorChar);
            if (path.length() != 0) {
                path.append(File.pathSeparatorChar);
            }
            path.append(pathComponent);
        }

        return path.toString();
    }

    /**
     * set the project description
     *
     * @param description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set a project property
     *
     * @param name the property name
     * @param value the property value
     */
    public void setProperty(String name, String value) {
        try {
            dataService.setMutableDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Set a property which must be a new value
     *
     * @param name the property name
     * @param value the property value
     */
    public void setNewProperty(String name, String value) {
        try {
            dataService.setDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Sets a userProperty of the Project. Note under Ant2, there is no
     * distinction between user and system properties
     *
     * @param name the property name
     * @param value the property value
     */
    public void setUserProperty(String name, String value) {
        try {
            dataService.setMutableDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Gets the Antlib factory of the Project
     *
     * @return The project's associated factory object
     */
    public AntLibFactory getFactory() {
        return factory;
    }

    /**
     * get the target hashtable
     *
     * @return hashtable, the contents of which can be cast to Target
     */
    public Hashtable getTargets() {
        return new Hashtable();// XXX can't get targets
    }

    /**
     * Gets the buildListeners of the Project
     *
     * @return A Vector of BuildListener instances
     */
    public Vector getBuildListeners() {
        return listeners;
    }

    /**
     * Gets the AntContext of the Project
     *
     * @return the AntContext
     */
    public AntContext getContext() {
        return context;
    }

    /**
     * get the project description
     *
     * @return description or null if no description has been set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the Project's default Target, if any
     *
     * @return the project's default target or null if there is no default.
     * @deprecated
     */
    public String getDefaultTarget() {
        throw new BuildException("The default project target is no longer " 
            + "available through this method.");
    }

    /**
     * Get a project property
     *
     * @param name the property name
     * @return the value of the property
     */
    public String getProperty(String name) {
        try {
            Object value = dataService.getDataValue(name);
            return value == null ? null : value.toString();
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get a project property. Ant2 does not distinguish between User and
     * system proerties
     *
     * @param name the property name
     * @return the value of the property
     */
    public String getUserProperty(String name) {
        try {
            return dataService.getDataValue(name).toString();
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get a reference to a project property. Note that in Ant2, properties
     * and references occupy the same namespace.
     *
     * @param refId the reference Id
     * @return the object specified by the reference id
     */
    public Object getReference(String refId) {
        try {
            return dataService.getDataValue(refId);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Gets the globalFilterSet of the Project
     *
     * @return the globalFilterSet
     */
    public FilterSet getGlobalFilterSet() {
        return globalFilterSet;
    }

    /**
     * Gets the baseDir of the Project
     *
     * @return the baseDir
     */
    public File getBaseDir() {
        return execService.getBaseDir();
    }

    /**
     * Gets the coreLoader of the Project
     *
     * @return the coreLoader value
     */
    public ClassLoader getCoreLoader() {
        return getClass().getClassLoader();
    }

    /**
     * get a copy of the property hashtable
     *
     * @return the hashtable containing all properties, user included
     */
    public Hashtable getProperties() {
        Map properties = dataService.getAllProperties();
        Hashtable result = new Hashtable();
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = properties.get(name);
            if (value instanceof String) {
                result.put(name, value);
            }
        }

        return result;
    }

    /**
     * get a copy of the property hashtable
     *
     * @return the hashtable containing all properties, user included
     */
    public Hashtable getUserProperties() {
        return getProperties();
    }

    /**
     * Get all references in the project
     *
     * @return the hashtable containing all references
     */
    public Hashtable getReferences() {
        Map properties = dataService.getAllProperties();
        Hashtable result = new Hashtable();
        for (Iterator i = properties.keySet().iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Object value = properties.get(name);
            if (!(value instanceof String)) {
                result.put(name, value);
            }
        }

        return result;
    }

    /**
     * build started event
     *
     * @param event build started event
     */
    public void buildStarted(org.apache.ant.common.event.BuildEvent event) {
        fireBuildStarted();
    }

    /**
     * build finished event
     *
     * @param event build finished event
     */
    public void buildFinished(org.apache.ant.common.event.BuildEvent event) {
        fireBuildFinished(event.getCause());
    }

    /**
     * target started event.
     *
     * @param event target started event.
     */
    public void targetStarted(org.apache.ant.common.event.BuildEvent event) {
        Target newTarget = new Target(this);
        org.apache.ant.common.model.Target realTarget =
            (org.apache.ant.common.model.Target)event.getSource();
        newTarget.setName(realTarget.getName());
        targetStack.push(newTarget);
        fireTargetStarted(newTarget);
    }

    /**
     * target finished event
     *
     * @param event target finished event
     */
    public void targetFinished(org.apache.ant.common.event.BuildEvent event) {
        org.apache.ant.common.model.Target realTarget =
            (org.apache.ant.common.model.Target)event.getSource();
        Target currentTarget = (Target)targetStack.pop();
        fireTargetFinished(currentTarget, event.getCause());
        currentTarget = null;
    }

    /**
     * task started event
     *
     * @param event task started event
     */
    public void taskStarted(org.apache.ant.common.event.BuildEvent event) {
    }

    /**
     * task finished event
     *
     * @param event task finished event
     */
    public void taskFinished(org.apache.ant.common.event.BuildEvent event) {
    }

    /**
     * message logged event
     *
     * @param event message logged event
     */
    public void messageLogged(org.apache.ant.common.event.BuildEvent event) {
    }

    /**
     * add a build listener to this project
     *
     * @param listener the listener to be added to the project
     */
    public void addBuildListener(BuildListener listener) {
        listeners.addElement(listener);
    }

    /**
     * remove a build listener from this project
     *
     * @param listener the listener to be removed
     */
    public void removeBuildListener(BuildListener listener) {
        listeners.removeElement(listener);
    }

    /**
     * Add a reference to an object. NOte that in Ant2 objects and
     * properties occupy the same namespace.
     *
     * @param name the reference name
     * @param value the object to be associated with the given name.
     */
    public void addReference(String name, Object value) {
        try {
            dataService.setDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }


    /**
     * Convienence method to copy a file from a source to a destination. No
     * filtering is performed.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used and if source files may
     * overwrite newer destination files.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used, if source files may
     * overwrite newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal to the last modified
     * time of <code>sourceFile</code>.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @param preserveLastModified true if the last modified time of the
     *      source file is preserved
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null,
            overwrite, preserveLastModified);
    }

    /**
     * Convienence method to copy a file from a source to a destination. No
     * filtering is performed.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used and if source files may
     * overwrite newer destination files.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used, if source files may
     * overwrite newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal to the last modified
     * time of <code>sourceFile</code>.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @param preserveLastModified true if the last modified time of the
     *      source file is preserved
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite, preserveLastModified);
    }

    /**
     * Initialise this porject
     *
     * @param context the context the project uses to access core services
     * @exception ExecutionException if the project cannot be initialised.
     */
    public void init(AntContext context) throws ExecutionException {
        this.context = context;
        fileService = (FileService)context.getCoreService(FileService.class);
        dataService = (DataService)context.getCoreService(DataService.class);
        execService = (ExecService)context.getCoreService(ExecService.class);
        componentService
             = (ComponentService)context.getCoreService(ComponentService.class);

        String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        try {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream(defs);
            if (in == null) {
                throw new BuildException("Can't load default task list");
            }
            props.load(in);
            in.close();

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String)enum.nextElement();
                String value = props.getProperty(key);
                try {
                    Class taskClass = Class.forName(value);
                    taskClassDefinitions.put(key, taskClass);
                } catch (NoClassDefFoundError ncdfe) {
                    log("Could not load a dependent class ("
                         + ncdfe.getMessage() + ") for task " + key, MSG_DEBUG);
                } catch (ClassNotFoundException cnfe) {
                    log("Could not load class (" + value
                         + ") for task " + key, MSG_DEBUG);
                }
            }
        } catch (IOException ioe) {
            throw new BuildException("Can't load default task list");
        }

        String dataDefs = "/org/apache/tools/ant/types/defaults.properties";

        try {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream(dataDefs);
            if (in == null) {
                throw new BuildException("Can't load default datatype list");
            }
            props.load(in);
            in.close();

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String)enum.nextElement();
                String value = props.getProperty(key);
                try {
                    Class dataClass = Class.forName(value);
                    dataClassDefinitions.put(key, dataClass);
                } catch (NoClassDefFoundError ncdfe) {
                    log("Could not load a dependent class ("
                         + ncdfe.getMessage() + ") for type " + key, MSG_DEBUG);
                } catch (ClassNotFoundException cnfe) {
                    log("Could not load class (" + value
                         + ") for type " + key, MSG_DEBUG);
                }
            }
        } catch (IOException ioe) {
            throw new BuildException("Can't load default datatype list");
        }
    }

    /**
     * Output a message to the log with the default log level of MSG_INFO
     *
     * @param msg text to log
     */

    public void log(String msg) {
        log(msg, MSG_INFO);
    }

    /**
     * Output a message to the log with the given log level and an event
     * scope of project
     *
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log(String msg, int msgLevel) {
        context.log(msg, msgLevel);
    }

    /**
     * Output a message to the log with the given log level and an event
     * scope of a task
     *
     * @param task task to use in the log
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log(Task task, String msg, int msgLevel) {
        context.log(msg, msgLevel);
    }

    /**
     * Resolve a file relative to the project's basedir
     *
     * @param fileName the file name
     * @return the file as a File resolved relative to the project's basedir
     */
    public File resolveFile(String fileName) {
        try {
            return fileService.resolveFile(fileName);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Replace property references (${} values) in the given string
     *
     * @param value the string in which property references are replaced
     * @return the string with the properties replaced.
     */
    public String replaceProperties(String value) {
        try {
            return dataService.replacePropertyRefs(value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * define a new task
     *
     * @param taskName the anme of the task in build files
     * @param taskClass the class that implements the task
     * @exception BuildException if the task cannot be defined
     */
    public void addTaskDefinition(String taskName, Class taskClass)
         throws BuildException {
        try {
            componentService.taskdef(factory, taskClass.getClassLoader(),
                taskName, taskClass.getName());
            taskClassDefinitions.put(taskName, taskClass);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Add a new type definition
     *
     * @param typeName the name of the type
     * @param typeClass the class which implements the type
     */
    public void addDataTypeDefinition(String typeName, Class typeClass) {
        try {
            componentService.typedef(factory, typeClass.getClassLoader(),
                typeName, typeClass.getName());
            dataClassDefinitions.put(typeName, typeClass);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Create a Task. This faced hard codes a few well known tasks at this
     * time
     *
     * @param taskType the name of the task to be created.
     * @return the created task instance
     *
     * @exception BuildException if there is a build problem
     */
    public Task createTask(String taskType) throws BuildException {
        Task task = null;
        Class taskClass = (Class)taskClassDefinitions.get(taskType);

        if (taskClass == null) {
            return null;
        }

        try {
            Object taskObject = componentService.createComponent(factory,
                context.getClassLoader(), taskClass, false, taskType);
            if (taskObject instanceof Task) {
                task = (Task)taskObject;
            } else {
                TaskAdapter adapter = new TaskAdapter();
                adapter.setProxy(taskObject);
                task = adapter;
            }
            task.setTaskType(taskType);
            task.setTaskName(taskType);
            return task;
        } catch (Throwable e) {
            throw new BuildException(e);
        }
    }

    /**
     * Creates a new instance of a data type.
     *
     * @param typeName The name of the data type to create an instance of.
     *      Must not be <code>null</code>.
     * @return an instance of the specified data type, or <code>null</code>
     *      if the data type name is not recognised.
     * @exception BuildException if the data type name is recognised but
     *      instance creation fails.
     */
    public Object createDataType(String typeName) throws BuildException {
        Class typeClass = (Class)dataClassDefinitions.get(typeName);

        if (typeClass == null) {
            return null;
        }

        try {
            Object dataInstance = componentService.createComponent(factory,
                context.getClassLoader(), typeClass, false, typeName);
            return dataInstance;
        } catch (Throwable e) {
            throw new BuildException(e);
        }
    }

    /** send build started event to the listeners */
    protected void fireBuildStarted() {
        BuildEvent event = new BuildEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.buildStarted(event);
        }
    }

    /**
     * send build finished event to the listeners
     *
     * @param exception exception which indicates failure if not null
     */
    protected void fireBuildFinished(Throwable exception) {
        BuildEvent event = new BuildEvent(this);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.buildFinished(event);
        }
    }


    /**
     * send target started event to the listeners
     *
     * @param target the target which has started
     */
    protected void fireTargetStarted(Target target) {
        BuildEvent event = new BuildEvent(target);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.targetStarted(event);
        }
    }

    /**
     * send build finished event to the listeners
     *
     * @param exception exception which indicates failure if not null
     * @param target the target which is just finished
     */
    protected void fireTargetFinished(Target target, Throwable exception) {
        BuildEvent event = new BuildEvent(target);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.targetFinished(event);
        }
    }

    /**
     * fire a task started event
     *
     * @param task the task which has started
     */
    protected void fireTaskStarted(Task task) {
        // register this as the current task on the current thread.
        // threadTasks.put(Thread.currentThread(), task);
        BuildEvent event = new BuildEvent(task);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.taskStarted(event);
        }
    }

    /**
     * Fire a task finished event
     *
     * @param task the task which has finsihed
     * @param exception the exception associated with the task
     */
    protected void fireTaskFinished(Task task, Throwable exception) {
        // threadTasks.remove(Thread.currentThread());
        //System.out.flush();
        // System.err.flush();
        BuildEvent event = new BuildEvent(task);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.taskFinished(event);
        }
    }

    /**
     * Fire a message event from the project
     *
     * @param project the project sending the event
     * @param message the message
     * @param priority the messsage priority
     */
    protected void fireMessageLogged(Project project, String message,
                                     int priority) {
        BuildEvent event = new BuildEvent(project);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Fire a message event from the project
     *
     * @param message the message
     * @param priority the messsage priority
     * @param target the target sending the message
     */
    protected void fireMessageLogged(Target target, String message,
                                     int priority) {
        BuildEvent event = new BuildEvent(target);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Fire a message event from the project
     *
     * @param message the message
     * @param priority the messsage priority
     * @param task the task sending the message
     */
    protected void fireMessageLogged(Task task, String message,
                                     int priority) {
        BuildEvent event = new BuildEvent(task);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Fire a message event from the project
     *
     * @param message the message
     * @param priority the messsage priority
     * @param event the message event
     */
    private void fireMessageLoggedEvent(BuildEvent event, String message,
                                        int priority) {
        event.setMessage(message, priority);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener)listeners.elementAt(i);
            listener.messageLogged(event);
        }
    }
    
    /**
     * Get the name of the project.
     *
     * @return the project name
     */
    public String getName() {
        return execService.getProjectName();
    }
}

