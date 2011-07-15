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
public class Path
{

    private final String path;

    private final State state;

    public Path( String path, State state )
    {
        this.path = path;
        this.state = state;
    }

    public String getPath()
    {
        return path;
    }

    public State getState()
    {
        return state;
    }

    public enum State
    {

        PRESENT,

        DELETED

    }

}
