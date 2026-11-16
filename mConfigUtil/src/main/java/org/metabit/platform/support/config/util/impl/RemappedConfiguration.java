package org.metabit.platform.support.config.util.impl;

import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only {@link Configuration} view with prefix remapping on {@code source}.
 * Keys starting with {@code newPrefix} are looked up as {@code oldPrefix + rest}; other keys are
 * looked up as they are. Key listings show the keys the way the view accepts them.
 * <p>
 * Values, entries and the schema are those of {@code source}: an entry reports the key, scope
 * and location it has there, and the schema knows the keys under {@code oldPrefix}.
 *
 * @see ConfigUtil#remapped(Configuration, String, String)
 */
public class RemappedConfiguration extends DelegatingConfigurationView
{
    private final String oldPrefix;
    private final String newPrefix;

    public RemappedConfiguration(Configuration source, String oldPrefix, String newPrefix)
        {
        super("RemappedConfiguration", source);
        this.oldPrefix = oldPrefix != null ? oldPrefix.trim() : "";
        this.newPrefix = newPrefix != null ? newPrefix.trim() : "";
        }

    @Override
    protected String mapKey(String key)
        {
        if (key.startsWith(newPrefix))
            { return oldPrefix+key.substring(newPrefix.length()); }
        return key;
        }

    /** the reverse of {@link #mapKey(String)}, for key listings. */
    private String shownKey(String sourceKey)
        {
        if (sourceKey.startsWith(oldPrefix))
            { return newPrefix+sourceKey.substring(oldPrefix.length()); }
        return sourceKey;
        }

    @Override
    public Set<String> getAllConfigurationKeysFlattened(EnumSet<ConfigScope> scopes)
        {
        Set<String> res = new HashSet<>();
        for (String sourceKey : source.getAllConfigurationKeysFlattened(scopes))
            { res.add(shownKey(sourceKey)); }
        return res;
        }

    @Override
    public Map<String, ConfigEntrySpecification> getAllConfigurationKeysWithSchemasFlattened(EnumSet<ConfigScope> scopes)
        {
        Map<String, ConfigEntrySpecification> res = new HashMap<>();
        for (Map.Entry<String, ConfigEntrySpecification> sourceEntry : source.getAllConfigurationKeysWithSchemasFlattened(scopes).entrySet())
            { res.put(shownKey(sourceEntry.getKey()), sourceEntry.getValue()); }
        return res;
        }

    @Override
    public Iterator<String> getEntryKeyTreeIterator()
        {
        List<String> keys = new ArrayList<>();
        source.getEntryKeyTreeIterator().forEachRemaining(sourceKey->keys.add(shownKey(sourceKey)));
        return keys.iterator();
        }
}
//___EOF___
