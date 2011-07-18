package org.eclipse.tesla.incremental.internal;

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
