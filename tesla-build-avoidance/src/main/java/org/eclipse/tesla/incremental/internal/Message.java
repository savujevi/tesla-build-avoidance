package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.Serializable;

class Message
    implements Serializable
{
    private final int line;

    private final int column;

    private final String message;

    private final int severity;

    private final Throwable cause;

    public Message( int line, int column, String message, int severity, Throwable cause )
    {
        this.line = line;
        this.column = column;
        this.message = message;
        this.severity = severity;
        this.cause = cause;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    public String getMessage()
    {
        return message;
    }

    public int getSeverity()
    {
        return severity;
    }

    public Throwable getCause()
    {
        return cause;
    }

}
