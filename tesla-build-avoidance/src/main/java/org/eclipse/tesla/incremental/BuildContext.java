package org.eclipse.tesla.incremental;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public interface BuildContext
{

    File getOutputDirectory();

    // record current plugin config for fileset, signal if changed since last run
    boolean setConfiguration( PathSet paths, byte[] digest );

    Collection<String> getInputs( PathSet paths, boolean fullBuild );

    OutputStream newOutputStream( File output )
        throws IOException;

    OutputStream newOutputStream( String output )
        throws IOException;

    void addOutputs( File input, File... outputs );

    void addOutputs( File input, String... outputs );

    // delete obsolete outputs and save context to disk, log any IO error as warning
    void finish();

    // really have addMessage() here? not sure eventing/logging and IO should be coupled like that, might make usage
    // simpler on the other hand

}
