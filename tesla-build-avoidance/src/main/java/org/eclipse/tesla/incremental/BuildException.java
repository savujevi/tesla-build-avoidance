package org.eclipse.tesla.incremental;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

/**
 * Signals one or more errors while processing the input files.
 */
public class BuildException
    extends RuntimeException
{

    /**
     * Creates a new build exception with the specified detail message.
     * 
     * @param message The detail message, may be {@code null}.
     */
    public BuildException( String message )
    {
        super( message );
    }

    /**
     * Creates a new build exception with the specified detail message and cause.
     * 
     * @param message The detail message, may be {@code null}.
     * @param cause The error cause, may be {@code null}.
     */
    public BuildException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates a new build exception with the specified detail cause.
     * 
     * @param cause The error cause, may be {@code null}.
     */
    public BuildException( Throwable cause )
    {
        super( cause );
    }

}
