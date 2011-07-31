package org.eclipse.tesla.incremental.maven;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.BuildContextManager;
import org.eclipse.tesla.incremental.PathSet;

/**
 * A simple mojo that demonstrates the use of the incremental build support and delegates the actual output generation
 * to another component which does not have direct access to the active build context due to its API.
 * 
 * @goal incremental-delegate
 * @phase process-resources
 */
public class IncrementalDelegateMojo
    extends AbstractMojo
{

    // --- usual plugin parameters ----------------------------------

    /**
     * @parameter default-value="${basedir}"
     * @readonly
     */
    private File projectDirectory;

    /**
     * @parameter default-value="src/main/tesla"
     */
    private File inputDirectory;

    /**
     * @parameter
     */
    private String[] includes;

    /**
     * @parameter
     */
    private String[] excludes;

    /**
     * @parameter default-value="${project.build.directory}/tesla"
     */
    private File outputDirectory;

    /**
     * @parameter default-value="${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * @parameter
     */
    private String targetPath = "";

    /**
     * @parameter default-value="true"
     */
    private boolean filtering;

    /**
     * @parameter default-value="${project.build.filters}"
     */
    private Collection<String> filters;

    /**
     * @component
     */
    private OutputGenerator outputGenerator;

    // --- incremental build support --------------------------------

    /**
     * @parameter default-value="${plugin.id}"
     * @readonly
     */
    private String pluginId;

    /**
     * @parameter default-value="${project.build.directory}/incremental"
     * @readonly
     */
    private File stateDirectory;

    /**
     * @component
     */
    private BuildContextManager contextManager;

    // --- mojo logic -----------------------------------------------

    public void execute()
        throws MojoExecutionException
    {
        // get build context for the output directory
        BuildContext context = contextManager.newContext( outputDirectory, stateDirectory, pluginId );

        try
        {
            // create fingerprint of our current configuration that is relevant for creation of output files
            byte[] digest = context.newDigester() //
            .string( encoding ).string( targetPath ).value( filtering ) // simple value
            .basedir( projectDirectory ).files( filters ) // potentially relative files, considers timestamp/length
            .finish();

            Properties filterProps = new Properties();
            IOUtils.load( filterProps, filters, projectDirectory );
            filterProps.putAll( System.getProperties() );

            // set up pathset defining the input files to process
            PathSet pathset = new PathSet( inputDirectory, includes, excludes );

            // check whether current configuration for pathset has changed since last build
            boolean fullBuild = context.setConfiguration( pathset, digest );

            // get input files that need processing
            Collection<String> paths = context.getInputs( pathset, fullBuild );

            // process input files
            for ( String path : paths )
            {
                File inputFile = new File( pathset.getBasedir(), path );
                File outputFile = new File( new File( context.getOutputDirectory(), targetPath ), path );

                getLog().info( "Processing input " + path + " > " + outputFile );

                // generate output
                outputGenerator.generate( inputFile, outputFile, filterProps );
            }
            if ( paths.isEmpty() )
            {
                getLog().info( "No inputs found to process" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            // persist build context back to disk and delete any stale output files, throw exception in case of errors
            context.finish();
        }
    }

}
