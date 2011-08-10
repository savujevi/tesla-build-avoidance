package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 */
public class BuildContextLogger
    extends AbstractLogEnabled
    implements Logger
{

    public boolean isDebugEnabled()
    {
        return getLogger().isDebugEnabled();
    }

    public void debug( String msg )
    {
        getLogger().debug( msg );
    }

    public void debug( String msg, Throwable error )
    {
        getLogger().debug( msg, error );
    }

    public boolean isWarnEnabled()
    {
        return getLogger().isWarnEnabled();
    }

    public void warn( String msg )
    {
        getLogger().warn( msg );
    }

    public void warn( String msg, Throwable error )
    {
        getLogger().warn( msg, error );
    }

    public boolean isErrorEnabled()
    {
        return getLogger().isErrorEnabled();
    }

    public void error( String msg )
    {
        getLogger().error( msg );
    }

    public void error( String msg, Throwable error )
    {
        getLogger().error( msg, error );
    }

}
