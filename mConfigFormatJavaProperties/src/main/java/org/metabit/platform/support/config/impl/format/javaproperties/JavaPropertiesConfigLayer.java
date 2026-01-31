package org.metabit.platform.support.config.impl.format.javaproperties;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/*
 possible improvements: for each entry, identify and keep the line it is in,
 so the location is precise for lookups.

 URL will contain the line as first element after '#' fragment marker, possible ',' for column(s)
    #15
    #15,20      line 15, starting column 20
    #15,20-33   line 15, columns 20-33
 */
public class JavaPropertiesConfigLayer implements ConfigLayerInterface
{
    private final FileJavaPropertiesFormat ourFormat;
    private final Properties               props;
    private final ConfigSource             source;
    private final boolean                  trimValueStringsFlag;
    private final boolean                  writeableFlag;
    private final boolean                  writeCacheFlag;
    private       int                      writeChanges;

    /**
     * @param settings settings to use
     * @param location the location this Java Properties ConfigLayer is found at
     * @param format   the format handler for Java Properties
     * @param path     path of the file. "null" when using stream inputs.
     */
    public JavaPropertiesConfigLayer(ConfigFactorySettings settings, ConfigLocation location, FileJavaPropertiesFormat format, Path path)
        {
        source = new ConfigLocationImpl(location, this, format, path);
        ourFormat = format;
        if (path != null)
            { writeableFlag = path.toFile().canWrite(); }
        else
            { writeableFlag = false; }
        trimValueStringsFlag = settings.getBoolean(ConfigFeature.TRIM_TEXTVALUE_SPACES);
        writeCacheFlag = !settings.getBoolean(ConfigFeature.WRITE_SYNC);
        props = new Properties();
        writeChanges = 0;
        }

    @Override
    public ConfigEntry getEntry(final String hierarchicalKey)
        {
        // @TODO split hierarchical key @DUMMY dummy-code
        String key = hierarchicalKey;

        String propsValue = props.getProperty(hierarchicalKey);
        if (propsValue == null)
            return null;
        if (trimValueStringsFlag)
            propsValue = propsValue.trim(); //.strip() would require JDK11

        ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
        StringConfigEntryLeaf ce = new StringConfigEntryLeaf(key, propsValue, meta);
        return ce;
        }


    @Override
    public boolean isEmpty()
        {
        return props.isEmpty();
        }

    @Override
    public ConfigScope getScope()
        { return source.getScope(); }

    @Override
    public int compareTo(ConfigLayerInterface o)
        { return this.getScope().ordinal()-o.getScope().ordinal(); }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        // the keys of java.util.Properties are Object, not String type.
        Iterator<Object> iit = props.keys().asIterator();
        Set<String> keys = new HashSet<>();
        while (iit.hasNext())
            {
            Object objKey = iit.next();
            if (objKey instanceof String)
                { keys.add((String) objKey); }
//            else { logger.debug message about omitting an properties entry whose key is not a string. }
            }
        return keys.iterator();
        }

    @Override
    public boolean isWriteable()
        {
        return writeableFlag;
        }

    public Properties internalGetProperties()
        {
        return props;
        }


    public ConfigSource getSource()
        {
        return source;
        }

    @Override
    public void writeEntry(ConfigEntry entryToWrite)
            throws ConfigCheckedException
        {
        if (!writeableFlag)
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
        props.put(entryToWrite.getKey(), entryToWrite.getValueAsString());
        //@TODO cacheing to be performed here - by the layer itself
        if (writeCacheFlag)
            {
            // we just return at this point
            writeChanges++;  // the interpretation of this may lead to misinterpretation if the same entry was changed multiple times.
            return;
            }
        // else sync write / direct write
        ourFormat.writePropertiesToFile(source, props);
        return;
        }

    /**
     * flush write-cache, if applicable.
     *
     * @return 0 for none. >0 number of entries flushed; <0 for status codes.
     */
    @Override
    public int flush()
            throws ConfigCheckedException
        {
        if (writeChanges > 0)
            ourFormat.writePropertiesToFile(source, props);
        int tmp = writeChanges;
        writeChanges = 0;
        return tmp;
        }


}
