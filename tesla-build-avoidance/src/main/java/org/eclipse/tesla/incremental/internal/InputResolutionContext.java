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

import org.eclipse.tesla.incremental.PathSet;

public interface InputResolutionContext
{

    File getOutputDirectory();

    boolean isFullBuild();

    PathSet getPathSet();

    boolean isSelected( String pathname );

    boolean isAncestorOfPotentiallySelected( String pathname );

    Collection<String> getDeletedInputPaths( Collection<File> existingInputs );

    Collection<String> getInputPaths( File outputFile );

    boolean isProcessingRequired( File input );

}
