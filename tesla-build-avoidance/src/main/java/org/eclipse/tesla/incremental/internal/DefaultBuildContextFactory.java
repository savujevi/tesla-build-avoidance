package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextFactory;
import org.eclipse.tesla.incremental.Digester;

@Named
@Component( role = BuildContextFactory.class )
public class DefaultBuildContextFactory
    implements BuildContextFactory
{

    @Requirement
    private Logger log;

    public DefaultBuildContextFactory()
    {
        this( null );
    }

    @Inject
    public DefaultBuildContextFactory( Logger log )
    {
        this.log = log;
    }

    protected Logger getLogger()
    {
        return log;
    }

    protected PathSetResolver getPathSetResolver()
    {
        return new DefaultPathSetResolver();
    }

    protected MessageHandler getMessageHandler()
    {
        return new DefaultMessageHandler( getLogger() );
    }

    protected OutputListener getOutputListener()
    {
        return NullOutputListener.INSTANCE;
    }

    public BuildContext newContext( File outputDirectory, File contextDirectory, String pluginId )
    {
        return new DefaultBuildContext( outputDirectory, contextDirectory, pluginId, getPathSetResolver(),
                                        getMessageHandler(), getOutputListener(), getLogger() );
    }

    public Digester newDigester()
    {
        return new DefaultDigester();
    }

}
