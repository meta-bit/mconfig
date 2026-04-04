package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A thread-safe, capped, and de-duplicating list for {@link ConfigEvent}s.
 * It prevents rapid-fire duplicates of the same event using a small ring buffer.
 */
public class ConfigEventListImpl implements List<ConfigEvent>
{
    private final List<ConfigEvent>  list;
    private final int                maxSize;
    private final Deque<ConfigEvent> recentDeque;
    private final Set<ConfigEvent>   recentSet;
    private final int                dedupLimit;

    public ConfigEventListImpl(int maxSize)
        {
        this(maxSize, 32);
        }

    public ConfigEventListImpl(int maxSize, int dedupLimit)
        {
        this.maxSize = maxSize > 0 ? maxSize : 1000;
        this.dedupLimit = Math.max(0, dedupLimit);
        this.list = new ArrayList<>();
        this.recentDeque = new ArrayDeque<>(this.dedupLimit + 1);
        this.recentSet = new HashSet<>(this.dedupLimit + 1);
        }

    @Override
    public synchronized boolean add(ConfigEvent event)
        {
        if (event == null) return false;

        // de-duplication via ring buffer
        if (dedupLimit > 0)
            {
            if (recentSet.contains(event))
                {
                return false; // skip duplicate
                }

            recentSet.add(event);
            recentDeque.addLast(event);
            if (recentDeque.size() > dedupLimit)
                {
                ConfigEvent oldest = recentDeque.removeFirst();
                recentSet.remove(oldest);
                }
            }

        if (list.size() >= maxSize)
            {
            list.remove(0); // drop oldest
            }
        return list.add(event);
        }

    @Override
    public synchronized int size() { return list.size(); }

    @Override
    public synchronized boolean isEmpty() { return list.isEmpty(); }

    @Override
    public synchronized boolean contains(Object o) { return list.contains(o); }

    @Override
    public synchronized Iterator<ConfigEvent> iterator() { return new ArrayList<>(list).iterator(); }

    @Override
    public synchronized Object[] toArray() { return list.toArray(); }

    @Override
    public synchronized <T> T[] toArray(T[] a) { return list.toArray(a); }

    @Override
    public synchronized boolean remove(Object o) { return list.remove(o); }

    @Override
    public synchronized boolean containsAll(Collection<?> c) { return list.containsAll(c); }

    @Override
    public synchronized boolean addAll(Collection<? extends ConfigEvent> c)
        {
        boolean modified = false;
        for (ConfigEvent e : c)
            {
            if (add(e)) modified = true;
            }
        return modified;
        }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends ConfigEvent> c)
        {
        throw new UnsupportedOperationException("Indexed addAll not supported on capped event list");
        }

    @Override
    public synchronized boolean removeAll(Collection<?> c) { return list.removeAll(c); }

    @Override
    public synchronized boolean retainAll(Collection<?> c) { return list.retainAll(c); }

    @Override
    public synchronized void clear() { list.clear(); recentDeque.clear(); recentSet.clear(); }

    @Override
    public synchronized ConfigEvent get(int index) { return list.get(index); }

    @Override
    public synchronized ConfigEvent set(int index, ConfigEvent element) { return list.set(index, element); }

    @Override
    public synchronized void add(int index, ConfigEvent element)
        {
        throw new UnsupportedOperationException("Indexed add not supported on capped event list");
        }

    @Override
    public synchronized ConfigEvent remove(int index) { return list.remove(index); }

    @Override
    public synchronized int indexOf(Object o) { return list.indexOf(o); }

    @Override
    public synchronized int lastIndexOf(Object o) { return list.lastIndexOf(o); }

    @Override
    public synchronized ListIterator<ConfigEvent> listIterator() { return new ArrayList<>(list).listIterator(); }

    @Override
    public synchronized ListIterator<ConfigEvent> listIterator(int index) { return new ArrayList<>(list).listIterator(index); }

    @Override
    public synchronized List<ConfigEvent> subList(int fromIndex, int toIndex) { return new ArrayList<>(list).subList(fromIndex, toIndex); }
}
