/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Basic Loader that is responsible for all the hackery to get classloader to work.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public final class Main
{
    /**
     * Magic entry point.
     *
     * @param args the CLI arguments
     * @exception Exception if an error occurs
     */
    public final static void main( final String[] args )
        throws Exception
    {
        try
        {
            //actually try to discover the install directory based on where
            // the myrmidon.jar is
            final File installDirectory = findInstallDir();
            System.setProperty( "myrmidon.home", installDirectory.toString() );

            //setup classloader appropriately for myrmidon jar
            final File libDir = new File( installDirectory, "lib" );
            final URL[] libUrls = buildURLList( libDir );
            final URLClassLoader libClassLoader = new URLClassLoader( libUrls );

            final File containerLibDir = new File( installDirectory, "bin" + File.separator + "lib" );
            final URL[] containerLibUrls = buildURLList( containerLibDir );
            final URLClassLoader classLoader = 
                new URLClassLoader( containerLibUrls, libClassLoader );

            //load class and retrieve appropriate main method.
            final Class clazz = classLoader.loadClass( "org.apache.myrmidon.frontends.CLIMain" );
            final Method method = clazz.getMethod( "main", new Class[] { args.getClass() } );

            Thread.currentThread().setContextClassLoader( classLoader );

            //kick the tires and light the fires....
            method.invoke( null, new Object[] { args } );
        }
        catch( final InvocationTargetException ite )
        {
            System.err.println( "Error: " + ite.getTargetException().getMessage() );
            ite.getTargetException().printStackTrace();
        }
        catch( final Throwable throwable )
        {
            System.err.println( "Error: " + throwable.getMessage() );
            throwable.printStackTrace();
        }
    }

    private final static URL[] buildURLList( final File dir )
        throws Exception
    {
        final ArrayList urlList = new ArrayList();

        final File[] contents = dir.listFiles();

        if( null == contents )
        {
            return new URL[ 0 ];
        }

        for( int i = 0; i < contents.length; i++ )
        {
            File file = contents[ i ];

            if( !file.isFile() || !file.canRead() )
            {
                //ignore non-files or unreadable files
                continue;
            }

            final String name = file.getName();
            if( !name.endsWith( ".jar" ) && !name.endsWith( ".zip" ) )
            {
                //Ifnore files in lib dir that are not jars or zips
                continue;
            }

            file = file.getCanonicalFile();
            
            urlList.add( file.toURL() );
        }

        return (URL[])urlList.toArray( new URL[ 0 ] );
    }

    /**
     *  Finds the myrmidon.jar file in the classpath.
     */
    private final static File findInstallDir()
        throws Exception
    {
        final String classpath = System.getProperty( "java.class.path" );
        final String pathSeparator = System.getProperty( "path.separator" );
        final StringTokenizer tokenizer = new StringTokenizer( classpath, pathSeparator );

        while( tokenizer.hasMoreTokens() )
        {
            final String element = tokenizer.nextToken();

            if( element.endsWith( "myrmidon-launcher.jar" ) )
            {
                File file = (new File( element )).getAbsoluteFile();
                file = file.getParentFile();

                if( null != file )
                {
                    file = file.getParentFile();
                }

                return file;
            }
        }

        throw new Exception( "Unable to locate ant.jar in classpath" );
    }
}

class MyClassLoader extends URLClassLoader
{
    MyClassLoader( final URL[] urls, final ClassLoader classLoader )
    {
        super( urls, classLoader );
    }

    public Class loadClass( final String name )
        throws ClassNotFoundException
    {
        System.out.println( "Loading: " + name );
        try
        {
            return super.loadClass( name );
        }
        catch( final ClassNotFoundException cnfe )
        {
            System.out.println( "Failed to Load: " + name );
            throw cnfe;
        }
    }

    protected Class findClass( final String name )
        throws ClassNotFoundException
    {
        System.out.println( "findClass: " + name );
        try
        {
            return super.findClass( name );
        }
        catch( final ClassNotFoundException cnfe )
        {
            System.out.println( "Failed to Load: " + name );
            throw cnfe;
        }
    }

    public Class loadClass( final String name, final boolean resolve )
        throws ClassNotFoundException
    {
        System.out.println( "Loading: " + name );
        try
        {
            return super.loadClass( name, resolve );
        }
        catch( final ClassNotFoundException cnfe )
        {
            System.out.println( "Failed to Load: " + name );
            throw cnfe;
        }
    }
}


