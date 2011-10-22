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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.tesla.incremental.BuildContext;
import org.eclipse.tesla.incremental.PathSet;

class BuildState
    implements Serializable
{

    private transient File stateFile;

    private Map<PathSet, byte[]> configurations;

    private Map<Serializable, Serializable> values;

    // input -> messages
    private Map<File, Collection<Message>> messages;

    // input -> (timestamp, size)
    private Map<File, FileState> inputStates;

    // output -> inputs
    private Map<File, Collection<File>> inputs;

    // input -> referenced inputs
    private Map<File, Collection<File>> referencedInputs;

    // referenced inputs -> (timestamp, size)
    private Map<File, FileState> referencedInputsStates;

    // input -> outputs
    private transient Map<File, Collection<File>> outputs;

    public BuildState( File stateFile )
    {
        if ( stateFile == null )
        {
            throw new IllegalArgumentException( "state file not specified" );
        }
        this.stateFile = stateFile;

        configurations = new HashMap<PathSet, byte[]>();
        values = new HashMap<Serializable, Serializable>();
        messages = new HashMap<File, Collection<Message>>();
        inputStates = new HashMap<File, FileState>( 256 );
        inputs = new HashMap<File, Collection<File>>( 256 );
        outputs = new HashMap<File, Collection<File>>( 256 );
        referencedInputs = new HashMap<File, Collection<File>>();
        referencedInputsStates = new HashMap<File, FileState>();
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

    public synchronized Serializable setValue( Serializable key, Serializable value )
    {
        if ( value == null )
        {
            return values.remove( key );
        }
        else
        {
            return values.put( key, value );
        }
    }

    public synchronized Serializable getValue( Serializable key )
    {
        return values.get( key );
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

    public synchronized void setReferencedInputs( File input, Collection<File> referencedInputs )
    {
        if ( referencedInputs != null && !referencedInputs.isEmpty() )
        {
            this.referencedInputs.put( input, new TreeSet<File>( referencedInputs ) );

            for ( File referencedInput : referencedInputs )
            {
                referencedInputsStates.put( referencedInput, new FileState( referencedInput ) );
            }
        }
        else
        {
            this.referencedInputs.remove( input );
        }
    }

    public synchronized Collection<File> removeInput( File input )
    {
        Collection<File> orphanedOutputs = Collections.emptySet();

        if ( input != null )
        {
            messages.remove( input );
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
        if ( isChangedOrDeleted( input, previousState ) )
        {
            return true;
        }

        if ( isOutputMissing( input ) )
        {
            return true;
        }

        if ( isReferencedInputChangedOrDeleted( input ) )
        {
            return true;
        }

        return false;
    }

    private boolean isReferencedInputChangedOrDeleted( File input )
    {
        Collection<File> referencedInputs = this.referencedInputs.get( input );
        if ( referencedInputs == null )
        {
            return false;
        }

        for ( File referencedInput : referencedInputs )
        {
            if ( isChangedOrDeleted( referencedInput, referencedInputsStates.get( referencedInput ) ) )
            {
                return true;
            }
        }

        return false;
    }

    private boolean isChangedOrDeleted( File file, FileState fileState )
    {
        if ( fileState == null )
        {
            return true;
        }
        if ( fileState.isDirectory() != file.isDirectory() )
        {
            return true;
        }
        if ( !fileState.isDirectory() )
        {
            if ( fileState.getTimestamp() != file.lastModified() )
            {
                return true;
            }
            if ( fileState.getSize() != file.length() )
            {
                return true;
            }
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
        return false;
    }

    public synchronized Collection<Message> clearErrors( File input )
    {
        return messages.remove( input );
    }

    public synchronized int getErrors( Collection<PathSet> pathSets )
    {
        int num = 0;

        for ( File input : getSelectedInputs( pathSets, messages.keySet(), referencedInputs ) )
        {
            Collection<Message> inputMessages = messages.get( input );
            if ( inputMessages != null )
            {
                for ( Message message : inputMessages )
                {
                    if ( BuildContext.SEVERITY_ERROR == message.getSeverity() )
                    {
                        num++;
                    }
                }
            }
        }

        return num;
    }

    public synchronized Map<File, Collection<Message>> getSelectedMessages( Collection<PathSet> pathSets,
                                                                            Map<File, Collection<Message>> messages )
    {
        Map<File, Collection<Message>> selected = new HashMap<File, Collection<Message>>();

        for ( File input : getSelectedInputs( pathSets, messages.keySet(), referencedInputs ) )
        {
            Collection<Message> inputMessages = messages.get( input );
            if ( inputMessages != null )
            {
                selected.put( input, new ArrayList<Message>( inputMessages ) );
            }
        }

        return selected;
    }

    private static Set<File> getSelectedInputs( Collection<PathSet> pathSets, Collection<File> inputs,
                                                Map<File, Collection<File>> referenced )
    {
        Set<File> selected = new HashSet<File>();

        if ( pathSets != null && !pathSets.isEmpty() )
        {
            Map<PathSet, Selector> selectors = new LinkedHashMap<PathSet, Selector>();
            for ( PathSet pathSet : pathSets )
            {
                selectors.put( pathSet, new GlobSelector( pathSet ) );
            }

            for ( File input : inputs )
            {
                if ( isSelected( selectors, input ) )
                {
                    selected.add( input );
                }
            }

            if ( referenced != null )
            {
                for ( Map.Entry<File, Collection<File>> entry : referenced.entrySet() )
                {
                    if ( isSelected( selectors, entry.getKey() ) )
                    {
                        selected.addAll( entry.getValue() );
                    }
                }
            }
        }

        return selected;
    }

    private static boolean isSelected( Map<PathSet, Selector> selectors, File file )
    {
        for ( Map.Entry<PathSet, Selector> ent : selectors.entrySet() )
        {
            File basedir = ent.getKey().getBasedir();
            String pathname = FileUtils.relativize( file, basedir );
            return pathname != null && ent.getValue().isSelected( pathname );
        }
        return false;
    }

    /**
     * Cleans up referenced inputs and reference inputs states that are not referenced from any input.
     */
    public void cleanupReferencedInputs()
    {
        referencedInputs.keySet().retainAll( outputs.keySet() );

        // this should be okay performance-wise as it is unlikely to have very large number of referenced inputs
        HashSet<File> allReferencedInputs = new HashSet<File>();
        for ( Collection<File> referencedInputs : this.referencedInputs.values() )
        {
            if ( referencedInputs != null )
            {
                allReferencedInputs.addAll( referencedInputs );
            }
        }
        referencedInputsStates.keySet().retainAll( allReferencedInputs );
    }

    /**
     * Returns old uncleared messages.
     */
    public Map<File, Collection<Message>> mergeMessages( Map<File, Collection<Message>> messages )
    {
        Map<File, Collection<Message>> oldMessages = new HashMap<File, Collection<Message>>( this.messages );
        this.messages.putAll( messages );
        return oldMessages;
    }
}
