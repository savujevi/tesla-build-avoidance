package org.eclipse.tesla.incremental;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class PathSet
    implements Serializable
{

    private static final long serialVersionUID = 6849901814800319427L;

    public enum Kind
    {

        FILES_ONLY,

        DIRECTORIES_ONLY,

        FILES_AND_DIRECTORIES,

    }

    private File basedir;

    private Collection<String> includes = new LinkedHashSet<String>();

    private Collection<String> excludes = new LinkedHashSet<String>();

    private boolean defaultExcludes = true;

    private Kind kind;

    public PathSet( File basedir )
    {
        if ( basedir == null )
        {
            throw new IllegalStateException( "base directory not specified" );
        }
        this.basedir = basedir.getAbsoluteFile();
        this.kind = Kind.FILES_ONLY;
    }

    public PathSet( File basedir, String[] includes, String[] excludes )
    {
        if ( basedir == null )
        {
            throw new IllegalStateException( "base directory not specified" );
        }
        this.basedir = basedir.getAbsoluteFile();
        addIncludes( includes );
        addExcludes( excludes );
        this.kind = Kind.FILES_ONLY;
    }

    public PathSet( File basedir, Collection<String> includes, Collection<String> excludes )
    {
        if ( basedir == null )
        {
            throw new IllegalStateException( "base directory not specified" );
        }
        this.basedir = basedir.getAbsoluteFile();
        addIncludes( includes );
        addExcludes( excludes );
        this.kind = Kind.FILES_ONLY;
    }

    public File getBasedir()
    {
        return basedir;
    }

    private String normalizePattern( String pattern )
    {
        String result = pattern;
        if ( pattern != null )
        {
            result = pattern.replace( File.separatorChar == '/' ? '\\' : '/', File.separatorChar );
            if ( result.endsWith( File.separator ) )
            {
                result += "**";
            }
        }
        return result;
    }

    public Collection<String> getIncludes()
    {
        return Collections.unmodifiableCollection( includes );
    }

    public PathSet addIncludes( String... includes )
    {
        if ( includes != null )
        {
            for ( String include : includes )
            {
                if ( include != null )
                {
                    this.includes.add( normalizePattern( include ) );
                }
            }
        }
        return this;
    }

    public PathSet addIncludes( Collection<String> includes )
    {
        if ( includes != null )
        {
            for ( String include : includes )
            {
                if ( include != null )
                {
                    this.includes.add( normalizePattern( include ) );
                }
            }
        }
        return this;
    }

    public Collection<String> getExcludes()
    {
        return Collections.unmodifiableCollection( excludes );
    }

    public PathSet addExcludes( String... excludes )
    {
        if ( excludes != null )
        {
            for ( String exclude : excludes )
            {
                if ( exclude != null )
                {
                    this.includes.add( normalizePattern( exclude ) );
                }
            }
        }
        return this;
    }

    public PathSet addExcludes( Collection<String> excludes )
    {
        if ( excludes != null )
        {
            for ( String exclude : excludes )
            {
                if ( exclude != null )
                {
                    this.excludes.add( normalizePattern( exclude ) );
                }
            }
        }
        return this;
    }

    public boolean isDefaultExcludes()
    {
        return defaultExcludes;
    }

    public PathSet setDefaultExcludes( boolean defaultExcludes )
    {
        this.defaultExcludes = defaultExcludes;
        return this;
    }

    public Kind getKind()
    {
        return kind;
    }

    public PathSet setKind( Kind kind )
    {
        if ( kind == null )
        {
            throw new IllegalArgumentException( "path set kind not specified" );
        }
        this.kind = kind;
        return this;
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
        PathSet that = (PathSet) obj;
        return this.basedir.equals( that.basedir ) && this.kind.equals( that.kind )
            && this.includes.equals( that.includes ) && this.excludes.equals( that.excludes )
            && this.defaultExcludes == that.defaultExcludes;
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = hash * 31 + basedir.hashCode();
        hash = hash * 31 + kind.hashCode();
        hash = hash * 31 + includes.hashCode();
        hash = hash * 31 + excludes.hashCode();
        hash = hash * 31 + ( defaultExcludes ? 1 : 0 );
        return hash;
    }

    @Override
    public String toString()
    {
        return getBasedir() + ", includes = " + getIncludes() + ", excludes = " + getExcludes() + ", kind = "
            + getKind();
    }

}
