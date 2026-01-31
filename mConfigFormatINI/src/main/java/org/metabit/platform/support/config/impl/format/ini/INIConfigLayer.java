package org.metabit.platform.support.config.impl.format.ini;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class INIConfigLayer implements ConfigLayerInterface
{
    private final INIFileFormat                  ourFormat;
    private final Map<String, Map<String, String>> data;
    private final ConfigSource                   source;
    private final boolean                        trimValueStringsFlag;
    private final boolean                        writeableFlag;
    private final boolean                        writeCacheFlag;
    private       int                            writeChanges;

    public INIConfigLayer(ConfigFactorySettings settings, ConfigLocation location, INIFileFormat format, Path path)
        {
        this.source = new ConfigLocationImpl(location, this, format, path);
        this.ourFormat = format;
        if (path != null)
            {
            this.writeableFlag = path.toFile().canWrite();
            }
        else
            {
            this.writeableFlag = false;
            }
        this.trimValueStringsFlag = settings.getBoolean(ConfigFeature.TRIM_TEXTVALUE_SPACES);
        this.writeCacheFlag = !settings.getBoolean(ConfigFeature.WRITE_SYNC);
        this.data = new LinkedHashMap<>();
        this.writeChanges = 0;
        }

    public void load(BufferedReader reader) throws IOException
        {
        String line;
        String currentSection = "";
        while ((line = reader.readLine()) != null)
            {
            line = line.trim();
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#"))
                {
                continue;
                }
            if (line.startsWith("[") && line.endsWith("]"))
                {
                currentSection = line.substring(1, line.length() - 1).trim();
                }
            else
                {
            int index = line.indexOf('=');
            if (index > 0)
                {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1);
                if (trimValueStringsFlag)
                    {
                    value = value.trim();
                    }
                
                // Hierarchical normalization: combine section and key, then re-split by last slash
                String fullKey = currentSection.isEmpty() ? key : currentSection + "/" + key;
                int lastSlash = fullKey.lastIndexOf('/');
                String normalizedSection = lastSlash >= 0 ? fullKey.substring(0, lastSlash) : "";
                String normalizedKey = lastSlash >= 0 ? fullKey.substring(lastSlash + 1) : fullKey;

                data.computeIfAbsent(normalizedSection, k -> new LinkedHashMap<>()).put(normalizedKey, value);
                }
                }
            }
        }

    @Override
    public ConfigEntry getEntry(String hierarchicalKey)
        {
        String section;
        String key;
        int index = hierarchicalKey.lastIndexOf('/');
        if (index >= 0)
            {
            section = hierarchicalKey.substring(0, index);
            key = hierarchicalKey.substring(index + 1);
            }
        else
            {
            section = "";
            key = hierarchicalKey;
            }

        Map<String, String> sectionData = data.get(section);
        if (sectionData != null)
            {
            String value = sectionData.get(key);
            if (value != null)
                {
                ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
                return new StringConfigEntryLeaf(hierarchicalKey, value, meta);
                }
            }
        return null;
        }

    @Override
    public boolean isEmpty()
        {
        return data.isEmpty();
        }

    @Override
    public ConfigScope getScope()
        {
        System.out.println("[DEBUG_LOG] INI getScope: " + source.getScope());
        return source.getScope();
        }

    @Override
    public int compareTo(ConfigLayerInterface o)
        {
        return this.getScope().ordinal() - o.getScope().ordinal();
        }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> sectionEntry : data.entrySet())
            {
            String sectionPrefix = sectionEntry.getKey().isEmpty() ? "" : sectionEntry.getKey() + "/";
            for (String key : sectionEntry.getValue().keySet())
                {
                keys.add(sectionPrefix + key);
                }
            }
        return keys.iterator();
        }

    @Override
    public boolean isWriteable()
        {
        return writeableFlag;
        }

    @Override
    public void writeEntry(ConfigEntry entryToWrite) throws ConfigCheckedException
        {
        if (!writeableFlag)
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        String hierarchicalKey = entryToWrite.getKey();
        String section;
        String key;
        int index = hierarchicalKey.lastIndexOf('/');
        if (index >= 0)
            {
            section = hierarchicalKey.substring(0, index);
            key = hierarchicalKey.substring(index + 1);
            }
        else
            {
            section = "";
            key = hierarchicalKey;
            }
        System.out.println("[DEBUG_LOG] INIConfigLayer.writeEntry: " + hierarchicalKey + "=" + entryToWrite.getValueAsString());
        data.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, entryToWrite.getValueAsString());
        writeChanges++;
        if (!writeCacheFlag)
            {
            ourFormat.writeINI(source, data);
            writeChanges = 0;
            }
        }

    @Override
    public int flush() throws ConfigCheckedException
        {
        if (writeChanges > 0)
            {
            ourFormat.writeINI(source, data);
            int tmp = writeChanges;
            writeChanges = 0;
            return tmp;
            }
        return 0;
        }

    @Override
    public ConfigSource getSource()
        {
        return source;
        }

    public Map<String, Map<String, String>> getData()
        {
        return data;
        }
}
