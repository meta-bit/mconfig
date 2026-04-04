package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * implementation of a multidimensional cursor.
 * For a LayeredConfiguration (is it depending? then it needs a name change)
 * it goes through all the layers.
 * <br/>
 * it moves through the branches of a 2D tree structure
 * (as JSON, YAML, Windows Registry etc. have), allowing to move up and down
 * in the branches.
 * <br/>
 * The really tricky bit is with the layers. We need to keep track of position
 * on all layers simultaneously, them being out of sync, while giving the
 * appearance of a coherent structure.
 * <br/>
 * Which might be easier, if we generate a combined map of all valid keys at start
 * (updating if needed! watch out for insert/remove and active change events!)
 * and using that map for navigation.
 * <br/>
 * In fact, we can simply define the contract as "getCursor() provides a snapshot,
 * with no guarantees yet this may be updated dynamically (but could be in the future)".
 *
 * 
 * @version $Id: $Id
 */
public class ConfigCursorImpl implements ConfigCursor
{
    private final LayeredConfiguration layeredConfig;
    private final java.util.Stack<String> pathStack = new java.util.Stack<>();
    private List<String> currentLevelKeys;
    private int currentIndex = -1;
    private String currentLevelPath = "";
    private List<Object> currentList;
    private boolean inListMode = false;

    /**
     * <p>Constructor for ConfigCursorImpl.</p>
     *
     * @param layeredConfig a {@link org.metabit.platform.support.config.impl.LayeredConfiguration} object
     */
    public ConfigCursorImpl(LayeredConfiguration layeredConfig)
        {
        this.layeredConfig = layeredConfig;
        }

    /** {@inheritDoc} */
    @Override
    public boolean canWrite()  { return layeredConfig.isWriteable(); }         //@TODO check

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() { return layeredConfig.isEmpty(); }

    ///  -----------------------------------------------------------------------

    /**
     * <p>reset.</p>
     *
     * @return a boolean
     */
    public boolean reset()
        {
        currentIndex = -1;
        currentLevelKeys = null;
        currentList = null;
        inListMode = false;
        pathStack.clear();
        currentLevelPath = "";
        return true;
        }

    @Override
    public boolean isOnList()
        {
        if (currentIndex < 0) return false;
        if (inListMode) return false; // Nested lists handled by another enter()

        ConfigEntry entry = getCurrentElement();
        if (entry == null) return false;
        return entry.getType() == ConfigEntryType.MULTIPLE_STRINGS || entry.getType() == ConfigEntryType.ENUM_SET;
        }

    /** {@inheritDoc} */
    @Override
    public boolean isOnMap()
        {
        if (currentIndex < 0 || currentLevelKeys == null) return true; // At a level root
        String key = currentLevelKeys.get(currentIndex);
        String fullPath = currentLevelPath.isEmpty() ? key : currentLevelPath + "/" + key;
        ConfigEntry entry = layeredConfig.getConfigEntryFromFullKey(fullPath, EnumSet.allOf(ConfigScope.class));
        return entry == null; // It's a map if there's no direct leaf entry
        }

    /** {@inheritDoc} */
    @Override
    public boolean isOnLeaf()
        {
        if (currentIndex < 0 || currentLevelKeys == null) return false;
        String key = currentLevelKeys.get(currentIndex);
        String fullPath = currentLevelPath.isEmpty() ? key : currentLevelPath + "/" + key;
        ConfigEntry entry = layeredConfig.getConfigEntryFromFullKey(fullPath, EnumSet.allOf(ConfigScope.class));
        return entry != null;
        }

    @Override
    public boolean hasNext()
        {
        if (inListMode)
            {
            return currentList != null && currentIndex + 1 < currentList.size();
            }
        return currentLevelKeys != null && currentIndex + 1 < currentLevelKeys.size();
        }

    /** {@inheritDoc} */
    @Override
    public boolean moveNext()
        {
        if (hasNext())
            {
            currentIndex++;
            return true;
            }
        return false;
        }

    /** {@inheritDoc} */
    @Override
    public boolean canEnter()
        {
        return isOnMap() || isOnList();
        }

    /** {@inheritDoc} */
    @Override
    public boolean canLeave()
        {
        return !pathStack.isEmpty() || inListMode;
        }

    /** {@inheritDoc} */
    @Override
    public boolean enter()
        {
        if (isOnList())
            {
            try
                {
                ConfigEntry entry = getCurrentElement();
                currentList = new java.util.ArrayList<>(entry.getValueAsStringList());
                pathStack.push(currentLevelKeys.get(currentIndex));
                currentLevelPath = String.join("/", pathStack);
                inListMode = true;
                currentIndex = -1;
                return true;
                }
            catch (ConfigCheckedException e)
                {
                return false;
                }
            }

        if (isOnMap())
            {
            if (currentIndex >= 0)
                {
                String key = currentLevelKeys.get(currentIndex);
                pathStack.push(key);
                currentLevelPath = String.join("/", pathStack);
                }
            
            // Collect keys at this new level
            currentLevelKeys = collectKeysAtCurrentLevel();
            currentIndex = -1;
            return !currentLevelKeys.isEmpty();
            }
        return false;
        }

    private List<String> collectKeysAtCurrentLevel()
        {
        if (inListMode)
            {
            if (currentList == null) return Collections.emptyList();
            List<String> indices = new java.util.ArrayList<>(currentList.size());
            for (int i = 0; i < currentList.size(); i++) indices.add(String.valueOf(i));
            return indices;
            }

        java.util.Set<String> keysAtLevel = new java.util.LinkedHashSet<>();
        java.util.Set<String> allKeys = layeredConfig.getAllConfigurationKeysFlattened(EnumSet.allOf(ConfigScope.class));
        for (String fullKey : allKeys)
            {
            if (currentLevelPath.isEmpty())
                {
                keysAtLevel.add(fullKey.split("/")[0]);
                }
            else if (fullKey.startsWith(currentLevelPath + "/"))
                {
                String remaining = fullKey.substring(currentLevelPath.length() + 1);
                keysAtLevel.add(remaining.split("/")[0]);
                }
            }
        return new java.util.ArrayList<>(keysAtLevel);
        }

    /** {@inheritDoc} */
    @Override
    public boolean leave()
        {
        if (inListMode)
            {
            inListMode = false;
            currentList = null;
            String key = pathStack.pop();
            currentLevelPath = String.join("/", pathStack);
            // restore currentIndex for the list key
            currentLevelKeys = collectKeysAtCurrentLevel();
            currentIndex = currentLevelKeys.indexOf(key);
            return true;
            }

        if (canLeave())
            {
            String leftKey = pathStack.pop();
            currentLevelPath = String.join("/", pathStack);
            
            // Re-collect keys at the parent level
            currentLevelKeys = collectKeysAtCurrentLevel();
            // After leave, we should be positioned on the element we just left
            currentIndex = currentLevelKeys.indexOf(leftKey);
            return true;
            }
        return false;
        }

    /** {@inheritDoc} */
    @Override
    public boolean moveTo(String keyWithPath)
        {
        if (keyWithPath == null || keyWithPath.isEmpty()) return false;
        
        // Try simple case: key at current level
        if (currentLevelKeys == null)
            {
            currentLevelKeys = collectKeysAtCurrentLevel();
            }
            
        for (int i = 0; i < currentLevelKeys.size(); i++)
            {
            if (currentLevelKeys.get(i).equalsIgnoreCase(keyWithPath))
                {
                currentIndex = i;
                return true;
                }
            }
            
        // If not found at current level, it might be a path.
        // For now, let's just support simple keys as that's what ConfigMapper uses.
        return false;
        }

    /** {@inheritDoc} */
    @Override
    public ConfigEntry getCurrentElement()
        {
        if (inListMode)
            {
            if (currentList == null || currentIndex < 0 || currentIndex >= currentList.size()) return null;
            Object val = currentList.get(currentIndex);
            String listKey = pathStack.peek();
            
            // Reconstruct path to list to get its metadata
            ConfigEntry listEntry = layeredConfig.getConfigEntryFromFullKey(currentLevelPath, EnumSet.allOf(ConfigScope.class));
            ConfigEntryMetadata meta = (listEntry instanceof AbstractConfigEntry) ? ((AbstractConfigEntry) listEntry).getMeta() : null;
            
            ConfigEntryType itemType = ConfigEntryType.STRING; // default
            if (listEntry != null)
                {
                if (listEntry.getType() == ConfigEntryType.ENUM_SET) itemType = ConfigEntryType.ENUM;
                }
            
            return new ListItemConfigEntry(listKey, currentIndex, val, itemType, meta);
            }

        if (currentLevelKeys != null && currentIndex >= 0 && currentIndex < currentLevelKeys.size())
            {
            String key = currentLevelKeys.get(currentIndex);
            String fullPath = currentLevelPath.isEmpty() ? key : currentLevelPath + "/" + key;
            ConfigEntry entry = layeredConfig.getConfigEntryFromFullKey(fullPath, EnumSet.allOf(ConfigScope.class));
            if (entry == null)
                {
                // Return a dummy entry representing a map/node
                return new MapConfigEntry(key);
                }
            
            // If entry is not null, it's a leaf. We should still return its simple key.
            if (!entry.getKey().equals(key))
                {
                return new SimpleKeyConfigEntryWrapper(entry, key);
                }
            return entry;
            }
        return null;
        }

    /**
     * {@inheritDoc}
     *
     * copyMapToObject - copies a map to an object
     */
    @Override
    public int copyMapToObject(Object targetPojo, String functionPrefix, String functionPostfix)
        {
        try
            {
            Class<?> mapperImplClass = Class.forName("org.metabit.platform.support.config.mapper.ConfigMapperImpl");
            Object mapper = mapperImplClass.getDeclaredConstructor().newInstance();
            Class<?> mapperInterface = Class.forName("org.metabit.platform.support.config.mapper.ConfigMapper");
            Method mapMethod = mapperInterface.getMethod("mapCursorToPojo", 
                                                    org.metabit.platform.support.config.ConfigCursor.class, 
                                                    Object.class, 
                                                    String.class, 
                                                    String.class);
            return (int) mapMethod.invoke(mapper, this, targetPojo, functionPrefix, functionPostfix);
            }
        catch (ClassNotFoundException e)
            {
            // Mapper module not available in classpath
            return 0;
            }
        catch (Exception e)
            {
            // Log error if possible, or ignore as per guidelines
            return 0;
            }
        }

    /**
     * {@inheritDoc}
     *
     * derived function:
     * Removes from the underlying collection the last element returned
     * by this iterator (optional operation).  This method can be called
     * only once per call to next().  The behavior of an iterator
     * is unspecified if the underlying collection is modified while the
     * iteration is in progress in any way other than by calling this
     * method.
     */
    @Override
    public void remove() throws ConfigException
        {
        if (!canWrite())
            { throw new ConfigException(ConfigException.ConfigExceptionReason.NOT_WRITEABLE, "Cursor not writable"); }
        if (currentIndex < 0)
            {
            throw new IllegalStateException("remove() called without current element (call moveNext() first)");
            }
        
        ConfigEntry entry = getCurrentElement();
        ConfigScope scope = (entry != null && entry.getScope() != null) ? entry.getScope() : ConfigScope.USER;

        String key = currentLevelKeys.get(currentIndex);
        String fullPath = currentLevelPath.isEmpty() ? key : currentLevelPath + "/" + key;
        
        // In mConfig, removing an entry often means setting it to null or empty in a writable layer,
        // or actually removing it from the storage if supported.
        // LayeredConfiguration.put with null/empty value might be used for removal.
        layeredConfig.put(fullPath, "", scope);
        
        // Refresh after modification
        currentLevelKeys = collectKeysAtCurrentLevel();
        }

    /** {@inheritDoc} */
    @Override
    public void put(String key, Object value, ConfigScope scope) throws ConfigException
    {
        String fullPath = currentLevelPath.isEmpty() ? key : currentLevelPath + "/" + key;
        if (value instanceof List)
            {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) value;
            layeredConfig.put(fullPath, list, scope);
            }
        else if (value instanceof byte[])
            {
            layeredConfig.put(fullPath, (byte[]) value, scope);
            }
        else
            {
            layeredConfig.put(fullPath, String.valueOf(value), scope);
            }
        // Refresh keys if we added a new one
        currentLevelKeys = collectKeysAtCurrentLevel();
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(Object value) throws ConfigException
    {
        if (inListMode)
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.NOT_WRITEABLE, "Writing to list items via cursor is not yet supported");
            }
        
        if (currentIndex < 0 || currentIndex >= currentLevelKeys.size())
            {
            throw new IllegalStateException("setValue() called without current element (call moveNext() first)");
            }

        String key = currentLevelKeys.get(currentIndex);
        String fullPath = currentLevelPath.isEmpty() ? key : currentLevelPath + "/" + key;
        
        ConfigEntry entry = getCurrentElement();
        if (entry != null && entry.getScope() != null)
            {
            try
                {
                entry.putValue(value, determineEntryType(value));
                }
            catch (ConfigCheckedException e)
                {
                throw new ConfigException(e);
                }
            }
        else
            {
            // Fallback to layeredConfig.put if entry not found or scope unknown
            // We use RUNTIME as a safe default for cursors if no other scope is known, 
            // but SESSION or USER might also be appropriate depending on context.
            // For general library use, USER is the standard writable scope.
            // @TODO discuss this preliminary plan
            put(key, value, ConfigScope.RUNTIME);
            layeredConfig.logger.warn("put falling back to RUNTIME scope");
            }
    }

    private ConfigEntryType determineEntryType(Object value)
        {
        if (value instanceof String) return ConfigEntryType.STRING;
        if (value instanceof Boolean) return ConfigEntryType.BOOLEAN;
        if (value instanceof Number) return ConfigEntryType.NUMBER;
        if (value instanceof List) return ConfigEntryType.MULTIPLE_STRINGS;
        if (value instanceof byte[]) return ConfigEntryType.BYTES;
        return ConfigEntryType.STRING;
        }
}
//___EOF___
