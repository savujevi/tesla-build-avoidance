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

public interface BuildContextManager
{

    public static final int SEVERITY_WARNING = BuildContext.SEVERITY_WARNING;

    public static final int SEVERITY_ERROR = BuildContext.SEVERITY_ERROR;

    BuildContext newContext( File outputDirectory, File contextDirectory, String pluginId );

    void addOutputs( File input, File... outputs );

    OutputStream newOutputStream( File output )
        throws IOException;

    void addMessage( File input, int line, int column, String message, int severity, Throwable cause );

    void clearMessages( File input );

}
