package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Inject;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextFactory;
import org.eclipse.tesla.incremental.PathSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.guice.bean.containers.InjectedTest;

public class DefaultBuildContextTest
    extends InjectedTest
{

    @Inject
    private BuildContextFactory factory;

    private File contextDirectory;

    private File outputDirectory;

    @Before
    public void init()
        throws Exception
    {
        outputDirectory = new File( "target/tests/output" ).getAbsoluteFile();
        contextDirectory =
            new File( "target/tests/" + getClass().getSimpleName() + Long.toHexString( System.currentTimeMillis() ) ).getAbsoluteFile();
        Utils.delete( contextDirectory );
    }

    @After
    public void exit()
        throws Exception
    {
        Utils.delete( contextDirectory );
    }

    private BuildContext newContext()
    {
        return newContext( outputDirectory, "test-plugin:1.0" );
    }

    private BuildContext newContext( File outputDirectory, String pluginId )
    {
        return factory.newContext( outputDirectory, contextDirectory, pluginId );
    }

    @Test
    public void testGetOutputDirectory()
    {
        File outputDirectory = new File( "target/tests/output" + System.nanoTime() ).getAbsoluteFile();
        BuildContext ctx = newContext( outputDirectory, "plugin-id" );
        assertEquals( outputDirectory, ctx.getOutputDirectory() );
    }

    @Test
    public void testLoad_ToleratesCorruptStateFiles()
        throws Exception
    {
        PathSet paths = new PathSet( new File( "" ) );

        BuildContext ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.finish();
        }

        Queue<File> directories = new LinkedList<File>();
        directories.add( contextDirectory );
        while ( !directories.isEmpty() )
        {
            File directory = directories.remove();
            File[] children = directory.listFiles();
            for ( File child : children )
            {
                if ( child.isDirectory() )
                {
                    directories.add( child );
                }
                else
                {
                    new FileOutputStream( child ).close();
                }
            }
        }

        ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.finish();
        }
    }

    @Test
    public void testSetConfiguration_SignalsChangeOfConfiguration()
    {
        PathSet paths = new PathSet( new File( "" ) );

        BuildContext ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.finish();
        }

        ctx = newContext();
        try
        {
            assertEquals( false, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.finish();
        }

        ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 2 } ) );
        }
        finally
        {
            ctx.finish();
        }

        ctx = newContext();
        try
        {
            assertEquals( false, ctx.setConfiguration( paths, new byte[] { 2 } ) );
        }
        finally
        {
            ctx.finish();
        }
    }

    @Test
    public void testSetConfiguration_ConsidersPluginId()
    {
        PathSet paths = new PathSet( new File( "" ) );

        BuildContext ctx = newContext( outputDirectory, "test-plugin:1" );
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.finish();
        }

        ctx = newContext( outputDirectory, "test-plugin:2" );
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.finish();
        }
    }

    @Test
    public void testNewOutputStream_ResolvesPathnameRelativeToOutputDirectory()
        throws Exception
    {
        String name = "dir/subdir/file" + System.currentTimeMillis();
        byte[] data = name.getBytes( "UTF-8" );

        BuildContext ctx = newContext();
        try
        {
            OutputStream os = ctx.newOutputStream( name );
            os.write( data );
            os.close();
        }
        finally
        {
            ctx.finish();
        }

        File output = new File( outputDirectory, name );
        assertEquals( output.getAbsolutePath(), false, output.equals( new File( name ) ) );

        assertEquals( output.getAbsolutePath(), true, output.isFile() );
        byte[] read = Utils.readBytes( output );
        assertArrayEquals( data, read );

        output.delete();
        assertEquals( output.getAbsolutePath(), false, output.exists() );

        ctx = newContext();
        try
        {
            OutputStream os = ctx.newOutputStream( new File( name ) );
            os.write( data );
            os.close();
        }
        finally
        {
            ctx.finish();
        }

        assertEquals( output.getAbsolutePath(), true, output.isFile() );
        read = Utils.readBytes( output );
        assertArrayEquals( data, read );
    }

    @Test
    public void testFinish_DeletesOutputsWhichAreNoLongerTouchedByAnyProcessedInputs()
        throws Exception
    {
        File input = new File( "src/input.java" );
        File output1 = new File( outputDirectory, "output.class" );
        File output2 = new File( outputDirectory, "output$inner.class" );
        File output3 = new File( outputDirectory, "new.class" );

        BuildContext ctx = newContext();
        try
        {
            output1.createNewFile();
            output2.createNewFile();
            ctx.addOutputs( input, output1, output2 );
        }
        finally
        {
            ctx.finish();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        assertEquals( output2.getAbsolutePath(), true, output2.isFile() );

        ctx = newContext();
        try
        {
            ctx.addOutputs( input, output1 );
        }
        finally
        {
            ctx.finish();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        assertEquals( output2.getAbsolutePath(), false, output2.exists() );

        ctx = newContext();
        try
        {
            output3.createNewFile();
            ctx.addOutputs( input, output3 );
        }
        finally
        {
            ctx.finish();
        }

        assertEquals( output3.getAbsolutePath(), true, output3.isFile() );
        assertEquals( output2.getAbsolutePath(), false, output2.exists() );
        assertEquals( output1.getAbsolutePath(), false, output1.exists() );
    }

    @Test
    public void testFinish_KeepsOutputsWhichAreStillTouchedBySomeInputs()
        throws Exception
    {
        File input1 = new File( "src/input1.java" );
        File input2 = new File( "src/input2.java" );
        File output1 = new File( outputDirectory, "output1.class" );
        File output2 = new File( outputDirectory, "output2.class" );

        BuildContext ctx = newContext();
        try
        {
            output1.createNewFile();
            output2.createNewFile();
            ctx.addOutputs( input1, output1 );
            ctx.addOutputs( input1, output2 );
            ctx.addOutputs( input2, output2 );
        }
        finally
        {
            ctx.finish();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        assertEquals( output2.getAbsolutePath(), true, output2.isFile() );

        ctx = newContext();
        try
        {
            ctx.addOutputs( input1, output1 );
        }
        finally
        {
            ctx.finish();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        assertEquals( output2.getAbsolutePath(), true, output2.isFile() );
    }

}
