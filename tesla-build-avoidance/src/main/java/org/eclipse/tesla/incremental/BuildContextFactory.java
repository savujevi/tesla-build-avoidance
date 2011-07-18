package org.eclipse.tesla.incremental;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;

public interface BuildContextFactory
{

    BuildContext newContext( File outputDirectory, File contextDirectory, String pluginId );

    Digester newDigester();

}
