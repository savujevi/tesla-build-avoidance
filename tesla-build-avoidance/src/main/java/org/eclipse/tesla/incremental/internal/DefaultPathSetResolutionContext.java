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

class DefaultPathSetResolutionContext
    implements PathSetResolutionContext
{

    private final File outputDirectory;

    private final PathSet pathSet;

    private final boolean fullBuild;

    private final Selector selector;

    // input -> (timestamp, size)
    private final Map<File, FileState> inputStates;

    // output -> input
    private final Map<File, Collection<File>> inputs;

    // input -> outputs
    private final Map<File, Collection<File>> outputs;

    public DefaultPathSetResolutionContext( BuildContext buildContext, PathSet pathSet, boolean fullBuild,
                                            Map<File, FileState> inputStates, Map<File, Collection<File>> inputs,
                                            Map<File, Collection<File>> outputs )
    {
        this.outputDirectory = buildContext.getOutputDirectory();
        this.pathSet = pathSet;
        this.fullBuild = fullBuild;
        this.inputStates = inputStates;
        this.inputs = inputs;
        this.outputs = outputs;

        selector =
            new Selector( pathSet.getIncludes(), pathSet.getExcludes(), pathSet.isDefaultExcludes(),
                          pathSet.isCaseSensitive() );
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
        for ( File file : inputStates.keySet() )
        {
            String pathname = FileUtils.relativize( file, basedir );
            if ( pathname != null && selector.isSelected( pathname ) && !existingInputs.contains( file ) )
            {
                pathnames.add( pathname );
            }
        }

        return pathnames;
    }

    public Collection<String> getInputPaths( File outputFile )
    {
        Collection<String> pathnames = new ArrayList<String>( 64 );

        Collection<File> inputs = this.inputs.get( outputFile );
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

        return pathnames;
    }

    public boolean isProcessingRequired( File input )
    {
        if ( fullBuild )
        {
            return true;
        }
        FileState previousState = inputStates.get( input );
        if ( previousState == null )
        {
            return true;
        }
        if ( previousState.getTimestamp() != input.lastModified() )
        {
            return true;
        }
        if ( previousState.getSize() != input.length() )
        {
            return true;
        }
        if ( isOutputMissing( input ) )
        {
            return true;
        }
        return false;
    }

    private boolean isOutputMissing( File input )
    {
        Collection<File> outputs = this.outputs.get( input );
        if ( outputs != null )
        {
            for ( File output : outputs )
            {
                if ( !output.exists() )
                {
                    return true;
                }
            }
        }
        return outputs == null || outputs.isEmpty();
    }

}
