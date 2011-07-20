package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.PathSet;

class DefaultBuildContext
    implements BuildContext
{

    private final MessageHandler messages;

    private final Logger log;

    private final File outputDirectory;

    private final File ioFile;

    private final File configFile;

    private final PathSetResolver pathSetResolver = new DefaultPathSetResolver();

    private Map<PathSet, byte[]> configurations;

    // input -> (timestamp, size)
    private Map<File, FileState> inputStates;

    // output -> input
    private Map<File, Set<File>> inputs;

    // input -> outputs
    private transient Map<File, Collection<File>> outputs;

    private transient Collection<File> deletedInputs;

    private transient Map<File, Collection<File>> addedOutputs;

    public DefaultBuildContext( File outputDirectory, File contextDirectory, String pluginId, MessageHandler messages,
                                Logger log )
    {
        if ( outputDirectory == null )
        {
            throw new IllegalArgumentException( "output directory not specified" );
        }
        if ( contextDirectory == null )
        {
            throw new IllegalArgumentException( "context directory not specified" );
        }
        if ( pluginId == null || pluginId.length() <= 0 )
        {
            throw new IllegalArgumentException( "plugin id not specified" );
        }

        this.messages = ( messages != null ) ? messages : NullMessageHandler.INSTANCE;
        this.log = ( log != null ) ? log : NullLogger.INSTANCE;

        this.outputDirectory = outputDirectory.getAbsoluteFile();

        String name = outputDirectory.getName();
        name = name.substring( 0, Math.min( 4, name.length() ) ) + Integer.toHexString( name.hashCode() );
        File workDir = new File( contextDirectory, name ).getAbsoluteFile();
        ioFile = new File( workDir, "io.ser" );
        configFile =
            new File( workDir, pluginId.substring( 0, Math.min( 4, pluginId.length() ) )
                + Integer.toHexString( pluginId.hashCode() ) + ".ser" );

        this.deletedInputs = new TreeSet<File>( Collections.reverseOrder() );
        this.addedOutputs = new HashMap<File, Collection<File>>();

        load();

        this.outputs = new HashMap<File, Collection<File>>();
        for ( Map.Entry<File, Set<File>> entry : inputs.entrySet() )
        {
            File outputFile = entry.getKey();
            for ( File inputFile : entry.getValue() )
            {
                Collection<File> outputFiles = outputs.get( inputFile );
                if ( outputFiles == null )
                {
                    outputFiles = new TreeSet<File>();
                    outputs.put( inputFile, outputFiles );
                }
                outputFiles.add( outputFile );
            }
        }
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean setConfiguration( PathSet paths, byte[] digest )
    {
        if ( paths == null )
        {
            throw new IllegalArgumentException( "path set not specified" );
        }
        if ( digest == null )
        {
            throw new IllegalArgumentException( "configuration digest not specified" );
        }
        byte[] old = configurations.put( paths, digest );
        return !Arrays.equals( digest, old );
    }

    public Collection<String> getInputs( PathSet paths, boolean fullBuild )
    {
        Collection<String> inputs = new ArrayList<String>();
        for ( Path path : pathSetResolver.resolve( paths, fullBuild ? null : inputStates, outputs ) )
        {
            if ( path.isDeleted() )
            {
                deletedInputs.add( new File( paths.getBasedir(), path.getPath() ) );
            }
            else
            {
                inputs.add( path.getPath() );
            }
        }
        return inputs;
    }

    public OutputStream newOutputStream( File output )
        throws IOException
    {
        if ( output == null )
        {
            throw new IllegalArgumentException( "output file not specified" );
        }
        output = FileUtils.resolve( output, getOutputDirectory() );
        output.getParentFile().mkdirs();
        return new IncrementalFileOutputStream( output );
    }

    public OutputStream newOutputStream( String output )
        throws IOException
    {
        if ( output == null )
        {
            throw new IllegalArgumentException( "output file not specified" );
        }
        return newOutputStream( new File( output ) );
    }

    public void addOutputs( File input, File... outputs )
    {
        if ( outputs == null || outputs.length <= 0 )
        {
            return;
        }

        Collection<File> resolvedOutputs = new ArrayList<File>( outputs.length );
        for ( File output : outputs )
        {
            File resolvedOutput = FileUtils.resolve( output, getOutputDirectory() );
            resolvedOutputs.add( resolvedOutput );
        }

        addOutputs( resolvedOutputs, input );
    }

    public void addOutputs( File input, String... outputs )
    {
        if ( outputs == null || outputs.length <= 0 )
        {
            return;
        }

        Collection<File> resolvedOutputs = new ArrayList<File>( outputs.length );
        for ( String output : outputs )
        {
            File resolvedOutput = FileUtils.resolve( new File( output ), getOutputDirectory() );
            resolvedOutputs.add( resolvedOutput );
        }

        addOutputs( resolvedOutputs, input );
    }

    private void addOutputs( Collection<File> outputs, File input )
    {
        input = input.getAbsoluteFile();

        Collection<File> addedOutputs = this.addedOutputs.get( input );
        if ( addedOutputs == null )
        {
            addedOutputs = new TreeSet<File>();
            this.addedOutputs.put( input, addedOutputs );
        }
        addedOutputs.addAll( outputs );

        inputStates.put( input, new FileState( input ) );

        for ( File output : outputs )
        {
            Set<File> inputs = this.inputs.get( output );
            if ( inputs == null )
            {
                inputs = new TreeSet<File>();
                this.inputs.put( output, inputs );
            }
            inputs.add( input );
        }
    }

    public void finish()
    {
        deleteOutputsWhoseInputStillExistsButNoLongerGeneratesThem();
        deleteOutputsWhoseInputHasBeenDeleted();
        save();
    }

    private void deleteOutputsWhoseInputHasBeenDeleted()
    {
        for ( File deletedInput : deletedInputs )
        {
            inputStates.remove( deletedInput );
            Collection<File> outputs = this.outputs.get( deletedInput );
            deleteSuperfluousOutputs( deletedInput, outputs );
        }
    }

    private void deleteOutputsWhoseInputStillExistsButNoLongerGeneratesThem()
    {
        for ( Map.Entry<File, Collection<File>> entry : addedOutputs.entrySet() )
        {
            File input = entry.getKey();
            Collection<File> currentOutputs = entry.getValue();
            Collection<File> previousOutputs = outputs.get( input );
            if ( previousOutputs == null || previousOutputs.isEmpty() )
            {
                continue;
            }
            previousOutputs = new HashSet<File>( previousOutputs );
            previousOutputs.removeAll( currentOutputs );
            deleteSuperfluousOutputs( input, previousOutputs );
        }
    }

    private void deleteSuperfluousOutputs( File input, Collection<File> outputs )
    {
        if ( outputs == null || outputs.isEmpty() )
        {
            return;
        }
        for ( File output : outputs )
        {
            Collection<File> inputs = this.inputs.get( output );
            if ( inputs == null )
            {
                continue;
            }
            inputs.remove( input );
            if ( inputs.isEmpty() )
            {
                this.inputs.remove( output );
                if ( !output.delete() && output.exists() )
                {
                    log.debug( "Failed to delete stale output " + output );
                }
                else
                {
                    log.debug( "Deleted stale output " + output );
                }
            }
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private void load()
    {
        Object[] objects = load( ioFile, Map.class, Map.class );
        inputs = (Map) objects[0];
        if ( inputs == null )
        {
            inputs = new HashMap<File, Set<File>>();
        }
        inputStates = (Map) objects[1];
        if ( inputStates == null )
        {
            inputStates = new HashMap<File, FileState>();
        }
        configurations = (Map) load( configFile, Map.class )[0];
        if ( configurations == null )
        {
            configurations = new HashMap<PathSet, byte[]>();
        }
    }

    private Object[] load( File inputFile, Class<?>... types )
    {
        Object[] objects = new Object[types.length];
        if ( inputFile.isFile() )
        {
            try
            {
                FileInputStream is = new FileInputStream( inputFile );
                try
                {
                    ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream( is ) );
                    try
                    {
                        for ( int i = 0; i < types.length; i++ )
                        {
                            objects[i] = types[i].cast( ois.readObject() );
                        }
                        return objects;
                    }
                    finally
                    {
                        ois.close();
                    }
                }
                catch ( ClassNotFoundException e )
                {
                    throw (IOException) new IOException( "Corrupted object stream" ).initCause( e );
                }
                catch ( ClassCastException e )
                {
                    throw (IOException) new IOException( "Corrupted object stream" ).initCause( e );
                }
                finally
                {
                    is.close();
                }
            }
            catch ( IOException e )
            {
                log.debug( "Could not deserialize incremental build state from " + inputFile, e );
            }
        }
        return objects;
    }

    private void save()
    {
        save( ioFile, inputs, inputStates );
        save( configFile, configurations );
    }

    private void save( File outputFile, Object... objects )
    {
        try
        {
            outputFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream( outputFile );
            try
            {
                ObjectOutputStream oos = new ObjectOutputStream( new BufferedOutputStream( fos ) );
                for ( Object object : objects )
                {
                    oos.writeObject( object );
                }
                oos.close();
            }
            finally
            {
                fos.close();
            }
        }
        catch ( IOException e )
        {
            log.debug( "Could not serialize incremental build state to " + outputFile, e );
        }
    }

    public void addMessage( File input, int line, int column, String message, int severity, Throwable cause )
    {
        messages.addMessage( input, line, column, message, severity, cause );
    }

    public void clearMessages( File input )
    {
        messages.clearMessages( input );
    }

}
