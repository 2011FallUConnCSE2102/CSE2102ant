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

import java.util.EventObject;
import org.apache.ant.core.model.*;

/*
 * A BuildEvent indicates the occurence of a significant event
 * in the build.
 *
 * All build events come from an ExecutionFrame or an ExecutionManager.
 * There are a number of different types of event and they will
 * generally be associated with some build element from the build model.
 */
public class BuildEvent extends EventObject {
    public static final int MSG_ERR = 0;
    public static final int MSG_WARN = 1;
    public static final int MSG_INFO = 2;
    public static final int MSG_VERBOSE = 3;
    public static final int MSG_DEBUG = 4;

    public static final int BUILD_STARTED = 1;
    public static final int BUILD_FINISHED = 2;
    public static final int TARGET_STARTED = 3;
    public static final int TARGET_FINISHED = 4;
    public static final int TASK_STARTED = 5;
    public static final int TASK_FINISHED = 6;
    public static final int MESSAGE = 7;

    private int eventType;
    private BuildElement buildElement = null;
    private Throwable cause = null;
    private String message = null;
    private int messagePriority;

    /**
     * Create a build event. 
     * 
     * @param soure the source of the build event.
     * @param eventType the type of the buildEvent.
     * @param buildElement the build element with which the event is associated.
     */
    public BuildEvent(Object source, int eventType, BuildElement buildElement) {
        super(source);
        this.eventType = eventType;
        this.buildElement = buildElement;
    }
    
    /**
     * Create a build event with an associated exception. 
     * 
     * @param soure the source of the build event.
     * @param eventType the type of the buildEvent.
     * @param buildElement the build element with which the event is associated.
     */
    public BuildEvent(Object source, int eventType, BuildElement buildElement, 
                      Throwable cause) {
        this(source, eventType, buildElement);                        
        this.cause = cause;
    }

    /**
     * Create a build event for a message 
     * 
     * @param soure the source of the build event.
     * @param buildElement the build element with which the event is associated.
     * @param message the message associated with this event
     * @param priority the message priority
     */
    public BuildEvent(Object source, BuildElement buildElement, String message, 
                      int priority) {
        this(source, MESSAGE, buildElement);                        
        this.message = message;
        this.messagePriority = priority;
    }
    
    /**
     * Get the type of this event
     *
     * @return the event type
     */
    public int getEventType() {
        return eventType;
    }
    
    /**
     * Get the build element involved in this event.
     *
     * @return the build element to which this event is associated.
     */
    public BuildElement getBuildElement() {
        return buildElement;
    }
    
    /**
     *  Returns the logging message. This field will only be set
     *  for "messageLogged" events.
     *
     */
    public String getMessage() {
        return message;
    }

    /**
     *  Returns the priority of the logging message. This field will only
     *  be set for "messageLogged" events.
     */
    public int getPriority(){
        return messagePriority;
    }

    /**
     *  Returns the exception that was thrown, if any. This field will only
     *  be set for "taskFinished", "targetFinished", and "buildFinished" events.
     */
    public Throwable getCause() {
        return cause;
    }
}
