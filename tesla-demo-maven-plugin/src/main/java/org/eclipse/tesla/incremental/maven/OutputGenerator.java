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
import java.util.Properties;

import org.eclipse.tesla.incremental.BuildContextManager;

/**
 * A simple component that demonstrates the use of the incremental build support when the active build context is not
 * directly accessible due to API restrictions.
 * 
 * @plexus.component role="org.eclipse.tesla.incremental.maven.OutputGenerator"
 */
public class OutputGenerator
{

    /**
     * @plexus.requirement
     */
    private BuildContextManager buildContextManager;

    public void generate( File inputFile, File outputFile, Properties filterProps )
    {
        // register output files
        buildContextManager.addOutput( inputFile, outputFile );

        // generate output files
        try
        {
            buildContextManager.clearMessages( inputFile );
            IOUtils.filter( inputFile, buildContextManager.newOutputStream( outputFile ), "UTF-8", filterProps );
        }
        catch ( IOException e )
        {
            buildContextManager.addMessage( inputFile, 0, 0, "Could not read file", BuildContextManager.SEVERITY_ERROR,
                                            e );
        }
    }

}
