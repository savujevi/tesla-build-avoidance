package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.Collection;

class NullOutputListener
    implements OutputListener
{

    public static final OutputListener INSTANCE = new NullOutputListener();

    public void outputUpdated( Collection<File> outputs )
    {
    }

}
