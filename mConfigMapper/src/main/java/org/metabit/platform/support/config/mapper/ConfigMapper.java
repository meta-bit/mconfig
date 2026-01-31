package org.metabit.platform.support.config.mapper;

import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * ConfigMapper provides functionality to map configuration data between ConfigCursor, POJOs and files.
 */
public interface ConfigMapper
{
    /**
     * Maps the configuration entries from the current map position of the cursor to the target POJO.
     * = reading from config into a POJO.
     *
     * @param cursor          The ConfigCursor positioned on a map.
     * @param targetPojo      The object to be populated.
     * @param functionPrefix  Prefix for setter methods (e.g., "set").
     * @param functionPostfix Postfix for setter methods (e.g., "").
     * @return Number of entries successfully mapped.
     *
     * @throws IllegalArgumentException if the cursor is not on a map.
     */
    int mapCursorToPojo(ConfigCursor cursor, Object targetPojo, String functionPrefix, String functionPostfix);

    /**
     * Maps the properties of a POJO back into the configuration at the current cursor position.
     * = write the POJO into the configuration.
     *
     * @param sourcePojo      The POJO providing the data.
     * @param cursor          The ConfigCursor where data should be written.
     * @param writingScopes   The scope or scopes this object is supposed to go into.
     * @param functionPrefix  Prefix for getter methods (e.g., "get" or "is"). null defaults to "get".
     * @param functionPostfix Postfix for getter methods (e.g., ""). null defaults to "".
     * @return Number of properties successfully mapped.
     *
     * @throws ConfigException if a configuration error occurs during mapping.
     */
    int mapPojoToCursor(Object sourcePojo, ConfigCursor cursor, EnumSet<ConfigScope> writingScopes, String functionPrefix, String functionPostfix)
            throws ConfigException;

    /**
     * Easy-to-use wrapper to read a POJO from the configuration at the current cursor position.
     * Uses default constructor and "set" prefix for mapping.
     *
     * @param cursor The ConfigCursor positioned on a map.
     * @param clazz  The class of the POJO to create and populate.
     * @param <T>    The type of the POJO.
     * @return The populated POJO instance.
     * @throws ConfigException if the POJO cannot be instantiated or populated.
     */
    <T> T readObject(ConfigCursor cursor, Class<T> clazz) throws ConfigException;

    /**
     * Easy-to-use wrapper to write a POJO's properties to the configuration at the current cursor position.
     * Uses "get" and "is" prefixes for mapping.
     *
     * @param sourcePojo The POJO providing the data.
     * @param cursor     The ConfigCursor where data should be written.
     * @return number of entries written
     *
     * @throws ConfigException if a configuration error occurs during mapping.
     */
    int writeObject(Object sourcePojo, ConfigCursor cursor) throws ConfigException;

    /**
     * Easy-to-use wrapper to write a POJO's properties to the configuration at the current cursor position,
     * targeting specific scopes.
     * Uses "get" and "is" prefixes for mapping.
     *
     * @param sourcePojo    The POJO providing the data.
     * @param cursor        The ConfigCursor where data should be written.
     * @param writingScopes The scopes to write to.
     * @return number of entries written
     *
     * @throws ConfigException if a configuration error occurs during mapping.
     */
    int writeObject(Object sourcePojo, ConfigCursor cursor, EnumSet<ConfigScope> writingScopes) throws ConfigException;

    /**
     * Saves the given configuration to a specific file.
     * The format is determined by the file extension.
     *
     * @param config     The configuration to save.
     * @param targetFile The target file path.
     * @throws IOException     if an I/O error occurs.
     * @throws ConfigException if a configuration error occurs.
     */
    void saveTo(Configuration config, Path targetFile) throws IOException, ConfigException;

    /**
     * Saves the given configuration to a specific file, filtering by scopes.
     * The format is determined by the file extension.
     *
     * @param config     The configuration to save.
     * @param targetFile The target file path.
     * @param scopes     The scopes to include in the saved file.
     * @throws IOException     if an I/O error occurs.
     * @throws ConfigException if a configuration error occurs.
     */
    void saveTo(Configuration config, Path targetFile, EnumSet<ConfigScope> scopes) throws IOException, ConfigException;

    /**
     * Factory method to create a new ConfigMapper instance.
     *
     * @return A new ConfigMapper instance.
     * @throws RuntimeException if the implementation cannot be instantiated.
     */
    static ConfigMapper create()
    {
        return new ConfigMapperImpl();
    }
}
