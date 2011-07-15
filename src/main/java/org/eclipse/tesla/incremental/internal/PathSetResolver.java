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
import java.util.Map;

import org.eclipse.tesla.incremental.PathSet;

public interface PathSetResolver
{

    Collection<Path> resolve( PathSet paths, Map<File, FileState> states, Map<File, Collection<File>> outputs );

}
