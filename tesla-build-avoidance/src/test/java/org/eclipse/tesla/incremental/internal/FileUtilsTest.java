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

import org.junit.Test;

public class FileUtilsTest
{

    @Test
    public void testRelativize()
    {
        File basedir = new File( "target/tests" ).getAbsoluteFile();
        assertEquals( "", FileUtils.relativize( basedir, basedir ) );
        assertEquals( "file.txt", FileUtils.relativize( new File( basedir, "file.txt" ), basedir ) );
        assertEquals( "dir" + File.separator + "file", FileUtils.relativize( new File( basedir, "dir/file" ), basedir ) );
        assertNull( FileUtils.relativize( new File( "" ).getAbsoluteFile(), basedir ) );
    }

}
