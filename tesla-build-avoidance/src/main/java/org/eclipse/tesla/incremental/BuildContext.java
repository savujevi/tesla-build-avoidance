package org.eclipse.tesla.incremental;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Collection;

public interface BuildContext
{

    public static final int SEVERITY_WARNING = 1;

    public static final int SEVERITY_ERROR = 2;

    Digester newDigester();

    File getOutputDirectory();

    // record current plugin config for fileset, signal if changed since last run
    boolean setConfiguration( PathSet paths, byte[] digest );

    Collection<String> getInputs( PathSet paths, boolean fullBuild );

    OutputStream newOutputStream( File output )
        throws FileNotFoundException;

    void addOutput( File input, File output );

    void addOutputs( File input, File... outputs );

    void addOutputs( File input, Collection<File> outputs );

    void addMessage( File input, int line, int column, String message, int severity, Throwable cause );

    void clearMessages( File input );

    // delete obsolete outputs and save context to disk, log any IO error as warning
    void finish();

}
