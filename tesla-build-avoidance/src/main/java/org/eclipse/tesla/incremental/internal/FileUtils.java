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
        else if ( file.getPath().startsWith( File.separator ) || basedir == null)
        {
            result = file.getAbsoluteFile();
        }
        else
        {
            result = new File( basedir, file.getPath() );
        }
        return result;
    }

}
