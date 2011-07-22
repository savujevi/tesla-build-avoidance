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

/**
 * Calculates the fingerprint of a component/plugin configuration. The resulting fingerprints are persisted as part of
 * the incremental build state and used to detect changes of configuration that affect the output of a given path set.
 */
public interface Digester
{

    /**
     * Updates the digester with the specified configuration value.
     * 
     * @param value The configuration value to include in the fingerprint.
     * @return This digester for chaining, never {@code null}.
     */
    Digester value( boolean value );

    /**
     * Updates the digester with the specified configuration value.
     * 
     * @param value The configuration value to include in the fingerprint.
     * @return This digester for chaining, never {@code null}.
     */
    Digester value( long value );

    /**
     * Updates the digester with the specified configuration value.
     * 
     * @param value The configuration value to include in the fingerprint.
     * @return This digester for chaining, never {@code null}.
     */
    Digester value( double value );

    /**
     * Updates the digester with the specified configuration value.
     * 
     * @param string The configuration value to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester string( String string );

    /**
     * Updates the digester with the specified configuration values.
     * 
     * @param strings The configuration values to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester strings( String... strings );

    /**
     * Updates the digester with the specified configuration values.
     * 
     * @param strings The configuration values to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester strings( Collection<String> strings );

    /**
     * Sets the base directory to use for resolution of relative files during future updates to the digester.
     * 
     * @param dir The base directory, may be {@code null} to use the current directory.
     * @return This digester for chaining, never {@code null}.
     */
    Digester basedir( File dir );

    /**
     * Updates the digester with the specified configuration file. The digester will consider both the file path as well
     * as its length and timestamp to account for changes to its contents.
     * 
     * @param file The configuration file to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester file( File file );

    /**
     * Updates the digester with the specified configuration files. The digester will consider both the file paths as
     * well as their lengths and timestamps to account for changes to their contents.
     * 
     * @param files The configuration files to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester files( File... files );

    /**
     * Updates the digester with the specified configuration files. The digester will consider both the file paths as
     * well as their lengths and timestamps to account for changes to their contents.
     * 
     * @param files The configuration files to include in the fingerprint, may be {@code null}. This collection may
     *            contain instances of either {@link String} or {@link File}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester files( Collection<?> files );

    /**
     * Updates the digester with the specified configuration file. The digester will consider both the file path as well
     * as its length and timestamp to account for changes to its contents.
     * 
     * @param file The configuration file to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester file( String file );

    /**
     * Updates the digester with the specified configuration files. The digester will consider both the file paths as
     * well as their lengths and timestamps to account for changes to their contents.
     * 
     * @param files The configuration files to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester files( String... files );

    /**
     * Updates the digester with the {@link Object#hashCode() hashCode()} of the specified object.
     * 
     * @param obj The configuration object to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester hash( Object obj );

    /**
     * Updates the digester with the {@link Object#hashCode() hashCode()} of the specified objects.
     * 
     * @param objs The configuration objects to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester hashes( Object... objs );

    /**
     * Updates the digester with the {@link Object#hashCode() hashCode()} of the specified objects.
     * 
     * @param objs The configuration objects to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester hashes( Collection<?> objs );

    /**
     * Updates the digester with the {@link Object#hashCode() hashCode()} of the specified key value pairs.
     * 
     * @param map The configuration map to include in the fingerprint, may be {@code null}.
     * @return This digester for chaining, never {@code null}.
     */
    Digester hashes( Map<?, ?> map );

    /**
     * Finishes the fingerprinting and resets the digester.
     * 
     * @return The fingerprint of the configuration, never {@code null}.
     */
    byte[] finish();

    /**
     * Creates a deep clone of this digester. This is primarily useful when multiple path sets with distinct
     * configurations that share common parameters are processed. The common parameters can be fed once into an initial
     * digester which is then cloned/forked in order to continue with the parameters specific to a given path set.
     * 
     * @return The cloned digester.
     */
    Digester clone();

}
