package org.eclipse.tesla.incremental.maven.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextManager;
import org.eclipse.tesla.incremental.Digester;
import org.eclipse.tesla.incremental.maven.MavenBuildContextManager;

@Named
@Singleton
public class DefaultMavenBuildContextManager
    implements MavenBuildContextManager
{
    private BuildContextManager manager;

    @Deprecated
    public DefaultMavenBuildContextManager()
    {
    }

    @Inject
    public DefaultMavenBuildContextManager( BuildContextManager manager )
    {
        this.manager = manager;
    }

    public BuildContext newContext( MavenSession session, MavenProject project, MojoExecution execution )
    {
        File outputDirectory = project.getBasedir(); // @TODO really need to get rid of this!

        File stateDirectory = new File( project.getBuild().getDirectory(), "incremental" );

        String builderId = execution.getMojoDescriptor().getId() + ":" + execution.getExecutionId();

        BuildContext context = manager.newContext( outputDirectory, stateDirectory, builderId );

        Digester digester = context.newDigester();

        // plugin artifacts define behaviour, rebuild whenever behaviour changes
        for ( Artifact artifact : execution.getMojoDescriptor().getPluginDescriptor().getArtifacts() )
        {
            digester.strings( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
            digester.file( artifact.getFile() );
        }

        // effective pom.xml defines project configuration, rebuild whenever project configuration changes
        // we can't be more specific here because mojo can access entire project model, not just its own configuration
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        {
            new MavenXpp3Writer().write( buf, project.getModel() );
        }
        catch ( IOException e )
        {
            // can't happen
        }
        digester.bytes( buf.toByteArray() );

        // TODO decide if we care about system properties

        // user properties define build parameters passed in from command line
        for ( Map.Entry<Object, Object> property : session.getUserProperties().entrySet() )
        {
            digester.strings( property.getKey().toString(), property.getValue().toString() );
        }

        context.setConfiguration( digester.finish() );

        return context;
    }

}
