/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.xml.sax.AttributeList;
import org.apache.tools.ant.helper.ProjectHelperImpl;

/**
 * Configures a Project (complete with Targets and Tasks) based on
 * a XML build file. It'll rely on a plugin to do the actual processing
 * of the xml file.
 *
 * This class also provide static wrappers for common introspection.
 *
 * All helper plugins must provide backward compatiblity with the
 * original ant patterns, unless a different behavior is explicitely
 * specified. For example, if namespace is used on the <project> tag
 * the helper can expect the entire build file to be namespace-enabled.
 * Namespaces or helper-specific tags can provide meta-information to
 * the helper, allowing it to use new ( or different policies ).
 *
 * However, if no namespace is used the behavior should be exactly
 * identical with the default helper.
 *
 * @author duncan@x180.com
 */
public class ProjectHelper {

    /**
     * Configures the project with the contents of the specified XML file.
     * 
     * @param project The project to configure. Must not be <code>null</code>.
     * @param buildFile An XML file giving the project's configuration.
     *                  Must not be <code>null</code>.
     * 
     * @exception BuildException if the configuration is invalid or cannot 
     *                           be read
     */
    public static void configureProject(Project project, File buildFile) throws BuildException {
        ProjectHelper helper=ProjectHelper.getProjectHelper();
        helper.parse(project, buildFile);
    }

    public ProjectHelper() {
    }

    /**
     * Parses the project file, configuring the project as it goes.
     *
     * @param project The project for the resulting ProjectHelper to configure. 
     *                Must not be <code>null</code>.
     * @param source The source for XML configuration. A helper must support
     *               at least File, for backward compatibility. Helpers may
     *               support URL, InputStream, etc or specialized types.
     *
     * @since Ant1.5
     * @exception BuildException if the configuration is invalid or cannot 
     *                           be read
     */
    public void parse(Project project, Object source) throws BuildException {
        throw new BuildException("ProjectHelper.parse() must be implemented in a helper plugin "
                                 + this.getClass().getName());
    }

    /* -------------------- Helper discovery -------------------- */
    public static final String HELPER_PROPERTY =
        "org.apache.tools.ant.ProjectHelper";
    
    public static final String SERVICE_ID =
        "/META-INF/services/org.apache.tools.ant.ProjectHelper";

    
    /** Discover a project helper instance. Uses the same patterns
     *  as JAXP, commons-logging, etc: a system property, a JDK1.3
     *  service discovery, default.
     */
    public static ProjectHelper getProjectHelper()
        throws BuildException
    {
        // Identify the class loader we will be using. Ant may be
        // in a webapp or embeded in a different app
        ProjectHelper helper=null;
        
        // First, try the system property
        try {
            String helperClass = System.getProperty(HELPER_PROPERTY);
            if (helperClass != null) {
                helper = newHelper(helperClass);
            }
        } catch (SecurityException e) {
            // It's ok, we'll try next option
            ;
        }

        // A JDK1.3 'service' ( like in JAXP ). That will plug a helper
        // automatically if in CLASSPATH, with the right META-INF/services.
        if( helper==null ) {
            try {
                ClassLoader classLoader=getContextClassLoader();
                InputStream is=null;
                if (classLoader != null) {
                    is=classLoader.getResourceAsStream( SERVICE_ID );
                }
                if( is==null ) {
                    is=ClassLoader.getSystemResourceAsStream( SERVICE_ID );
                }
                
                if( is != null ) {
                    // This code is needed by EBCDIC and other strange systems.
                    // It's a fix for bugs reported in xerces
                    BufferedReader rd;
                    try {
                        rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        rd = new BufferedReader(new InputStreamReader(is));
                    }
                    
                    String helperClassName = rd.readLine();
                    rd.close();
                    
                    if (helperClassName != null &&
                        ! "".equals(helperClassName)) {
                        
                        helper= newHelper( helperClassName );
                    }
                }
            } catch( Exception ex ) {
                ;
            }
        }

        if( helper!=null ) {
            return helper;
        } else {
            // Default
            return new ProjectHelperImpl();
        }
    }

    /** Create a new helper. It'll first try the thread class loader,
     *  then Class.forName() will load from the same loader that
     *  loaded this class.
     */
    private static ProjectHelper newHelper(String helperClass)
        throws BuildException
    {
        ClassLoader classLoader = getContextClassLoader();
        try {
            Class clazz = null;
            if (classLoader != null) {
                try {
                    clazz = classLoader.loadClass(helperClass);
                } catch( ClassNotFoundException ex ) {
                    // try next method
                }
            }
            if( clazz==null ) {
                clazz = Class.forName(helperClass);
            }
            return ((ProjectHelper) clazz.newInstance());
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     *  JDK1.1 compatible access to the context class loader.
     *  Cut&paste from Jaxp.
     */
    public static ClassLoader getContextClassLoader()
        throws BuildException
    {
        // Are we running on a JDK 1.2 or later system?
        Method method = null;
        try {
            method = Thread.class.getMethod("getContextClassLoader", null);
        } catch (NoSuchMethodException e) {
            // we are running on JDK 1.1
            return null; 
        }

        // Get the thread context class loader (if there is one)
        ClassLoader classLoader = null;
        try {
            classLoader = (ClassLoader)
                method.invoke(Thread.currentThread(), null);
        } catch (IllegalAccessException e) {
            throw new BuildException
                ("Unexpected IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            throw new BuildException
                ("Unexpected InvocationTargetException", e);
        }

        // Return the selected class loader
        return (classLoader);
    }

    // -------------------- Static utils, used by most helpers -------------------- 

    /**
     * Configures an object using an introspection handler.
     * 
     * @param target The target object to be configured.
     *               Must not be <code>null</code>.
     * @param attrs  A list of attributes to configure within the target.
     *               Must not be <code>null</code>.
     * @param project The project containing the target. 
     *                Must not be <code>null</code>.
     * 
     * @exception BuildException if any of the attributes can't be handled by
     *                           the target
     */
    public static void configure(Object target, AttributeList attrs, 
                                 Project project) throws BuildException {
        if( target instanceof TaskAdapter ) {
            target=((TaskAdapter)target).getProxy();
        }

        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());

        project.addBuildListener(ih);

        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value=replaceProperties(project, attrs.getValue(i), 
                                           project.getProperties() );
            try {
                ih.setAttribute(project, target, 
                                attrs.getName(i).toLowerCase(Locale.US), value);

            } catch (BuildException be) {
                // id attribute must be set externally
                if (!attrs.getName(i).equals("id")) {
                    throw be;
                }
            }
        }
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     * 
     * @param project The project containing the target. 
     *                Must not be <code>null</code>.
     * @param target  The target object to be configured.
     *                Must not be <code>null</code>.
     * @param buf A character array of the text within the element.
     *            Will not be <code>null</code>.
     * @param start The start element in the array.
     * @param count The number of characters to read from the array.
     * 
     * @exception BuildException if the target object doesn't accept text
     */
    public static void addText(Project project, Object target, char[] buf, int start, int count)
        throws BuildException {
        addText(project, target, new String(buf, start, count));
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     * 
     * @param project The project containing the target. 
     *                Must not be <code>null</code>.
     * @param target  The target object to be configured.
     *                Must not be <code>null</code>.
     * @param text    Text to add to the target.
     *                May be <code>null</code>, in which case this
     *                method call is a no-op.
     * 
     * @exception BuildException if the target object doesn't accept text
     */
    public static void addText(Project project, Object target, String text)
        throws BuildException {

        if (text == null ) {
            return;
        }

        if(target instanceof TaskAdapter) {
            target = ((TaskAdapter) target).getProxy();
        }

        IntrospectionHelper.getHelper(target.getClass()).addText(project, target, text);
    }

    /**
     * Stores a configured child element within its parent object.
     * 
     * @param project Project containing the objects.
     *                May be <code>null</code>.
     * @param parent  Parent object to add child to.
     *                Must not be <code>null</code>.
     * @param child   Child object to store in parent.
     *                Should not be <code>null</code>.
     * @param tag     Name of element which generated the child.
     *                May be <code>null</code>, in which case
     *                the child is not stored.
     */
    public static void storeChild(Project project, Object parent, Object child, String tag) {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parent.getClass());
        ih.storeElement(project, parent, child, tag);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value with 
     * the string value of the corresponding properties.
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>.
     *
     * @exception BuildException if the string contains an opening 
     *                           <code>${</code> without a closing 
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     * 
     * @since 1.5
     */
     public static String replaceProperties(Project project, String value)
            throws BuildException {
         return project.replaceProperties(value);
     }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value 
     * with the string value of the corresponding data types.
     *
     * @param project The container project. This is used solely for
     *                logging purposes. Must not be <code>null</code>.
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     * @param keys  Mapping (String to String) of property names to their 
     *              values. Must not be <code>null</code>.
     * 
     * @exception BuildException if the string contains an opening 
     *                           <code>${</code> without a closing 
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
     public static String replaceProperties(Project project, String value, Hashtable keys)
            throws BuildException {
        if (value == null) {
            return null;
        }

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        while (i.hasMoreElements()) {
            String fragment = (String)i.nextElement();
            if (fragment == null) {
                String propertyName = (String)j.nextElement();
                if (!keys.containsKey(propertyName)) {
                    project.log("Property ${" + propertyName + "} has not been set", Project.MSG_VERBOSE);
                }
                fragment = (keys.containsKey(propertyName)) ? (String) keys.get(propertyName) 
                                                            : "${" + propertyName + "}"; 
            }
            sb.append(fragment);
        }                        
        
        return sb.toString();
    }

    /**
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property 
     * reference from the second list.
     * 
     * @param value     Text to parse. Must not be <code>null</code>.
     * @param fragments List to add text fragments to. 
     *                  Must not be <code>null</code>.
     * @param propertyRefs List to add property names to.
     *                     Must not be <code>null</code>.
     * 
     * @exception BuildException if the string contains an opening 
     *                           <code>${</code> without a closing 
     *                           <code>}</code>
     */
    public static void parsePropertyString(String value, Vector fragments, Vector propertyRefs) 
        throws BuildException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }

            if( pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            }
            else if (value.charAt(pos + 1) != '{' ) {
                fragments.addElement(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new BuildException("Syntax error in property: " 
                                                 + value );
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }

        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }
}
