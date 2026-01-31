package org.metabit.platform.support.config.impl.format.raw;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.impl.entry.BlobConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class FileRawConfigLayer implements ConfigLayerInterface
{
    private       String                    contents;
    private       byte[]                    binaryContents;
    private final ConfigSource              source;
    private final ConfigFileFormatInterface format;

    public FileRawConfigLayer(ConfigLocation location, ConfigFileFormatInterface format, Path path)
        {
        // location, layer interface, format interface, storage object reference
        this.source = new ConfigLocationImpl(location, this, format, path);
        this.format = format;
        }

    @Override
    public ConfigEntry getEntry(final String hierarchicalKey)
        {
        if (!hierarchicalKey.isEmpty()) // hierarchicalKey.equals("")
            return null;
        ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
        if (binaryContents != null || (format != null && format.getFormatID().equals("binary_file")))
            {
            return new BlobConfigEntryLeaf("", binaryContents, meta);
            }
        return new StringConfigEntryLeaf("", contents, meta);
        }

    @Override
    public boolean isEmpty()
        {
        if (binaryContents != null)
            {
            return binaryContents.length == 0;
            }
        if (contents == null)
            return true;
        return contents.isEmpty();
        }

    /**
     * @return the scope of this ConfigLayer
     */
    @Override
    public ConfigScope getScope() { return source.getScope(); }

    /**
     * @param entryToWrite the entry to write
     * @throws ConfigCheckedException if writing failed
     */
    @Override
    public void writeEntry(ConfigEntry entryToWrite)
            throws ConfigCheckedException
        {
        if (!entryToWrite.getKey().isEmpty())
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);

        if (entryToWrite.getType() == ConfigEntryType.BYTES)
            {
            this.binaryContents = entryToWrite.getValueAsBytes();
            this.contents = null;
            }
        else
            {
            this.contents = entryToWrite.getValueAsString();
            this.binaryContents = null;
            }

        if (format != null)
            {
            format.writeFile(this);
            }
        // notify storage of change
        source.getStorage().triggerChangeCheck(source.getStorageInstanceHandle());
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
        return 0;
        }

    /**
     * @return get source of this ConfigLayer
     */
    @Override
    public ConfigSource getSource()
        {
        return source;
        }

    @Override
    public int compareTo(ConfigLayerInterface o)
        { return this.getScope().ordinal() - o.getScope().ordinal(); }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        return java.util.Collections.singleton("").iterator();
        }

    @Override
    public boolean isWriteable()
        {
        Path path = (Path) source.getStorageInstanceHandle();
        if (path == null) return false;
        if (Files.exists(path))
            return Files.isWritable(path);
        else
            return Files.isWritable(path.getParent());
        }

    public String getContents()
        {
        return contents;
        }

    public void setContents(final String contents)
        {
        this.contents = contents;
        }

    public byte[] getBinaryContents()
        {
        return binaryContents;
        }

    public void setBinaryContents(final byte[] binaryContents)
        {
        this.binaryContents = binaryContents;
        }

}
