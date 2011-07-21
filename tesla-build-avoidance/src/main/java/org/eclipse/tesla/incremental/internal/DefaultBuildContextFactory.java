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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextFactory;

@Named
@Singleton
@Component( role = BuildContextFactory.class )
public class DefaultBuildContextFactory
    implements BuildContextFactory
{

    @Requirement
    protected Logger log;

    final Map<File, WeakReference<BuildState>> buildStates;

    public DefaultBuildContextFactory()
    {
        this( null );
    }

    @Inject
    public DefaultBuildContextFactory( Logger log )
    {
        this.log = ( log != null ) ? log : NullLogger.INSTANCE;
        buildStates = new HashMap<File, WeakReference<BuildState>>();
    }

    protected PathSetResolver getPathSetResolver()
    {
        return new DefaultPathSetResolver();
    }

    protected MessageHandler getMessageHandler()
    {
        return new DefaultMessageHandler( log );
    }

    protected OutputListener getOutputListener()
    {
        return NullOutputListener.INSTANCE;
    }

    public BuildContext newContext( File outputDirectory, File contextDirectory, String pluginId )
    {
        BuildState buildState = getBuildState( outputDirectory, contextDirectory, pluginId );
        return new DefaultBuildContext( outputDirectory, buildState, getPathSetResolver(), getMessageHandler(),
                                        getOutputListener(), log );
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

}
