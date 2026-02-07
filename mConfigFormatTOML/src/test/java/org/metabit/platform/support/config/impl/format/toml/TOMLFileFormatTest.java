package org.metabit.platform.support.config.impl.format.toml;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.core.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TOMLFileFormatTest
{
    @Test
    void parseBasicValues() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());

        String toml = "title = \"TOML\"\n" +
                "\n" +
                "[owner]\n" +
                "name = \"Tom\"\n" +
                "ports = [8000, 8001]\n" +
                "tool = { name = \"mConfig\", version = \"1\" }\n" +
                "\n" +
                "[[servers]]\n" +
                "ip = \"1.1.1.1\"\n" +
                "\n" +
                "[[servers]]\n" +
                "ip = \"2.2.2.2\"\n";

        InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new TestConfigLocation();

        ConfigLayerInterface layer = format.readStream(is, location);
        assertNotNull(layer);
        assertEquals("TOML", layer.getEntry("title").getValueAsString());
        assertEquals("Tom", layer.getEntry("owner/name").getValueAsString());
        assertEquals(List.of("8000", "8001"), layer.getEntry("owner/ports").getValueAsStringList());
        assertEquals("mConfig", layer.getEntry("owner/tool/name").getValueAsString());
        assertEquals("1.1.1.1", layer.getEntry("servers/0/ip").getValueAsString());
        assertEquals("2.2.2.2", layer.getEntry("servers/1/ip").getValueAsString());
        }

    @Test
    void duplicateKeysFailParsing() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());

        String toml = "key = \"first\"\n" +
                "key = \"second\"\n";

        InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8));
        ConfigLocation location = new TestConfigLocation();

        ConfigLayerInterface layer = format.readStream(is, location);
        assertNull(layer, "TOML parser should reject duplicate keys");
        }

    @Test
    void basicAndLiteralStrings() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "basic = \"line\\nnext\"\n"
                + "literal = 'c:\\\\path\\\\file'\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("line\nnext", layer.getEntry("basic").getValueAsString());
        assertEquals("c:\\\\path\\\\file", layer.getEntry("literal").getValueAsString());
        }

    @Test
    void multilineStrings() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "multibasic = \"\"\"first\nsecond\"\"\"\n"
                + "multiliteral = '''raw\ntext'''\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("first\nsecond", layer.getEntry("multibasic").getValueAsString());
        assertEquals("raw\ntext", layer.getEntry("multiliteral").getValueAsString());
        }

    @Test
    void multilineLineContinuation() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = "multi = \"\"\"a\\\n  b\"\"\"\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("ab", layer.getEntry("multi").getValueAsString());
        }

    @Test
    void unicodeEscapes() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "escape_unicode = \"snowman: \\u2603\"\n"
                + "escape_unicode_big = \"music: \\U0001D11E\"\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("snowman: \u2603", layer.getEntry("escape_unicode").getValueAsString());
        assertEquals("music: \uD834\uDD1E", layer.getEntry("escape_unicode_big").getValueAsString());
        }

    @Test
    void multilineStringNewlineNormalization() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = "multi = \"\"\"line1\r\nline2\r\n\"\"\"\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("line1\nline2\n", layer.getEntry("multi").getValueAsString());
        }

    @Test
    void invalidEscapesRejected()
        {
        assertNull(parseToml("bad = \"\\x20\"\n"));
        assertNull(parseToml("bad = \"\\u12G4\"\n"));
        assertNull(parseToml("bad = \"\\U00110000\"\n"));
        }

    @Test
    void lineContinuation() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = "continued = \"a\\\n  b\"\n";
        assertNull(parseToml(toml));
        }

    @Test
    void dateTimeTypes() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "local_date = 1979-05-27\n"
                + "local_time = 07:32:00\n"
                + "local_datetime = 1979-05-27T07:32:00\n"
                + "offset_datetime = 1979-05-27T07:32:00Z\n";

        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEntryType(layer, "local_date", org.metabit.platform.support.config.ConfigEntryType.DATE);
        assertEntryType(layer, "local_time", org.metabit.platform.support.config.ConfigEntryType.TIME);
        assertEntryType(layer, "local_datetime", org.metabit.platform.support.config.ConfigEntryType.DATETIME);
        assertEntryType(layer, "offset_datetime", org.metabit.platform.support.config.ConfigEntryType.DATETIME);
        }

    @Test
    void invalidDateTimeRejected()
        {
        String toml = "bad_date = 1979-02-30\n";
        assertNull(parseToml(toml));
        }

    @Test
    void dottedKeysMergeAndOrdering() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "a.b.c = 1\n"
                + "[a]\n"
                + "d = 2\n"
                + "[a.e]\n"
                + "f = 3\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("1", layer.getEntry("a/b/c").getValueAsString());
        assertEquals("2", layer.getEntry("a/d").getValueAsString());
        assertEquals("3", layer.getEntry("a/e/f").getValueAsString());
        }

    @Test
    void dottedKeyConflictsFailParsing()
        {
        String toml = ""
                + "a.b = 1\n"
                + "[a.b]\n"
                + "c = 2\n";
        assertNull(parseToml(toml));
        }

    @Test
    void dottedKeyOrderingWithInlineTables() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "a = { b = { c = 1 } }\n"
                + "a.b.d = 2\n";
        assertNull(parseToml(toml));
        }

    @Test
    void arraysSupportAndRules() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "nested = [[1, 2], [3, 4]]\n"
                + "trailing = [1, 2,]\n"
                + "multiline = [\n"
                + "  1,\n"
                + "  # comment\n"
                + "  2,\n"
                + "]\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals(List.of("1", "2"), layer.getEntry("trailing").getValueAsStringList());
        assertEquals(List.of("1", "2"), layer.getEntry("multiline").getValueAsStringList());
        assertNull(layer.getEntry("nested"), "Nested arrays are parsed but not exposed as entries");
        assertEquals("1", layer.getEntry("nested/0/0").getValueAsString());
        assertEquals("4", layer.getEntry("nested/1/1").getValueAsString());

        String mixed = "bad = [1, \"two\"]\n";
        assertNull(parseToml(mixed));
        }

    @Test
    void iteratorIncludesNestedArrayElements() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = "nested = [[1, 2], [3, 4]]\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        List<String> keys = new ArrayList<>();
        for (java.util.Iterator<String> it = layer.tryToGetKeyIterator(); it != null && it.hasNext(); )
            {
            keys.add(it.next());
            }
        assertTrue(keys.contains("nested/0"));
        assertTrue(keys.contains("nested/1"));
        }

    @Test
    void inlineTablesRules() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = "tool = { name = \"mConfig\", version = 1, meta.version = 2 }\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("mConfig", layer.getEntry("tool/name").getValueAsString());
        assertEquals("1", layer.getEntry("tool/version").getValueAsString());
        assertEquals("2", layer.getEntry("tool/meta/version").getValueAsString());

        String newlineInline = "bad = { a = 1,\n b = 2 }\n";
        assertNull(parseToml(newlineInline));

        String inlineConflict = ""
                + "a = { b = 1 }\n"
                + "a.c = 2\n";
        assertNull(parseToml(inlineConflict));
        }

    @Test
    void commentsAndWhitespace() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "key = 1 # trailing comment\n"
                + "arr = [1, # inline comment\n"
                + "  2] # after closing bracket\n"
                + "\n"
                + "[table] # comment after header\n"
                + "  spaced_key  \t =\t 3\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("1", layer.getEntry("key").getValueAsString());
        assertEquals(List.of("1", "2"), layer.getEntry("arr").getValueAsStringList());
        assertEquals("3", layer.getEntry("table/spaced_key").getValueAsString());
        }

    @Test
    void whitespaceAroundCommasAndKeys() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "arr = [1 ,2 , 3]\n"
                + " key = 1\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals(List.of("1", "2", "3"), layer.getEntry("arr").getValueAsStringList());
        assertEquals("1", layer.getEntry("key").getValueAsString());
        }

    @Test
    void numberFormats() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "int = 1_000\n"
                + "plus = +7\n"
                + "hex = 0x1f\n"
                + "oct = 0o10\n"
                + "bin = 0b1010\n"
                + "float_exp = 1.2e3\n"
                + "inf = +inf\n"
                + "nan = nan\n"
                + "plus_nan = +nan\n"
                + "neg_zero = -0\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("1000", layer.getEntry("int").getValueAsString());
        assertEquals("7", layer.getEntry("plus").getValueAsString());
        assertEquals("31", layer.getEntry("hex").getValueAsString());
        assertEquals("8", layer.getEntry("oct").getValueAsString());
        assertEquals("10", layer.getEntry("bin").getValueAsString());
        assertEquals(1200.0, layer.getEntry("float_exp").getValueAsDouble(), 0.0001);
        assertTrue(Double.isInfinite(layer.getEntry("inf").getValueAsDouble()));
        assertTrue(Double.isNaN(layer.getEntry("nan").getValueAsDouble()));
        assertTrue(Double.isNaN(layer.getEntry("plus_nan").getValueAsDouble()));
        assertEquals("0", layer.getEntry("neg_zero").getValueAsString());

        String leadingZero = "bad = 01\n";
        assertNull(parseToml(leadingZero));

        String badFloatZero = "bad = 01.2\n";
        assertNull(parseToml(badFloatZero));

        String badFloatDot = "bad = 1.\n";
        assertNull(parseToml(badFloatDot));

        String badUnderscore = "bad = 1__2\n";
        assertNull(parseToml(badUnderscore));

        String badHexUnderscore = "bad = 0x_1\n";
        assertNull(parseToml(badHexUnderscore));
        }

    @Test
    void utf8KeysAndStrings() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "\"\u043A\u043B\u044E\u0447\" = \"cafe\"\n"
                + "emoji = \"\u2603\"\n"
                + "name = \"caf\u00E9\"\n"
                + "\"\u0065\u0301\" = \"decomposed\"\n"
                + "\"\u00E9\" = \"composed\"\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        assertEquals("cafe", layer.getEntry("\u043A\u043B\u044E\u0447").getValueAsString());
        assertEquals("\u2603", layer.getEntry("emoji").getValueAsString());
        assertEquals("caf\u00E9", layer.getEntry("name").getValueAsString());
        assertEquals("decomposed", layer.getEntry("\u0065\u0301").getValueAsString());
        assertEquals("composed", layer.getEntry("\u00E9").getValueAsString());
        }

    @Test
    void commentsRoundTripAndOrder() throws org.metabit.platform.support.config.ConfigCheckedException
        {
        String toml = ""
                + "# top\n"
                + "title = \"TOML\" # inline\n"
                + "# owner table\n"
                + "[owner] # header\n"
                + "# name comment\n"
                + "name = \"Tom\"\n"
                + "nums = [1, # first\n"
                + "  # second\n"
                + "  2]\n";
        ConfigLayerInterface layer = parseToml(toml);
        assertNotNull(layer);
        TomlWriter writer = new TomlWriter();
        String out = writer.write(((TOMLConfigLayer) layer).getRoot());
        int topIndex = out.indexOf("# top");
        int titleIndex = out.indexOf("title = \"TOML\"");
        int ownerCommentIndex = out.indexOf("# owner table");
        int ownerHeaderIndex = out.indexOf("[owner]");
        int nameCommentIndex = out.indexOf("# name comment");
        int nameIndex = out.indexOf("name = \"Tom\"");
        assertTrue(topIndex >= 0);
        assertTrue(titleIndex > topIndex);
        assertTrue(out.contains("title = \"TOML\" # inline"));
        assertTrue(ownerCommentIndex > titleIndex);
        assertTrue(ownerHeaderIndex > ownerCommentIndex);
        assertTrue(out.contains("[owner] # header"));
        assertTrue(nameCommentIndex > ownerHeaderIndex);
        assertTrue(nameIndex > nameCommentIndex);
        assertTrue(out.contains("# second"));
        assertTrue(out.contains("nums = ["));
        }

    private ConfigLayerInterface parseToml(String toml)
        {
        TOMLFileFormat format = new TOMLFileFormat();
        format.testComponent(new ConfigFactorySettings(), NullLogging.getSingletonInstance());
        InputStream is = new ByteArrayInputStream(toml.getBytes(StandardCharsets.UTF_8));
        return format.readStream(is, new TestConfigLocation());
        }

    private void assertEntryType(ConfigLayerInterface layer, String key, org.metabit.platform.support.config.ConfigEntryType type)
        {
        assertNotNull(layer.getEntry(key), "Missing entry for key: " + key);
        assertEquals(type, layer.getEntry(key).getType());
        }

    private static final class TestConfigLocation implements ConfigLocation
        {
        @Override
        public String toLocationString()
            {
            return "test://toml";
            }

        @Override
        public URI getURI(String key, String optionalFragment)
            {
            return URI.create("test://toml");
            }

        @Override
        public ConfigScope getScope()
            {
            return ConfigScope.SESSION;
            }

        @Override
        public boolean isWriteable()
            {
            return false;
            }

        @Override
        public ConfigStorageInterface getStorage()
            {
            return null;
            }

        @Override
        public Object getStorageInstanceHandle()
            {
            return null;
            }

        @Override
        public ConfigLocation derive(Path file)
            {
            return this;
            }

        @Override
        public ConfigLocation derive(URI uri)
            {
            return this;
            }
        }
}
