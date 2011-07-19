package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

/**
 */
public class Path
{

    private final String path;

    private final boolean deleted;

    public Path( String path )
    {
        this( path, false );
    }

    public Path( String path, boolean deleted )
    {
        if ( path == null )
        {
            throw new IllegalArgumentException( "path not specified" );
        }
        this.path = path;
        this.deleted = deleted;
    }

    public String getPath()
    {
        return path;
    }

    public boolean isDeleted()
    {
        return deleted;
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
        Path that = (Path) obj;
        return this.deleted == that.deleted && this.path.equals( that.path );
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + path.hashCode();
        hash = hash * 31 + ( deleted ? 1 : 0 );
        return hash;
    }

    @Override
    public String toString()
    {
        return getPath() + ( isDeleted() ? " (-)" : " (+)" );
    }

}
