/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.smb;

import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileObject;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.AbstractFileSystemProvider;
import org.apache.aut.vfs.provider.DefaultFileName;
import org.apache.aut.vfs.provider.FileSystem;
import org.apache.aut.vfs.provider.FileSystemProvider;
import org.apache.aut.vfs.provider.ParsedUri;

/**
 * A provider for SMB (Samba, Windows share) file systems.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="file-system" name="smb"
 */
public class SmbFileSystemProvider extends AbstractFileSystemProvider implements FileSystemProvider
{
    private final SmbFileNameParser m_parser = new SmbFileNameParser();

    /**
     * Parses a URI into its components.
     */
    protected ParsedUri parseUri( final FileObject baseFile,
                                  final String uri )
        throws FileSystemException
    {
        return m_parser.parseSmbUri( uri );
    }

    /**
     * Creates the filesystem.
     */
    protected FileSystem createFileSystem( final ParsedUri uri )
        throws FileSystemException
    {
        final ParsedSmbUri smbUri = (ParsedSmbUri)uri;
        final FileName rootName = new DefaultFileName( m_parser, smbUri.getRootUri(), "/" );
        return new SmbFileSystem( getContext(), rootName );
    }
}
