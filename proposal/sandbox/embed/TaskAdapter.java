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

import java.lang.reflect.Method;



/**
 *  Use introspection to "adapt" an arbitrary Bean ( not extending Task, but with similar
 *  patterns).
 *
 *  The adapter can also be used to wrap tasks that are loaded in a different class loader
 *  by ant, when used in programatic mode.
 *
 * @author Costin Manolache
 */
public class TaskAdapter extends Task {

    private Object proxy;
    private String methodName="execute";
    
    /**
     * Checks a class, whether it is suitable to be adapted by TaskAdapter.
     *
     * Checks conditions only, which are additionally required for a tasks
     * adapted by TaskAdapter. Thus, this method should be called by
     * {@link Project#checkTaskClass}.
     *
     * Throws a BuildException and logs as Project.MSG_ERR for
     * conditions, that will cause the task execution to fail.
     * Logs other suspicious conditions with Project.MSG_WARN.
     */
    public static void checkTaskClass(final Class taskClass, final Project project) {
        // Any task can be used via adapter. If it doesn't have any execute()
        // method, no problem - it will do nothing, but still get an 'id'
        // and be registered in the project reference table and useable by other
        // tasks.
        
        if( true )
            return;

        // don't have to check for interface, since then
        // taskClass would be abstract too.
        try {
            final Method executeM = taskClass.getMethod( "execute", null );
            // don't have to check for public, since
            // getMethod finds public method only.
            // don't have to check for abstract, since then
            // taskClass would be abstract too.
            if(!Void.TYPE.equals(executeM.getReturnType())) {
                final String message =
                    "return type of execute() should be void but was \""+
                    executeM.getReturnType()+"\" in " + taskClass;
                project.log(message, Project.MSG_WARN);
            }
        } catch(NoSuchMethodException e) {
            final String message = "No public execute() in " + taskClass;
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
    }

    private IntrospectionHelper ih;

    void setIntrospectionHelper( IntrospectionHelper ih ) {
        this.ih=ih;
    }

    IntrospectionHelper getIntrospectionHelper() {
        if( ih==null ) {
            ih = IntrospectionHelper.getHelper(target.getClass());
        }
        return ih;
    }
    
    /** Experimental, non-public method for better 'adaptation'
     *
     */
    void setAttribute( String name, String value )
        throws BuildException
    {
        try {
            ih.setAttribute( project, proxy, name, value );
        } catch( BuildException ex ) {
            if( "do".equals( name ) ) {
                setDo( value );
            } else {
                throw ex;
            }
        }
    }
        
    /** Set the 'action' method. This allow beans implementing multiple
     * actions or using methods other than 'execute()' to be used in ant
     * without any modification.
     * 
     *  @ant:experimental 
     */
    public void setDo(String methodName ) {
        this.methodName=methodName;
    }
    
    /**
     * Do the execution.
     */
    public void execute() throws BuildException {
        Method setProjectM = null;
        try {
            Class c = proxy.getClass();
            setProjectM = 
                c.getMethod( "setProject", new Class[] {Project.class});
            if(setProjectM != null) {
                setProjectM.invoke(proxy, new Object[] {project});
            }
        } catch (NoSuchMethodException e) {
            // ignore this if the class being used as a task does not have
            // a set project method.
        } catch( Exception ex ) {
            log("Error setting project in " + proxy.getClass(), 
                Project.MSG_ERR);
            throw new BuildException( ex );
        }


        Method executeM=null;
        try {
            Class c=proxy.getClass();
            executeM=c.getMethod( methodName, new Class[0] );
            if( executeM == null ) {
                log("No public " + methodName + "() in " + proxy.getClass(), Project.MSG_ERR);
                throw new BuildException("No public " + methodName +"() in " + proxy.getClass());
            }
            executeM.invoke(proxy, null);
            return; 
        } catch (java.lang.reflect.InvocationTargetException ie) {
            log("Error in " + proxy.getClass(), Project.MSG_ERR);
            Throwable t = ie.getTargetException();
            if (t instanceof BuildException) {
                throw ((BuildException) t);
            } else {
                throw new BuildException(t);
            }
        } catch( Exception ex ) {
            log("Error in " + proxy.getClass(), Project.MSG_ERR);
            throw new BuildException( ex );
        }

    }
    
    /**
     * Set the target object class
     */
    public void setProxy(Object o) {
        this.proxy = o;
    }

    public Object getProxy() {
        return this.proxy ;
    }

}
