package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.tesla.incremental.PathSet;

class Selector
{

    private static final String DEEP_TREE_SUFFIX = File.separator + "**";

    private static final String[] DEFAULT_EXCLUDES = {

        // Miscellaneous typical temporary files
        "**/*~", "**/#*#", "**/.#*", "**/%*%", "**/._*",

        // CVS
        "**/CVS", "**/CVS/**", "**/.cvsignore",

        // RCS
        "**/RCS", "**/RCS/**",

        // SCCS
        "**/SCCS", "**/SCCS/**",

        // Visual SourceSafe
        "**/vssver.scc",

        // Subversion
        "**/.svn", "**/.svn/**",

        // Arch
        "**/.arch-ids", "**/.arch-ids/**",

        // Bazaar
        "**/.bzr", "**/.bzr/**", "**/.bzrignore",

        // SurroundSCM
        "**/.MySCMServerInfo",

        // Mac
        "**/.DS_Store",

        // Serena Dimensions Version 10
        "**/.metadata", "**/.metadata/**",

        // Mercurial
        "**/.hg", "**/.hg/**", "**/.hgignore", "**/.hgsub", "**/.hgsubstate", "**/.hgtags",

        // git
        "**/.git", "**/.git/**", "**/.gitattributes", "**/.gitignore", "**/.gitmodules",

        // BitKeeper
        "**/BitKeeper", "**/BitKeeper/**", "**/ChangeSet", "**/ChangeSet/**",

        // darcs
        "**/_darcs", "**/_darcs/**", "**/.darcsrepo", "**/.darcsrepo/**", "**/-darcs-backup*", "**/.darcs-temp-mail"

    };

    private final String[] includes;

    private final String[] excludes;

    private final boolean caseSensitive;

    public Selector( PathSet pathSet )
    {
        this( pathSet.getIncludes(), pathSet.getExcludes(), pathSet.isDefaultExcludes(), pathSet.isCaseSensitive() );
    }

    public Selector( Collection<String> includes, Collection<String> excludes, boolean defaultExcludes,
                     boolean caseSensitive )
    {
        Collection<String> normalizedIncludes = new LinkedHashSet<String>( 128 );
        if ( includes != null )
        {
            for ( String include : includes )
            {
                if ( include != null )
                {
                    normalizedIncludes.add( normalizePattern( include ) );
                }
            }
        }

        Collection<String> normalizedExcludes = new LinkedHashSet<String>( 128 );
        if ( excludes != null )
        {
            for ( String exclude : excludes )
            {
                if ( exclude != null )
                {
                    normalizedExcludes.add( normalizePattern( exclude ) );
                }
            }
        }
        if ( defaultExcludes )
        {
            for ( String exclude : DEFAULT_EXCLUDES )
            {
                normalizedExcludes.add( normalizePattern( exclude ) );
            }
        }

        this.includes = normalizedIncludes.toArray( new String[normalizedIncludes.size()] );
        this.excludes = normalizedExcludes.toArray( new String[normalizedExcludes.size()] );
        this.caseSensitive = caseSensitive;
    }

    private static String normalizePattern( String pattern )
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

    public boolean isSelected( String pathname )
    {
        if ( includes.length > 0 && !isMatched( pathname, includes ) )
        {
            return false;
        }
        if ( excludes.length > 0 && isMatched( pathname, excludes ) )
        {
            return false;
        }
        return true;
    }

    private boolean isMatched( String pathname, String[] patterns )
    {
        for ( int i = patterns.length - 1; i >= 0; i-- )
        {
            String pattern = patterns[i];
            if ( SelectorUtils.matchPath( pattern, pathname, caseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean isAncestorOfPotentiallySelected( String pathname )
    {
        return !isEveryDescendantSurelyExcluded( pathname ) && isAnyDescendantPotentiallyIncluded( pathname );
    }

    private boolean isAnyDescendantPotentiallyIncluded( String pathname )
    {
        for ( int i = 0; i < includes.length; i++ )
        {
            if ( SelectorUtils.matchPatternStart( includes[i], pathname, caseSensitive ) )
            {
                return true;
            }
        }
        return includes.length <= 0;
    }

    private boolean isEveryDescendantSurelyExcluded( String pathname )
    {
        for ( int i = 0; i < excludes.length; i++ )
        {
            String exclude = excludes[i];
            if ( exclude.endsWith( DEEP_TREE_SUFFIX )
                && SelectorUtils.matchPath( exclude.substring( 0, exclude.length() - DEEP_TREE_SUFFIX.length() ),
                                            pathname, caseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

}
