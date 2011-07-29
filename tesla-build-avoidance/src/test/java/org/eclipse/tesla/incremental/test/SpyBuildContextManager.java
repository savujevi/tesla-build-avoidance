package org.eclipse.tesla.incremental.test;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.tesla.incremental.internal.DefaultBuildContextManager;

/**
 * A build context manager specialised to help unit testing of components using the incremental build support.
 */
public class SpyBuildContextManager
    extends DefaultBuildContextManager
{

    private static final InheritableThreadLocal<Collection<File>> updatedOutputs =
        new InheritableThreadLocal<Collection<File>>()
        {
            protected Collection<File> initialValue()
            {
                return Collections.synchronizedSet( new LinkedHashSet<File>() );
            }
        };

    public static Collection<File> getUpdatedOutputs()
    {
        return updatedOutputs.get();
    }

    public static void clear()
    {
        updatedOutputs.get().clear();
    }

    public SpyBuildContextManager()
    {
        updatedOutputs.get().clear();
    }

    @Override
    protected void outputUpdated( Collection<File> outputs )
    {
        updatedOutputs.get().addAll( outputs );

        super.outputUpdated( outputs );
    }

}
