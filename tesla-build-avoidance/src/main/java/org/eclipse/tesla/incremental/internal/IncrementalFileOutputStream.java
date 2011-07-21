package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Outputs stream that only modifies destination file if new contents actually changed.
 */
class IncrementalFileOutputStream
    extends OutputStream
{

    private final DefaultBuildContext context;

    private final File file;

    private final RandomAccessFile raf;

    private final byte[] buffer;

    private boolean modified;

    public IncrementalFileOutputStream( File file, DefaultBuildContext context )
        throws FileNotFoundException
    {
        this.file = file;
        this.context = context;
        raf = new RandomAccessFile( file, "rw" );
        buffer = new byte[1024 * 16];
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        long pos = raf.getFilePointer();
        if ( pos < raf.length() )
        {
            modified = true;
            raf.setLength( pos );
        }
        if ( !modified && context != null )
        {
            context.addOutput( file, false );
        }
        raf.close();
    }

    @Override
    public synchronized void write( byte[] b, int off, int len )
        throws IOException
    {
        if ( modified )
        {
            raf.write( b, off, len );
        }
        else
        {
            for ( int n = len; n > 0; )
            {
                int read = raf.read( buffer, 0, Math.min( buffer.length, n ) );
                if ( read < 0 || !arrayEquals( b, off + len - n, buffer, 0, read ) )
                {
                    modified = true;
                    if ( read > 0 )
                    {
                        raf.seek( raf.getFilePointer() - read );
                    }
                    raf.write( b, off, len );
                    break;
                }
                else
                {
                    n -= read;
                }
            }
        }
    }

    private boolean arrayEquals( byte[] a1, int off1, byte[] a2, int off2, int len )
    {
        for ( int i = 0; i < len; i++ )
        {
            if ( a1[off1 + i] != a2[off2 + i] )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized void write( int b )
        throws IOException
    {
        if ( modified )
        {
            raf.write( b );
        }
        else
        {
            int i = raf.read();
            if ( i < 0 || i != ( b & 0xFF ) )
            {
                modified = true;
                if ( i >= 0 )
                {
                    raf.seek( raf.getFilePointer() - 1 );
                }
                raf.write( b );
            }
        }
    }

}
