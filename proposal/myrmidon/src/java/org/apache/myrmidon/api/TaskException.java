/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.api;

import org.apache.avalon.framework.CascadingException;

/**
 * TaskException thrown when a problem with tasks etc.
 * It is cascading so that further embedded information can be contained.
 * ie TaskException was caused by IOException etc.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class TaskException
    extends CascadingException
{
    /**
     * Basic constructor with a message
     *
     * @param message the message
     */
    public TaskException( final String message )
    {
        this( message, null );
    }

    /**
     * Constructor that builds cascade so that other exception information can be retained.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public TaskException( final String message, final Throwable throwable )
    {
        super( message, throwable );
    }
}

