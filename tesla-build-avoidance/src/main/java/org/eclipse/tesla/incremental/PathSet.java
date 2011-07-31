package org.eclipse.tesla.incremental;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Describes a collection of files/directories. Path sets are rooted at some base directory from which files/directories
 * are selected via <a href="http://ant.apache.org/manual/dirtasks.html#patterns">glob-like include/exclude
 * patterns</a>.
 */
public class PathSet
    implements Serializable
{

    private static final long serialVersionUID = 6849901814800319427L;

    /**
     * Specifies the kind of paths included in a path set.
     */
    public enum Kind
    {

        /**
         * Denotes a path set that includes only ordinary files.
         */
        FILES_ONLY,

        /**
         * Denotes a path set that includes only directories.
         */
        DIRECTORIES_ONLY,

        /**
         * Denotes a path set that includes only both ordinary files and directories.
         */
        FILES_AND_DIRECTORIES,

    }

    private File basedir;

    private Collection<String> includes = new LinkedHashSet<String>();

    private Collection<String> excludes = new LinkedHashSet<String>();

    private boolean defaultExcludes = true;

    private boolean caseSensitive = true;

    private Kind kind = Kind.FILES_ONLY;

    /**
     * Creates a path set that includes only the specified file.
     * 
     * @param file The file to wrap into a path set, may be {@code null}.
     * @return The resulting path set or {@code null} if the specified file was {@code null}.
     */
    public static PathSet fromFile( File file )
    {
        if ( file == null )
        {
            return null;
        }

        file = file.getAbsoluteFile();
        File basedir = file.getParentFile();
        String include = file.getName();

        return new PathSet( basedir ).addIncludes( include ).setDefaultExcludes( false );
    }

    /**
     * Creates one or more path sets that wrap the specified files.
     * 
     * @param basedir The base directory to resolve any relative files against, may be {@code null} to use the current
     *            directory.
     * @param files The absolute or relative files to wrap into path sets, may be {@code null}.
     * @return The (possibly empty) list of path sets, never {@code null}.
     */
    public static List<PathSet> fromFiles( String basedir, String... files )
    {
        return fromFiles( ( basedir != null ? new File( basedir ) : null ), files );
    }

    /**
     * Creates one or more path sets that wrap the specified files.
     * 
     * @param basedir The base directory to resolve any relative files against, may be {@code null} to use the current
     *            directory.
     * @param files The absolute or relative files to wrap into path sets, may be {@code null}.
     * @return The (possibly empty) list of path sets, never {@code null}.
     */
    public static List<PathSet> fromFiles( File basedir, String... files )
    {
        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsoluteFile();
        }

        List<PathSet> pathSets = new ArrayList<PathSet>();
        PathSet basePathSet = null;

        if ( files != null )
        {
            for ( String file : files )
            {
                if ( file == null )
                {
                    continue;
                }
                File f = new File( file );
                if ( f.isAbsolute() )
                {
                    pathSets.add( fromFile( f ) );
                }
                else if ( f.getPath().startsWith( File.separator ) )
                {
                    pathSets.add( fromFile( f.getAbsoluteFile() ) );
                }
                else
                {
                    if ( basePathSet == null )
                    {
                        basePathSet = new PathSet( basedir ).setDefaultExcludes( false );
                        pathSets.add( basePathSet );
                    }
                    basePathSet.addIncludes( file );
                }
            }
        }

        return pathSets;
    }

    /**
     * Creates a path set rooted at the specified base directory. The new path set includes all ordinary files, uses
     * default exclusions and case-sensitive pattern matching.
     * 
     * @param basedir The (possibly non-existent) base directory, must not be {@code null}.
     */
    public PathSet( File basedir )
    {
        if ( basedir == null )
        {
            throw new IllegalArgumentException( "base directory for path set not specified" );
        }
        this.basedir = basedir.getAbsoluteFile();
    }

    /**
     * Creates a path set rooted at the specified base directory and with the given include/exclude patterns. The new
     * path set includes only ordinary files, uses default exclusions and case-sensitive pattern matching.
     * 
     * @param basedir The (possibly non-existent) base directory, must not be {@code null}.
     * @param includes The include patterns, may be {@code null} or empty to include all files.
     * @param excludes The exclude patterns, may be {@code null} or empty to exclude no files.
     */
    public PathSet( File basedir, String[] includes, String[] excludes )
    {
        this( basedir );
        addIncludes( includes );
        addExcludes( excludes );
    }

    /**
     * Creates a path set rooted at the specified base directory and with the given include/exclude patterns. The new
     * path set includes only ordinary files, uses default exclusions and case-sensitive pattern matching.
     * 
     * @param basedir The (possibly non-existent) base directory, must not be {@code null}.
     * @param includes The include patterns, may be {@code null} or empty to include all files.
     * @param excludes The exclude patterns, may be {@code null} or empty to exclude no files.
     */
    public PathSet( File basedir, Collection<String> includes, Collection<String> excludes )
    {
        this( basedir );
        addIncludes( includes );
        addExcludes( excludes );
    }

    /**
     * Creates a copy of the specified path set.
     * 
     * @param pathSet The path set to copy, must not be {@code null}.
     */
    public PathSet( PathSet pathSet )
    {
        if ( pathSet == null )
        {
            throw new IllegalArgumentException( "path set not specified" );
        }
        this.basedir = pathSet.getBasedir();
        addIncludes( pathSet.getIncludes() );
        addExcludes( pathSet.getExcludes() );
        setDefaultExcludes( pathSet.isDefaultExcludes() );
        setCaseSensitive( pathSet.isCaseSensitive() );
        setKind( pathSet.getKind() );
    }

    /**
     * Gets the base directory of this path set.
     * 
     * @return The (possibly non-existent) base directory, never {@code null}.
     */
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

    /**
     * Gets the include patterns of this path set.
     * 
     * @return The (read-only) include patterns, never {@code null}.
     */
    public Collection<String> getIncludes()
    {
        return Collections.unmodifiableCollection( includes );
    }

    /**
     * Adds the specified include patterns to this path set. Patterns may use both the forward slash and the backward
     * slash to separate directories.
     * 
     * @param includes The include patterns, may be {@code null}.
     * @return This path set for chaining, never {@code null}.
     */
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

    /**
     * Adds the specified include patterns to this path set. Patterns may use both the forward slash and the backward
     * slash to separate directories.
     * 
     * @param includes The include patterns, may be {@code null}.
     * @return This path set for chaining, never {@code null}.
     */
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

    /**
     * Gets the exclude patterns of this path set.
     * 
     * @return The (read-only) exclude patterns, never {@code null}.
     */
    public Collection<String> getExcludes()
    {
        return Collections.unmodifiableCollection( excludes );
    }

    /**
     * Adds the specified exclude patterns to this path set. Patterns may use both the forward slash and the backward
     * slash to separate directories.
     * 
     * @param excludes The exclude patterns, may be {@code null}.
     * @return This path set for chaining, never {@code null}.
     */
    public PathSet addExcludes( String... excludes )
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

    /**
     * Adds the specified exclude patterns to this path set. Patterns may use both the forward slash and the backward
     * slash to separate directories.
     * 
     * @param excludes The exclude patterns, may be {@code null}.
     * @return This path set for chaining, never {@code null}.
     */
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

    /**
     * Indicates whether common default exclude patterns are considered in addition to the explicitly configured
     * exclusions.
     * 
     * @return {@code true} when automatically excluding SCM metadata etc., {@code false} when only using the configured
     *         exclusions.
     */
    public boolean isDefaultExcludes()
    {
        return defaultExcludes;
    }

    /**
     * Specifies whether common default exclude patterns should be considered in addition to the explicitly configured
     * exclusions.
     * 
     * @param defaultExcludes {@code true} to automatically exclude SCM metadata etc., {@code false} to only use the
     *            configured exclusions.
     * @return This path set for chaining, never {@code null}.
     */
    public PathSet setDefaultExcludes( boolean defaultExcludes )
    {
        this.defaultExcludes = defaultExcludes;
        return this;
    }

    /**
     * Indicates whether matching of pathnames with the configured include/exclude patterns is case-sensitive.
     * 
     * @return {@code true} when doing case-sensitive path matching, {@code false} for case-insensitive matching.
     */
    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }

    /**
     * Specifies whether matching of pathnames with the configured include/exclude patterns should be case-sensitive.
     * 
     * @param caseSensitive {@code true} to do case-sensitive path matching, {@code false} for case-insensitive
     *            matching.
     * @return This path set for chaining, never {@code null}.
     */
    public PathSet setCaseSensitive( boolean caseSensitive )
    {
        this.caseSensitive = caseSensitive;
        return this;
    }

    /**
     * Gets the kind of paths included in this path set.
     * 
     * @return The kind of paths included in this path set, never {@code null}.
     */
    public Kind getKind()
    {
        return kind;
    }

    /**
     * Indicates whether this path set includes directories.
     * 
     * @return {@code true} if directories are included, {@code false} if only ordinary files are included.
     */
    public boolean isIncludingDirectories()
    {
        return !PathSet.Kind.FILES_ONLY.equals( getKind() );
    }

    /**
     * Indicates whether this path set includes ordinary files.
     * 
     * @return {@code true} if ordinary files are included, {@code false} if only directories are included.
     */
    public boolean isIncludingFiles()
    {
        return !PathSet.Kind.DIRECTORIES_ONLY.equals( getKind() );
    }

    /**
     * Sets the kind of paths included in this path set.
     * 
     * @param kind The kind of paths, must not be {@code null}.
     * @return This path set for chaining, never {@code null}.
     */
    public PathSet setKind( Kind kind )
    {
        if ( kind == null )
        {
            throw new IllegalArgumentException( "kind of path set not specified" );
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
            && this.defaultExcludes == that.defaultExcludes && this.caseSensitive == that.caseSensitive;
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
        hash = hash * 31 + ( caseSensitive ? 1 : 0 );
        return hash;
    }

    @Override
    public String toString()
    {
        return getBasedir() + ", includes = " + getIncludes() + ", excludes = " + getExcludes()
            + ( isDefaultExcludes() ? "*" : "" ) + ", kind = " + getKind() + ( isCaseSensitive() ? "*" : "" );
    }

}
