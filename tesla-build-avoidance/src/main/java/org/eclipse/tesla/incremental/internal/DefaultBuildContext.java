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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.Digester;
import org.eclipse.tesla.incremental.PathSet;

class DefaultBuildContext
    implements BuildContext
{

    private final DefaultBuildContextManager manager;

    final WeakReference<BuildContext> reference;

    private final Logger log;

    private final File outputDirectory;

    private final BuildState buildState;

    private final Collection<File> deletedInputs;

    private final Map<File, Collection<File>> addedOutputs;

    private final Collection<File> modifiedOutputs;

    private final Collection<File> unmodifiedOutputs;

    private final long start;

    public DefaultBuildContext( DefaultBuildContextManager manager, File outputDirectory, BuildState buildState )
    {
        if ( manager == null )
        {
            throw new IllegalArgumentException( "build context factory not specified" );
        }
        if ( outputDirectory == null )
        {
            throw new IllegalArgumentException( "output directory not specified" );
        }
        if ( buildState == null )
        {
            throw new IllegalArgumentException( "build state not specified" );
        }

        reference = new WeakReference<BuildContext>( this );

        start = System.currentTimeMillis();

        this.manager = manager;
        this.outputDirectory = outputDirectory;
        this.buildState = buildState;
        this.log = manager.log;

        this.deletedInputs = new TreeSet<File>( Collections.reverseOrder() );
        this.addedOutputs = new HashMap<File, Collection<File>>();
        this.modifiedOutputs = new HashSet<File>();
        this.unmodifiedOutputs = new HashSet<File>();
    }

    public Digester newDigester()
    {
        failIfFinished();

        return manager.newDigester();
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean setConfiguration( PathSet paths, byte[] digest )
    {
        failIfFinished();

        return buildState.setConfiguration( paths, digest );
    }

    public synchronized Collection<String> getInputs( PathSet paths, boolean fullBuild )
    {
        failIfFinished();

        InputResolutionContext context = new DefaultInputResolutionContext( this, paths, fullBuild, buildState );

        Collection<String> inputs = new ArrayList<String>();

        for ( Path path : manager.resolve( context ) )
        {
            if ( path.isDeleted() )
            {
                deletedInputs.add( new File( paths.getBasedir(), path.getPath() ) );
            }
            else
            {
                inputs.add( path.getPath() );
            }
        }

        return inputs;
    }

    public OutputStream newOutputStream( File output )
        throws FileNotFoundException
    {
        failIfFinished();

        output = FileUtils.resolve( output, null );

        return new IncrementalFileOutputStream( output, this );
    }

    public void addOutput( File input, File output )
    {
        failIfFinished();

        if ( output != null )
        {
            addOutputs( Collections.singleton( output ), input );
        }
    }

    public void addOutputs( File input, File... outputs )
    {
        failIfFinished();

        if ( outputs != null && outputs.length > 0 )
        {
            addOutputs( Arrays.asList( outputs ), input );
        }
    }

    public void addOutputs( File input, Collection<File> outputs )
    {
        failIfFinished();

        addOutputs( outputs, input );
    }

    private synchronized void addOutputs( Collection<File> outputs, File input )
    {
        if ( outputs == null || outputs.isEmpty() )
        {
            return;
        }

        input = FileUtils.resolve( input, null );

        Collection<File> addedOutputs = null;
        if ( input != null )
        {
            addedOutputs = this.addedOutputs.get( input );
            if ( addedOutputs == null )
            {
                addedOutputs = new TreeSet<File>();
                this.addedOutputs.put( input, addedOutputs );
            }
        }

        for ( File output : outputs )
        {
            if ( output != null )
            {
                output = FileUtils.resolve( output, null );

                modifiedOutputs.add( output );

                if ( addedOutputs != null )
                {
                    addedOutputs.add( output );
                }
            }
        }
    }

    synchronized void addOutput( File output, boolean modified )
    {
        if ( modified )
        {
            modifiedOutputs.add( output );
        }
        else
        {
            unmodifiedOutputs.add( output );
        }
    }

    public synchronized void finish()
    {
        if ( reference.get() == null )
        {
            return;
        }

        reference.clear();

        modifiedOutputs.removeAll( unmodifiedOutputs );
        int produced = modifiedOutputs.size();

        int deletedObsolete = 0;
        for ( Map.Entry<File, Collection<File>> entry : addedOutputs.entrySet() )
        {
            File input = entry.getKey();
            Collection<File> outputs = entry.getValue();
            Collection<File> obsoleteOutputs = buildState.setOutputs( input, outputs );
            modifiedOutputs.addAll( obsoleteOutputs );
            deletedObsolete += deleteSuperfluousOutputs( obsoleteOutputs, "obsolete" );
        }

        int deletedOrphaned = 0;
        for ( File deletedInput : deletedInputs )
        {
            Collection<File> orphanedOutputs = buildState.removeInput( deletedInput );
            modifiedOutputs.addAll( orphanedOutputs );
            deletedOrphaned += deleteSuperfluousOutputs( orphanedOutputs, "orphaned" );
        }

        save();

        if ( !modifiedOutputs.isEmpty() )
        {
            manager.outputUpdated( modifiedOutputs );
        }

        if ( log.isDebugEnabled() )
        {
            long millis = System.currentTimeMillis() - start;
            log.debug( produced + " outputs produced, " + deletedObsolete + " obsolete outputs deleted, "
                + deletedOrphaned + " orphaned outputs deleted, " + millis + " ms" );
        }
    }

    private int deleteSuperfluousOutputs( Collection<File> outputs, String type )
    {
        int deleted = 0;
        if ( outputs != null && !outputs.isEmpty() )
        {
            for ( File output : outputs )
            {
                if ( output.delete() )
                {
                    deleted++;
                    log.debug( "Deleted " + type + " output " + output );
                }
                else if ( output.exists() )
                {
                    log.debug( "Failed to delete " + type + " output " + output );
                }
            }
        }
        return deleted;
    }

    private void save()
    {
        try
        {
            buildState.save();
        }
        catch ( IOException e )
        {
            log.warn( "Could not serialize incremental build state to " + buildState.getStateFile(),
                      log.isDebugEnabled() ? e : null );
        }
    }

    public void addMessage( File input, int line, int column, String message, int severity, Throwable cause )
    {
        failIfFinished();

        manager.addMessage( input, line, column, message, severity, cause );
    }

    public void clearMessages( File input )
    {
        failIfFinished();

        manager.clearMessages( input );
    }

    private void failIfFinished()
    {
        if ( reference.get() == null )
        {
            throw new IllegalStateException( "build context has already been finished" );
        }
    }

}
