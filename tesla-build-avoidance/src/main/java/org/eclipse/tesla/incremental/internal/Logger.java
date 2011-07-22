package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

/**
 */
public interface Logger
{

    /**
     * Indicates whether debug logging is enabled.
     * 
     * @return {@code true} if debug logging is enabled, {@code false} otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Emits the specified message.
     * 
     * @param msg The message to log, must not be {@code null}.
     */
    void debug( String msg );

    /**
     * Emits the specified message along with a stack trace of the given exception.
     * 
     * @param msg The message to log, must not be {@code null}.
     * @param error The exception to log, may be {@code null}.
     */
    void debug( String msg, Throwable error );

    /**
     * Indicates whether warn logging is enabled.
     * 
     * @return {@code true} if warn logging is enabled, {@code false} otherwise.
     */
    boolean isWarnEnabled();

    /**
     * Emits the specified message.
     * 
     * @param msg The message to log, must not be {@code null}.
     */
    void warn( String msg );

    /**
     * Emits the specified message along with a stack trace of the given exception.
     * 
     * @param msg The message to log, must not be {@code null}.
     * @param error The exception to log, may be {@code null}.
     */
    void warn( String msg, Throwable error );

    /**
     * Indicates whether error logging is enabled.
     * 
     * @return {@code true} if error logging is enabled, {@code false} otherwise.
     */
    boolean isErrorEnabled();

    /**
     * Emits the specified message.
     * 
     * @param msg The message to log, must not be {@code null}.
     */
    void error( String msg );

    /**
     * Emits the specified message along with a stack trace of the given exception.
     * 
     * @param msg The message to log, must not be {@code null}.
     * @param error The exception to log, may be {@code null}.
     */
    void error( String msg, Throwable error );

}
