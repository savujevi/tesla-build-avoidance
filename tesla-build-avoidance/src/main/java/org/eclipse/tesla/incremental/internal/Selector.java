package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.util.Collection;

class Selector
{

    private static final String[] DEFAULTEXCLUDES = {
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
        "**/_darcs", "**/_darcs/**", "**/.darcsrepo", "**/.darcsrepo/**", "**/-darcs-backup*", "**/.darcs-temp-mail" };

    private String[] includes;

    private String[] excludes;

    public Selector( Collection<String> includes, Collection<String> excludes, boolean defaultExcludes )
    {
        this.includes = includes.toArray( new String[includes.size()] );
        this.excludes =
            excludes.toArray( new String[excludes.size() + ( defaultExcludes ? DEFAULTEXCLUDES.length : 0 )] );
        if ( defaultExcludes )
        {
            System.arraycopy( DEFAULTEXCLUDES, 0, this.excludes, excludes.size(), DEFAULTEXCLUDES.length );
        }
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

    private static boolean isMatched( String pathname, String[] patterns )
    {
        for ( int i = patterns.length - 1; i >= 0; i-- )
        {
            String pattern = patterns[i];
            if ( SelectorUtils.matchPath( pattern, pathname ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean couldHoldIncluded( String name )
    {
        for ( int i = 0; i < includes.length; i++ )
        {
            if ( SelectorUtils.matchPatternStart( includes[i], name ) )
            {
                return true;
            }
        }
        return includes.length <= 0;
    }

}
