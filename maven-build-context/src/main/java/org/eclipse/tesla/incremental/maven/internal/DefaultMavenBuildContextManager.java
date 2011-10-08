package org.eclipse.tesla.incremental.maven.internal;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextManager;
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

        return manager.newContext( outputDirectory, stateDirectory, builderId );
    }

}
