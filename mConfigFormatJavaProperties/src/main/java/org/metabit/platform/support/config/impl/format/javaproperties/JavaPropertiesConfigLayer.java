package org.metabit.platform.support.config.impl.format.javaproperties;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

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
    private final LinkedHashMap<String, String> ordered;
    private final LinkedHashMap<String, List<String>> leadingComments;
    private final List<String> globalHeaderComments;
    private final List<String> trailingComments;
    private final ConfigSource             source;
    private final boolean                  trimValueStringsFlag;
    private final boolean                  writeableFlag;
    private final boolean                  writeCacheFlag;
    private final boolean                  readCommentsFlag;
    private final boolean                  writeCommentsFlag;
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
        readCommentsFlag = settings.getBoolean(ConfigFeature.COMMENTS_READING);
        writeCommentsFlag = settings.getBoolean(ConfigFeature.COMMENTS_WRITING);
        props = new Properties();
        ordered = new LinkedHashMap<>();
        leadingComments = new LinkedHashMap<>();
        globalHeaderComments = new ArrayList<>();
        trailingComments = new ArrayList<>();
        writeChanges = 0;
        }

    public void load(BufferedReader reader) throws IOException
        {
        String line;
        List<String> pendingComments = new ArrayList<>();
        boolean firstKeyFound = false;
        while ((line = reader.readLine()) != null)
            {
            String logical = readLogicalLine(line, reader);
            if (logical == null)
                {
                break;
                }
            String trimmed = trimLeading(logical);
            if (trimmed.isEmpty())
                {
                if (!pendingComments.isEmpty())
                    {
                    if (!firstKeyFound)
                        {
                        globalHeaderComments.addAll(pendingComments);
                        }
                    else
                        {
                        // Orphaned comments between keys - keep them as trailing for the previous key or general?
                        // For simplicity, add to trailing of previous key if we can, or just keep as global.
                        // Guidelines say "file comment headers will be treated as orphaned/dangling".
                        globalHeaderComments.addAll(pendingComments);
                        }
                    pendingComments.clear();
                    }
                continue;
                }
            if (trimmed.startsWith("#") || trimmed.startsWith("!"))
                {
                pendingComments.add(trimmed);
                continue;
                }
            firstKeyFound = true;
            KeyValue kv = parseKeyValue(trimmed);
            String key = unescape(kv.key);
            String value = unescape(kv.value);
            if (trimValueStringsFlag)
                {
                value = value.trim();
                }
            props.put(key, value);
            ordered.put(key, value);
            if (!pendingComments.isEmpty())
                {
                leadingComments.put(key, new ArrayList<>(pendingComments));
                pendingComments.clear();
                }
            }
        if (!pendingComments.isEmpty())
            {
            trailingComments.addAll(pendingComments);
            }
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
        if (readCommentsFlag)
            {
            List<String> comments = leadingComments.get(key);
            if (comments != null && !comments.isEmpty())
                {
                meta.setComment(String.join("\n", comments));
                }
            }
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
        return ordered.keySet().iterator();
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

    public LinkedHashMap<String, String> internalGetOrdered()
        {
        return ordered;
        }

    public LinkedHashMap<String, List<String>> internalGetLeadingComments()
        {
        return leadingComments;
        }

    public List<String> internalGetGlobalHeaderComments()
        {
        return globalHeaderComments;
        }

    public List<String> internalGetTrailingComments()
        {
        return trailingComments;
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
        String key = entryToWrite.getKey();
        String value = entryToWrite.getValueAsString();
        props.put(key, value);
        ordered.put(key, value);
        if (writeCommentsFlag && entryToWrite.getComment() != null)
            {
            String comment = entryToWrite.getComment();
            List<String> commentLines = new ArrayList<>();
            // Merge logic: Existing comments first, then programmatic
            List<String> existing = leadingComments.get(key);
            if (existing != null && !readCommentsFlag)
                {
                commentLines.addAll(existing);
                }

            for (String line : comment.split("\n"))
                {
                String formattedLine;
                if (!line.startsWith("#") && !line.startsWith("!"))
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
            leadingComments.put(key, commentLines);
            }
        //@TODO cacheing to be performed here - by the layer itself
        if (writeCacheFlag)
            {
            // we just return at this point @TODO check
            writeChanges++;
            return;
            }
        // else sync write / direct write
        ourFormat.writeProperties(this);
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
            ourFormat.writeProperties(this);
        int tmp = writeChanges;
        writeChanges = 0;
        return tmp;
        }

    private String readLogicalLine(String first, BufferedReader reader) throws IOException
        {
        String line = first;
        while (line != null && isContinuation(line))
            {
            String next = reader.readLine();
            if (next == null)
                {
                line = line.substring(0, line.length() - 1);
                break;
                }
            line = line.substring(0, line.length() - 1) + trimLeading(next);
            }
        return line;
        }

    private boolean isContinuation(String line)
        {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--)
            {
            backslashes++;
            }
        return (backslashes % 2) == 1;
        }

    private KeyValue parseKeyValue(String line)
        {
        int len = line.length();
        int keyEnd = 0;
        boolean escaped = false;
        while (keyEnd < len)
            {
            char ch = line.charAt(keyEnd);
            if (!escaped)
                {
                boolean separator = false;
                switch (ch)
                    {
                    case '=':
                    case ':':
                    case ' ':
                    case '\t':
                    case '\f':
                        separator = true;
                        break;
                    case '\\':
                        escaped = true;
                        break;
                    default:
                        escaped = false;
                        break;
                    }
                if (separator)
                    {
                    break;
                    }
                }
            else
                {
                escaped = false;
                }
            keyEnd++;
            }
        int valueStart = keyEnd;
        while (valueStart < len && isWhitespace(line.charAt(valueStart)))
            {
            valueStart++;
            }
        if (valueStart < len && (line.charAt(valueStart) == '=' || line.charAt(valueStart) == ':'))
            {
            valueStart++;
            }
        while (valueStart < len && isWhitespace(line.charAt(valueStart)))
            {
            valueStart++;
            }
        String key = line.substring(0, keyEnd);
        String value = valueStart < len ? line.substring(valueStart) : "";
        return new KeyValue(key, value);
        }

    private boolean isWhitespace(char ch)
        {
        return ch == ' ' || ch == '\t' || ch == '\f';
        }

    private String trimLeading(String value)
        {
        int i = 0;
        while (i < value.length() && isWhitespace(value.charAt(i)))
            {
            i++;
            }
        return value.substring(i);
        }

    private String unescape(String value)
        {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++)
            {
            char ch = value.charAt(i);
            if (ch != '\\')
                {
                sb.append(ch);
                continue;
                }
            if (i == value.length() - 1)
                {
                sb.append('\\');
                break;
                }
            char next = value.charAt(++i);
            switch (next)
                {
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'u':
                    if (i + 4 <= value.length() - 1)
                        {
                        String hex = value.substring(i + 1, i + 5);
                        try
                            {
                            int code = Integer.parseInt(hex, 16);
                            sb.append((char) code);
                            i += 4;
                            break;
                            }
                        catch (NumberFormatException ignored)
                            {
                            }
                        }
                    sb.append('u');
                    break;
                default:
                    sb.append(next);
                    break;
                }
            }
        return sb.toString();
        }

    private static final class KeyValue
    {
        private final String key;
        private final String value;

        private KeyValue(String key, String value)
            {
            this.key = key;
            this.value = value;
            }
    }


}
