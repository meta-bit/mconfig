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
    private final Map<String, List<String>>       sectionLeadingComments;
    private final List<String>                    globalHeaderComments;
    private final Map<String, String>             sectionInlineComments;
    private final Map<String, Map<String, List<String>>> keyLeadingComments;
    private final Map<String, Map<String, String>> keyInlineComments;
    private final ConfigSource                   source;
    private final boolean                        trimValueStringsFlag;
    private final boolean                        writeableFlag;
    private final boolean                        writeCacheFlag;
    private final boolean                        readCommentsFlag;
    private final boolean                        writeCommentsFlag;
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
        this.readCommentsFlag = settings.getBoolean(ConfigFeature.COMMENTS_READING);
        this.writeCommentsFlag = settings.getBoolean(ConfigFeature.COMMENTS_WRITING);
        this.data = new LinkedHashMap<>();
        this.sectionLeadingComments = new LinkedHashMap<>();
        this.globalHeaderComments = new ArrayList<>();
        this.sectionInlineComments = new LinkedHashMap<>();
        this.keyLeadingComments = new LinkedHashMap<>();
        this.keyInlineComments = new LinkedHashMap<>();
        this.writeChanges = 0;
        }

    public void load(BufferedReader reader) throws IOException
        {
        String line;
        String currentSection = "";
        List<String> pendingComments = new ArrayList<>();
        while ((line = reader.readLine()) != null)
            {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                {
                if (!pendingComments.isEmpty())
                    {
                    globalHeaderComments.addAll(pendingComments);
                    pendingComments.clear();
                    }
                continue;
                }
            if (trimmed.startsWith(";") || trimmed.startsWith("#"))
                {
                pendingComments.add(trimmed);
                continue;
                }
            if (trimmed.startsWith("["))
                {
                int close = trimmed.indexOf(']');
                if (close < 0)
                    {
                    continue;
                    }
                currentSection = trimmed.substring(1, close).trim();
                if (!pendingComments.isEmpty())
                    {
                    sectionLeadingComments.put(currentSection, new ArrayList<>(pendingComments));
                    pendingComments.clear();
                    }
                String remainder = trimmed.substring(close + 1).trim();
                String inlineComment = parseInlineComment(remainder);
                if (inlineComment != null)
                    {
                    sectionInlineComments.put(currentSection, inlineComment);
                    }
                }
            else
                {
            int index = line.indexOf('=');
            if (index > 0)
                {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1);
                InlineCommentSplit split = splitInlineComment(value);
                value = split.value;
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
                if (!pendingComments.isEmpty())
                    {
                    keyLeadingComments
                            .computeIfAbsent(normalizedSection, k -> new LinkedHashMap<>())
                            .put(normalizedKey, new ArrayList<>(pendingComments));
                    pendingComments.clear();
                    }
                if (split.inlineComment != null)
                    {
                    keyInlineComments
                            .computeIfAbsent(normalizedSection, k -> new LinkedHashMap<>())
                            .put(normalizedKey, split.inlineComment);
                    }
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
                if (readCommentsFlag)
                    {
                    List<String> leading = getKeyLeadingComments(section, key);
                    String inline = getKeyInlineComment(section, key);
                    if (leading != null && !leading.isEmpty())
                        {
                        meta.setComment(String.join("\n", leading));
                        }
                    if (inline != null)
                        {
                        String current = meta.getComment();
                        meta.setComment(current == null ? inline : current + " " + inline);
                        }
                    }
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
        data.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, entryToWrite.getValueAsString());

        if (writeCommentsFlag && entryToWrite.getComment() != null)
            {
            String comment = entryToWrite.getComment();
            List<String> commentLines = new ArrayList<>();
            // Merge logic: Existing comments first, then programmatic
            List<String> existing = getKeyLeadingComments(section, key);
            if (existing != null && !readCommentsFlag)
                {
                commentLines.addAll(existing);
                }

            for (String line : comment.split("\n"))
                {
                String formattedLine;
                if (!line.startsWith(";") && !line.startsWith("#"))
                    {
                    formattedLine = "; " + line;
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
            keyLeadingComments.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, commentLines);
            }

        writeChanges++;
        if (!writeCacheFlag)
            {
            ourFormat.writeINI(this);
            writeChanges = 0;
            }
        }

    @Override
    public int flush() throws ConfigCheckedException
        {
        if (writeChanges > 0)
            {
            ourFormat.writeINI(this);
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

    List<String> getSectionLeadingComments(String section)
        {
        return sectionLeadingComments.get(section);
        }

    String getSectionInlineComment(String section)
        {
        return sectionInlineComments.get(section);
        }

    public List<String> getGlobalHeaderComments()
        {
        return globalHeaderComments;
        }

    List<String> getKeyLeadingComments(String section, String key)
        {
        Map<String, List<String>> sectionMap = keyLeadingComments.get(section);
        if (sectionMap == null)
            {
            return null;
            }
        return sectionMap.get(key);
        }

    String getKeyInlineComment(String section, String key)
        {
        Map<String, String> sectionMap = keyInlineComments.get(section);
        if (sectionMap == null)
            {
            return null;
            }
        return sectionMap.get(key);
        }

    private String parseInlineComment(String value)
        {
        if (value == null || value.isEmpty())
            {
            return null;
            }
        InlineCommentSplit split = splitInlineComment(value);
        return split.inlineComment;
        }

    private InlineCommentSplit splitInlineComment(String valuePart)
        {
        if (valuePart == null)
            {
            return new InlineCommentSplit("", null);
            }
        int idx = -1;
        char delimiter = 0;
        boolean seenNonSpace = false;
        for (int i = 0; i < valuePart.length(); i++)
            {
            char ch = valuePart.charAt(i);
            if (ch == ';' || ch == '#')
                {
                if (!seenNonSpace)
                    {
                    idx = i;
                    delimiter = ch;
                    break;
                    }
                if (i > 0 && Character.isWhitespace(valuePart.charAt(i - 1)))
                    {
                    idx = i;
                    delimiter = ch;
                    break;
                    }
                }
            if (!Character.isWhitespace(ch))
                {
                seenNonSpace = true;
                }
            }
        if (idx < 0)
            {
            return new InlineCommentSplit(valuePart, null);
            }
        String value = valuePart.substring(0, idx);
        String commentBody = valuePart.substring(idx + 1).trim();
        String inline = delimiter + (commentBody.isEmpty() ? "" : " " + commentBody);
        return new InlineCommentSplit(value, inline);
        }

    private static final class InlineCommentSplit
    {
        private final String value;
        private final String inlineComment;

        InlineCommentSplit(String value, String inlineComment)
            {
            this.value = value;
            this.inlineComment = inlineComment;
            }
    }
}
