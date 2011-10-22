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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextManager;
import org.eclipse.tesla.incremental.Digester;
import org.eclipse.tesla.incremental.PathSet;

@Named
@Singleton
public class DefaultBuildContextManager
    implements BuildContextManager
{

    protected Logger log;

    final Map<File, WeakReference<BuildState>> buildStates;

    private final InheritableThreadLocal<SortedMap<File, WeakReference<BuildContext>>> buildContexts =
        new InheritableThreadLocal<SortedMap<File, WeakReference<BuildContext>>>()
        {
            protected SortedMap<File, WeakReference<BuildContext>> initialValue()
            {
                return new TreeMap<File, WeakReference<BuildContext>>( Collections.reverseOrder() );
            }

            protected SortedMap<File, WeakReference<BuildContext>> childValue( SortedMap<File, WeakReference<BuildContext>> parentValue )
            {
                return new TreeMap<File, WeakReference<BuildContext>>( parentValue );
            }
        };

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
        logMessage( input, line, column, message, severity, cause );
    }

    public void logMessage( File input, int line, int column, String message, int severity, Throwable cause )
    {
        String msg = getMessage( input, line, column, message, cause );
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

    protected String getMessage( File file, int line, int column, String message, Throwable cause )
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
        if ( message == null || message.length() <= 0 )
        {
            if ( cause != null )
            {
                message = cause.getMessage();
                if ( message == null || message.length() <= 0 )
                {
                    message = cause.toString();
                }
            }
            else
            {
                message = "(unknown issue)";
            }
        }
        sb.append( message );
        return sb.toString();
    }

    public void clearMessages( File input )
    {
        // defaults to noop
    }

    public BuildContext newContext( File outputDirectory, File stateDirectory, String builderId )
    {
        if ( outputDirectory == null )
        {
            throw new IllegalArgumentException( "output directory not specified" );
        }
        if ( stateDirectory == null )
        {
            throw new IllegalArgumentException( "build state directory not specified" );
        }
        if ( builderId == null )
        {
            throw new IllegalArgumentException( "builder identifier not specified" );
        }

        outputDirectory = FileUtils.resolve( outputDirectory, null );

        boolean fullBuild = isFullBuild( outputDirectory, stateDirectory, builderId );

        BuildState buildState = getBuildState( outputDirectory, stateDirectory, builderId, fullBuild );

        DefaultBuildContext context = new DefaultBuildContext( this, outputDirectory, buildState, fullBuild );
        buildContexts.get().put( outputDirectory, context.reference );

        return context;
    }

    protected Digester newDigester( File outputDirectory )
    {
        return new DefaultDigester();
    }

    protected boolean isFullBuild( File outputDirectory, File stateDirectory, String builderId )
    {
        // hook to externally enforce full build
        return false;
    }

    void destroy(BuildState buildState)
    {
        File stateFile = buildState.getStateFile();
        synchronized ( buildStates )
        {
            buildStates.remove( stateFile );
            stateFile.delete();
        }
    }

    private BuildState getBuildState( File outputDirectory, File stateDirectory, String builderId, boolean fullBuild )
    {
        File stateFile = getStateFile( outputDirectory, stateDirectory, builderId );

        synchronized ( buildStates )
        {
            BuildState buildState = null;

            WeakReference<BuildState> ref = buildStates.get( stateFile );
            if ( !fullBuild && ref != null )
            {
                buildState = ref.get();
            }

            purgeBuildStates();

            if ( buildState == null )
            {
                if ( !fullBuild && stateFile.isFile() )
                {
                    try
                    {
                        buildState = BuildState.load( stateFile );
                    }
                    catch ( IOException e )
                    {
                        log.warn( "Could not deserialize incremental build state from " + stateFile,
                                  log.isDebugEnabled() ? e : null );
                    }
                }

                if ( buildState == null )
                {
                    buildState = new BuildState( stateFile );
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

    protected File getStateFile( File outputDirectory, File stateDirectory, String builderId )
    {
        Digester digester = newDigester( outputDirectory );
        String digest1 =
            DigestUtils.toHexString( digester.string( FileUtils.normalize( outputDirectory ).getPath() ).finish() );
        String digest2 = DigestUtils.toHexString( digester.string( builderId ).finish() );
        return new File( stateDirectory.getAbsolutePath(), digest1 + "-" + digest2 + ".ser" );
    }

    protected void outputUpdated( Collection<File> outputs )
    {
        // defaults to noop, useful for refreshing of IDE
    }

    protected Collection<Path> resolveInputs( final InputResolutionContext context )
    {
        final Collection<Path> dirtyPaths = new ArrayList<Path>();
        final Collection<File> selectedFiles = new HashSet<File>( 128 );

        Selector selector = new Selector()
        {
            public boolean isSelected( String pathname )
            {
                return context.isSelected( pathname );
            }

            public boolean isAncestorOfPotentiallySelected( String pathname )
            {
                return context.isAncestorOfPotentiallySelected( pathname );
            }
        };

        PathSet pathSet = context.getPathSet();

        DirectoryScan scan =
            new DirectoryScan( pathSet.getBasedir(), selector, pathSet.isIncludingDirectories(),
                               pathSet.isIncludingFiles() )
            {
                @Override
                protected void onItem( String pathname, File file )
                {
                    selectedFiles.add( file );
                    if ( context.isProcessingRequired( file ) )
                    {
                        dirtyPaths.add( new Path( pathname ) );
                    }
                }
            };
        scan.run();

        for ( String pathname : context.getDeletedInputPaths( selectedFiles ) )
        {
            dirtyPaths.add( new Path( pathname, true ) );
        }

        return dirtyPaths;
    }

    protected Collection<File> resolveOutputs( PathSet pathSet )
    {
        final Collection<File> selectedFiles = new HashSet<File>( 128 );

        Selector selector = new GlobSelector( pathSet );

        DirectoryScan scan =
            new DirectoryScan( pathSet.getBasedir(), selector, pathSet.isIncludingDirectories(),
                               pathSet.isIncludingFiles() )
            {
                @Override
                protected void onItem( String pathname, File file )
                {
                    selectedFiles.add( file );
                }
            };
        scan.run();

        return selectedFiles;
    }

    public void addOutput( File input, File output )
    {
        if ( output != null )
        {
            output = FileUtils.resolve( output, null );

            BuildContext buildContext = getBuildContext( output );
            if ( buildContext != null )
            {
                buildContext.addOutput( input, output );
            }
            else
            {
                outputUpdated( Collections.singleton( output ) );
            }
        }
    }

    public void addOutputs( File input, File... outputs )
    {
        if ( outputs != null && outputs.length > 0 )
        {
            Collection<File> updateOutputs = new HashSet<File>( outputs.length );

            for ( File output : outputs )
            {
                if ( output != null )
                {
                    output = FileUtils.resolve( output, null );

                    BuildContext buildContext = getBuildContext( output );
                    if ( buildContext != null )
                    {
                        buildContext.addOutput( input, output );
                    }
                    else
                    {
                        updateOutputs.add( output );
                    }
                }
            }

            if ( !updateOutputs.isEmpty() )
            {
                outputUpdated( updateOutputs );
            }
        }
    }

    public void addOutputs( File input, Collection<File> outputs )
    {
        if ( outputs != null && !outputs.isEmpty() )
        {
            Collection<File> updateOutputs = new HashSet<File>( outputs.size() );

            for ( File output : outputs )
            {
                if ( output != null )
                {
                    output = FileUtils.resolve( output, null );

                    BuildContext buildContext = getBuildContext( output );
                    if ( buildContext != null )
                    {
                        buildContext.addOutput( input, output );
                    }
                    else
                    {
                        updateOutputs.add( output );
                    }
                }
            }

            if ( !updateOutputs.isEmpty() )
            {
                outputUpdated( updateOutputs );
            }
        }
    }

    public void addOutputs( File input, PathSet outputs )
    {
        if ( outputs != null )
        {
            BuildContext buildContext = getBuildContext( outputs.getBasedir() );
            if ( buildContext != null )
            {
                buildContext.addOutputs( input, outputs );
            }
            else
            {
                Collection<File> updateOutputs = resolveOutputs( outputs );

                if ( !updateOutputs.isEmpty() )
                {
                    outputUpdated( updateOutputs );
                }
            }
        }
    }

    public OutputStream newOutputStream( File output )
        throws FileNotFoundException
    {
        output = FileUtils.resolve( output, null );

        BuildContext buildContext = getBuildContext( output );
        if ( buildContext != null )
        {
            return buildContext.newOutputStream( output );
        }

        return new IncrementalFileOutputStream( output, null );
    }

    protected BuildContext getBuildContext( File output )
    {
        for ( Iterator<Map.Entry<File, WeakReference<BuildContext>>> it = buildContexts.get().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<File, WeakReference<BuildContext>> entry = it.next();
            BuildContext context = entry.getValue().get();
            if ( context == null )
            {
                it.remove();
            }
            else if ( FileUtils.relativize( output, context.getOutputDirectory() ) != null )
            {
                return context;
            }
        }
        return null;
    }

}
