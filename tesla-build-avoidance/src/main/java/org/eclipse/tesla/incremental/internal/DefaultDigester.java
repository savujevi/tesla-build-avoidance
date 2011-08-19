package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.tesla.incremental.Digester;

class DefaultDigester
    implements Digester, Cloneable
{

    private MessageDigest digester;

    private byte[] digest;

    private File basedir;

    public DefaultDigester()
    {
        digester = DigestUtils.newMessageDigest();
        digest = digester.digest();
        basedir = new File( "" ).getAbsoluteFile();
    }

    private static void xor( byte[] dst, byte[] src )
    {
        for ( int i = 0; i < src.length; i++ )
        {
            dst[i] ^= src[i];
        }
    }

    private void update( Object object )
    {
        if ( object != null )
        {
            update( object.hashCode() );
        }
    }

    private void update( String string )
    {
        if ( string != null )
        {
            try
            {
                digester.update( string.getBytes( "UTF-8" ) );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new IllegalStateException( "Broken JVM", e );
            }
        }
    }

    private void update( long value )
    {
        byte[] tmp = new byte[8];
        tmp[0] = (byte) ( ( value >> 0x00 ) & 0xFF );
        tmp[1] = (byte) ( ( value >> 0x08 ) & 0xFF );
        tmp[2] = (byte) ( ( value >> 0x10 ) & 0xFF );
        tmp[3] = (byte) ( ( value >> 0x18 ) & 0xFF );
        tmp[4] = (byte) ( ( value >> 0x20 ) & 0xFF );
        tmp[5] = (byte) ( ( value >> 0x28 ) & 0xFF );
        tmp[6] = (byte) ( ( value >> 0x30 ) & 0xFF );
        tmp[7] = (byte) ( ( value >> 0x38 ) & 0xFF );
        digester.update( tmp, 0, 8 );
    }

    private void update( File file )
    {
        file = FileUtils.resolve( file, basedir );
        if ( file != null )
        {
            update( file.getAbsolutePath() );
            if ( file.isFile() )
            {
                update( file.lastModified() );
                update( file.length() );
            }
        }
    }

    private Digester digest()
    {
        digester.update( digest );
        digest = digester.digest();
        return this;
    }

    public Digester value( boolean value )
    {
        digester.update( value ? 0 : Byte.MIN_VALUE );
        return digest();
    }

    public Digester value( long value )
    {
        update( value );
        return digest();
    }

    public Digester value( double value )
    {
        return value( Double.doubleToLongBits( value ) );
    }

    public Digester string( String string )
    {
        update( string );
        return digest();
    }

    public Digester strings( String... strings )
    {
        if ( strings != null )
        {
            for ( String string : strings )
            {
                update( string );
            }
        }
        return digest();
    }

    public Digester strings( Collection<String> strings )
    {
        if ( strings instanceof Set )
        {
            byte[] tmp = new byte[digest.length];
            for ( String string : strings )
            {
                update( string );
                xor( tmp, digester.digest() );
            }
            digester.update( tmp, 0, digest.length );
        }
        else if ( strings != null )
        {
            for ( String string : strings )
            {
                update( string );
            }
        }
        return digest();
    }

    public Digester basedir( File dir )
    {
        if ( dir != null )
        {
            basedir = dir.getAbsoluteFile();
        }
        else
        {
            basedir = new File( "" ).getAbsoluteFile();
        }
        return this;
    }

    public Digester file( File file )
    {
        update( file );
        return digest();
    }

    public Digester files( File... files )
    {
        if ( files != null )
        {
            for ( File file : files )
            {
                update( file );
            }
        }
        return digest();
    }

    public Digester files( Collection<?> files )
    {
        if ( files instanceof Set )
        {
            byte[] tmp = new byte[digest.length];
            for ( Object file : files )
            {
                if ( file instanceof File )
                {
                    update( (File) file );
                }
                else if ( file instanceof String )
                {
                    update( new File( (String) file ) );
                }
                else
                {
                    update( (File) null );
                }
                xor( tmp, digester.digest() );
            }
            digester.update( tmp, 0, digest.length );
        }
        else if ( files != null )
        {
            for ( Object file : files )
            {
                if ( file instanceof File )
                {
                    update( (File) file );
                }
                else if ( file instanceof String )
                {
                    update( new File( (String) file ) );
                }
                else
                {
                    update( (File) null );
                }
            }
        }
        return digest();
    }

    public Digester file( String file )
    {
        update( ( file != null ) ? new File( file ) : null );
        return digest();
    }

    public Digester files( String... files )
    {
        if ( files != null )
        {
            for ( String file : files )
            {
                update( ( file != null ) ? new File( file ) : null );
            }
        }
        return digest();
    }

    public Digester hash( Object obj )
    {
        update( obj );
        return digest();
    }

    public Digester hashes( Object... objs )
    {
        if ( objs != null )
        {
            for ( Object obj : objs )
            {
                update( obj );
            }
        }
        return digest();
    }

    public Digester hashes( Collection<?> objs )
    {
        if ( objs instanceof Set )
        {
            byte[] tmp = new byte[digest.length];
            for ( Object obj : objs )
            {
                update( obj );
                xor( tmp, digester.digest() );
            }
            digester.update( tmp, 0, digest.length );
        }
        else if ( objs != null )
        {
            for ( Object obj : objs )
            {
                update( obj );
            }
        }
        return digest();
    }

    public Digester hashes( Map<?, ?> map )
    {
        if ( map != null )
        {
            byte[] tmp = new byte[digest.length];
            for ( Map.Entry<?, ?> entry : map.entrySet() )
            {
                Object key = entry.getKey();
                Object val = entry.getValue();
                if ( key instanceof File )
                {
                    update( (File) key );
                }
                else if ( key instanceof String )
                {
                    update( (String) key );
                }
                else
                {
                    update( key );
                }
                if ( val instanceof File )
                {
                    update( (File) val );
                }
                else if ( val instanceof String )
                {
                    update( (String) val );
                }
                else
                {
                    update( val );
                }
                xor( tmp, digester.digest() );
            }
            digester.update( tmp, 0, digest.length );
        }
        return digest();
    }

    public byte[] finish()
    {
        byte[] result = digest;
        digest = digester.digest();
        return result;
    }

    @Override
    public Digester clone()
    {
        try
        {
            DefaultDigester clone = (DefaultDigester) super.clone();
            clone.digester = DigestUtils.newMessageDigest();
            return clone;
        }
        catch ( CloneNotSupportedException e )
        {
            throw new IllegalStateException( "Broken JVM", e );
        }
    }

}
