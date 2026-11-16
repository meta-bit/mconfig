package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * A format for blob-like content (files, JAR resources, ZooKeeper nodes, Kubernetes data entries).
 * <p>
 * <b>Contract between formats and storages (since 0.8.17).</b> A format is a helper of the storage that found the
 * content: it knows the syntax, the storage knows the context (which configuration, its schema, whether the location
 * is a secret store).
 * <ul>
 * <li>{@link #readFile} and {@link #readStream} return a layer, or throw {@link ConfigParseException}. They do not
 *     return {@code null}; empty content gives an empty layer.</li>
 * <li>Content that cannot be read as this format (syntax errors, unsupported top-level structure, unreadable
 *     input) is reported by throwing {@link ConfigParseException} with what the parser knows: position, key path,
 *     the parser's exception. The format does not log the problem and records no event itself.</li>
 * <li>The storage catches the exception and hands it to
 *     {@code org.metabit.platform.support.config.impl.ParseProblemReporter}, together with its context. The reporter
 *     decides whether the parser's own text, which may quote content, can be shown, and writes the log line and the
 *     PARSE event. The storage then continues without that layer.</li>
 * </ul>
 *
 * @version $Id: $Id
 */
public interface ConfigFileFormatInterface extends ConfigFormatInterface
{
    /**
     * <p>getFilenameExtensions.</p>
     *
     * @return a {@link java.util.List} object
     */
    List<String> getFilenameExtensions(); // must not be null, return empty List if none.

    /**
     * read file from given location, applying format interpretation, and construct a ConfigLayer from it.
     *
     * @param file a {@link java.io.File} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @return config layer; never null
     * @throws ConfigParseException if the content cannot be read as this format; see the interface description
     */
    ConfigLayerInterface readFile(File file, final ConfigLocation location) throws ConfigParseException;

    /**
     * <p>readStream.</p>
     *
     * @param inputStream a {@link java.io.InputStream} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @return config layer; never null
     * @throws ConfigParseException if the content cannot be read as this format; see the interface description
     */
    ConfigLayerInterface readStream(InputStream inputStream, final ConfigLocation location) throws ConfigParseException;
    /**
     * <p>createFile.</p>
     *
     * @param fileWithFullPath a {@link java.nio.file.Path} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     */
    ConfigLayerInterface createFile(Path fileWithFullPath, final ConfigLocation location);

    /**
     * Write the contents of a ConfigLayer back to its source (e.g. file).
     *
     * @param layer the layer to write back.
     * @throws ConfigCheckedException on I/O or format conversion errors.
     */
    void writeFile(ConfigLayerInterface layer) throws ConfigCheckedException;
}
//___EOF___
