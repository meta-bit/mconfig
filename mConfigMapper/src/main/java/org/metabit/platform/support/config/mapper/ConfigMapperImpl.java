package org.metabit.platform.support.config.mapper;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * ConfigMapper provides functionality to map configuration data from a ConfigCursor to POJO objects using reflection.
 * that is: reading from config into a POJO, and back.
 */
///  1.  **POJO Scan:** The mapper first inspects the **target POJO** class to
///      identify all available setter methods that match the provided `functionPrefix` (default "set")
///      and `functionPostfix` (default "").
///      It extracts the property names from these methods and stores them in a map for quick lookup.
///  2.  **Config Iteration:** The mapper then **iterates through the configuration entries**
///      at the current cursor position.
///  3.  **Matching:** For each configuration entry found, it checks if its key
///      matches one of the property names discovered in the POJO (case-insensitively).
///  4.  **Invocation:** If a match is found, the mapper converts the configuration value
///      to the appropriate type required by the POJO's setter and invokes it.
///
///  This approach ensures that it only attempts to map entries that the POJO is actually capable of receiving,
///  while remaining efficient by only processing the configuration data present at the cursor's location.
public class ConfigMapperImpl implements ConfigMapper
{
    @Override
    public int mapCursorToPojo(ConfigCursor cursor, Object targetPojo, String functionPrefix, String functionPostfix)
        {
        if (!cursor.isOnMap())
            {
            throw new IllegalArgumentException("Cursor must be positioned on a map to perform mapping.");
            }
        if (functionPrefix == null) functionPrefix = "set";
        if (functionPostfix == null) functionPostfix = "";

        int count = 0;
        Map<String, Method> setterMap = getSetterMap(targetPojo.getClass(), functionPrefix, functionPostfix);

        if (cursor.enter())
            {
            while (cursor.moveNext())
                {
                ConfigEntry entry = cursor.getCurrentElement();
                if (entry != null)
                    {
                    String key = entry.getKey();
                    Method setter = setterMap.get(key.toLowerCase());

                    if (setter != null)
                        {
                        try
                            {
                            Object value = convertEntryToType(entry, setter.getParameterTypes()[0], cursor);
                            if (value != null)
                                {
                                setter.invoke(targetPojo, value);
                                count++;
                                }
                            }
                        catch (Exception e)
                            {
                            // Guidelines: "If the use of a setter is not permitted, this is not cause for abort or error."
                            }
                        }
                    }
                }
            cursor.leave();
            }
        return count;
        }

    /**
     * Maps POJO properties to config cursor entries. that is, writing from POJO into config.
     */
    @Override
    public int mapPojoToCursor(Object sourcePojo, ConfigCursor cursor, EnumSet<ConfigScope> targetScopes, String functionPrefix, String functionPostfix)
            throws ConfigException
        {
        if (sourcePojo == null || cursor == null)
            {
            throw new IllegalArgumentException("Source POJO and cursor must not be null");
            }
        if (functionPrefix == null) functionPrefix = "get";
        if (functionPostfix == null) functionPostfix = "";

        int count = 0;
        Class<?> clazz = sourcePojo.getClass();
        for (Method method : clazz.getMethods())
            {
            String name = method.getName();
            if (name.startsWith(functionPrefix) && name.endsWith(functionPostfix) && method.getParameterCount() == 0 && !name.equals("getClass"))
                {
                String propertyName = name.substring(functionPrefix.length(), name.length()-functionPostfix.length());

                if (!propertyName.isEmpty())
                    {
                    // Convert property name to camelCase (e.g., Name -> name)
                    if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(0)) && Character.isLowerCase(propertyName.charAt(1)))
                        {
                        propertyName = Character.toLowerCase(propertyName.charAt(0))+propertyName.substring(1);
                        }
                    else if (propertyName.length() == 1)
                        {
                        propertyName = propertyName.toLowerCase();
                        }

                    try
                        {
                        Object value = method.invoke(sourcePojo);
                        if (value != null)
                            {
                            boolean moved = cursor.moveTo(propertyName);
                            ConfigEntry entry = moved ? cursor.getCurrentElement() : null;

                            if (entry != null && (targetScopes == null || targetScopes.isEmpty() || targetScopes.contains(entry.getScope())))
                                {
                                entry.putValue(value, determineEntryType(value));
                                count++;
                                }
                            else
                                {
                                // Either entry doesn't exist, or it's in the wrong scope.
                                // We need to write to one of the target scopes instead.
                                try
                                    {
                                    ConfigScope targetScope = (targetScopes == null || targetScopes.isEmpty())
                                            ? ConfigScope.USER : (ConfigScope) targetScopes.toArray()[0];

                                    cursor.put(propertyName, value, targetScope);
                                    count++;
                                    }
                                catch (Exception e)
                                    {
                                    // Fallback if cursor.put fails
                                    if (entry != null)
                                        {
                                        entry.putValue(value, determineEntryType(value));
                                        count++;
                                        }
                                    }
                                }
                            }
                        }
                    catch (Exception e)
                        {
                        // Ignore mapping errors as per guidelines
                        }
                    }
                }
            }
        return count;
        }

    @Override
    public <T> T readObject(ConfigCursor cursor, Class<T> clazz)
            throws ConfigException
        {
        try
            {
            T pojo = clazz.getDeclaredConstructor().newInstance();
            mapCursorToPojo(cursor, pojo, "set", "");
            return pojo;
            }
        catch (Exception e)
            {
            throw new ConfigException("Failed to instantiate or populate POJO of type "+clazz.getName()+": "+e.getMessage());
            }
        }

    @Override
    public int writeObject(Object sourcePojo, ConfigCursor cursor)
            throws ConfigException
        {
        return writeObject(sourcePojo, cursor, EnumSet.of(ConfigScope.USER));
        }

    @Override
    public int writeObject(Object sourcePojo, ConfigCursor cursor, EnumSet<ConfigScope> writingScopes)
            throws ConfigException
        {
        // For writeObject, we try both "get" and "is" prefixes if they match standard POJO patterns
        int count = mapPojoToCursor(sourcePojo, cursor, writingScopes, "get", "");
        count += mapPojoToCursor(sourcePojo, cursor, writingScopes, "is", "");
        return count;
        }

    private ConfigEntryType determineEntryType(Object value)
        {
        if (value instanceof String) return ConfigEntryType.STRING;
        if (value instanceof Boolean) return ConfigEntryType.BOOLEAN;
        if (value instanceof Integer || value instanceof Long || value instanceof java.math.BigInteger || value instanceof java.math.BigDecimal) return ConfigEntryType.NUMBER;
        if (value instanceof byte[]) return ConfigEntryType.BYTES;
        if (value instanceof java.util.List) return ConfigEntryType.MULTIPLE_STRINGS;
        return ConfigEntryType.STRING; // Fallback
        }

    private Map<String, Method> getSetterMap(Class<?> clazz, String prefix, String postfix)
        {
        Map<String, Method> map = new HashMap<>();
        for (Method method : clazz.getMethods())
            {
            String name = method.getName();
            if (name.startsWith(prefix) && name.endsWith(postfix) && method.getParameterCount() == 1)
                {
                String propertyName = name.substring(prefix.length(), name.length()-postfix.length());
                if (!propertyName.isEmpty())
                    {
                    map.put(propertyName.toLowerCase(), method);
                    }
                }
            }
        return map;
        }

    private Object convertEntryToType(ConfigEntry entry, Class<?> type, ConfigCursor cursor)
            throws ConfigCheckedException
        {
        if (type == String.class)
            {
            return entry.getValueAsString();
            }
        else if (type == Integer.class || type == int.class)
            {
            return entry.getValueAsInteger();
            }
        else if (type == Boolean.class || type == boolean.class)
            {
            return entry.getValueAsBoolean();
            }
        else if (type == Long.class || type == long.class)
            {
            return entry.getValueAsLong();
            }
        else if (type == Double.class || type == double.class)
            {
            return entry.getValueAsDouble();
            }
        else if (type == byte[].class)
            {
            return entry.getValueAsBytes();
            }
        // Recursive mapping for nested objects
        else if (!type.isPrimitive() && cursor.isOnMap())
            {
            try
                {
                Object nestedPojo = type.getDeclaredConstructor().newInstance();
                if (cursor.enter())
                    {
                    mapCursorToPojo(cursor, nestedPojo, "set", "");
                    cursor.leave();
                    return nestedPojo;
                    }
                }
            catch (Exception e)
                {
                // Failed to instantiate or map nested object
                }
            }
        return null;
        }


    @Override
    public void saveTo(Configuration config, Path targetFile)
            throws IOException, ConfigException
        {
        saveTo(config, targetFile, EnumSet.allOf(ConfigScope.class));
        }

    @Override
    public void saveTo(Configuration config, Path targetFile, EnumSet<ConfigScope> scopes)
            throws IOException, ConfigException
        {
        if (config == null || targetFile == null)
            {
            throw new IllegalArgumentException("Configuration and target path must not be null");
            }

        // 1. Find the appropriate format based on file extension
        String fileName = targetFile.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1)
            {
            throw new ConfigException("Target file must have an extension to determine the format");
            }
        String extension = fileName.substring(lastDotIndex).toLowerCase(); // Include the dot

        ConfigFileFormatInterface format = findFormatForExtension(extension);
        if (format == null)
            {
            throw new ConfigException("No format handler found for extension: "+extension);
            }

        // Ensure format is somewhat initialized if it's not already
        // (ServiceLoader instances might not be initialized)
        ConfigFactorySettings settings = new ConfigFactorySettings();
        // try to preserve comments by default
        settings.put(ConfigFeature.COMMENTS_WRITING, true);
        settings.put(ConfigFeature.COMMENTS_READING, true);
        format.testComponent(settings, org.metabit.platform.support.config.impl.core.NullLogging.getSingletonInstance());

        // 2. Create the file and get a writeable layer for it
        ConfigLocation location = new ConfigLocationImpl(ConfigScope.SESSION, new org.metabit.platform.support.config.source.core.InMemoryLayerSource(), null, targetFile);
        ConfigLayerInterface targetLayer;
        try
            {
            // Ensure parent directories exist
            if (targetFile.getParent() != null)
                {
                Files.createDirectories(targetFile.getParent());
                }
            // If file exists, delete it to start fresh with createFile
            Files.deleteIfExists(targetFile);
            targetLayer = format.createFile(targetFile, location);
            }
        catch (IOException e)
            {
            throw new ConfigException("Failed to prepare target file "+targetFile+": "+e.getMessage());
            }

        if (targetLayer == null || !targetLayer.isWriteable())
            {
            throw new ConfigException("Could not create a writeable layer for "+targetFile);
            }

        // 3. Export data from configuration to the new layer
        try
            {
            ConfigCursor cursor = config.getConfigCursor();
            copyCursorToLayer(cursor, targetLayer);
            targetLayer.flush();
            }
        catch (ConfigCheckedException e)
            {
            throw new ConfigException("Failed to write configuration to "+targetFile+": "+e.getMessage());
            }
        }

    /* recursive implemementation */
    private void copyCursorToLayer(ConfigCursor cursor, ConfigLayerInterface layer)
            throws ConfigCheckedException
        {
        copyCursorToLayerRecursive(cursor, layer, "");
        }

    private void copyCursorToLayerRecursive(ConfigCursor cursor, ConfigLayerInterface layer, String pathPrefix)
            throws ConfigCheckedException
        {
        if (cursor.enter())
            {
            while (cursor.moveNext())
                {
                if (cursor.isOnLeaf())
                    {
                    ConfigEntry entry = cursor.getCurrentElement();
                    if (entry != null)
                        {
                        if (pathPrefix.isEmpty())
                            {
                            layer.writeEntry(entry);
                            }
                        else
                            {
                            // Wrap entry to provide full path if we are in a sub-node
                            String fullKey = pathPrefix+entry.getKey();
                            layer.writeEntry(new RelativeConfigEntryWrapper(entry, fullKey));
                            }
                        }
                    }
                else if (cursor.isOnMap())
                    {
                    String newPrefix = pathPrefix+cursor.getCurrentElement().getKey()+"/";
                    copyCursorToLayerRecursive(cursor, layer, newPrefix);
                    }
                }
            cursor.leave();
            }
        }

    /**
     * Internal wrapper to provide a different key for an existing ConfigEntry.
     */
    private static class RelativeConfigEntryWrapper implements ConfigEntry
    {
        private final ConfigEntry delegate;
        private final String      fullKey;

        RelativeConfigEntryWrapper(ConfigEntry delegate, String fullKey)
            {
            this.delegate = delegate;
            this.fullKey = fullKey;
            }

        @Override
        public String getValueAsString()
                throws ConfigCheckedException
            { return delegate.getValueAsString(); }

        @Override
        public Boolean getValueAsBoolean()
                throws ConfigCheckedException
            { return delegate.getValueAsBoolean(); }

        @Override
        public Integer getValueAsInteger()
                throws ConfigCheckedException
            { return delegate.getValueAsInteger(); }

        @Override
        public Long getValueAsLong()
                throws ConfigCheckedException
            { return delegate.getValueAsLong(); }

        @Override
        public Double getValueAsDouble()
                throws ConfigCheckedException
            { return delegate.getValueAsDouble(); }

        @Override
        public java.math.BigInteger getValueAsBigInteger()
                throws ConfigCheckedException
            { return delegate.getValueAsBigInteger(); }

        @Override
        public java.math.BigDecimal getValueAsBigDecimal()
                throws ConfigCheckedException
            { return delegate.getValueAsBigDecimal(); }

        @Override
        public byte[] getValueAsBytes()
                throws ConfigCheckedException
            { return delegate.getValueAsBytes(); }

        @Override
        public java.util.List<String> getValueAsStringList()
                throws ConfigCheckedException
            { return delegate.getValueAsStringList(); }

        @Override
        public String getKey() { return fullKey; }

        @Override
        public org.metabit.platform.support.config.ConfigScope getScope() { return delegate.getScope(); }

        @Override
        public org.metabit.platform.support.config.ConfigEntryType getType() { return delegate.getType(); }

        @Override
        public org.metabit.platform.support.config.ConfigLocation getLocation() { return delegate.getLocation(); }

        @Override
        public java.net.URI getURI() { return delegate.getURI(); }

        @Override
        public java.net.URI getValueAsURI()
                throws ConfigCheckedException
            { return delegate.getValueAsURI(); }

        @Override
        public java.nio.file.Path getValueAsPath()
                throws ConfigCheckedException
            { return delegate.getValueAsPath(); }

        @Override
        public java.nio.file.Path getValueAsPath(java.nio.file.FileSystem fs)
                throws ConfigCheckedException
            { return delegate.getValueAsPath(fs); }

        @Override
        public java.time.LocalDate getValueAsLocalDate()
                throws ConfigCheckedException
            { return delegate.getValueAsLocalDate(); }

        @Override
        public java.time.LocalTime getValueAsLocalTime()
                throws ConfigCheckedException
            { return delegate.getValueAsLocalTime(); }

        @Override
        public java.time.LocalDateTime getValueAsLocalDateTime()
                throws ConfigCheckedException
            { return delegate.getValueAsLocalDateTime(); }

        @Override
        public java.time.OffsetDateTime getValueAsOffsetDateTime()
                throws ConfigCheckedException
            { return delegate.getValueAsOffsetDateTime(); }

        @Override
        public java.time.ZonedDateTime getValueAsZonedDateTime()
                throws ConfigCheckedException
            { return delegate.getValueAsZonedDateTime(); }

        @Override
        public java.time.Duration getValueAsDuration()
                throws ConfigCheckedException
            { return delegate.getValueAsDuration(); }

        @Override
        public org.metabit.platform.support.config.interfaces.ConfigEntrySpecification getSpecification()
                throws ConfigCheckedException
            { return delegate.getSpecification(); }

        @Override
        public void putString(String value)
                throws ConfigCheckedException
            { delegate.putString(value); }

        @Override
        public void putValue(Object value, org.metabit.platform.support.config.ConfigEntryType valueType)
                throws ConfigCheckedException
            { delegate.putValue(value, valueType); }
    }

    private ConfigFileFormatInterface findFormatForExtension(String extension)
        {
        ServiceLoader<ConfigFormatInterface> loader = ServiceLoader.load(ConfigFormatInterface.class);
        for (ConfigFormatInterface format : loader)
            {
            if (format instanceof ConfigFileFormatInterface)
                {
                ConfigFileFormatInterface fileFormat = (ConfigFileFormatInterface) format;
                if (fileFormat.getFilenameExtensions().contains(extension))
                    {
                    return fileFormat;
                    }
                }
            }
        return null;
        }

}
