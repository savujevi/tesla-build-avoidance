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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.PathSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class DefaultBuildContextManagerTest
{

    @Rule
    public TestName testName = new TestName();

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

    @Test
    public void testIsFullBuild_EnforcesFullBuildEvenIfClientAsksForIncrementalBuild()
    {
        final List<Boolean> flags = new ArrayList<Boolean>();

        DefaultBuildContextManager manager = new DefaultBuildContextManager()
        {
            @Override
            protected boolean isFullBuild( File outputDirectory, File stateDirectory, String builderId )
            {
                return true;
            }

            @Override
            protected Collection<Path> resolveInputs( InputResolutionContext context )
            {
                flags.add( context.isFullBuild() );
                return super.resolveInputs( context );
            }
        };

        BuildContext ctx = manager.newContext( outputDirectory, stateDirectory, "test-plugin:0.1" );
        try
        {
            ctx.getInputs( new PathSet( inputDirectory ) );
        }
        finally
        {
            ctx.close();
        }

        assertEquals( flags.toString(), 1, flags.size() );
        assertEquals( Boolean.TRUE, flags.get( 0 ) );
    }

}
