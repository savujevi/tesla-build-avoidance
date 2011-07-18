package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.Serializable;

class FileState
    implements Serializable
{

    private static final long serialVersionUID = 61840001373933967L;

    private final long timestamp;

    private final long size;

    public FileState( File file )
    {
        if ( file == null )
        {
            throw new IllegalArgumentException( "file not specified" );
        }
        this.timestamp = file.lastModified();
        this.size = file.length();
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public long getSize()
    {
        return size;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        else if ( obj == null || !obj.getClass().equals( getClass() ) )
        {
            return false;
        }
        FileState that = (FileState) obj;
        return timestamp == that.timestamp && size == that.size;
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + (int) size;
        hash = hash * 31 + (int) timestamp;
        hash = hash * 31 + (int) ( timestamp >> 32 );
        return hash;
    }

    @Override
    public String toString()
    {
        return size + " bytes, " + timestamp;
    }

}
