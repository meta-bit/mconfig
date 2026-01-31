package org.metabit.platform.support.config.impl.source.filesystem;

import java.nio.file.Path;
import java.util.*;

public class PathPathMultimap
{
    final Map<Path, Set<Path>> internalRepresentation;


    public PathPathMultimap() { internalRepresentation = new HashMap<>(); }

    public void clear() { internalRepresentation.clear(); }

    public boolean containsKey(Path key) { return internalRepresentation.containsKey(key); }

    public void put(Path key, Path value)
        {
        if (!internalRepresentation.containsKey(key))
            {
            internalRepresentation.put(key, new HashSet<>());
            }
        internalRepresentation.get(key).add(value); // avoids duplicates implicitly
        return;
        }

    /**
     * remove a pair
     *
     * @param key   key/upper
     * @param value value/lower
     * @return true if there is no value left associated with the key
     */
    public boolean remove(final Path key, final Path value)
        {
        if (internalRepresentation.containsKey(key))
            internalRepresentation.get(key).remove(value);
        // else: strange if it doesn't contain the key.
        boolean empty = internalRepresentation.get(key).isEmpty();
        if (empty)
            { internalRepresentation.remove(key); }
        return empty;
        }


    public Set<Path> getConstKeyMap()
        {
        return Collections.synchronizedSet(internalRepresentation.keySet());
        }

    public Set<Path> removeAllValues(final Path key)
        {
        Set<Path> entry = internalRepresentation.remove(key);
        return Objects.requireNonNullElse(entry, Collections.emptySet());
        }

    public int size()
        {
        return internalRepresentation.size();
        }
}
