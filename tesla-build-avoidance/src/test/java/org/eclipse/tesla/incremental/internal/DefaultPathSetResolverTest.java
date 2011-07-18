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

import javax.inject.Inject;

import org.junit.Test;
import org.sonatype.guice.bean.containers.InjectedTest;

public class DefaultPathSetResolverTest
    extends InjectedTest
{

    @Inject
    private DefaultPathSetResolver resolver;

    @Test
    public void testRelativize()
    {
        File basedir = new File( "target/tests" ).getAbsoluteFile();
        assertEquals( "", resolver.relativize( basedir, basedir ) );
        assertEquals( "file.txt", resolver.relativize( new File( basedir, "file.txt" ), basedir ) );
        assertEquals( "dir" + File.separator + "file", resolver.relativize( new File( basedir, "dir/file" ), basedir ) );
        assertNull( resolver.relativize( new File( "" ).getAbsoluteFile(), basedir ) );
    }

}
