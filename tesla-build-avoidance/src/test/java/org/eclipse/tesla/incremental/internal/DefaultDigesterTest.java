package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.tesla.incremental.Digester;
import org.eclipse.tesla.incremental.internal.DefaultDigester;
import org.junit.Test;

public class DefaultDigesterTest
{

    private DefaultDigester newDigester()
    {
        return new DefaultDigester();
    }

    private <E> Collection<E> newSet( E... items )
    {
        Collection<E> set = new LinkedHashSet<E>();
        Collections.addAll( set, items );
        return set;
    }

    @Test
    public void testBasedirNullSafe()
    {
        Digester digester = newDigester();
        digester.basedir( null );
        assertNotNull( digester.finish() );
    }

    @Test
    public void testDigestNullSafe()
    {
        Digester digester = newDigester();

        digester.hash( null );
        digester.hashes( (Object[]) null );
        digester.hashes( new Object[] { null, null } );
        digester.hashes( (Collection<Object>) null );
        digester.hashes( Arrays.asList( null, null ) );
        digester.hashes( (Map<?, ?>) null );
        digester.hashes( Collections.singletonMap( null, null ) );

        digester.string( null );
        digester.strings( (String[]) null );
        digester.strings( new String[] { null, null } );
        digester.strings( (Collection<String>) null );
        digester.strings( Arrays.<String> asList( null, null ) );

        digester.file( (File) null );
        digester.file( (String) null );
        digester.files( (String[]) null );
        digester.files( new String[] { null, null } );
        digester.files( (File[]) null );
        digester.files( new File[] { null, null } );
        digester.files( (Collection<File>) null );
        digester.files( Arrays.<File> asList( null, null ) );

        assertNotNull( digester.finish() );
    }

    @Test
    public void testRelativeFile()
    {
        File basedir = new File( "target/tests" ).getAbsoluteFile();
        Digester digester = newDigester();
        byte[] digest1 = digester.basedir( basedir ).file( new File( "foo.txt" ) ).finish();
        digester = newDigester();
        byte[] digest2 = digester.file( new File( basedir, "foo.txt" ) ).finish();
        assertArrayEquals( digest1, digest2 );
    }

    @Test
    public void testRelativeFilename()
    {
        File basedir = new File( "target/tests" ).getAbsoluteFile();
        Digester digester = newDigester();
        byte[] digest1 = digester.basedir( basedir ).file( "foo.txt" ).finish();
        digester = newDigester();
        byte[] digest2 = digester.file( new File( basedir, "foo.txt" ) ).finish();
        assertArrayEquals( digest1, digest2 );
    }

    @Test
    public void testFileDigestConsidersTimestamp()
        throws Exception
    {
        File file = File.createTempFile( "tesla", ".tmp" );
        file.deleteOnExit();

        byte[] digest1 = newDigester().file( file ).finish();
        file.setLastModified( file.lastModified() - 120 * 1000 );
        byte[] digest2 = newDigester().file( file ).finish();
        assertEquals( false, Arrays.equals( digest1, digest2 ) );
        file.delete();
        byte[] digest3 = newDigester().file( file ).finish();
        assertEquals( false, Arrays.equals( digest1, digest3 ) );
        assertEquals( false, Arrays.equals( digest2, digest3 ) );
    }

    @Test
    public void testCloneIsDeep()
    {
        Digester digester = newDigester();
        byte[] ref = digester.value( 123456 ).value( true ).finish();

        Digester digester1 = newDigester();
        Digester digester2 = digester1.value( 123456 ).clone();
        byte[] digest2 = digester2.value( false ).finish();
        byte[] digest1 = digester1.value( true ).finish();

        assertEquals( true, Arrays.equals( ref, digest1 ) );
        assertEquals( false, Arrays.equals( ref, digest2 ) );
    }

    @Test
    public void testHashingSetOfStringsIsInsensitiveToOrder()
    {
        byte[] digest1 = newDigester().strings( newSet( "aaa", "bbb", "ccc" ) ).finish();
        byte[] digest2 = newDigester().strings( newSet( "ccc", "bbb", "aaa" ) ).finish();
        assertArrayEquals( digest1, digest2 );
    }

    @Test
    public void testHashingSetOfObjectsIsInsensitiveToOrder()
    {
        byte[] digest1 = newDigester().hashes( newSet( "aaa", "bbb", "ccc" ) ).finish();
        byte[] digest2 = newDigester().hashes( newSet( "ccc", "bbb", "aaa" ) ).finish();
        assertArrayEquals( digest1, digest2 );
    }

    @Test
    public void testHashingSetOfFilesIsInsensitiveToOrder()
    {
        byte[] digest1 =
            newDigester().files( newSet( new File( "aaa" ), new File( "bbb" ), new File( "ccc" ) ) ).finish();
        byte[] digest2 =
            newDigester().files( newSet( new File( "ccc" ), new File( "bbb" ), new File( "aaa" ) ) ).finish();
        assertArrayEquals( digest1, digest2 );
    }

    @Test
    public void testHashingMapOfObjectsIsInsensitiveToOrder()
    {
        Map<Object, Object> map1 = new LinkedHashMap<Object, Object>();
        map1.put( "aaa", "a" );
        map1.put( "bbb", "b" );
        map1.put( "ccc", "c" );

        Map<Object, Object> map2 = new LinkedHashMap<Object, Object>();
        map2.put( "ccc", "c" );
        map2.put( "bbb", "b" );
        map2.put( "aaa", "a" );

        byte[] digest1 = newDigester().hashes( map1 ).finish();
        byte[] digest2 = newDigester().hashes( map2 ).finish();
        assertArrayEquals( digest1, digest2 );
    }

}
