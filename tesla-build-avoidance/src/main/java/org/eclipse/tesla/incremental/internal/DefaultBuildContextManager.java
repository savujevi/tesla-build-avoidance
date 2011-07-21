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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextManager;
import org.eclipse.tesla.incremental.Digester;

@Named
@Singleton
@Component( role = BuildContextManager.class )
public class DefaultBuildContextManager
    implements BuildContextManager
{

    @Requirement
    protected Logger log;

    final Map<File, WeakReference<BuildState>> buildStates;

    public DefaultBuildContextManager()
    {
        this( null );
    }

    @Inject
    public DefaultBuildContextManager( Logger log )
    {
        this.log = ( log != null ) ? log : NullLogger.INSTANCE;
        buildStates = new HashMap<File, WeakReference<BuildState>>();
    }

    public void addMessage( File input, int line, int column, String message, int severity, Throwable cause )
    {
        String msg = getMessage( input, line, column, message );
        switch ( severity )
        {
            case BuildContext.SEVERITY_WARNING:
                log.warn( msg, cause );
                break;
            case BuildContext.SEVERITY_ERROR:
                log.error( msg, cause );
                break;
            default:
                log.debug( msg, cause );
                break;
        }
    }

    protected String getMessage( File file, int line, int column, String message )
    {
        StringBuilder sb = new StringBuilder( 256 );
        sb.append( file.getAbsolutePath() );
        if ( line > 0 )
        {
            sb.append( " [" );
            sb.append( line );
            if ( column > 0 )
            {
                sb.append( ':' );
                sb.append( column );
            }
            sb.append( "]" );
        }
        sb.append( ": " );
        sb.append( message );
        return sb.toString();
    }

    public void clearMessages( File input )
    {
        // defaults to noop
    }

    public BuildContext newContext( File outputDirectory, File contextDirectory, String pluginId )
    {
        BuildState buildState = getBuildState( outputDirectory, contextDirectory, pluginId );
        return new DefaultBuildContext( this, outputDirectory, buildState );
    }

    protected Digester newDigester()
    {
        return new DefaultDigester();
    }

    protected BuildState getBuildState( File outputDirectory, File contextDirectory, String pluginId )
    {
        File stateFile = getStateFile( outputDirectory, contextDirectory, pluginId );

        synchronized ( buildStates )
        {
            BuildState buildState = null;

            WeakReference<BuildState> ref = buildStates.get( stateFile );
            if ( ref != null )
            {
                buildState = ref.get();
            }

            purgeBuildStates();

            if ( buildState == null )
            {
                try
                {
                    buildState = BuildState.load( stateFile );
                }
                catch ( IOException e )
                {
                    buildState = new BuildState( stateFile );
                    if ( stateFile.isFile() )
                    {
                        log.warn( "Could not deserialize incremental build state from " + stateFile,
                                  log.isDebugEnabled() ? e : null );
                    }
                }
                buildStates.put( stateFile, new WeakReference<BuildState>( buildState ) );
            }

            return buildState;
        }
    }

    private void purgeBuildStates()
    {
        for ( Iterator<Map.Entry<File, WeakReference<BuildState>>> it = buildStates.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<File, WeakReference<BuildState>> entry = it.next();
            if ( entry.getValue().get() == null )
            {
                it.remove();
            }
        }
    }

    protected File getStateFile( File outputDirectory, File contextDirectory, String pluginId )
    {
        String name = outputDirectory.getName();
        name = name.substring( 0, Math.min( 4, name.length() ) ) + Integer.toHexString( name.hashCode() );
        File workDir = new File( contextDirectory.getAbsolutePath(), name );
        return new File( workDir, pluginId.substring( 0, Math.min( 4, pluginId.length() ) )
            + Integer.toHexString( pluginId.hashCode() ) + ".ser" );
    }

    protected void outputUpdated( Collection<File> outputs )
    {
        // defaults to noop, useful for refreshing of IDE
    }

    protected Collection<Path> resolve( PathSetResolutionContext context )
    {
        Collection<Path> dirtyPaths = new ArrayList<Path>();
        Collection<File> selectedFiles = new HashSet<File>();

        File basedir = context.getPathSet().getBasedir();
        String[] children = basedir.list();
        if ( children != null )
        {
            if ( context.getPathSet().isIncludingDirectories() && context.isSelected( "" ) )
            {
                if ( context.isProcessingRequired( basedir ) )
                {
                    dirtyPaths.add( new Path( "" ) );
                }
                selectedFiles.add( basedir );
            }
            scan( selectedFiles, dirtyPaths, basedir, "", children, context );
        }

        for ( String pathname : context.getDeletedInputPaths( selectedFiles ) )
        {
            dirtyPaths.add( new Path( pathname, true ) );
        }

        return dirtyPaths;
    }

    private void scan( Collection<File> selectedFiles, Collection<Path> paths, File dir, String pathPrefix,
                       String[] files, PathSetResolutionContext context )
    {
        boolean includeDirs = context.getPathSet().isIncludingDirectories();
        boolean includeFiles = context.getPathSet().isIncludingFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            String pathname = pathPrefix + files[i];
            File file = new File( dir, files[i] );
            String[] children = file.list();

            if ( children == null || ( children.length <= 0 && file.isFile() ) )
            {
                if ( includeFiles && context.isSelected( pathname ) )
                {
                    selectedFiles.add( file );
                    if ( context.isProcessingRequired( file ) )
                    {
                        paths.add( new Path( pathname ) );
                    }
                }
            }
            else
            {
                if ( includeDirs && context.isSelected( pathname ) )
                {
                    selectedFiles.add( file );
                    if ( context.isProcessingRequired( file ) )
                    {
                        paths.add( new Path( pathname ) );
                    }
                }
                if ( context.isAncestorOfPotentiallySelected( pathname ) )
                {
                    scan( selectedFiles, paths, file, pathname + File.separator, children, context );
                }
            }
        }
    }

}
