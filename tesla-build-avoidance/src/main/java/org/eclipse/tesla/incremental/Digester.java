package org.eclipse.tesla.incremental;

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

public interface Digester
{

    Digester value( boolean value );

    Digester value( long value );

    Digester value( double value );

    Digester string( String string );

    Digester strings( String... strings );

    Digester strings( Collection<String> strings );

    Digester basedir( File dir );

    Digester file( File file );

    Digester files( File... files );

    Digester files( Collection<?> files );

    Digester file( String file );

    Digester files( String... files );

    Digester hash( Object obj );

    Digester hashes( Object... objs );

    Digester hashes( Collection<?> objs );

    Digester hashes( Map<?, ?> map );

    byte[] finish();

    Digester clone();

}
