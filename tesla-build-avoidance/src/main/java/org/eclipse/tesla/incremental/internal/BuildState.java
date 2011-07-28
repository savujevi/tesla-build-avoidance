package org.eclipse.tesla.incremental.internal;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.tesla.incremental.PathSet;

class BuildState
    implements Serializable
{

    private transient File stateFile;

    private Map<PathSet, byte[]> configurations;

    // input -> #errors
    private Map<File, Integer> errors;

    // input -> (timestamp, size)
    private Map<File, FileState> inputStates;

    // output -> input
    private Map<File, Collection<File>> inputs;

    // input -> outputs
    private transient Map<File, Collection<File>> outputs;

    public BuildState( File stateFile )
    {
        if ( stateFile == null )
        {
            throw new IllegalArgumentException( "state file not specified" );
        }
        this.stateFile = stateFile;

        errors = new HashMap<File, Integer>();
        inputStates = new HashMap<File, FileState>( 256 );
        inputs = new HashMap<File, Collection<File>>( 256 );
        configurations = new HashMap<PathSet, byte[]>( 256 );
        outputs = new HashMap<File, Collection<File>>( 256 );
    }

    public File getStateFile()
    {
        return stateFile;
    }

    public static BuildState load( File stateFile )
        throws IOException
    {
        FileInputStream is = new FileInputStream( stateFile );
        try
        {
            ObjectInputStream ois = new ObjectInputStream( new BufferedInputStream( is ) );
            try
            {
                BuildState state = (BuildState) ois.readObject();
                state.stateFile = stateFile;
                return state;
            }
            catch ( ClassNotFoundException e )
            {
                throw (IOException) new IOException( "Corrupted build state file" ).initCause( e );
            }
            catch ( ClassCastException e )
            {
                throw (IOException) new IOException( "Corrupted build state file" ).initCause( e );
            }
            finally
            {
                ois.close();
            }
        }
        finally
        {
            is.close();
        }
    }

    private void readObject( ObjectInputStream ois )
        throws IOException, ClassNotFoundException
    {
        ois.defaultReadObject();

        outputs = new HashMap<File, Collection<File>>( inputs.size() );

        for ( Map.Entry<File, Collection<File>> entry : inputs.entrySet() )
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

    public synchronized void save()
        throws IOException
    {
        stateFile.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream( stateFile );
        try
        {
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( this );
            oos.close();
        }
        finally
        {
            fos.close();
        }
    }

    public synchronized boolean setConfiguration( PathSet paths, byte[] digest )
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

    public synchronized Collection<File> setOutputs( File input, Collection<File> outputs )
    {
        Collection<File> obsoleteOutputs = Collections.emptySet();

        if ( input != null )
        {
            inputStates.put( input, new FileState( input ) );

            Collection<File> outputsOfInput = new TreeSet<File>();
            obsoleteOutputs = this.outputs.put( input, outputsOfInput );

            if ( outputs != null && !outputs.isEmpty() )
            {
                for ( File output : outputs )
                {
                    if ( output != null )
                    {
                        outputsOfInput.add( output );

                        Collection<File> inputsForOutput = inputs.get( output );
                        if ( inputsForOutput == null )
                        {
                            inputsForOutput = new TreeSet<File>();
                            inputs.put( output, inputsForOutput );
                        }
                        inputsForOutput.add( input );
                    }
                }
            }

            if ( obsoleteOutputs == null )
            {
                obsoleteOutputs = Collections.emptySet();
            }
            else if ( !obsoleteOutputs.isEmpty() )
            {
                obsoleteOutputs.removeAll( outputsOfInput );
                obsoleteOutputs = removeInput( input, obsoleteOutputs );
            }
        }

        return obsoleteOutputs;
    }

    public synchronized Collection<File> removeInput( File input )
    {
        Collection<File> orphanedOutputs = Collections.emptySet();

        if ( input != null )
        {
            errors.remove( input );
            inputStates.remove( input );

            Collection<File> outputsOfInput = outputs.remove( input );

            orphanedOutputs = removeInput( input, outputsOfInput );
        }

        return orphanedOutputs;
    }

    private Collection<File> removeInput( File input, Collection<File> outputs )
    {
        Collection<File> superfluousOutputs;

        if ( outputs != null && !outputs.isEmpty() )
        {
            superfluousOutputs = new ArrayList<File>();

            for ( File output : outputs )
            {
                Collection<File> inputsForOutput = inputs.get( output );
                if ( inputsForOutput == null )
                {
                    continue;
                }
                inputsForOutput.remove( input );
                if ( inputsForOutput.isEmpty() )
                {
                    inputs.remove( output );
                    superfluousOutputs.add( output );
                }
            }
        }
        else
        {
            superfluousOutputs = Collections.emptySet();
        }

        return superfluousOutputs;
    }

    public synchronized Collection<File> getInputs( File output )
    {
        Collection<File> inputsForOutput = inputs.get( output );
        if ( inputsForOutput == null )
        {
            inputsForOutput = Collections.emptySet();
        }
        return inputsForOutput;
    }

    public synchronized Collection<File> getOutputs( File input )
    {
        Collection<File> outputsOfInput = outputs.get( input );
        if ( outputsOfInput == null )
        {
            outputsOfInput = Collections.emptySet();
        }
        return outputsOfInput;
    }

    public synchronized FileState getInputState( File input )
    {
        return inputStates.get( input );
    }

    public synchronized Map<File, FileState> getInputStates()
    {
        return inputStates;
    }

    public synchronized boolean isProcessingRequired( File input )
    {
        FileState previousState = inputStates.get( input );
        if ( previousState == null )
        {
            return true;
        }
        if ( previousState.getTimestamp() != input.lastModified() )
        {
            return true;
        }
        if ( previousState.getSize() != input.length() )
        {
            return true;
        }

        if ( isOutputMissing( input ) )
        {
            return true;
        }

        return false;
    }

    private boolean isOutputMissing( File input )
    {
        Collection<File> outputsOfInput = outputs.get( input );
        if ( outputsOfInput != null )
        {
            for ( File output : outputsOfInput )
            {
                if ( !output.exists() )
                {
                    return true;
                }
            }
        }
        return outputsOfInput == null || outputsOfInput.isEmpty();
    }

    public synchronized void addError( File input )
    {
        Integer num = errors.get( input );
        if ( num == null )
        {
            num = Integer.valueOf( 1 );
        }
        else
        {
            num = Integer.valueOf( num.intValue() + 1 );
        }
        errors.put( input, num );
    }

    public synchronized int clearErrors( File input )
    {
        Integer num = errors.remove( input );
        return ( num != null ) ? num.intValue() : 0;
    }

    public synchronized int getErrors()
    {
        int num = 0;
        for ( Integer n : errors.values() )
        {
            num += n.intValue();
        }
        return num;
    }

}
