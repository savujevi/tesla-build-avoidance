package org.eclipse.tesla.incremental.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextFactory;
import org.eclipse.tesla.incremental.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Component( role = BuildContextFactory.class )
public class DefaultBuildContextFactory
    implements BuildContextFactory
{

    @Inject
    private Logger log = LoggerFactory.getLogger( DefaultBuildContextFactory.class );

    public BuildContext newContext( File outputDirectory, File contextDirectory, String pluginId )
    {
        return new DefaultBuildContext( outputDirectory, contextDirectory, pluginId, log );
    }

    public Digester newDigester()
    {
        return new DefaultDigester();
    }

}
