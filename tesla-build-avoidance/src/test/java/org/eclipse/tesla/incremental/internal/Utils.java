package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;

public class Utils
{

    public static byte[] readBytes( File file )
        throws IOException
    {
        RandomAccessFile raf = new RandomAccessFile( file, "r" );
        try
        {
            byte[] bytes = new byte[(int) raf.length()];
            raf.readFully( bytes );
            return bytes;
        }
        finally
        {
            raf.close();
        }
    }

    public static File writeBytes( File file, byte... bytes )
        throws IOException
    {
        file.getAbsoluteFile().getParentFile().mkdirs();
        RandomAccessFile raf = new RandomAccessFile( file, "rw" );
        try
        {
            if ( bytes != null )
            {
                raf.write( bytes );
            }
            return file;
        }
        finally
        {
            raf.close();
        }
    }

    public static void move( File src, File dst )
        throws IOException
    {
        long timestamp = src.lastModified();
        dst.getAbsoluteFile().getParentFile().mkdirs();
        if ( !src.renameTo( dst ) )
        {
            byte[] data = readBytes( src );
            writeBytes( dst, data );
            src.delete();
        }
        dst.setLastModified( timestamp );
    }

    public static void delete( File file )
        throws IOException
    {
        if ( file == null )
        {
            return;
        }

        Collection<File> undeletables = new ArrayList<File>();

        delete( file, undeletables );

        if ( !undeletables.isEmpty() )
        {
            throw new IOException( "Failed to delete " + undeletables );
        }
    }

    private static void delete( File file, Collection<File> undeletables )
    {
        if ( file.isDirectory() )
        {
            for ( String child : file.list() )
            {
                delete( new File( file, child ), undeletables );
            }
        }

        if ( !file.delete() && file.exists() )
        {
            undeletables.add( file.getAbsoluteFile() );
        }
    }

}
