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
package org.apache.ant.common.antlib;
import org.apache.ant.common.util.ExecutionException;

/**
 * An Ant Library Factory is a class is used to create instances of the
 * classes in the AntLibrary. An separate instance of the factory will be
 * created by each ExecutiuonFrame which uses this library. The factory will
 * be created before any instances are required.
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public interface AntLibFactory {
    /**
     * Initialise the factory
     *
     * @param context the factory's context
     * @exception ExecutionException if the factory cannot be initialized
     */
    void init(AntContext context) throws ExecutionException;

    /**
     * Create an instance of the given component class
     *
     * @param componentClass the class for which an instance is required
     * @param localName the name within the library under which the task is
     *      defined
     * @return an instance of the required class
     * @exception InstantiationException if the class cannot be instantiated
     * @exception IllegalAccessException if the instance cannot be accessed
     * @exception ExecutionException if there is a problem creating the task
     */
    Object createComponent(Class componentClass, String localName)
         throws InstantiationException, IllegalAccessException,
        ExecutionException;

    /**
     * Create an instance of the given converter class
     *
     * @param converterClass the converter class for which an instance is
     *      required
     * @return a converter instance
     * @exception InstantiationException if the class cannot be instantiated
     * @exception IllegalAccessException if the instance cannot be accessed
     * @exception ExecutionException if there is a problem creating the
     *      converter
     */
    Converter createConverter(Class converterClass)
         throws InstantiationException, IllegalAccessException,
        ExecutionException;


    /**
     * Register an element which has been created as the result of calling a
     * create method.
     *
     * @param createdElement the element that the component created
     * @exception ExecutionException if there is a problem registering the
     *      element
     */
    void registerCreatedElement(Object createdElement)
         throws ExecutionException;

}

