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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class FileUtilsTest
{

    private boolean isFileSystemCaseInsensitive()
    {
        return new File( "target" ).equals( new File( "TARGET" ) );
    }

    @Test
    public void testNormalize_NullFile()
    {
        assertNull( FileUtils.normalize( null ) );
    }

    @Test
    public void testResolve()
    {
        File basedir = new File( "target" ).getAbsoluteFile();
        String pathname = "relative/file.txt";
        assertEquals( new File( basedir, pathname ), FileUtils.resolve( new File( pathname ), basedir ) );
    }

    @Test
    public void testResolve_NullFile()
    {
        assertNull( FileUtils.resolve( null, new File( "" ) ) );
    }

    @Test
    public void testResolve_NullBasedir()
    {
        File workdir = new File( "" ).getAbsoluteFile();
        String pathname = "relative/file.txt";
        assertEquals( new File( workdir, pathname ), FileUtils.resolve( new File( pathname ), null ) );
    }

    @Test
    public void testResolve_RelativeBasedir()
    {
        File basedir = new File( "target" );
        String pathname = "relative/file.txt";
        assertEquals( new File( basedir, pathname ).getAbsoluteFile(),
                      FileUtils.resolve( new File( pathname ), basedir ) );
    }

    @Test
    public void testRelativize()
    {
        File basedir = new File( "target/TESTS" ).getAbsoluteFile();

        List<File> testdirs;
        if ( isFileSystemCaseInsensitive() )
        {
            String path = basedir.getAbsolutePath();
            testdirs =
                Arrays.asList( new File( path ), new File( path.toLowerCase( Locale.ENGLISH ) ),
                               new File( path.toUpperCase( Locale.ENGLISH ) ) );
        }
        else
        {
            testdirs = Arrays.asList( basedir );
        }

        for ( File testdir : testdirs )
        {
            assertEquals( "", FileUtils.relativize( testdir, basedir ) );
            assertEquals( "file.txt", FileUtils.relativize( new File( testdir, "file.txt" ), basedir ) );
            assertEquals( "dir" + File.separator + "file",
                          FileUtils.relativize( new File( testdir, "dir/file" ), basedir ) );
            assertNull( FileUtils.relativize( new File( "" ).getAbsoluteFile(), basedir ) );
        }
    }

    @Test
    public void testRelativize_NullFile()
    {
        assertNull( FileUtils.relativize( null, new File( "" ) ) );
    }

    @Test
    public void testRelativize_NullBasedir()
    {
        File workdir = new File( "" ).getAbsoluteFile();
        File file = new File( workdir, "file.txt" );
        assertEquals( file.getName(), FileUtils.relativize( file, null ) );
    }

    @Test
    public void testRelativize_RelativeBasedir()
    {
        File basedir = new File( "target" );
        File file = new File( basedir, "file.txt" ).getAbsoluteFile();
        assertEquals( file.getName(), FileUtils.relativize( file, basedir ) );
    }

    @Test
    public void testRelativize_RootDriveAsBasedir()
    {
        File basedir = new File( "D:/" );
        File file = new File( basedir, "file.txt" ).getAbsoluteFile();
        assertEquals( file.getName(), FileUtils.relativize( file, basedir ) );
    }

}
