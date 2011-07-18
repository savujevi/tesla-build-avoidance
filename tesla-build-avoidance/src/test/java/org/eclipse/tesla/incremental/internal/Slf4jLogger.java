package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import javax.inject.Named;

import org.slf4j.LoggerFactory;

@Named
public class Slf4jLogger
    implements Logger
{

    private org.slf4j.Logger log = LoggerFactory.getLogger( Slf4jLogger.class );

    public boolean isDebugEnabled()
    {
        return log.isDebugEnabled();
    }

    public void debug( String msg )
    {
        log.debug( msg );
    }

    public void debug( String msg, Throwable error )
    {
        log.debug( msg, error );
    }

    public boolean isWarnEnabled()
    {
        return log.isWarnEnabled();
    }

    public void warn( String msg )
    {
        log.warn( msg );
    }

    public void warn( String msg, Throwable error )
    {
        log.warn( msg, error );
    }

    public boolean isErrorEnabled()
    {
        return log.isErrorEnabled();
    }

    public void error( String msg )
    {
        log.error( msg );
    }

    public void error( String msg, Throwable error )
    {
        log.error( msg, error );
    }

}
