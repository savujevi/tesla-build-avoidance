package org.eclipse.tesla.incremental.test;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.incremental.internal.MavenBuildContextManager;
import org.apache.maven.incremental.internal.MojoExecutionModule;
import org.apache.maven.incremental.internal.MojoExecutionScope;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tesla.incremental.BuildContext;

public class AbstractBuildAvoidanceTest
    extends AbstractMojoTestCase
{
    @Override
    protected ContainerConfiguration setupContainerConfiguration()
    {
        ContainerConfiguration configuration = super.setupContainerConfiguration();
        configuration.setClassPathScanning( PlexusConstants.SCANNING_INDEX ).setAutoWiring( true );
        return configuration;
    }

    protected MavenProject readMavenProject( File basedir )
        throws ProjectBuildingException, Exception
    {
        File pom = new File( basedir, "pom.xml" );
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory( basedir );
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        MavenProject project = lookup( ProjectBuilder.class ).build( pom, configuration ).getProject();
        assertNotNull( project );
        return project;
    }

    protected File getBasedir( String location )
        throws IOException
    {
        File src = new File( location );
        File basedir = new File( getWorkingDirectory(), src.getName() ).getCanonicalFile();
        FileUtils.deleteDirectory( basedir );
        assertTrue( basedir.mkdirs() );
        FileUtils.copyDirectoryStructure( src, basedir );
        return basedir;
    }

    protected File getWorkingDirectory()
    {
        return new File( "target/ut/" );
    }

    protected void executeMojo( MavenSession session, MavenProject project, MojoExecution execution )
        throws Exception
    {
        MojoExecutionScope scope = lookup( MojoExecutionScope.class, MojoExecutionModule.SCOPE_NAME );
        try
        {
            scope.enter();

            BuildContext buildContext = lookup( MavenBuildContextManager.class ).newContext( session, execution );

            scope.seed( BuildContext.class, buildContext );
            try
            {
                Mojo mojo = lookupConfiguredMojo( session, execution );
                mojo.execute();
            }
            finally
            {
                buildContext.close();
            }
        }
        finally
        {
            scope.exit();
        }
    }

    protected void executeMojo( File basedir, String goal )
        throws Exception
    {
        MavenProject project = readMavenProject( basedir );
        MojoExecution execution = newMojoExecution( goal );
        MavenSession session = newMavenSession( project );

        executeMojo( session, project, execution );
    }
}
