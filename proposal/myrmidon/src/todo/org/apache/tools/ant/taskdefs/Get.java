/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Get a particular file from a URL source. Options include verbose reporting,
 * timestamp based fetches and controlling actions on failures. NB: access
 * through a firewall only works if the whole Java runtime is correctly
 * configured.
 *
 * @author costin@dnt.ro
 * @author gg@grtmail.com (Added Java 1.1 style HTTP basic auth)
 */
public class Get extends AbstractTask
{// required
    private boolean verbose = false;
    private boolean useTimestamp = false;//off by default
    private boolean ignoreErrors = false;
    private String uname = null;
    private String pword = null;// required
    private File dest;
    private URL source;

    /**
     * Where to copy the source file.
     *
     * @param dest Path to file.
     */
    public void setDest( File dest )
    {
        this.dest = dest;
    }

    /**
     * Don't stop if get fails if set to "<CODE>true</CODE>".
     *
     * @param v if "true" then don't report download errors up to ant
     */
    public void setIgnoreErrors( boolean v )
    {
        ignoreErrors = v;
    }

    /**
     * password for the basic auth.
     *
     * @param p password for authentication
     */
    public void setPassword( String p )
    {
        this.pword = p;
    }

    /**
     * Set the URL.
     *
     * @param u URL for the file.
     */
    public void setSrc( URL u )
    {
        this.source = u;
    }

    /**
     * Use timestamps, if set to "<CODE>true</CODE>". <p>
     *
     * In this situation, the if-modified-since header is set so that the file
     * is only fetched if it is newer than the local file (or there is no local
     * file) This flag is only valid on HTTP connections, it is ignored in other
     * cases. When the flag is set, the local copy of the downloaded file will
     * also have its timestamp set to the remote file time. <br>
     * Note that remote files of date 1/1/1970 (GMT) are treated as 'no
     * timestamp', and web servers often serve files with a timestamp in the
     * future by replacing their timestamp with that of the current time. Also,
     * inter-computer clock differences can cause no end of grief.
     *
     * @param v "true" to enable file time fetching
     */
    public void setUseTimestamp( boolean v )
    {
        useTimestamp = v;
    }

    /**
     * Username for basic auth.
     *
     * @param u username for authentication
     */
    public void setUsername( String u )
    {
        this.uname = u;
    }

    /**
     * Be verbose, if set to "<CODE>true</CODE>".
     *
     * @param v if "true" then be verbose
     */
    public void setVerbose( boolean v )
    {
        verbose = v;
    }

    /**
     * Does the work.
     *
     * @exception TaskException Thrown in unrecoverable error.
     */
    public void execute()
        throws TaskException
    {
        if( source == null )
        {
            throw new TaskException( "src attribute is required" );
        }

        if( dest == null )
        {
            throw new TaskException( "dest attribute is required" );
        }

        if( dest.exists() && dest.isDirectory() )
        {
            throw new TaskException( "The specified destination is a directory" );
        }

        if( dest.exists() && !dest.canWrite() )
        {
            throw new TaskException( "Can't write to " + dest.getAbsolutePath() );
        }

        try
        {
            getLogger().info( "Getting: " + source );

            //set the timestamp to the file date.
            long timestamp = 0;

            boolean hasTimestamp = false;
            if( useTimestamp && dest.exists() )
            {
                timestamp = dest.lastModified();
                if( verbose )
                {
                    Date t = new Date( timestamp );
                    getLogger().info( "local file date : " + t.toString() );
                }

                hasTimestamp = true;
            }

            //set up the URL connection
            URLConnection connection = source.openConnection();
            //modify the headers
            //NB: things like user authentication could go in here too.
            if( useTimestamp && hasTimestamp )
            {
                connection.setIfModifiedSince( timestamp );
            }
            // prepare Java 1.1 style credentials
            if( uname != null || pword != null )
            {
                String up = uname + ":" + pword;
                String encoding;
                // check to see if sun's Base64 encoder is available.
                try
                {
                    sun.misc.BASE64Encoder encoder =
                        (sun.misc.BASE64Encoder)Class.forName( "sun.misc.BASE64Encoder" ).newInstance();
                    encoding = encoder.encode( up.getBytes() );

                }
                catch( Exception ex )
                {// sun's base64 encoder isn't available
                    Base64Converter encoder = new Base64Converter();
                    encoding = encoder.encode( up.getBytes() );
                }
                connection.setRequestProperty( "Authorization", "Basic " + encoding );
            }

            //connect to the remote site (may take some time)
            connection.connect();
            //next test for a 304 result (HTTP only)
            if( connection instanceof HttpURLConnection )
            {
                HttpURLConnection httpConnection = (HttpURLConnection)connection;
                if( httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED )
                {
                    //not modified so no file download. just return instead
                    //and trace out something so the user doesn't think that the
                    //download happened when it didnt
                    getLogger().info( "Not modified - so not downloaded" );
                    return;
                }
                // test for 401 result (HTTP only)
                if( httpConnection.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED )
                {
                    getLogger().info( "Not authorized - check " + dest + " for details" );
                    return;
                }

            }

            //REVISIT: at this point even non HTTP connections may support the if-modified-since
            //behaviour -we just check the date of the content and skip the write if it is not
            //newer. Some protocols (FTP) dont include dates, of course.

            FileOutputStream fos = new FileOutputStream( dest );

            InputStream is = null;
            for( int i = 0; i < 3; i++ )
            {
                try
                {
                    is = connection.getInputStream();
                    break;
                }
                catch( IOException ex )
                {
                    getLogger().info( "Error opening connection " + ex );
                }
            }
            if( is == null )
            {
                getLogger().info( "Can't get " + source + " to " + dest );
                if( ignoreErrors )
                {
                    return;
                }
                throw new TaskException( "Can't get " + source + " to " + dest );
            }

            byte[] buffer = new byte[ 100 * 1024 ];
            int length;

            while( ( length = is.read( buffer ) ) >= 0 )
            {
                fos.write( buffer, 0, length );
                if( verbose )
                {
                    System.out.print( "." );
                }
            }
            if( verbose )
            {
                System.out.println();
            }
            fos.close();
            is.close();

            //if (and only if) the use file time option is set, then the
            //saved file now has its timestamp set to that of the downloaded file
            if( useTimestamp )
            {
                long remoteTimestamp = connection.getLastModified();
                if( verbose )
                {
                    Date t = new Date( remoteTimestamp );
                    getLogger().info( "last modified = " + t.toString()
                                      + ( ( remoteTimestamp == 0 ) ? " - using current time instead" : "" ) );
                }

                if( remoteTimestamp != 0 )
                {
                    dest.setLastModified( remoteTimestamp );
                }
            }
        }
        catch( IOException ioe )
        {
            getLogger().info( "Error getting " + source + " to " + dest );
            if( ignoreErrors )
            {
                return;
            }
            throw new TaskException( "Error", ioe );
        }
    }

    /**
     * BASE 64 encoding of a String or an array of bytes. Based on RFC 1421.
     *
     * @author Unknown
     * @author <a HREF="gg@grtmail.com">Gautam Guliani</a>
     */

    class Base64Converter
    {

        public final char[] alphabet = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', //  0 to  7
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', //  8 to 15
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', // 16 to 23
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', // 24 to 31
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', // 32 to 39
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', // 40 to 47
            'w', 'x', 'y', 'z', '0', '1', '2', '3', // 48 to 55
            '4', '5', '6', '7', '8', '9', '+', '/'};// 56 to 63

        public String encode( String s )
        {
            return encode( s.getBytes() );
        }

        public String encode( byte[] octetString )
        {
            int bits24;
            int bits6;

            char[] out
                = new char[ ( ( octetString.length - 1 ) / 3 + 1 ) * 4 ];

            int outIndex = 0;
            int i = 0;

            while( ( i + 3 ) <= octetString.length )
            {
                // store the octets
                bits24 = ( octetString[ i++ ] & 0xFF ) << 16;
                bits24 |= ( octetString[ i++ ] & 0xFF ) << 8;

                bits6 = ( bits24 & 0x00FC0000 ) >> 18;
                out[ outIndex++ ] = alphabet[ bits6 ];
                bits6 = ( bits24 & 0x0003F000 ) >> 12;
                out[ outIndex++ ] = alphabet[ bits6 ];
                bits6 = ( bits24 & 0x00000FC0 ) >> 6;
                out[ outIndex++ ] = alphabet[ bits6 ];
                bits6 = ( bits24 & 0x0000003F );
                out[ outIndex++ ] = alphabet[ bits6 ];
            }

            if( octetString.length - i == 2 )
            {
                // store the octets
                bits24 = ( octetString[ i ] & 0xFF ) << 16;
                bits24 |= ( octetString[ i + 1 ] & 0xFF ) << 8;
                bits6 = ( bits24 & 0x00FC0000 ) >> 18;
                out[ outIndex++ ] = alphabet[ bits6 ];
                bits6 = ( bits24 & 0x0003F000 ) >> 12;
                out[ outIndex++ ] = alphabet[ bits6 ];
                bits6 = ( bits24 & 0x00000FC0 ) >> 6;
                out[ outIndex++ ] = alphabet[ bits6 ];

                // padding
                out[ outIndex++ ] = '=';
            }
            else if( octetString.length - i == 1 )
            {
                // store the octets
                bits24 = ( octetString[ i ] & 0xFF ) << 16;
                bits6 = ( bits24 & 0x00FC0000 ) >> 18;
                out[ outIndex++ ] = alphabet[ bits6 ];
                bits6 = ( bits24 & 0x0003F000 ) >> 12;
                out[ outIndex++ ] = alphabet[ bits6 ];

                // padding
                out[ outIndex++ ] = '=';
                out[ outIndex++ ] = '=';
            }

            return new String( out );
        }
    }
}
