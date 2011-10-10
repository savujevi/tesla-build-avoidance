package org.eclipse.tesla.incremental.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.tesla.incremental.BuildContext;

/**
 * Maven specific BuildContext factory interface.
 * <p>
 * Differences compared to BuildContextManager
 * <ul>
 * <li>Conventional location of incremental build state under ${build.build.directory}/incremental. In the future, this
 * may become configurable via well-known project property.</li>
 * <li>Automatically detect configuration changes based on
 * <ul>
 * <li>Maven plugin artifacts GAVs, file sizes and timestamps</li>
 * <li>Project effective pom.xml. In the future, this may be narrowed down.</li>
 * <li>Effective settings</li>
 * <li>Maven session user and system properties.</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @TODO decide how to handle volatile properties like ${maven.build.timestamp}. Should we always ignore them? Are there
 *       cases where output has to be always regenerated just to include new build timestamp?
 * @TODO decide if settings (or effective settings) can contribute anything not already covered by project and session
 */
public interface MavenBuildContextManager
{
    BuildContext newContext( MavenSession session, MavenProject project, MojoExecution execution );
}
