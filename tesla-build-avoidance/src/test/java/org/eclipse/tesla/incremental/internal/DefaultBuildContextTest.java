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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextManager;
import org.eclipse.tesla.incremental.BuildException;
import org.eclipse.tesla.incremental.PathSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.sonatype.guice.bean.containers.InjectedTest;

public class DefaultBuildContextTest
    extends InjectedTest
{

    @Rule
    public TestName testName = new TestName();

    @Inject
    private BuildContextManager manager;

    private File stateDirectory;

    private File inputDirectory;

    private File outputDirectory;

    @Before
    public void init()
        throws Exception
    {
        System.out.println( "========== " + testName.getMethodName() );
        String name = getClass().getSimpleName() + UUID.randomUUID().toString().replace( "-", "" );
        outputDirectory = new File( "target/tests/" + name + "out" ).getAbsoluteFile();
        outputDirectory.mkdirs();
        inputDirectory = new File( "target/tests/" + name + "in" ).getAbsoluteFile();
        inputDirectory.mkdirs();
        stateDirectory = new File( "target/tests/" + name + "ctx" ).getAbsoluteFile();
    }

    @After
    public void exit()
        throws Exception
    {
        Utils.delete( stateDirectory );
        Utils.delete( inputDirectory );
        Utils.delete( outputDirectory );
    }

    private BuildContext newContext()
    {
        return newContext( outputDirectory, "test-plugin:1.0" );
    }

    private BuildContext newContext( File outputDirectory, String builderId )
    {
        return manager.newContext( outputDirectory, stateDirectory, builderId );
    }

    private void assertSetEquals( Collection<?> actual, Object... expected )
    {
        assertEquals( new HashSet<Object>( Arrays.asList( expected ) ), new HashSet<Object>( actual ) );
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
            ctx.close();
        }

        Queue<File> directories = new LinkedList<File>();
        directories.add( stateDirectory );
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

        ( (DefaultBuildContextManager) manager ).buildStates.clear();

        ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.close();
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
            ctx.close();
        }

        ctx = newContext();
        try
        {
            assertEquals( false, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.close();
        }

        ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 2 } ) );
        }
        finally
        {
            ctx.close();
        }

        ctx = newContext();
        try
        {
            assertEquals( false, ctx.setConfiguration( paths, new byte[] { 2 } ) );
        }
        finally
        {
            ctx.close();
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
            ctx.close();
        }

        ctx = newContext( outputDirectory, "test-plugin:2" );
        try
        {
            assertEquals( true, ctx.setConfiguration( paths, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testSetConfiguration_ConsidersPathSet()
    {
        PathSet paths1 = new PathSet( new File( "" ) );
        PathSet paths2 = new PathSet( new File( "" ) ).addIncludes( "*.java" );

        BuildContext ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths1, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.close();
        }

        ctx = newContext();
        try
        {
            assertEquals( true, ctx.setConfiguration( paths2, new byte[] { 1 } ) );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testNewOutputStream_ResolvesPathnameRelativeToCurrentDirectory()
        throws Exception
    {
        String name = "target/tests/dir/subdir/file" + System.currentTimeMillis();
        byte[] data = name.getBytes( "UTF-8" );

        BuildContext ctx = newContext();
        try
        {
            OutputStream os = ctx.newOutputStream( new File( name ) );
            os.write( data );
            os.close();
        }
        finally
        {
            ctx.close();
        }

        File output = new File( name ).getAbsoluteFile();
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
            ctx.close();
        }

        assertEquals( output.getAbsolutePath(), true, output.isFile() );
        read = Utils.readBytes( output );
        assertArrayEquals( data, read );
    }

    @Test
    public void testNewOutputStream_DoesNotModifyOutputFileUnlessContentActuallyChanged()
        throws Exception
    {
        BuildContext ctx = newContext();
        try
        {
            File output = new File( outputDirectory, "test.txt" );

            OutputStream os = ctx.newOutputStream( output );
            os.write( "Hello".getBytes( "UTF-8" ) );
            os.write( " World!".getBytes( "UTF-8" ) );
            os.write( '\n' );
            os.close();

            byte[] data = Utils.readBytes( output );
            assertArrayEquals( "Hello World!\n".getBytes( "UTF-8" ), data );

            long timestamp = System.currentTimeMillis() - 5 * 60 * 1000;
            assertTrue( output.setLastModified( timestamp ) );
            timestamp = output.lastModified();

            os = ctx.newOutputStream( output );
            os.write( 'H' );
            os.write( "ello World!\n".getBytes( "UTF-8" ) );
            os.close();

            assertEquals( timestamp, output.lastModified() );
            data = Utils.readBytes( output );
            assertArrayEquals( "Hello World!\n".getBytes( "UTF-8" ), data );

            os = ctx.newOutputStream( output );
            os.write( 'h' );
            os.write( "ello".getBytes( "UTF-8" ) );
            os.close();

            assertTrue( timestamp != output.lastModified() );
            data = Utils.readBytes( output );
            assertArrayEquals( "hello".getBytes( "UTF-8" ), data );

            assertTrue( output.setLastModified( timestamp ) );
            timestamp = output.lastModified();

            os = ctx.newOutputStream( output );
            os.write( "Hello world!".getBytes( "UTF-8" ) );
            os.close();

            assertTrue( timestamp != output.lastModified() );
            data = Utils.readBytes( output );
            assertArrayEquals( "Hello world!".getBytes( "UTF-8" ), data );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testFinish_DeletesOutputsWhoseInputsHaveBeenDeleted()
        throws Exception
    {
        File input = new File( inputDirectory, "input.java" );
        input.createNewFile();
        File output1 = new File( outputDirectory, "output.class" );
        File output2 = new File( outputDirectory, "output$inner.class" );

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input.java" );
            output1.createNewFile();
            output2.createNewFile();
            ctx.addOutputs( input, output1, output2 );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        assertEquals( output2.getAbsolutePath(), true, output2.isFile() );

        input.delete();
        assertEquals( input.getAbsolutePath(), false, input.exists() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), false, output1.exists() );
        assertEquals( output2.getAbsolutePath(), false, output2.exists() );
    }

    @Test
    public void testFinish_DeletesOutputDirsWhoseInputsHaveBeenDeleted()
        throws Exception
    {
        File input1 = new File( inputDirectory, "input" );
        File input2 = new File( inputDirectory, "input/subdir" );
        input1.mkdirs();
        input2.mkdirs();
        File output1 = new File( outputDirectory, "output" );
        File output2 = new File( outputDirectory, "output/subdir" );

        PathSet paths = new PathSet( inputDirectory ).setKind( PathSet.Kind.FILES_AND_DIRECTORIES );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "", "input", "input" + File.separator + "subdir" );
            output1.mkdirs();
            output2.mkdirs();
            ctx.addOutputs( input1, output1 );
            ctx.addOutputs( input2, output2 );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isDirectory() );
        assertEquals( output2.getAbsolutePath(), true, output2.isDirectory() );

        input2.delete();
        input1.delete();
        assertEquals( input1.getAbsolutePath(), false, input1.exists() );
        assertEquals( input2.getAbsolutePath(), false, input2.exists() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "" );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), false, output1.exists() );
        assertEquals( output2.getAbsolutePath(), false, output2.exists() );
    }

    @Test
    public void testFinish_KeepsOutputsWhoseInputsExistAndHaveBeenExcludedDuringIncrementalBuild()
        throws Exception
    {
        File input = new File( inputDirectory, "input.java" );
        input.createNewFile();
        File output = new File( outputDirectory, "output.class" );

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input.java" );
            output.createNewFile();
            ctx.addOutputs( input, output );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output.getAbsolutePath(), true, output.isFile() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output.getAbsolutePath(), true, output.isFile() );
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
            ctx.close();
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
            ctx.close();
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
            ctx.close();
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
            ctx.close();
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
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        assertEquals( output2.getAbsolutePath(), true, output2.isFile() );
    }

    @Test
    public void testFinish_FinishAgainIsHarmless()
    {
        BuildContext ctx = newContext();
        ctx.close();
        ctx.close();
    }

    @Test( expected = IllegalStateException.class )
    public void testAddOutput_AfterFinishIsInvalid()
    {
        BuildContext ctx = newContext();
        ctx.close();
        ctx.addOutput( inputDirectory, outputDirectory );
    }

    @Test
    public void testGetInputs_IncrementalBuildExcludesUnmodifiedFilesAndDirectories()
        throws Exception
    {
        File input = new File( inputDirectory, "input.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory ).setKind( PathSet.Kind.FILES_AND_DIRECTORIES );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "", "input.java" );
        }
        finally
        {
            ctx.close();
        }

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testGetInputs_FullBuildIncludesUnmodifiedFilesAndDirectories()
        throws Exception
    {
        File input = new File( inputDirectory, "input.java" );
        input.createNewFile();
        File output = new File( outputDirectory, "output.class" );

        PathSet paths = new PathSet( inputDirectory ).setKind( PathSet.Kind.FILES_AND_DIRECTORIES );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "", "input.java" );
            output.createNewFile();
            ctx.addOutputs( inputDirectory, outputDirectory );
            ctx.addOutputs( input, output );
        }
        finally
        {
            ctx.close();
        }

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, true );
            assertSetEquals( inputs, "", "input.java" );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testGetInputs_IncrementalBuildIncludesModifiedFiles()
        throws Exception
    {
        File input = new File( inputDirectory, "input.java" );
        input.createNewFile();
        File output = new File( outputDirectory, "output.class" );

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input.java" );
            output.createNewFile();
            ctx.addOutputs( input, output );
        }
        finally
        {
            ctx.close();
        }

        Utils.writeBytes( input, (byte) 32 );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input.java" );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testGetInputs_IncrementalBuildIncludesUnmodifiedFilesWhoseOutputHasBeenDeleted()
        throws Exception
    {
        File input = new File( inputDirectory, "input.java" );
        input.createNewFile();
        File output = new File( outputDirectory, "output.class" );

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input.java" );
            output.createNewFile();
            ctx.addOutputs( input, output );
        }
        finally
        {
            ctx.close();
        }

        output.delete();
        assertEquals( output.getAbsolutePath(), false, output.exists() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input.java" );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testGetInputs_IncrementalBuildIncludesFilesWhoseOutputIsMissingDueToRenamingOfInput()
        throws Exception
    {
        File input1 = new File( inputDirectory, "input1.java" );
        File input2 = new File( inputDirectory, "dir/input2.java" );
        input1.createNewFile();
        File output1 = new File( outputDirectory, "output1.class" );
        File output2 = new File( outputDirectory, "dir/output2.class" );

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            output1.createNewFile();
            ctx.addOutputs( input1, output1 );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), true, output1.isFile() );
        Utils.move( input1, input2 );
        assertEquals( input1.getAbsolutePath(), false, input1.exists() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "dir" + File.separator + "input2.java" );
            Utils.writeBytes( output2 );
            ctx.addOutputs( input2, output2 );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( output1.getAbsolutePath(), false, output1.exists() );
        assertEquals( output2.getAbsolutePath(), true, output2.isFile() );

        Utils.move( input2, input1 );
        assertEquals( input2.getAbsolutePath(), false, input2.exists() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testAddMessage_ErrorCausesBuildExceptionUponFinish()
        throws Exception
    {
        File input = new File( inputDirectory, "input1.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            ctx.addMessage( input, 0, 0, "test", BuildContext.SEVERITY_ERROR, null );
        }
        finally
        {
            try
            {
                ctx.close();
                fail( "Build errors did not raise exception" );
            }
            catch ( BuildException e )
            {
                assertTrue( true );
            }
        }
    }

    @Test
    public void testAddMessage_UnclearedErrorsCauseBuildExceptionUponFinishOfIncrementalBuild()
        throws Exception
    {
        File input = new File( inputDirectory, "input1.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            ctx.addMessage( input, 0, 0, "test", BuildContext.SEVERITY_ERROR, null );
        }
        finally
        {
            try
            {
                ctx.close();
                fail( "Build errors did not raise exception" );
            }
            catch ( BuildException e )
            {
                assertTrue( true );
            }
        }

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs );
            ctx.close();
            fail( "Build errors did not raise exception" );
        }
        catch ( BuildException e )
        {
            assertTrue( true );
        }
    }

    @Test
    public void testAddMessage_UnclearedErrorsInUnprocessedInputsDoNotCauseBuildExceptionUponFinishOfIncrementalBuild()
        throws Exception
    {
        File input = new File( inputDirectory, "input1.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            ctx.addMessage( input, 0, 0, "test", BuildContext.SEVERITY_ERROR, null );
        }
        finally
        {
            try
            {
                ctx.close();
                fail( "Build errors did not raise exception" );
            }
            catch ( BuildException e )
            {
                assertTrue( true );
            }
        }

        paths.addExcludes( "*.java" );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs );
        }
        finally
        {
            ctx.close();
        }

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, true );
            assertSetEquals( inputs );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testAddMessage_WarningDoesNotCauseBuildExceptionUponFinish()
        throws Exception
    {
        File input = new File( inputDirectory, "input1.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            ctx.addMessage( input, 0, 0, "test", BuildContext.SEVERITY_WARNING, null );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testAddMessage_DeletionOfInputClearsAnyAssociatedErrors()
        throws Exception
    {
        File input = new File( inputDirectory, "input1.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            ctx.addMessage( input, 0, 0, "test", BuildContext.SEVERITY_ERROR, null );
        }
        finally
        {
            try
            {
                ctx.close();
                fail( "Build errors did not raise exception" );
            }
            catch ( BuildException e )
            {
                assertTrue( true );
            }
        }

        input.delete();
        assertEquals( input.getAbsolutePath(), false, input.exists() );

        ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs );
        }
        finally
        {
            ctx.close();
        }
    }

    @Test
    public void testClearMessages_ClearedErrorsDoNotCauseBuildExceptionUponFinish()
        throws Exception
    {
        File input = new File( inputDirectory, "input1.java" );
        input.createNewFile();

        PathSet paths = new PathSet( inputDirectory );

        BuildContext ctx = newContext();
        try
        {
            Collection<String> inputs = ctx.getInputs( paths, false );
            assertSetEquals( inputs, "input1.java" );
            ctx.addMessage( input, 1, 0, "test-1", BuildContext.SEVERITY_ERROR, null );
            ctx.addMessage( input, 2, 0, "test-2", BuildContext.SEVERITY_ERROR, null );
        }
        finally
        {
            try
            {
                ctx.close();
                fail( "Build errors did not raise exception" );
            }
            catch ( BuildException e )
            {
                assertTrue( true );
            }
        }

        ctx = newContext();
        ctx.clearMessages( input );
        ctx.close();
    }

}
