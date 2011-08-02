package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;

class DirectoryScan
{

    private final File basedir;

    private final Selector selector;

    private final boolean includeDirectories;

    private final boolean includeFiles;

    public DirectoryScan( File basedir, Selector selector, boolean includeDirectories, boolean includeFiles )
    {
        this.basedir = basedir;
        this.selector = selector;
        this.includeDirectories = includeDirectories;
        this.includeFiles = includeFiles;
    }

    public void run()
    {
        String[] children = basedir.list();
        if ( children != null )
        {
            if ( includeDirectories && selector.isSelected( "" ) )
            {
                onDirectory( "", basedir );
            }
            scan( basedir, "", children );
        }
    }

    private void scan( File dir, String pathPrefix, String[] files )
    {
        for ( int i = 0; i < files.length; i++ )
        {
            String pathname = pathPrefix + files[i];
            File file = new File( dir, files[i] );

            String[] children = file.list();

            if ( children == null || ( children.length <= 0 && file.isFile() ) )
            {
                if ( includeFiles && selector.isSelected( pathname ) )
                {
                    onFile( pathname, file );
                }
            }
            else
            {
                if ( includeDirectories && selector.isSelected( pathname ) )
                {
                    onDirectory( pathname, file );
                }
                if ( selector.isAncestorOfPotentiallySelected( pathname ) )
                {
                    scan( file, pathname + File.separatorChar, children );
                }
            }
        }
    }

    protected void onFile( String pathname, File file )
    {
        onItem( pathname, file );
    }

    protected void onDirectory( String pathname, File file )
    {
        onItem( pathname, file );
    }

    protected void onItem( String pathname, File file )
    {

    }

}
