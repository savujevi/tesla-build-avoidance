package org.eclipse.tesla.incremental.test;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

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

    private static final InheritableThreadLocal<Map<File, Collection<String>>> messages =
        new InheritableThreadLocal<Map<File, Collection<String>>>()
        {
            protected Map<File, Collection<String>> initialValue()
            {
                return Collections.synchronizedMap( new LinkedHashMap<File, Collection<String>>() );
            }
        };

    public static Collection<File> getUpdatedOutputs()
    {
        return updatedOutputs.get();
    }

    public static void clear()
    {
        updatedOutputs.get().clear();
        messages.get().clear();
    }

    public SpyBuildContextManager()
    {
        SpyBuildContextManager.clear();
    }

    @Override
    protected void outputUpdated( Collection<File> outputs )
    {
        updatedOutputs.get().addAll( outputs );

        super.outputUpdated( outputs );
    }

    @Override
    public void logMessage( File input, int line, int column, String message, int severity, Throwable cause )
    {
        Collection<String> messages = SpyBuildContextManager.messages.get().get( input );
        if ( messages == null )
        {
            messages = new ArrayList<String>();
            SpyBuildContextManager.messages.get().put( input, messages );
        }
        messages.add( message );

        super.logMessage( input, line, column, message, severity, cause );
    }

    public static Collection<String> getLogMessages( File input )
    {
        Collection<String> messages = SpyBuildContextManager.messages.get().get( input );
        return messages != null ? messages : Collections.<String> emptySet();
    }
}
