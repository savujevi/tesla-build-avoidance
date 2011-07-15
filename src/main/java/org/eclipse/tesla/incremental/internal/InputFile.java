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

// TODO: UNUSED, DELETE

class InputFile
    implements Serializable
{

    private static final long serialVersionUID = 907622125255405098L;

    private final File file;

    private final long timestamp;

    private final long size;

    public InputFile( File file )
    {
        if ( file == null )
        {
            throw new IllegalArgumentException( "input file not specified" );
        }
        this.file = file;
        this.timestamp = file.lastModified();
        this.size = file.length();
    }

    public File getFile()
    {
        return file;
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
        InputFile that = (InputFile) obj;
        return file.equals( that.file );
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + file.hashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return getFile() + " @ " + getTimestamp();
    }

}
