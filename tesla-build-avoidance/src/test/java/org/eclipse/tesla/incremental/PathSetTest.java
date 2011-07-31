package org.eclipse.tesla.incremental;

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
import java.util.List;

import org.junit.Test;

public class PathSetTest
{

    @Test
    public void testFromFile_Null()
    {
        assertNull( PathSet.fromFile( null ) );
    }

    @Test
    public void testFromFile_File()
    {
        File file = new File( "Test.java" ).getAbsoluteFile();
        PathSet ps = PathSet.fromFile( file );
        assertEquals( file.getParentFile(), ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "Test.java" ) );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
    }

    @Test
    public void testFromFiles_NullFiles()
    {
        List<PathSet> pss = PathSet.fromFiles( new File( "" ), (String[]) null );
        assertNotNull( pss );
        assertEquals( 0, pss.size() );
    }

    @Test
    public void testFromFiles_NullBasedirResolvesRelativeFilesAgainstCurrentDirectory()
    {
        File basedir = new File( "" ).getAbsoluteFile();
        List<PathSet> pss = PathSet.fromFiles( (File) null, "Test.java" );
        assertNotNull( pss );
        assertEquals( 1, pss.size() );
        PathSet ps = pss.iterator().next();
        assertEquals( basedir, ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "Test.java" ) );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
    }

    @Test
    public void testFromFiles_OnlyRelativeFiles()
    {
        File basedir = new File( "dir/subdir" ).getAbsoluteFile();
        List<PathSet> pss = PathSet.fromFiles( basedir, "One.java", "Two.properties" );
        assertNotNull( pss );
        assertEquals( 1, pss.size() );
        PathSet ps = pss.iterator().next();
        assertEquals( basedir, ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "One.java" ) );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "Two.properties" ) );
        assertEquals( 2, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
    }

    @Test
    public void testFromFiles_OnlyAbsoluteFiles()
    {
        File basedir = new File( "dir/subdir" ).getAbsoluteFile();
        List<PathSet> pss =
            PathSet.fromFiles( basedir, new File( "One.java" ).getAbsolutePath(),
                               new File( "Two.properties" ).getAbsolutePath() );
        assertNotNull( pss );
        assertEquals( 2, pss.size() );
        PathSet ps = pss.get( 0 );
        assertEquals( new File( "" ).getAbsoluteFile(), ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "One.java" ) );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
        ps = pss.get( 1 );
        assertEquals( new File( "" ).getAbsoluteFile(), ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "Two.properties" ) );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
    }

    @Test
    public void testFromFiles_RelativeAndAbsoluteFiles()
    {
        File basedir = new File( "dir/subdir" ).getAbsoluteFile();
        List<PathSet> pss =
            PathSet.fromFiles( basedir, "One.java", new File( "Two.properties" ).getAbsolutePath(), "three.TXT" );
        assertNotNull( pss );
        assertEquals( 2, pss.size() );
        PathSet ps = pss.get( 0 );
        assertEquals( basedir, ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "One.java" ) );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "three.TXT" ) );
        assertEquals( 2, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
        ps = pss.get( 1 );
        assertEquals( new File( "" ).getAbsoluteFile(), ps.getBasedir() );
        assertEquals( ps.getIncludes().toString(), true, ps.getIncludes().contains( "Two.properties" ) );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( 0, ps.getExcludes().size() );
        assertEquals( PathSet.Kind.FILES_ONLY, ps.getKind() );
    }

    @Test
    public void testAddIncludes_NullArray()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addIncludes( (String[]) null );
        assertEquals( 0, ps.getIncludes().size() );
    }

    @Test
    public void testAddIncludes_NullArrayElement()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addIncludes( null, "include" );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( true, ps.getIncludes().contains( "include" ) );
    }

    @Test
    public void testAddIncludes_NullCollection()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addIncludes( (Collection<String>) null );
        assertEquals( 0, ps.getIncludes().size() );
    }

    @Test
    public void testAddIncludes_NullCollectionElement()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addIncludes( Arrays.asList( null, "include" ) );
        assertEquals( 1, ps.getIncludes().size() );
        assertEquals( true, ps.getIncludes().contains( "include" ) );
    }

    @Test
    public void testAddExcludes_NullArray()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addExcludes( (String[]) null );
        assertEquals( 0, ps.getExcludes().size() );
    }

    @Test
    public void testAddExcludes_NullArrayElement()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addExcludes( null, "exclude" );
        assertEquals( 1, ps.getExcludes().size() );
        assertEquals( true, ps.getExcludes().contains( "exclude" ) );
    }

    @Test
    public void testAddExcludes_NullCollection()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addExcludes( (Collection<String>) null );
        assertEquals( 0, ps.getExcludes().size() );
    }

    @Test
    public void testAddExcludes_NullCollectionElement()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.addExcludes( Arrays.asList( null, "exclude" ) );
        assertEquals( 1, ps.getExcludes().size() );
        assertEquals( true, ps.getExcludes().contains( "exclude" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testSetKind_Null()
    {
        PathSet ps = new PathSet( new File( "" ).getAbsoluteFile() );
        ps.setKind( null );
    }

    @Test
    public void testIsIncludingFiles()
    {
        File basedir = new File( "" ).getAbsoluteFile();
        PathSet ps = new PathSet( basedir );
        ps.setKind( PathSet.Kind.FILES_ONLY );
        assertEquals( true, ps.isIncludingFiles() );
        ps.setKind( PathSet.Kind.FILES_AND_DIRECTORIES );
        assertEquals( true, ps.isIncludingFiles() );
        ps.setKind( PathSet.Kind.DIRECTORIES_ONLY );
        assertEquals( false, ps.isIncludingFiles() );
    }

    @Test
    public void testIsIncludingDirectories()
    {
        File basedir = new File( "" ).getAbsoluteFile();
        PathSet ps = new PathSet( basedir );
        ps.setKind( PathSet.Kind.FILES_ONLY );
        assertEquals( false, ps.isIncludingDirectories() );
        ps.setKind( PathSet.Kind.FILES_AND_DIRECTORIES );
        assertEquals( true, ps.isIncludingDirectories() );
        ps.setKind( PathSet.Kind.DIRECTORIES_ONLY );
        assertEquals( true, ps.isIncludingDirectories() );
    }

    @Test
    public void testEquals()
    {
        File basedir = new File( "" ).getAbsoluteFile();
        PathSet ps1 = new PathSet( basedir ).addIncludes( "inc" ).addExcludes( "exc" );
        assertTrue( ps1.equals( ps1 ) );
        PathSet ps2 = new PathSet( basedir ).addIncludes( "inc" ).addExcludes( "exc" );
        assertTrue( ps1.equals( ps2 ) );
        assertTrue( ps2.equals( ps1 ) );
        PathSet ps3 = new PathSet( basedir ).addIncludes( "inc" ).addExcludes( "exc" ).setDefaultExcludes( false );
        assertFalse( ps1.equals( ps3 ) );
        assertFalse( ps3.equals( ps1 ) );
    }

    @Test
    public void testHashCode()
    {
        File basedir = new File( "" ).getAbsoluteFile();
        PathSet ps1 = new PathSet( basedir ).addIncludes( "inc" ).addExcludes( "exc" );
        assertTrue( ps1.equals( ps1 ) );
        PathSet ps2 = new PathSet( basedir ).addIncludes( "inc" ).addExcludes( "exc" );
        assertEquals( ps1.hashCode(), ps2.hashCode() );
    }

}
