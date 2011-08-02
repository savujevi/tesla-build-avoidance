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
import java.util.Map;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.PathSet;

class DefaultInputResolutionContext
    implements InputResolutionContext
{

    private final File outputDirectory;

    private final PathSet pathSet;

    private final boolean fullBuild;

    private final Selector selector;

    private final BuildState buildState;

    public DefaultInputResolutionContext( BuildContext buildContext, PathSet pathSet, boolean fullBuild,
                                          BuildState buildState )
    {
        this.outputDirectory = buildContext.getOutputDirectory();
        this.pathSet = pathSet;
        this.fullBuild = fullBuild;
        this.buildState = buildState;

        selector = new GlobSelector( pathSet );
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean isFullBuild()
    {
        return fullBuild;
    }

    public PathSet getPathSet()
    {
        return pathSet;
    }

    public boolean isSelected( String pathname )
    {
        return selector.isSelected( pathname );
    }

    public boolean isAncestorOfPotentiallySelected( String pathname )
    {
        return selector.isAncestorOfPotentiallySelected( pathname );
    }

    public Collection<String> getDeletedInputPaths( Collection<File> existingInputs )
    {
        Collection<String> pathnames = new ArrayList<String>( 64 );

        File basedir = pathSet.getBasedir();
        boolean includeFiles = pathSet.isIncludingFiles();
        boolean includeDirs = pathSet.isIncludingDirectories();

        synchronized ( buildState )
        {
            for ( Map.Entry<File, FileState> entry : buildState.getInputStates().entrySet() )
            {
                if ( entry.getValue().isDirectory() )
                {
                    if ( !includeDirs )
                    {
                        continue;
                    }
                }
                else
                {
                    if ( !includeFiles )
                    {
                        continue;
                    }
                }

                File file = entry.getKey();
                String pathname = FileUtils.relativize( file, basedir );
                if ( pathname != null && selector.isSelected( pathname ) && !existingInputs.contains( file ) )
                {
                    pathnames.add( pathname );
                }
            }
        }

        return pathnames;
    }

    public Collection<String> getInputPaths( File outputFile )
    {
        Collection<String> pathnames = new ArrayList<String>( 64 );

        synchronized ( buildState )
        {
            Collection<File> inputs = buildState.getInputs( outputFile );
            if ( inputs != null && !inputs.isEmpty() )
            {
                File basedir = pathSet.getBasedir();
                boolean includeFiles = pathSet.isIncludingFiles();
                boolean includeDirs = pathSet.isIncludingDirectories();

                for ( File file : inputs )
                {
                    if ( file.isDirectory() )
                    {
                        if ( !includeDirs )
                        {
                            continue;
                        }
                    }
                    else if ( file.isFile() )
                    {
                        if ( !includeFiles )
                        {
                            continue;
                        }
                    }
                    else
                    {
                        continue;
                    }

                    String pathname = FileUtils.relativize( file, basedir );
                    if ( pathname != null && selector.isSelected( pathname ) )
                    {
                        pathnames.add( pathname );
                    }
                }
            }
        }

        return pathnames;
    }

    public boolean isProcessingRequired( File input )
    {
        if ( fullBuild )
        {
            return true;
        }
        return buildState.isProcessingRequired( input );
    }

}
