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

import org.junit.Test;

public class SelectorTest
{

    private static final Collection<String> EMPTY = Collections.emptySet();

    @Test
    public void testIsSelected_DefaultExcludes()
    {
        Selector selector = new Selector( Arrays.asList( "**" ), EMPTY, true, true );
        assertEquals( false, selector.isSelected( ".git" ) );
        assertEquals( false, selector.isSelected( "dir" + File.separator + ".svn" + File.separator + "entries" ) );
    }

    @Test
    public void testIsSelected_NoDefaultExcludes()
    {
        Selector selector = new Selector( Arrays.asList( "**" ), EMPTY, false, true );
        assertEquals( true, selector.isSelected( ".git" ) );
        assertEquals( true, selector.isSelected( "dir" + File.separator + ".svn" + File.separator + "entries" ) );
    }

    @Test
    public void testIsSelected_CaseSensitive()
    {
        Selector selector = new Selector( Arrays.asList( "*.java" ), EMPTY, false, true );
        assertEquals( true, selector.isSelected( "Test.java" ) );
        assertEquals( false, selector.isSelected( "Test.JAVA" ) );
    }

    @Test
    public void testIsSelected_CaseInsensitive()
    {
        Selector selector = new Selector( Arrays.asList( "*.java" ), EMPTY, false, false );
        assertEquals( true, selector.isSelected( "Test.java" ) );
        assertEquals( true, selector.isSelected( "Test.JAVA" ) );
    }

    @Test
    public void testIsAncestorOfPotentiallySelected()
    {
        Selector selector = new Selector( Arrays.asList( "**/*.java" ), Arrays.asList( "**/.svn/**" ), false, true );
        assertEquals( true, selector.isAncestorOfPotentiallySelected( "dir" ) );
        assertEquals( false, selector.isAncestorOfPotentiallySelected( ".svn" ) );
    }

}
