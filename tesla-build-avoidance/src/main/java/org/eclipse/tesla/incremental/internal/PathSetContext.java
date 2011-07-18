package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.eclipse.tesla.incremental.PathSet;

public class PathSetContext
{

    private final PathSet paths;

    private final Map<File, FileState> states;

    private final Map<File, Collection<File>> outputs;

    PathSetContext( PathSet paths, Map<File, FileState> states, Map<File, Collection<File>> outputs )
    {
        this.paths = paths;
        this.states = states;
        this.outputs = outputs;
    }

    public File getBasedir()
    {
        return paths.getBasedir();
    }

    public boolean isIncludingDirectories()
    {
        return !PathSet.Kind.FILES_ONLY.equals( paths.getKind() );
    }

    public boolean isIncludingFiles()
    {
        return !PathSet.Kind.DIRECTORIES_ONLY.equals( paths.getKind() );
    }

    public boolean isSelected( String pathname )
    {
        return false;
    }

    public Collection<File> getPreviousInputs()
    {
        return states.keySet();
    }

    public boolean isProcessingRequired( File file )
    {
        FileState previousState = ( states != null ) ? states.get( file ) : null;
        if ( previousState == null )
        {
            return true;
        }
        else if ( previousState.getTimestamp() != file.lastModified() )
        {
            return true;
        }
        else if ( previousState.getSize() != file.length() )
        {
            return true;
        }
        else if ( isOutputMissing( file ) )
        {
            return true;
        }
        return false;
    }

    private boolean isOutputMissing( File input )
    {
        Collection<File> outs = outputs.get( input );
        if ( outs != null )
        {
            for ( File out : outs )
            {
                if ( !out.exists() )
                {
                    return true;
                }
            }
        }
        return false;
    }

}
