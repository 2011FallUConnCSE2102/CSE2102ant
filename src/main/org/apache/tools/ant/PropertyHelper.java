/*
 * Copyright  2002-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;


/* ISSUES:
 - ns param. It could be used to provide "namespaces" for properties, which
 may be more flexible.
 - Object value. In ant1.5 String is used for Properties - but it would be nice
 to support generic Objects (the property remains immutable - you can't change
 the associated object). This will also allow JSP-EL style setting using the
 Object if an attribute contains only the property (name="${property}" could
 avoid Object->String->Object conversion)
 - Currently we "chain" only for get and set property (probably most users
 will only need that - if they need more they can replace the top helper).
 Need to discuss this and find if we need more.
 */

/**
 *
 * Deals with properties - substitution, dynamic properties, etc.
 *
 * This is the same code as in Ant1.5. The main addition is the ability
 * to chain multiple PropertyHelpers and to replace the default.
 *
 * @since Ant 1.6
 */
public class PropertyHelper {

    /**
     * Opaque interface for localproperties
     * Allows a user to retrive, copy and replace
     * the localproperties - currently used by the
     * parallel task.
     */
    public interface LocalProperties {
        /**
         * @return a copy of the local properties
         */
        LocalProperties copy();
    }


    /**   Local Properties */
    private ThreadLocalProperties threadLocalProperties
        = new ThreadLocalProperties();


    private Project project;
    private PropertyHelper next;

    /** Project properties map (usually String to String). */
    private HashMap properties = new HashMap(); // Contains normal and user properties

    /**
     * Map of "user" properties (as created in the Ant task, for example).
     * Note that these key/value pairs are also always put into the
     * project properties, so only the project properties need to be queried.
     * Mapping is String to String.
     */
    private Hashtable userProperties = new Hashtable();

    /**
     * Map of inherited "user" properties - that are those "user"
     * properties that have been created by tasks and not been set
     * from the command line or a GUI tool.
     * Mapping is String to String.
     */
    private Hashtable inheritedProperties = new Hashtable();

    /**
     * Default constructor.
     */
    protected PropertyHelper() {
    }

    // --------------------  Hook management  --------------------

    /**
     * Set the project for which this helper is performing property resolution
     *
     * @param p the project instance.
     */
    public void setProject(Project p) {
        this.project = p;
    }

    /** There are 2 ways to hook into property handling:
     *  - you can replace the main PropertyHelper. The replacement is required
     * to support the same semantics (of course :-)
     *
     *  - you can chain a property helper capable of storing some properties.
     *  Again, you are required to respect the immutability semantics (at
     *  least for non-dynamic properties)
     *
     * @param next the next property helper in the chain.
     */
    public void setNext(PropertyHelper next) {
        this.next = next;
    }

    /**
     * Get the next property helper in the chain.
     *
     * @return the next property helper.
     */
    public PropertyHelper getNext() {
        return next;
    }

    /**
     * Factory method to create a property processor.
     * Users can provide their own or replace it using "ant.PropertyHelper"
     * reference. User tasks can also add themselves to the chain, and provide
     * dynamic properties.
     *
     * @param project the project fro which the property helper is required.
     *
     * @return the project's property helper.
     */
    public static synchronized
        PropertyHelper getPropertyHelper(Project project) {
        PropertyHelper helper
            = (PropertyHelper) project.getReference("ant.PropertyHelper");
        if (helper != null) {
            return helper;
        }
        helper = new PropertyHelper();
        helper.setProject(project);

        project.addReference("ant.PropertyHelper", helper);
        return helper;
    }

    // --------------------  Methods to override  --------------------

    /**
     * Sets a property. Any existing property of the same name
     * is overwritten, unless it is a user property. Will be called
     * from setProperty().
     *
     * If all helpers return false, the property will be saved in
     * the default properties table by setProperty.
     *
     * @param ns   The namespace that the property is in (currently
     *             not used.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @param inherited True if this property is inherited (an [sub]ant[call] property).
     * @param user      True if this property is a user property.
     * @param isNew     True is this is a new property.
     * @return true if this helper has stored the property, false if it
     *    couldn't. Each helper should delegate to the next one (unless it
     *    has a good reason not to).
     */
    public boolean setPropertyHook(String ns, String name,
                                   Object value,
                                   boolean inherited, boolean user,
                                   boolean isNew) {
        if (getNext() != null) {
            boolean subst = getNext().setPropertyHook(ns, name, value,
                    inherited, user, isNew);
            // If next has handled the property
            if (subst) {
                return true;
            }
        }

        // Check if this is a local property
        LocalProperty l = threadLocalProperties.getLocalProperty(name);
        if (l != null) {
            l.setValue(value);
            return true;
        }

        return false;
    }

    /** Get a property. If all hooks return null, the default
     * tables will be used.
     *
     * @param ns namespace of the sought property.
     * @param name name of the sought property.
     * @param user True if this is a user property.
     * @return The property, if returned by a hook, or null if none.
     */
    public Object getPropertyHook(String ns, String name, boolean user) {
        if (getNext() != null) {
            Object o = getNext().getPropertyHook(ns, name, user);
            if (o != null) {
                return o;
            }
        }
        LocalProperty l = threadLocalProperties.getLocalProperty(name);
        if (l != null) {
            return l.getValue();
        }

        // Experimental/Testing, will be removed
        if (name.startsWith("toString:")) {
            name = name.substring("toString:".length());
            Object v = project.getReference(name);
            return (v == null) ? null : v.toString();
        }
        return null;
    }

    /**
     * @return the local properties
     */
    public LocalProperties getLocalProperties() {
        return (LocalProperties) threadLocalProperties.get();
    }

    /**
     * Set the local properties
     * @param localProperties the new local properties, may be null.
     */
    public void setLocalProperties(LocalProperties localProperties) {
        if (localProperties == null) {
            localProperties = new LocalPropertyStack(null);
        }
        threadLocalProperties.set(localProperties);
    }

    /**
     * Set the local properties without overriding the user props
     * Used by ant.java to set the local properties, without
     * modifing the user properties set in the param elements.
     * @param localProperties the new local properties, may be null.
     */
    public void setNotOverrideLocalProperties(
        LocalProperties localProperties) {
        if (localProperties == null) {
            localProperties = new LocalPropertyStack(null);
        }
        LocalPropertyStack s = (LocalPropertyStack) localProperties;
        for (Iterator i = s.props.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            if (userProperties.get(entry.getKey()) != null) {
                i.remove();
            }
        }
        threadLocalProperties.set(localProperties);
    }

    /**
     * Add a local property, with an optional initial value
     *
     * @param name the name of the local property
     * @param value the initial value of the localproperty, may be null
     */
    public void addLocalProperty(String name, Object value) {
        threadLocalProperties.addProperty(name, value);
    }

    /**
     * A new scope for local properties.
     *
     */
    public void enterLocalPropertyScope() {
        threadLocalProperties.enterLocalPropertyScope();
    }

    /**
     * Exit a scope of local properties, removing the
     * local properties in the scope.
     *
     */
    public void exitLocalPropertyScope() {
        threadLocalProperties.exitLocalPropertyScope();
    }

    // -------------------- Optional methods   --------------------
    // You can override those methods if you want to optimize or
    // do advanced things (like support a special syntax).
    // The methods do not chain - you should use them when embedding ant
    // (by replacing the main helper)

    /**
     * Parses a string containing <code>${xxx}</code> style property
     * references into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names.
     * <code>null</code> entries in the first list indicate a property
     * reference from the second list.
     *
     * It can be overridden with a more efficient or customized version.
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
    public void parsePropertyString(String value, Vector fragments,
                                    Vector propertyRefs)
        throws BuildException {
        parsePropertyStringDefault(value, fragments, propertyRefs);
    }

    /**
     * Replaces <code>${xxx}</code> style constructions in the given value
     * with the string value of the corresponding data types.
     *
     * @param ns    The namespace for the property.
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>, in which case this
     *              method returns immediately with no effect.
     * @param keys  Mapping (String to String) of property names to their
     *              values. If <code>null</code>, only project properties will
     *              be used.
     *
     * @exception BuildException if the string contains an opening
     *                           <code>${</code> without a closing
     *                           <code>}</code>
     * @return the original string with the properties replaced, or
     *         <code>null</code> if the original string is <code>null</code>.
     */
    public String replaceProperties(String ns, String value, Hashtable keys)
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
            String fragment = (String) i.nextElement();
            if (fragment == null) {
                String propertyName = (String) j.nextElement();
                Object replacement = null;

                // try to get it from the project or keys
                // Backward compatibility
                if (keys != null) {
                    replacement = keys.get(propertyName);
                }
                if (replacement == null) {
                    replacement = getProperty(ns, propertyName);
                }

                if (replacement == null) {
                    project.log("Property \"" + propertyName
                            + "\" has not been set", Project.MSG_VERBOSE);
                }
                fragment = (replacement != null)
                        ? replacement.toString()
                        : "${" + propertyName + "}";
            }
            sb.append(fragment);
        }
        return sb.toString();
    }

    // -------------------- Default implementation  --------------------
    // Methods used to support the default behavior and provide backward
    // compatibility. Some will be deprecated, you should avoid calling them.


    /** Default implementation of setProperty. Will be called from Project.
     *  This is the original 1.5 implementation, with calls to the hook
     *  added.
     *  @param ns      The namespace for the property (currently not used).
     *  @param name    The name of the property.
     *  @param value   The value to set the property to.
     *  @param verbose If this is true output extra log messages.
     *  @return true if the property is set.
     */
    public synchronized boolean setProperty(String ns, String name,
                                            Object value, boolean verbose) {
        // user (CLI) properties take precedence
        if (null != userProperties.get(name)) {
            if (verbose) {
                project.log("Override ignored for user property \"" + name
                    + "\"", Project.MSG_VERBOSE);
            }
            return false;
        }

        boolean done = setPropertyHook(ns, name, value, false, false, false);
        if (done) {
            return true;
        }

        if (null != properties.get(name) && verbose) {
            project.log("Overriding previous definition of property \"" + name
                + "\"", Project.MSG_VERBOSE);
        }

        if (verbose) {
            project.log("Setting project property: " + name + " -> "
                + value, Project.MSG_DEBUG);
        }
        properties.put(name, value);
        return true;
    }

    /**
     * Sets a property if no value currently exists. If the property
     * exists already, a message is logged and the method returns with
     * no other effect.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @since Ant 1.6
     */
    public synchronized void setNewProperty(String ns, String name,
                                            Object value) {
        LocalProperty local = threadLocalProperties.getLocalProperty(name);
        boolean localPropertySet =
            local != null && local.getValue() != null;
        boolean localProperty = local != null;

        if ((properties.get(name) != null && !localProperty)
            || localPropertySet) {
            project.log("Override ignored for property \"" + name
                        + "\"", Project.MSG_VERBOSE);
            return;
        }

        boolean done = setPropertyHook(ns, name, value, false, false, true);
        if (done) {
            return;
        }

        project.log("Setting project property: " + name + " -> "
            + value, Project.MSG_DEBUG);
        if (name != null && value != null) {
            properties.put(name, value);
        }
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public synchronized void setUserProperty(String ns, String name,
                                             Object value) {
        project.log("Setting ro project property: " + name + " -> "
            + value, Project.MSG_DEBUG);
        userProperties.put(name, value);

        boolean done = setPropertyHook(ns, name, value, false, true, false);
        if (done) {
            return;
        }
        properties.put(name, value);
    }

    /**
     * Sets an inherited user property, which cannot be overwritten by set/unset
     * property calls. Any previous value is overwritten. Also marks
     * these properties as properties that have not come from the
     * command line.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public synchronized void setInheritedProperty(String ns, String name,
                                                  Object value) {
        inheritedProperties.put(name, value);

        project.log("Setting ro project property: " + name + " -> "
            + value, Project.MSG_DEBUG);
        userProperties.put(name, value);

        boolean done = setPropertyHook(ns, name, value, true, false, false);
        if (done) {
            return;
        }
        properties.put(name, value);
    }

    // -------------------- Getting properties  --------------------

    /**
     * Returns the value of a property, if it is set.  You can override
     * this method in order to plug your own storage.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public synchronized Object getProperty(String ns, String name) {
        if (name == null) {
            return null;
        }

        Object o = getPropertyHook(ns, name, false);
        if (o != null || threadLocalProperties.getLocalProperty(name) != null) {
            return o;
        }

        return properties.get(name);
    }
    /**
     * Returns the value of a user property, if it is set.
     *
     * @param ns   The namespace for the property (currently not used).
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public synchronized Object getUserProperty(String ns, String name) {
        if (name == null) {
            return null;
        }
        Object o = getPropertyHook(ns, name, true);
        if (o != null) {
            return o;
        }
        // check if null local property
        if (threadLocalProperties.getLocalProperty(name) != null) {
            return null;
        }

        return  userProperties.get(name);
    }


    // -------------------- Access to property tables  --------------------
    // This is used to support ant call and similar tasks. It should be
    // deprecated, it is possible to use a better (more efficient)
    // mechanism to preserve the context.


    /**
     * Returns a copy of the properties table.
     * @return a hashtable containing all properties
     *         (including user properties and local properties).
     */
    public Hashtable getProperties() {
        Hashtable ret = new Hashtable(properties);
        Map locals = threadLocalProperties.getProps();
        for (Iterator i = locals.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            List l = (List) e.getValue();
            if (l != null && l.size() > 0) {
                LocalProperty p = (LocalProperty) l.get(l.size() - 1);
                if (p.getValue() == null) {
                    if (ret.get(e.getKey()) != null) {
                        ret.remove(e.getKey());
                    }
                } else {
                    ret.put(e.getKey(), p.getValue());
                }
            }
        }
        return ret;

        // There is a better way to save the context. This shouldn't
        // delegate to next, it's for backward compatibility only.
    }

    /**
     * Returns a copy of the user property hashtable
     * @return a hashtable containing just the user properties
     */
    public Hashtable getUserProperties() {
        return new Hashtable(userProperties);
    }

    /**
     * Returns a copy of the local properties
     * @return a map containing the local properties as string->string
     */
    public Map getLocalPropertiesCopy() {
        Map copy = new HashMap();
        Map locals = threadLocalProperties.getProps();
        for (Iterator i = locals.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            List l = (List) e.getValue();
            if (l != null && l.size() > 0) {
                LocalProperty p = (LocalProperty) l.get(l.size() - 1);
                copy.put(e.getKey(), p.getValue());
            }
        }
        return copy;
    }

    /**
     * Copies all user properties that have not been set on the
     * command line or a GUI tool from this instance to the Project
     * instance given as the argument.
     *
     * <p>To copy all "user" properties, you will also have to call
     * {@link #copyUserProperties copyUserProperties}.</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.6
     */
    public void copyInheritedProperties(Project other) {
        Enumeration e = inheritedProperties.keys();
        while (e.hasMoreElements()) {
            String arg = e.nextElement().toString();
            if (other.getUserProperty(arg) != null) {
                continue;
            }
            Object value = inheritedProperties.get(arg);
            other.setInheritedProperty(arg, value.toString());
        }
    }

    /**
     * Copies all user properties that have been set on the command
     * line or a GUI tool from this instance to the Project instance
     * given as the argument.
     *
     * <p>To copy all "user" properties, you will also have to call
     * {@link #copyInheritedProperties copyInheritedProperties}.</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.6
     */
    public void copyUserProperties(Project other) {
        Enumeration e = userProperties.keys();
        while (e.hasMoreElements()) {
            Object arg = e.nextElement();
            if (inheritedProperties.containsKey(arg)) {
                continue;
            }
            Object value = userProperties.get(arg);
            other.setUserProperty(arg.toString(), value.toString());
        }
    }

    // -------------------- Property parsing  --------------------
    // Moved from ProjectHelper. You can override the static method -
    // this is used for backward compatibility (for code that calls
    // the parse method in ProjectHelper).

    /** Default parsing method. It is here only to support backward compatibility
     * for the static ProjectHelper.parsePropertyString().
     */
    static void parsePropertyStringDefault(String value, Vector fragments,
                                    Vector propertyRefs)
        throws BuildException {
        int prev = 0;
        int pos;
        //search for the next instance of $ from the 'prev' position
        while ((pos = value.indexOf("$", prev)) >= 0) {

            //if there was any text before this, add it as a fragment
            //TODO, this check could be modified to go if pos>prev;
            //seems like this current version could stick empty strings
            //into the list
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }
            //if we are at the end of the string, we tack on a $
            //then move past it
            if (pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                //peek ahead to see if the next char is a property or not
                //not a property: insert the char as a literal
                /*
                fragments.addElement(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
                */
                if (value.charAt(pos + 1) == '$') {
                    //backwards compatibility two $ map to one mode
                    fragments.addElement("$");
                    prev = pos + 2;
                } else {
                    //new behaviour: $X maps to $X for all values of X!='$'
                    fragments.addElement(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }

            } else {
                //property found, extract its name or bail on a typo
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new BuildException("Syntax error in property: "
                                                 + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }
        //no more $ signs found
        //if there is any tail to the file, append it
        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }

    /**
     * A holder class for a local property value
     */
    private class LocalProperty {
        private int level;
        private Object value;
        public LocalProperty(int level, Object value) {
            this.level = level;
            this.value = value;
        }

        public LocalProperty copy() {
            return new LocalProperty(level, value);
        }

        public int getLevel() {
            return level;
        }

        public Object getValue() {
            return value;
        }

        void setValue(Object value) {
            this.value = value;
        }
    }

    /**
     * A class implementing a local property stack.
     */
    private class LocalPropertyStack
        implements LocalProperties {
        LocalPropertyStack(LocalPropertyStack owner) {
            if (owner == null) {
                init();
            }
            this.owner = owner;
        }
        private int level = 0;
        private  LocalPropertyStack owner;
        // HashMap<String, ListArray<LocalPropertyValue>>
        private HashMap props;
        

        // ArrayList<ArrayList<String>>
        private List    stack;

        private void init() {
            props = new HashMap();
            stack = new ArrayList();
        }

        private List getStack() {
            return stack == null ? owner.stack : stack;
        }

        public LocalProperties copy() {
            LocalPropertyStack copy = new LocalPropertyStack(null);
            copy.stack = new ArrayList();
            copy.level = level;
            for (int i = 0; i < getStack().size(); ++i) {
                copy.stack.add(((ArrayList) getStack().get(i)).clone());
            }
            copy.props = new HashMap();
            for (Iterator i = getProps().entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                ArrayList from = (ArrayList) entry.getValue();
                List l2 = new ArrayList();
                for (Iterator l = from.iterator(); l.hasNext();) {
                    LocalProperty v = (LocalProperty) l.next();
                    l2.add(v.copy());
                }
                copy.props.put(entry.getKey(), l2);
            }
            return copy;
        }

        private void shallowCopyParent() {
            if (stack != null) {
                return;
            }
            stack = new ArrayList();
            level = owner.level;
            for (int i = 0; i < stack.size(); ++i) {
                stack.add(((ArrayList) owner.stack.get(i)).clone());
            }
            props = new HashMap();
            for (Iterator i = owner.props.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                ArrayList from = (ArrayList) entry.getValue();
                List l2 = new ArrayList();
                for (Iterator l = from.iterator(); l.hasNext();) {
                    LocalProperty v = (LocalProperty) l.next();
                    l2.add(v);
                }
                props.put(entry.getKey(), l2);
            }
        }

        public void enterLocalPropertyScope() {
            if (stack == null) {
                shallowCopyParent();
            }
            stack.add(new ArrayList());
            level++;
        }

        public void addProperty(String name, Object value) {
            if (stack == null) {
                shallowCopyParent();
            }
            if (stack.size() == 0) {
                return;
            }
            List list = (List) stack.get(stack.size() - 1);
            list.add(name);
            List local = (List) props.get(name);
            if (local == null) {
                local = new ArrayList();
                props.put(name, local);
            } else {
                LocalProperty l = (LocalProperty) local.get(local.size() - 1);
                if (l.getLevel() == level) {
                    throw new BuildException(
                        "Attempt to add another local of the same name");
                }
            }
            LocalProperty l = new LocalProperty(level, value);
            local.add(l);
        }

        public void exitLocalPropertyScope() {
            if (stack == null) {
                shallowCopyParent();
            }
            if (stack.size() == 0) {
                return;
            }
            level--;
            List list = (List) stack.remove(stack.size() - 1);
            for (Iterator i = list.iterator(); i.hasNext();) {
                String name = (String) i.next();
                List local = (List) props.get(name);
                if (local != null && local.size() != 0) {
                    local.remove(local.size() - 1);
                    if (local.size() == 0) {
                        props.remove(name);
                    }
                }
            }
        }

        
        public LocalProperty getLocalProperty(String name) {
            if (stack == null) {
                shallowCopyParent();
            }
            if (props == null) {
                return owner.getLocalProperty(name);
            }
            List l = (List) props.get(name);
            if (l != null && l.size() != 0) {
                return (LocalProperty) l.get(l.size() - 1);
            }
            return null;
        }

        public Map getProps() {
            return props == null ? owner.props : props;
        }

    }

    /**
     * A set of local properties stack for each thread
     */

    private class ThreadLocalProperties extends InheritableThreadLocal {
        protected synchronized Object initialValue() {
            return new LocalPropertyStack(null);
        }
        protected synchronized Object childValue(Object obj) {
            //return ((LocalPropertyStack) obj).shallowCopy();
            return new LocalPropertyStack((LocalPropertyStack) obj);
        }
        public LocalProperty getLocalProperty(String name) {
            return ((LocalPropertyStack) get()).getLocalProperty(name);
        }

        public void enterLocalPropertyScope() {
            ((LocalPropertyStack) get()).enterLocalPropertyScope();
        }

        public void addProperty(String name, Object value) {
            ((LocalPropertyStack) get()).addProperty(name, value);
        }

        public void exitLocalPropertyScope() {
            ((LocalPropertyStack) get()).exitLocalPropertyScope();
        }
        public Map getProps() {
            return ((LocalPropertyStack) get()).getProps();
        }
    }

}
