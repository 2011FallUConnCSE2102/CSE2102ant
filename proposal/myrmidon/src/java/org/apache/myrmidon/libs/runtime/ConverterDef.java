/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.libs.runtime;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.converter.ConverterRegistry;
import org.apache.myrmidon.components.type.DefaultTypeFactory;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.converter.Converter;

/**
 * Task to define a converter.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class ConverterDef
    extends AbstractTask
    implements Composable
{
    private String              m_sourceType;
    private String              m_destinationType;
    private File                m_lib;
    private String              m_classname;
    private ConverterRegistry   m_converterRegistry;
    private TypeManager         m_typeManager;

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_converterRegistry = (ConverterRegistry)componentManager.lookup( ConverterRegistry.ROLE );
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
    }

    public void setLib( final File lib )
    {
        m_lib = lib;
    }

    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    public void setSourceType( final String sourceType )
    {
        m_sourceType = sourceType;
    }

    public void setDestinationType( final String destinationType )
    {
        m_destinationType = destinationType;
    }

    public void execute()
        throws TaskException
    {
        if( null == m_classname )
        {
            throw new TaskException( "Must specify classname parameter" );
        }
        else if( null == m_sourceType )
        {
            throw new TaskException( "Must specify the source-type parameter" );
        }
        else if( null == m_destinationType )
        {
            throw new TaskException( "Must specify the destination-type parameter" );
        }
        else if( null == m_lib )
        {
            throw new TaskException( "Must specify the lib parameter" );
        }

        try
        {
            m_converterRegistry.registerConverter( m_classname, m_sourceType, m_destinationType );

            final URL url = m_lib.toURL();
            final DefaultTypeFactory factory = new DefaultTypeFactory( new URL[] { url } );
            factory.addNameClassMapping( m_classname, m_classname );

            m_typeManager.registerType( Converter.ROLE, m_classname, factory );
        }
        catch( final Exception e )
        {
            throw new TaskException( "Failed to register converter " + m_classname, e );
        }
    }
}
