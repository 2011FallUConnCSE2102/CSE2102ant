/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.test;

import org.apache.aut.vfs.provider.smb.SmbFileSystemProvider;
import org.apache.aut.vfs.test.AbstractWritableFileSystemTestCase;
import org.apache.aut.vfs.FileObject;

/**
 * Tests for the SMB file system.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class SmbFileSystemTestCase extends AbstractWritableFileSystemTestCase
{
    public SmbFileSystemTestCase( String name )
    {
        super( name );
    }

    /**
     * Returns the URI for the base folder.
     */
    protected FileObject getBaseFolder() throws Exception
    {
        final String uri = System.getProperty( "test.smb.uri" ) + "/read-tests";
        m_manager.addProvider( "smb", new SmbFileSystemProvider() );
        return m_manager.resolveFile( uri );
    }

    /**
     * Returns the URI for the area to do tests in.
     */
    protected FileObject getWriteFolder() throws Exception
    {
        final String uri = System.getProperty( "test.smb.uri" ) + "/write-tests";
        return m_manager.resolveFile( uri );
    }
}
