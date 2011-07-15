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

import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tesla.incremental.PathSet;

@Named
@Component( role = PathSetResolver.class )
public class DefaultPathSetResolver
    implements PathSetResolver
{

    public Collection<Path> resolve( PathSet paths )
    {
        Selector selector = new Selector( paths.getIncludes(), paths.getExcludes(), paths.isDefaultExcludes() );
        Collection<Path> result = new ArrayList<Path>();
        scan( result, paths.getBasedir(), "", paths.getKind(), selector );
        return result;
    }

    private void scan( Collection<Path> paths, File dir, String pathPrefix, PathSet.Kind kind, Selector selector )
    {
        String[] files = dir.list();
        if ( files == null || files.length <= 0 )
        {
            return;
        }
        for ( int i = 0; i < files.length; i++ )
        {
            String path = pathPrefix + files[i];
            File file = new File( path );
            if ( file.isDirectory() )
            {
                if ( !PathSet.Kind.FILES_ONLY.equals( kind ) && selector.isSelected( path ) )
                {
                    paths.add( new Path( path, Path.State.ADDED ) );
                }
                if ( selector.couldHoldIncluded( path ) )
                {
                    scan( paths, file, path + File.separator, kind, selector );
                }
            }
            else if ( file.isFile() )
            {
                if ( !PathSet.Kind.DIRECTORIES_ONLY.equals( kind ) && selector.isSelected( path ) )
                {
                    paths.add( new Path( path, Path.State.ADDED ) );
                }
            }
        }
    }

}
