/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.tar;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The TarOutputStream writes a UNIX tar archive as an OutputStream. Methods are
 * provided to put entries, and then write their contents by writing to this
 * stream using write().
 *
 * @author Timothy Gerard Endres <a href="mailto:time@ice.com">time@ice.com</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class TarOutputStream
    extends FilterOutputStream
{
    public final static int LONGFILE_ERROR = 0;
    public final static int LONGFILE_TRUNCATE = 1;
    public final static int LONGFILE_GNU = 2;

    private int m_longFileMode = LONGFILE_ERROR;
    private byte[] m_assemBuf;
    private int m_assemLen;
    private TarBuffer m_buffer;
    private int m_currBytes;
    private int m_currSize;

    private boolean m_debug;
    private byte[] m_oneBuf;
    private byte[] m_recordBuf;

    public TarOutputStream( final OutputStream output )
    {
        this( output, TarBuffer.DEFAULT_BLKSIZE, TarBuffer.DEFAULT_RCDSIZE );
    }

    public TarOutputStream( final OutputStream output, final int blockSize )
    {
        this( output, blockSize, TarBuffer.DEFAULT_RCDSIZE );
    }

    public TarOutputStream( final OutputStream output,
                            final int blockSize,
                            final int recordSize )
    {
        super( output );

        m_buffer = new TarBuffer( output, blockSize, recordSize );
        m_debug = false;
        m_assemLen = 0;
        m_assemBuf = new byte[ recordSize ];
        m_recordBuf = new byte[ recordSize ];
        m_oneBuf = new byte[ 1 ];
    }

    /**
     * Sets the debugging flag in this stream's TarBuffer.
     *
     * @param debug The new BufferDebug value
     */
    public void setBufferDebug( boolean debug )
    {
        m_buffer.setDebug( debug );
    }

    /**
     * Sets the debugging flag.
     *
     * @param debugF True to turn on debugging.
     */
    public void setDebug( boolean debug )
    {
        m_debug = debug;
    }

    public void setLongFileMode( final int longFileMode )
    {
        m_longFileMode = longFileMode;
    }

    /**
     * Get the record size being used by this stream's TarBuffer.
     *
     * @return The TarBuffer record size.
     */
    public int getRecordSize()
    {
        return m_buffer.getRecordSize();
    }

    /**
     * Ends the TAR archive and closes the underlying OutputStream. This means
     * that finish() is called followed by calling the TarBuffer's close().
     *
     * @exception IOException Description of Exception
     */
    public void close()
        throws IOException
    {
        finish();
        m_buffer.close();
    }

    /**
     * Close an entry. This method MUST be called for all file entries that
     * contain data. The reason is that we must buffer data written to the
     * stream in order to satisfy the buffer's record based writes. Thus, there
     * may be data fragments still being assembled that must be written to the
     * output stream before this entry is closed and the next entry written.
     *
     * @exception IOException Description of Exception
     */
    public void closeEntry()
        throws IOException
    {
        if( m_assemLen > 0 )
        {
            for( int i = m_assemLen; i < m_assemBuf.length; ++i )
            {
                m_assemBuf[ i ] = 0;
            }

            m_buffer.writeRecord( m_assemBuf );

            m_currBytes += m_assemLen;
            m_assemLen = 0;
        }

        if( m_currBytes < m_currSize )
        {
            final String message = "entry closed at '" + m_currBytes +
                "' before the '" + m_currSize +
                "' bytes specified in the header were written";
            throw new IOException( message );
        }
    }

    /**
     * Ends the TAR archive without closing the underlying OutputStream. The
     * result is that the EOF record of nulls is written.
     *
     * @exception IOException Description of Exception
     */
    public void finish()
        throws IOException
    {
        writeEOFRecord();
    }

    /**
     * Put an entry on the output stream. This writes the entry's header record
     * and positions the output stream for writing the contents of the entry.
     * Once this method is called, the stream is ready for calls to write() to
     * write the entry's contents. Once the contents are written, closeEntry()
     * <B>MUST</B> be called to ensure that all buffered data is completely
     * written to the output stream.
     *
     * @param entry The TarEntry to be written to the archive.
     * @exception IOException Description of Exception
     */
    public void putNextEntry( final TarEntry entry )
        throws IOException
    {
        if( entry.getName().length() >= TarEntry.NAMELEN )
        {

            if( m_longFileMode == LONGFILE_GNU )
            {
                // create a TarEntry for the LongLink, the contents
                // of which are the entry's name
                final TarEntry longLinkEntry =
                    new TarEntry( TarConstants.GNU_LONGLINK,
                                  TarConstants.LF_GNUTYPE_LONGNAME );

                longLinkEntry.setSize( entry.getName().length() + 1 );
                putNextEntry( longLinkEntry );
                write( entry.getName().getBytes() );
                write( 0 );
                closeEntry();
            }
            else if( m_longFileMode != LONGFILE_TRUNCATE )
            {
                final String message = "file name '" + entry.getName() +
                    "' is too long ( > " + TarEntry.NAMELEN + " bytes)";
                throw new IOException( message );
            }
        }

        entry.writeEntryHeader( m_recordBuf );
        m_buffer.writeRecord( m_recordBuf );

        m_currBytes = 0;

        if( entry.isDirectory() )
        {
            m_currSize = 0;
        }
        else
        {
            m_currSize = (int)entry.getSize();
        }
    }

    /**
     * Writes a byte to the current tar archive entry. This method simply calls
     * read( byte[], int, int ).
     *
     * @param data The byte written.
     * @exception IOException Description of Exception
     */
    public void write( final int data )
        throws IOException
    {
        m_oneBuf[ 0 ] = (byte)data;

        write( m_oneBuf, 0, 1 );
    }

    /**
     * Writes bytes to the current tar archive entry. This method simply calls
     * write( byte[], int, int ).
     *
     * @param wBuf The buffer to write to the archive.
     */
    public void write( final byte[] buffer )
        throws IOException
    {
        write( buffer, 0, buffer.length );
    }

    /**
     * Writes bytes to the current tar archive entry. This method is aware of
     * the current entry and will throw an exception if you attempt to write
     * bytes past the length specified for the current entry. The method is also
     * (painfully) aware of the record buffering required by TarBuffer, and
     * manages buffers that are not a multiple of recordsize in length,
     * including assembling records from small buffers.
     *
     * @param buffer The buffer to write to the archive.
     * @param offset The offset in the buffer from which to get bytes.
     * @param count The number of bytes to write.
     */
    public void write( final byte[] buffer,
                       final int offset,
                       final int count )
        throws IOException
    {
        int position = offset;
        int numToWrite = count;
        if( ( m_currBytes + numToWrite ) > m_currSize )
        {
            final String message = "request to write '" + numToWrite +
                "' bytes exceeds size in header of '" + m_currSize + "' bytes";
            throw new IOException( message );
            //
            // We have to deal with assembly!!!
            // The programmer can be writing little 32 byte chunks for all
            // we know, and we must assemble complete records for writing.
            // REVIEW Maybe this should be in TarBuffer? Could that help to
            // eliminate some of the buffer copying.
            //
        }

        if( m_assemLen > 0 )
        {
            if( ( m_assemLen + numToWrite ) >= m_recordBuf.length )
            {
                final int length = m_recordBuf.length - m_assemLen;

                System.arraycopy( m_assemBuf, 0, m_recordBuf, 0,
                                  m_assemLen );
                System.arraycopy( buffer, position, m_recordBuf,
                                  m_assemLen, length );
                m_buffer.writeRecord( m_recordBuf );

                m_currBytes += m_recordBuf.length;
                position += length;
                numToWrite -= length;
                m_assemLen = 0;
            }
            else
            {
                System.arraycopy( buffer, position, m_assemBuf, m_assemLen,
                                  numToWrite );

                position += numToWrite;
                m_assemLen += numToWrite;
                numToWrite -= numToWrite;
            }
        }

        //
        // When we get here we have EITHER:
        // o An empty "assemble" buffer.
        // o No bytes to write (numToWrite == 0)
        //
        while( numToWrite > 0 )
        {
            if( numToWrite < m_recordBuf.length )
            {
                System.arraycopy( buffer, position, m_assemBuf, m_assemLen,
                                  numToWrite );

                m_assemLen += numToWrite;

                break;
            }

            m_buffer.writeRecord( buffer, position );

            int num = m_recordBuf.length;

            m_currBytes += num;
            numToWrite -= num;
            position += num;
        }
    }

    /**
     * Write an EOF (end of archive) record to the tar archive. An EOF record
     * consists of a record of all zeros.
     */
    private void writeEOFRecord()
        throws IOException
    {
        for( int i = 0; i < m_recordBuf.length; ++i )
        {
            m_recordBuf[ i ] = 0;
        }

        m_buffer.writeRecord( m_recordBuf );
    }
}