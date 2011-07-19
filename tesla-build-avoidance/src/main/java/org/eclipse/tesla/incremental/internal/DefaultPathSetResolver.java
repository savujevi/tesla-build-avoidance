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
import java.util.Map;

import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tesla.incremental.PathSet;

@Named
@Component( role = PathSetResolver.class )
public class DefaultPathSetResolver
    implements PathSetResolver
{

    public Collection<Path> resolve( PathSet paths, Map<File, FileState> states, Map<File, Collection<File>> outputs )
    {
        Collection<Path> dirtyPaths = new ArrayList<Path>();
        Collection<File> selectedFiles = new HashSet<File>();

        Selector selector = new Selector( paths.getIncludes(), paths.getExcludes(), paths.isDefaultExcludes() );

        File basedir = paths.getBasedir();
        if ( !PathSet.Kind.FILES_ONLY.equals( paths.getKind() ) && selector.isSelected( "" ) )
        {
            dirtyPaths.add( new Path( "" ) );
            selectedFiles.add( basedir );
        }
        scan( selectedFiles, dirtyPaths, basedir, "", paths.getKind(), selector, states, outputs );

        if ( states != null )
        {
            for ( File file : states.keySet() )
            {
                String pathname = relativize( file, basedir );
                if ( pathname != null && selector.isSelected( pathname ) && !selectedFiles.contains( file ) )
                {
                    dirtyPaths.add( new Path( pathname, true ) );
                }
            }
        }

        return dirtyPaths;
    }

    String relativize( File file, File basedir )
    {
        String pathname = "";
        for ( File current = file; !basedir.equals( current ); )
        {
            String filename = current.getName();
            current = current.getParentFile();
            if ( current == null )
            {
                return null;
            }
            if ( pathname.length() > 0 )
            {
                pathname = filename + File.separatorChar + pathname;
            }
            else
            {
                pathname = filename;
            }
        }
        return pathname;
    }

    private void scan( Collection<File> selectedFiles, Collection<Path> paths, File dir, String pathPrefix,
                       PathSet.Kind kind, Selector selector, Map<File, FileState> states,
                       Map<File, Collection<File>> outputs )
    {
        String[] files = dir.list();
        if ( files == null )
        {
            return;
        }
        for ( int i = 0; i < files.length; i++ )
        {
            String path = pathPrefix + files[i];
            File file = new File( dir, files[i] );
            if ( file.isDirectory() )
            {
                if ( !PathSet.Kind.FILES_ONLY.equals( kind ) && selector.isSelected( path ) )
                {
                    selectedFiles.add( file );
                    paths.add( new Path( path ) );
                }
                if ( selector.couldHoldIncluded( path ) )
                {
                    scan( selectedFiles, paths, file, path + File.separator, kind, selector, states, outputs );
                }
            }
            else if ( file.isFile() )
            {
                if ( !PathSet.Kind.DIRECTORIES_ONLY.equals( kind ) && selector.isSelected( path ) )
                {
                    selectedFiles.add( file );
                    FileState previousState = ( states != null ) ? states.get( file ) : null;
                    if ( previousState == null || previousState.getTimestamp() != file.lastModified()
                        || previousState.getSize() != file.length() || isOutputMissing( file, outputs ) )
                    {
                        paths.add( new Path( path ) );
                    }
                }
            }
        }
    }

    private boolean isOutputMissing( File input, Map<File, Collection<File>> outputs )
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
        return outs == null || outs.isEmpty();
    }

}
