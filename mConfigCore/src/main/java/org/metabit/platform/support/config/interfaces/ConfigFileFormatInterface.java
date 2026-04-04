package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigLocation;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * <p>ConfigFileFormatInterface interface.</p>
 *
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
     * @return config layer, or null.
     */
    ConfigLayerInterface readFile(File file, final ConfigLocation location);

    /**
     * <p>readStream.</p>
     *
     * @param inputStream a {@link java.io.InputStream} object
     * @param location a {@link org.metabit.platform.support.config.ConfigLocation} object
     * @return a {@link org.metabit.platform.support.config.interfaces.ConfigLayerInterface} object
     */
    ConfigLayerInterface readStream(InputStream inputStream, final ConfigLocation location);
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
