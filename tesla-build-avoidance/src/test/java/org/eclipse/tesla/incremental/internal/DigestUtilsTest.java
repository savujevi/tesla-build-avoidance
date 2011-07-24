package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import static org.junit.Assert.*;

import org.junit.Test;

public class DigestUtilsTest
{

    @Test
    public void testToHexString()
    {
        String hex =
            DigestUtils.toHexString( new byte[] { 0x00, 0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xAB, (byte) 0xCD,
                (byte) 0xEF } );
        assertEquals( "001234567890abcdef", hex );
    }

}
