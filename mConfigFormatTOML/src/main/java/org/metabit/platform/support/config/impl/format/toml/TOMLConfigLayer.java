package org.metabit.platform.support.config.impl.format.toml;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArray;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArrayTable;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlArrayItem;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlScalar;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlTable;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlType;
import org.metabit.platform.support.config.impl.format.toml.TomlModel.TomlValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TOMLConfigLayer implements ConfigLayerInterface
{
    private final TOMLFileFormat format;
    private final TomlTable      root;
    private final ConfigSource   source;
    private final List<String>   globalHeaderComments;
    private final boolean        writeable;
    private final boolean        writeCache;
    private final boolean        readCommentsFlag;
    private final boolean        writeCommentsFlag;
    private       int            writeChanges;

    TOMLConfigLayer(ConfigFactorySettings settings, ConfigLocation location, TOMLFileFormat format, TomlTable root, Path path)
        {
        this.format = format;
        this.root = root;
        this.source = new ConfigLocationImpl(location, this, format, path);
        this.globalHeaderComments = new ArrayList<>();
        this.writeable = path != null && path.toFile().canWrite();
        this.writeCache = !settings.getBoolean(ConfigFeature.WRITE_SYNC);
        this.readCommentsFlag = settings.getBoolean(ConfigFeature.COMMENTS_READING);
        this.writeCommentsFlag = settings.getBoolean(ConfigFeature.COMMENTS_WRITING);
        this.writeChanges = 0;
        }

    public List<String> getGlobalHeaderComments()
        {
        return globalHeaderComments;
        }

    @Override
    public ConfigEntry getEntry(String hierarchicalKey)
        {
        TomlValue value = resolveValue(hierarchicalKey);
        if (value == null)
            {
            return null;
            }

        ConfigEntryMetadata meta = new ConfigEntryMetadata(source);
        if (readCommentsFlag)
            {
            attachComments(hierarchicalKey, value, meta);
            }

        if (value instanceof TomlArray)
            {
            List<String> list = arrayToStringList((TomlArray) value);
            if (list == null)
                {
                return null;
                }
            return new TypedConfigEntryLeaf(hierarchicalKey, list, ConfigEntryType.MULTIPLE_STRINGS, meta);
            }
        if (value instanceof TomlScalar)
            {
            return scalarToEntry(hierarchicalKey, (TomlScalar) value, meta);
            }
        return null;
        }

    private void attachComments(String hierarchicalKey, TomlValue tomlValue, ConfigEntryMetadata meta)
        {
        String lastSegment;
        TomlTable table;
        if (hierarchicalKey.isEmpty())
            {
            // Root blob? Doesn't really apply to TOML structured keys, 
            // but just in case we are looking for root comments.
            return;
            }
        
        int lastSlash = hierarchicalKey.lastIndexOf('/');
        if (lastSlash < 0)
            {
            lastSegment = hierarchicalKey;
            table = root;
            }
        else
            {
            String path = hierarchicalKey.substring(0, lastSlash);
            lastSegment = hierarchicalKey.substring(lastSlash + 1);
            TomlValue resolvedPath = resolveValue(path);
            if (resolvedPath instanceof TomlTable)
                {
                table = (TomlTable) resolvedPath;
                }
            else
                {
                return;
                }
            }

        List<String> leading = table.getLeadingComments(lastSegment);
        String inline = table.getInlineComment(lastSegment);

        StringBuilder sb = new StringBuilder();
        if (leading != null && !leading.isEmpty())
            {
            sb.append(String.join("\n", leading));
            }
        if (inline != null)
            {
            if (sb.length() > 0)
                {
                sb.append(" ");
                }
            sb.append(inline);
            }
        if (sb.length() > 0)
            {
            meta.setComment(sb.toString());
            }
        }

    @Override
    public boolean isWriteable()
        {
        return writeable;
        }

    @Override
    public boolean isEmpty()
        {
        return root.getValues().isEmpty();
        }

    @Override
    public ConfigScope getScope()
        {
        return source.getScope();
        }

    @Override
    public void writeEntry(ConfigEntry entryToWrite) throws ConfigCheckedException
        {
        if (!writeable)
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.NOT_WRITEABLE);
            }
        String[] segments = entryToWrite.getKey().split("/");
        TomlTable table = root;
        for (int i = 0; i < segments.length - 1; i++)
            {
            String segment = segments[i];
            TomlValue existing = table.getValues().get(segment);
            if (existing == null)
                {
                TomlTable next = new TomlTable(false);
                table.getValues().put(segment, next);
                table = next;
                continue;
                }
            if (existing instanceof TomlTable && !((TomlTable) existing).isInline())
                {
                table = (TomlTable) existing;
                continue;
                }
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        String key = segments[segments.length - 1];
        table.getValues().put(key, configEntryToTomlValue(entryToWrite));

        if (writeCommentsFlag && entryToWrite.getComment() != null)
            {
            String comment = entryToWrite.getComment();
            List<String> commentLines = new ArrayList<>();
            // Merge logic: Existing comments first, then programmatic
            List<String> existing = table.getLeadingComments(key);
            if (existing != null && !readCommentsFlag)
                {
                commentLines.addAll(existing);
                }

            for (String line : comment.split("\n"))
                {
                String formattedLine;
                if (!line.startsWith("#"))
                    {
                    formattedLine = "# " + line;
                    }
                else
                    {
                    formattedLine = line;
                    }

                if (!commentLines.contains(formattedLine))
                    {
                    commentLines.add(formattedLine);
                    }
                }
            table.setLeadingComments(key, commentLines);
            }

        writeChanges++;
        if (!writeCache)
            {
            format.writeFile(this);
            writeChanges = 0;
            }
        }

    @Override
    public int flush() throws ConfigCheckedException
        {
        if (writeChanges > 0)
            {
            format.writeFile(this);
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

    @Override
    public int compareTo(ConfigLayerInterface o)
        {
        return this.getScope().ordinal() - o.getScope().ordinal();
        }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        List<String> keys = new ArrayList<>();
        collectKeys(root, "", keys);
        return keys.iterator();
        }

    TomlTable getRoot()
        {
        return root;
        }

    private TomlValue resolveValue(String keyPath)
        {
        String[] segments = keyPath.split("/");
        Object current = root;
        for (String segment : segments)
            {
            if (current instanceof TomlTable)
                {
                TomlTable table = (TomlTable) current;
                TomlValue value = table.getValues().get(segment);
                if (value == null)
                    {
                    return null;
                    }
                current = value;
                continue;
                }
            if (current instanceof TomlArrayTable)
                {
                int index = parseIndex(segment);
                if (index < 0)
                    {
                    return null;
                    }
                TomlArrayTable arrayTable = (TomlArrayTable) current;
                if (index >= arrayTable.getTables().size())
                    {
                    return null;
                    }
                current = arrayTable.getTables().get(index);
                continue;
                }
            if (current instanceof TomlArray)
                {
                int index = parseIndex(segment);
                if (index < 0)
                    {
                    return null;
                    }
                TomlArray array = (TomlArray) current;
                if (index >= array.getValues().size())
                    {
                    return null;
                    }
                current = array.getValues().get(index);
                continue;
                }
            return null;
            }
        if (current instanceof TomlValue)
            {
            return (TomlValue) current;
            }
        return null;
        }

    private int parseIndex(String segment)
        {
        try
            {
            return Integer.parseInt(segment);
            }
        catch (NumberFormatException e)
            {
            return -1;
            }
        }

    private void collectKeys(TomlTable table, String prefix, List<String> keys)
        {
        for (Map.Entry<String, TomlValue> entry : table.getValues().entrySet())
            {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "/" + entry.getKey();
            TomlValue value = entry.getValue();
            if (value instanceof TomlTable)
                {
                collectKeys((TomlTable) value, key, keys);
                }
            else if (value instanceof TomlArrayTable)
                {
                TomlArrayTable arrayTable = (TomlArrayTable) value;
                for (int i = 0; i < arrayTable.getTables().size(); i++)
                    {
                    collectKeys(arrayTable.getTables().get(i), key + "/" + i, keys);
                    }
                }
            else if (value instanceof TomlArray)
                {
                TomlArray array = (TomlArray) value;
                if (isScalarArray(array))
                    {
                    keys.add(key);
                    }
                else
                    {
                    collectArrayKeys(array, key, keys);
                    }
                }
            else
                {
                keys.add(key);
                }
            }
        }

    private boolean isScalarArray(TomlArray array)
        {
        for (TomlValue value : array.getValues())
            {
            if (!(value instanceof TomlScalar))
                {
                return false;
                }
            }
        return true;
        }

    private void collectArrayKeys(TomlArray array, String prefix, List<String> keys)
        {
        if (isScalarArray(array))
            {
            keys.add(prefix);
            return;
            }
        for (int i = 0; i < array.getValues().size(); i++)
            {
            TomlValue element = array.getValues().get(i);
            String key = prefix + "/" + i;
            if (element instanceof TomlTable)
                {
                collectKeys((TomlTable) element, key, keys);
                }
            else if (element instanceof TomlArray)
                {
                collectArrayKeys((TomlArray) element, key, keys);
                }
            else if (element instanceof TomlScalar)
                {
                keys.add(key);
                }
            }
        }

    private List<String> arrayToStringList(TomlArray array)
        {
        List<String> list = new ArrayList<>();
        for (TomlValue value : array.getValues())
            {
            if (value instanceof TomlScalar)
                {
                list.add(scalarToString((TomlScalar) value));
                }
            else
                {
                return null;
                }
            }
        return list;
        }

    private ConfigEntry scalarToEntry(String key, TomlScalar scalar, ConfigEntryMetadata meta)
        {
        switch (scalar.getType())
            {
            case STRING:
                return new TypedConfigEntryLeaf(key, scalar.getValue(), ConfigEntryType.STRING, meta);
            case BOOLEAN:
                return new TypedConfigEntryLeaf(key, scalar.getValue(), ConfigEntryType.BOOLEAN, meta);
            case INTEGER:
            case FLOAT:
                return new TypedConfigEntryLeaf(key, scalar.getValue(), ConfigEntryType.NUMBER, meta);
            case DATE:
                return new TypedConfigEntryLeaf(key, scalar.getValue(), ConfigEntryType.DATE, meta);
            case TIME:
                return new TypedConfigEntryLeaf(key, scalar.getValue(), ConfigEntryType.TIME, meta);
            case DATETIME:
                return new TypedConfigEntryLeaf(key, scalar.getValue(), ConfigEntryType.DATETIME, meta);
            default:
                return null;
            }
        }

    private String scalarToString(TomlScalar scalar)
        {
        Object value = scalar.getValue();
        if (value == null)
            {
            return "";
            }
        if (value instanceof BigDecimal)
            {
            return ((BigDecimal) value).toPlainString();
            }
        return String.valueOf(value);
        }

    private TomlValue configEntryToTomlValue(ConfigEntry entry) throws ConfigCheckedException
        {
        ConfigEntryType type = entry.getType();
        switch (type)
            {
            case BOOLEAN:
                return new TomlScalar(TomlType.BOOLEAN, entry.getValueAsBoolean());
            case NUMBER:
                return numberToToml(entry);
            case MULTIPLE_STRINGS:
                List<TomlArrayItem> values = new ArrayList<>();
                for (String value : entry.getValueAsStringList())
                    {
                    values.add(new TomlArrayItem(new TomlScalar(TomlType.STRING, value), null, null));
                    }
                return new TomlArray(values, null);
            case DATE:
                LocalDate date = entry.getValueAsLocalDate();
                return new TomlScalar(TomlType.DATE, date);
            case TIME:
                LocalTime time = entry.getValueAsLocalTime();
                return new TomlScalar(TomlType.TIME, time);
            case DATETIME:
                LocalDateTime dateTime = entry.getValueAsLocalDateTime();
                if (dateTime != null)
                    {
                    return new TomlScalar(TomlType.DATETIME, dateTime);
                    }
                OffsetDateTime offsetDateTime = entry.getValueAsOffsetDateTime();
                if (offsetDateTime != null)
                    {
                    return new TomlScalar(TomlType.DATETIME, offsetDateTime);
                    }
                return new TomlScalar(TomlType.DATETIME, entry.getValueAsString());
            case BYTES:
                return new TomlScalar(TomlType.STRING, Base64.getEncoder().encodeToString(entry.getValueAsBytes()));
            case STRING:
            default:
                return new TomlScalar(TomlType.STRING, entry.getValueAsString());
            }
        }

    private TomlValue numberToToml(ConfigEntry entry) throws ConfigCheckedException
        {
        try
            {
            BigDecimal decimal = entry.getValueAsBigDecimal();
            if (decimal != null)
                {
                BigDecimal stripped = decimal.stripTrailingZeros();
                if (stripped.scale() <= 0)
                    {
                    return new TomlScalar(TomlType.INTEGER, stripped.toBigInteger());
                    }
                return new TomlScalar(TomlType.FLOAT, decimal);
                }
            }
        catch (Exception ignored)
            {
            }
        try
            {
            BigInteger integer = entry.getValueAsBigInteger();
            if (integer != null)
                {
                return new TomlScalar(TomlType.INTEGER, integer);
                }
            }
        catch (Exception ignored)
            {
            }
        try
            {
            return new TomlScalar(TomlType.INTEGER, entry.getValueAsLong());
            }
        catch (Exception ignored)
            {
            }
        try
            {
            return new TomlScalar(TomlType.FLOAT, entry.getValueAsDouble());
            }
        catch (Exception ignored)
            {
            }
        return new TomlScalar(TomlType.STRING, entry.getValueAsString());
        }
}
