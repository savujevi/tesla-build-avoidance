package org.eclipse.tesla.incremental.internal;

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
import java.util.HashSet;

class DefaultPathSetResolver
    implements PathSetResolver
{

    public Collection<Path> resolve( PathSetResolutionContext context )
    {
        Collection<Path> dirtyPaths = new ArrayList<Path>();
        Collection<File> selectedFiles = new HashSet<File>();

        File basedir = context.getPathSet().getBasedir();
        if ( context.getPathSet().isIncludingDirectories() && context.isSelected( "" ) )
        {
            if ( context.isProcessingRequired( basedir ) )
            {
                dirtyPaths.add( new Path( "" ) );
            }
            selectedFiles.add( basedir );
        }
        scan( selectedFiles, dirtyPaths, basedir, "", context );

        for ( String pathname : context.getDeletedInputPaths( selectedFiles ) )
        {
            dirtyPaths.add( new Path( pathname, true ) );
        }

        return dirtyPaths;
    }

    private void scan( Collection<File> selectedFiles, Collection<Path> paths, File dir, String pathPrefix,
                       PathSetResolutionContext context )
    {
        String[] files = dir.list();
        if ( files == null )
        {
            return;
        }

        boolean includeDirs = context.getPathSet().isIncludingDirectories();
        boolean includeFiles = context.getPathSet().isIncludingFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            String pathname = pathPrefix + files[i];
            File file = new File( dir, files[i] );

            if ( file.isDirectory() )
            {
                if ( includeDirs && context.isSelected( pathname ) )
                {
                    selectedFiles.add( file );
                    if ( context.isProcessingRequired( file ) )
                    {
                        paths.add( new Path( pathname ) );
                    }
                }
                if ( context.isAncestorOfPotentiallySelected( pathname ) )
                {
                    scan( selectedFiles, paths, file, pathname + File.separator, context );
                }
            }
            else if ( file.isFile() )
            {
                if ( includeFiles && context.isSelected( pathname ) )
                {
                    selectedFiles.add( file );
                    if ( context.isProcessingRequired( file ) )
                    {
                        paths.add( new Path( pathname ) );
                    }
                }
            }
        }
    }

}
