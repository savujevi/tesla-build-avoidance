package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;

class NullMessageHandler
    implements MessageHandler
{

    public static final MessageHandler INSTANCE = new NullMessageHandler();

    public void addMessage( File input, int line, int column, String message, int severity, Throwable cause )
    {
    }

    public void clearMessages( File input )
    {
    }

}
