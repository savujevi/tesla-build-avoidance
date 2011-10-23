package org.eclipse.tesla.incremental;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Provides incremental build support for code generators and similar tooling that produces output from some input
 * files. The general usage pattern is demonstrated by this simplified example snippet:
 * 
 * <pre>
 * BuildContext buildContext = buildContextManager.newContext( outDir, stateDir, &quot;my-plugin&quot; );
 * try
 * {
 *     PathSet pathSet = new PathSet( inputDir, includes, excludes );
 *     byte[] configDigest = buildContext.newDigester().string( someParameter ).finish();
 *     boolean fullBuild = buildContext.setConfiguration( pathSet, configDigest );
 *     for ( String inputPath : buildContext.getInputs( pathSet, fullBuild ) )
 *     {
 *         File inputFile = new File( inputDir, inputPath );
 *         File outputFile = new File( outputDir, inputPath );
 *         // actually produce output file
 *         buildContext.addOutput( inputFile, outputFile );
 *     }
 *     buildContext.commit();
 * }
 * finally
 * {
 *     buildContext.close();
 * }
 * </pre>
 * 
 * Some methods are provided both via {@link BuildContextManager} and {@link BuildContext}. For efficiency, those
 * methods should be invoked via a {@link BuildContext} instance when possible. The equivalent methods in
 * {@link BuildContextManager} are only provided for cases when a component has no direct access to a
 * {@link BuildContext} instance, e.g. due to API constraints, but still should participate in an active build context
 * that has been created by a component higher up in the call hierarchy.
 */
public interface BuildContextManager
{

    // NOTE: The following constants are duplicated here for more convenient API use.

    /**
     * Message severity to report a warning for an input file.
     * 
     * @see #addMessage(File, int, int, String, int, Throwable)
     */
    public static final int SEVERITY_WARNING = BuildContext.SEVERITY_WARNING;

    /**
     * Message severity to report an error for an input file.
     * 
     * @see #addMessage(File, int, int, String, int, Throwable)
     */
    public static final int SEVERITY_ERROR = BuildContext.SEVERITY_ERROR;

    /**
     * Creates a new build context to update files in the specified output directory.
     * 
     * @param outputDirectory The output directory for the files to be produced, must not be {@code null}.
     * @param stateDirectory The temporary directory where auxiliary state related to incremental building is stored,
     *            must not be {@code null}. This directory can safely be shared among all components/plugins producing
     *            build output and use a well-known location. However, this directory should reside in a location which
     *            gets automatically deleted during a full clean of the project output.
     * @param builderId The unique identifier of the component using the build context, must not be {@code null}.
     * @return The new build context, never {@code null}.
     */
    BuildContext newContext( File outputDirectory, File stateDirectory, String builderId );

    /**
     * Registers the specified output for an input file/directory. This method may be called repeatedly for the same
     * input file, e.g. in case a single input file produces more than one output file. If the output is not produced
     * from a particular file but rather a possibly empty collection of input files that get aggregated, the input
     * parameter should be {@code null}.<br>
     * <br>
     * <strong>Note:</strong> The various {@code addOutput*()} methods are used to describe the precise relationship
     * between input files and output files, their proper use is crucial for incremental building to work flawlessly.
     * 
     * @param input The input file/directory, may be {@code null}.
     * @param output The output file/directory, may be {@code null}.
     */
    void addOutput( File input, File output );

    /**
     * Registers the specified outputs for an input file. This method may be called repeatedly for the same input file,
     * e.g. in case a single input file produces more than one output file. If the output is not produced from a
     * particular file but rather a possibly empty collection of input files that get aggregated, the input parameter
     * should be {@code null}.<br>
     * <br>
     * <strong>Note:</strong> The various {@code addOutput*()} methods are used to describe the precise relationship
     * between input files and output files, their proper use is crucial for incremental building to work flawlessly.
     * 
     * @param input The input file/directory, may be {@code null}.
     * @param outputs The output files/directories, may be {@code null}.
     */
    void addOutputs( File input, File... outputs );

    /**
     * Registers the specified outputs for an input file. This method may be called repeatedly for the same input file,
     * e.g. in case a single input file produces more than one output file. If the output is not produced from a
     * particular file but rather a possibly empty collection of input files that get aggregated, the input parameter
     * should be {@code null}.<br>
     * <br>
     * <strong>Note:</strong> The various {@code addOutput*()} methods are used to describe the precise relationship
     * between input files and output files, their proper use is crucial for incremental building to work flawlessly.
     * 
     * @param input The input file/directory, may be {@code null}.
     * @param outputs The output files/directories, may be {@code null}.
     */
    void addOutputs( File input, Collection<File> outputs );

    /**
     * Registers the specified outputs for an input file. This method may be called repeatedly for the same input file,
     * e.g. in case a single input file produces more than one output file. If the output is not produced from a
     * particular file but rather a possibly empty collection of input files that get aggregated, the input parameter
     * should be {@code null}.<br>
     * <br>
     * <strong>Note:</strong> The various {@code addOutput*()} methods are used to describe the precise relationship
     * between input files and output files, their proper use is crucial for incremental building to work flawlessly.
     * 
     * @param input The input file/directory, may be {@code null}.
     * @param outputs The path set describing the output files/directories, may be {@code null}. <strong>Note:</strong>
     *            For incremental building to work properly, this path set should only describe the outputs from the
     *            given input and not overlap with the output of other inputs.
     */
    void addOutputs( File input, PathSet outputs );

    /**
     * Opens an output stream to the specified file. Use of this method is not obligatory but still strongly
     * recommended. Unlike the mere invocation of {@link java.io.FileInputStream#FileInputStream(File) new
     * FileInputStream( output )}, this method ensures that any parent directories of the output file are created.
     * Furthermore, the returned stream can better cooperate with the incremental build support, i.e. suppress
     * unnecessary file modifications.
     * 
     * @param output The output file to open the stream to, must not be {@code null}.
     * @return The new output stream, never {@code null}.
     * @throws FileNotFoundException If the file could not be opened.
     */
    OutputStream newOutputStream( File output )
        throws FileNotFoundException;

    /**
     * Adds a warning/error message about a problem with the specified file to the build. For a build on the
     * commandline, these messages are usually turned into ordinary log messages send to the console. IDEs however may
     * use different means to signal the problem to the user like error markers shown in the file's editor.<br>
     * <br>
     * The warnings/errors are generally persistent until the underlying problem has been solved. Hence use of this
     * method must be preceded with a call to {@link #clearMessages(File)} when processing an input file in order to
     * reset any previous build state like this:
     * 
     * <pre>
     * buildContextManager.clearMessages( input );
     * ...
     * buildContextManager.addMessage( input, ... );
     * ...
     * buildContextManager.addMessage( input, ... );
     * </pre>
     * 
     * @param input The input file to add a message for, must not be {@code null}.
     * @param line The one-based line number inside the file where the problem exists, may be non-positive if the line
     *            number is unknown.
     * @param column The one-based column number inside the file where the problem exists, may be non-positive if the
     *            column number is unknown.
     * @param message The warning/error message to add, may be {@code null} to derive a message from the issue cause.
     * @param severity The severity of the problem, one of {@link #SEVERITY_WARNING} or {@link #SEVERITY_ERROR}.
     * @param cause The cause of the problem, may be {@code null} if none.
     * @see #clearMessages(File)
     */
    void addMessage( File input, int line, int column, String message, int severity, Throwable cause );

    /**
     * Clears any previously generated warning/error messages for the specified input file. This method should be called
     * before an input file is processed and any current problems are reported.
     * 
     * @param input The input file whose warning/error messages should be cleared, must not be {@code null}.
     * @see #addMessage(File, int, int, String, int, Throwable)
     */
    void clearMessages( File input );

}
