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

}
