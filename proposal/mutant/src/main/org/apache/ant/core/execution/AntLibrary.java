/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.execution;

import org.apache.ant.core.support.*;
import java.util.*;
import java.net.URL;

/**
 * This object represents an Ant library definition. An Ant library
 * is a set of plug-in for Ant consisting primarily of tasks but may include
 * other ant components.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public class AntLibrary {
    /**
     * The task definitions contained by this library
     */
    private Map taskDefinitions = new HashMap();
    
    /**
     * The converter definitions contained by this library
     */
    private Map converterDefinitions = new HashMap();
    
    /**
     * The aspect handler definitions contained by this library
     */
    private Map aspectDefinitions = new HashMap();
    
    /**
     * Add a task definition to this library
     */
    public void addTaskDefinition(TaskDefinition taskDefinition) {
        String taskName = taskDefinition.getName();
        taskDefinitions.put(taskName, taskDefinition);
    }
    
    /**
     * Add a converter definition to this library
     */
    public void addConverterDefinition(ConverterDefinition converterDef) {
        String targetClassname = converterDef.getTargetClassName();
        converterDefinitions.put(targetClassname, converterDef);
    }

    /**
     * Add an aspect handler definition to this library
     */
    public void addAspectDefinition(AspectDefinition aspectDef) {
        String aspectPrefix = aspectDef.getAspectPrefix();
        aspectDefinitions.put(aspectPrefix, aspectDef);
    }

    /**
     * Get the task definitions
     *
     * @return an iterator which returns TaskDefinition objects.
     */
    public Iterator getTaskDefinitions() {
        return taskDefinitions.values().iterator();
    }
    
   
    /**
     * Get the converter definitions
     *
     * @return an iterator which returns ConverterDefinition objects.
     */
    public Iterator getConverterDefinitions() {
        return converterDefinitions.values().iterator();
    }

    /**
     * Get the aspect handler definitions
     *
     * @return an iterator which returns AspectDefinition objects.
     */
    public Iterator getAspectDefinitions() {
        return aspectDefinitions.values().iterator();
    }
    
}

