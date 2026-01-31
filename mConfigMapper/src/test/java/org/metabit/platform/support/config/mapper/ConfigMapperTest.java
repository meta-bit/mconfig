package org.metabit.platform.support.config.mapper;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigMapperTest
{
    @Test
    public void testFactoryCreation()
    {
        ConfigMapper mapper = ConfigMapper.create();
        assertNotNull(mapper);
        assertTrue(mapper instanceof ConfigMapperImpl);
    }

    @Test
    public void testMapping()
        {
        ConfigMapper mapper = new ConfigMapperImpl();
        MockConfigCursor cursor = new MockConfigCursor();
        // test class in the same package as this test.
        TestPojo pojo = new TestPojo();
        
        // Setup mock cursor with some data
        cursor.addEntry("name", "John Doe");
        cursor.addEntry("age", "30");
        cursor.addEntry("active", "true");

        int mappedCount = mapper.mapCursorToPojo(cursor, pojo, "set", "");

        assertEquals(3, mappedCount);
        assertEquals("John Doe", pojo.getName());
        assertEquals(30, pojo.getAge());
        assertTrue(pojo.isActive());
        }

    @Test
    public void testMapPojoToCursor() throws ConfigException, ConfigCheckedException
        {
        ConfigMapper mapper = new ConfigMapperImpl();
        MockConfigCursor cursor = new MockConfigCursor();
        TestPojo pojo = new TestPojo();
        pojo.setName("Jane Doe");
        pojo.setAge(25);
        pojo.setActive(false);

        // Pre-add entries to mock cursor so moveTo works
        cursor.addEntry("Name", "");
        cursor.addEntry("Age", "0");
        cursor.addEntry("Active", "true");

        int mappedCount = mapper.mapPojoToCursor(pojo, cursor, EnumSet.allOf(ConfigScope.class), "get", "");
        mappedCount += mapper.mapPojoToCursor(pojo, cursor, EnumSet.allOf(ConfigScope.class), "is", "");

        assertEquals(3, mappedCount);
        assertEquals("Jane Doe", cursor.getEntryValue("Name"));
        assertEquals("25", cursor.getEntryValue("Age"));
        assertEquals("false", cursor.getEntryValue("Active"));
        }

    @Test
    public void testReadObject() throws ConfigException
        {
        ConfigMapper mapper = new ConfigMapperImpl();
        MockConfigCursor cursor = new MockConfigCursor();
        cursor.addEntry("name", "Bob");
        cursor.addEntry("age", "40");
        cursor.addEntry("active", "true");

        TestPojo pojo = mapper.readObject(cursor, TestPojo.class);

        assertNotNull(pojo);
        assertEquals("Bob", pojo.getName());
        assertEquals(40, pojo.getAge());
        assertTrue(pojo.isActive());
        }

    @Test
    public void testWriteObject() throws ConfigException, ConfigCheckedException
        {
        ConfigMapper mapper = new ConfigMapperImpl();
        MockConfigCursor cursor = new MockConfigCursor();
        TestPojo pojo = new TestPojo();
        pojo.setName("Alice");
        pojo.setAge(35);
        pojo.setActive(true);

        cursor.addEntry("Name", "");
        cursor.addEntry("Age", "0");
        cursor.addEntry("Active", "false");

        mapper.writeObject(pojo, cursor);

        assertEquals("Alice", cursor.getEntryValue("Name"));
        assertEquals("35", cursor.getEntryValue("Age"));
        assertEquals("true", cursor.getEntryValue("Active"));
        }

    // A simple mock cursor for testing
    private static class MockConfigCursor implements ConfigCursor
    {
        private final java.util.Map<String, ConfigEntry> entries = new java.util.LinkedHashMap<>();
        private       java.util.Iterator<ConfigEntry>    iterator;
        private ConfigEntry current;

        public void addEntry(String key, String value)
            {
            entries.put(key, new MockConfigEntry(key, value));
            }

        public String getEntryValue(String key) throws ConfigCheckedException
            {
            ConfigEntry entry = entries.get(key);
            return entry != null ? entry.getValueAsString() : null;
            }

        @Override public boolean isOnMap() { return true; }
        @Override public boolean enter() { iterator = entries.values().iterator(); return !entries.isEmpty(); }
        @Override public boolean moveNext() { if (iterator != null && iterator.hasNext()) { current = iterator.next(); return true; } return false; }
        @Override public ConfigEntry getCurrentElement() { return current; }
        @Override public boolean leave() { return true; }

        @Override
        public boolean moveTo(String keyWithPath)
            {
            for (String key : entries.keySet())
                {
                if (key.equalsIgnoreCase(keyWithPath))
                    {
                    current = entries.get(key);
                    return true;
                    }
                }
            return false;
            }

        // Other methods not needed for this simple test
        @Override public boolean canWrite() { return true; }
        @Override public boolean isEmpty() { return entries.isEmpty(); }
        public boolean reset() { return false; }
        @Override public boolean isOnList() { return false; }
        @Override public boolean isOnLeaf() { return false; }
        @Override public boolean hasNext() { return iterator != null && iterator.hasNext(); }
        @Override public boolean canEnter() { return false; }
        @Override public boolean canLeave() { return false; }
        @Override public int copyMapToObject(Object targetPojo, String functionPrefix, String functionPostfix) { return 0; }
        @Override public void remove() {}
    }

    private static class MockConfigEntry implements ConfigEntry
    {
        private final String key;
        private Object value;

        public MockConfigEntry(String key, Object value)
            {
            this.key = key;
            this.value = value;
            }

        @Override public String getValueAsString() throws ConfigCheckedException { return String.valueOf(value); }
        @Override public Boolean getValueAsBoolean() throws ConfigCheckedException { return Boolean.valueOf(String.valueOf(value)); }
        @Override public Integer getValueAsInteger() throws ConfigCheckedException { return Integer.valueOf(String.valueOf(value)); }
        @Override public Long getValueAsLong() throws ConfigCheckedException { return Long.valueOf(String.valueOf(value)); }
        @Override public Double getValueAsDouble() throws ConfigCheckedException { return Double.valueOf(String.valueOf(value)); }
        @Override public java.math.BigInteger getValueAsBigInteger() throws ConfigCheckedException { return new java.math.BigInteger(String.valueOf(value)); }
        @Override public java.math.BigDecimal getValueAsBigDecimal() throws ConfigCheckedException { return new java.math.BigDecimal(String.valueOf(value)); }
        @Override public byte[] getValueAsBytes() throws ConfigCheckedException { return String.valueOf(value).getBytes(); }
        @Override public java.util.List<String> getValueAsStringList() throws ConfigCheckedException { return java.util.Arrays.asList(String.valueOf(value)); }
        @Override public String getKey() { return key; }
        @Override public ConfigScope getScope() { return null; }
        @Override public ConfigEntryType getType() { return null; }
        @Override public ConfigLocation getLocation() { return null; }
        @Override public java.net.URI getURI() { return null; }
        @Override public java.net.URI getValueAsURI() throws ConfigCheckedException { try { return new java.net.URI(String.valueOf(value)); } catch (Exception e) { throw new ConfigCheckedException(e); } }
        @Override public java.nio.file.Path getValueAsPath() throws ConfigCheckedException { return java.nio.file.Paths.get(String.valueOf(value)); }
        @Override public java.nio.file.Path getValueAsPath(java.nio.file.FileSystem fs) throws ConfigCheckedException { return fs.getPath(String.valueOf(value)); }
        @Override public java.time.LocalDate getValueAsLocalDate() throws ConfigCheckedException { return java.time.LocalDate.parse(String.valueOf(value)); }
        @Override public java.time.LocalTime getValueAsLocalTime() throws ConfigCheckedException { return java.time.LocalTime.parse(String.valueOf(value)); }
        @Override public java.time.LocalDateTime getValueAsLocalDateTime() throws ConfigCheckedException { return java.time.LocalDateTime.parse(String.valueOf(value)); }
        @Override public java.time.OffsetDateTime getValueAsOffsetDateTime() throws ConfigCheckedException { return java.time.OffsetDateTime.parse(String.valueOf(value)); }
        @Override public java.time.ZonedDateTime getValueAsZonedDateTime() throws ConfigCheckedException { return java.time.ZonedDateTime.parse(String.valueOf(value)); }
        @Override public java.time.Duration getValueAsDuration() throws ConfigCheckedException { return java.time.Duration.parse(String.valueOf(value)); }
        @Override public org.metabit.platform.support.config.interfaces.ConfigEntrySpecification getSpecification() throws ConfigCheckedException { return null; }
        @Override public void putString(String value) throws ConfigCheckedException { this.value = value; }
        @Override public void putValue(Object value, ConfigEntryType valueType) throws ConfigCheckedException { this.value = value; }
    }

}
