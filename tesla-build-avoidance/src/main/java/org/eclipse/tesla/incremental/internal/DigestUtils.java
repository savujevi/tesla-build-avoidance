package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class DigestUtils
{

    public static MessageDigest newMessageDigest()
    {
        try
        {
            return MessageDigest.getInstance( "SHA-1" );
        }
        catch ( NoSuchAlgorithmException e )
        {
            try
            {
                return MessageDigest.getInstance( "MD5" );
            }
            catch ( NoSuchAlgorithmException e2 )
            {
                throw new IllegalStateException( "Could not initialize configuration digester", e );
            }
        }
    }

    public static String toHexString( byte[] digest )
    {
        String chars = "0123456789abcdef";
        StringBuilder buffer = new StringBuilder( digest.length * 2 );
        for ( int i = 0; i < digest.length; i++ )
        {
            int b = digest[i] & 0xFF;
            buffer.append( chars.charAt( b >> 4 ) );
            buffer.append( chars.charAt( b & 0xF ) );
        }
        return buffer.toString();
    }

}
