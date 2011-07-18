package org.eclipse.tesla.incremental.maven;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Properties;

class IOUtils
{

    public static void load( Properties props, Collection<String> filters, File basedir )
        throws IOException
    {
        for ( String filter : filters )
        {
            File propFile = new File( filter );
            if ( !propFile.isAbsolute() )
            {
                propFile = new File( basedir, filter ).getAbsoluteFile();
            }
            FileInputStream is = new FileInputStream( propFile );
            try
            {
                props.load( is );
            }
            finally
            {
                is.close();
            }
        }
    }

    public static void filter( File file, OutputStream os, String encoding, Properties filterProps )
        throws IOException
    {
        BufferedReader reader = IOUtils.newReader( file, encoding );
        BufferedWriter writer = IOUtils.newWriter( os, encoding );
        for ( String line = reader.readLine(); line != null; line = reader.readLine() )
        {
            line = IOUtils.filter( line, filterProps );
            writer.write( line );
            writer.newLine();
        }
        reader.close();
        writer.close();
    }

    private static BufferedReader newReader( File file, String encoding )
        throws IOException
    {
        if ( encoding == null || encoding.length() <= 0 )
        {
            return new BufferedReader( new InputStreamReader( new FileInputStream( file ) ) );
        }
        return new BufferedReader( new InputStreamReader( new FileInputStream( file ), encoding ) );
    }

    private static BufferedWriter newWriter( OutputStream os, String encoding )
        throws IOException
    {
        if ( encoding == null || encoding.length() <= 0 )
        {
            return new BufferedWriter( new OutputStreamWriter( os ) );
        }
        return new BufferedWriter( new OutputStreamWriter( os, encoding ) );
    }

    private static String filter( String string, Properties props )
    {
        String result = string;
        for ( Object key : props.keySet() )
        {
            String val = props.getProperty( key.toString(), "" );
            result = result.replace( "${" + key + "}", val );
        }
        return result;
    }

}
