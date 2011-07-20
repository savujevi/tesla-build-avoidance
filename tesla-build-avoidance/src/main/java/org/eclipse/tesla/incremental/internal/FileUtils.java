package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;

class FileUtils
{

    public static File resolve( File file, File basedir )
    {
        File result;
        if ( file == null || file.isAbsolute() )
        {
            result = file;
        }
        else if ( file.getPath().startsWith( File.separator ) || basedir == null )
        {
            result = file.getAbsoluteFile();
        }
        else
        {
            result = new File( basedir, file.getPath() );
        }
        return result;
    }

    public static String relativize( File file, File basedir )
    {
        String pathname;

        String basePath = basedir.getPath();
        String filePath = file.getPath();
        if ( filePath.startsWith( basePath ) )
        {
            if ( filePath.length() == basePath.length() )
            {
                pathname = "";
            }
            else if ( filePath.charAt( basePath.length() ) == File.separatorChar )
            {
                pathname = filePath.substring( basePath.length() + 1 );
            }
            else
            {
                pathname = null;
            }
        }
        else
        {
            pathname = "";
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
        }

        return pathname;
    }

}
