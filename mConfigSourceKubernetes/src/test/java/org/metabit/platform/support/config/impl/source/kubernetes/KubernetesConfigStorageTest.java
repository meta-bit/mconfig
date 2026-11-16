package org.metabit.platform.support.config.impl.source.kubernetes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.impl.entry.BasicSecretValue;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.SecretType;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.source.kubernetes.KubernetesConfigStorage.KubernetesSecretLayer;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.mockito.ArgumentCaptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KubernetesConfigStorageTest
{
    @Mock
    private ConfigFactoryInstanceContext context;
    @Mock
    private ConfigLoggingInterface logger;
    @Mock
    private ConfigFactorySettings settings;

    @Test
    public void testGetStorageID()
        {
        KubernetesConfigStorage storage = new KubernetesConfigStorage();
        assertEquals("kubernetes", storage.getStorageID());
        }

    @Test
    public void testBypassTest()
        {
        KubernetesConfigStorage storage = new KubernetesConfigStorage();
        when(settings.getBoolean(KubernetesFeatures.BYPASS_TEST)).thenReturn(true);
        assertTrue(storage.test(settings, logger));
        }

    @Test
    public void testGenerallyWriteable()
        {
        KubernetesConfigStorage storage = new KubernetesConfigStorage();
        assertFalse(storage.isGenerallyWriteable());
        }

    @Test
    public void testSecretMetadataPreservation()
        {
        ConfigSource mockSource = mock(ConfigSource.class);
        ConfigEntryMetadata meta = new ConfigEntryMetadata(mockSource);
        SecretValue value = new BasicSecretValue("topsecret".getBytes(), SecretType.PLAIN_TEXT);

        SecretConfigEntryLeaf leaf = new SecretConfigEntryLeaf("key", value, meta);

        assertEquals("key", leaf.getKey());
        assertEquals(value, leaf.getSecretValue());
        assertEquals(mockSource, leaf.getMeta().getSource());
        }

    @Test
    public void testKubernetesSecretLayerErase()
        {
        when(context.getLogger()).thenReturn(logger);
        ConfigLocation location = mock(ConfigLocation.class, withSettings().extraInterfaces(ConfigSource.class));
        Map<String, byte[]> data = new HashMap<>();
        byte[] secretBytes = "secretValue".getBytes(StandardCharsets.UTF_8);
        data.put("mysecret", secretBytes);

        KubernetesSecretLayer layer = new KubernetesSecretLayer(context, location, data);

        // Verify initial state
        assertNotNull(layer.getEntry("mysecret"));

        // Erase
        layer.erase();

        // Verify that buffer is zeroed
        for (byte b : secretBytes)
            {
            assertEquals(0, b, "Byte should be zeroed");
            }

        // Verify that entry is now gone (data map cleared)
        assertNull(layer.getEntry("mysecret"));
        }

    private static ConfigFileFormatInterface format(String... extensions)
        {
        ConfigFileFormatInterface fmt = mock(ConfigFileFormatInterface.class);
        lenient().when(fmt.getFilenameExtensions()).thenReturn(Arrays.asList(extensions));
        return fmt;
        }

    /** formats list extensions with the dot; until 0.8.17 the key's extension was compared without it. */
    @Test
    public void formatIsFoundByTheKeysExtension()
        {
        ConfigFileFormatInterface yaml = format(".yaml", ".yml");
        ConfigFileFormatInterface props = format(".properties");
        List<ConfigFormatInterface> formats = Arrays.asList(yaml, props);

        assertSame(yaml, KubernetesConfigStorage.findFormatForKey("app.yaml", formats));
        assertSame(yaml, KubernetesConfigStorage.findFormatForKey("app.config.yml", formats));
        assertSame(props, KubernetesConfigStorage.findFormatForKey("db.properties", formats));
        assertNull(KubernetesConfigStorage.findFormatForKey("ca.crt", formats));
        assertNull(KubernetesConfigStorage.findFormatForKey("plainkey", formats));
        assertNull(KubernetesConfigStorage.findFormatForKey(".yaml", formats), "a name that is only an extension");
        assertNull(KubernetesConfigStorage.findFormatForKey("trailing.", formats));
        }

    private KubernetesConfigStorage storageWith(ConfigFileFormatInterface fmt) throws Exception
        {
        KubernetesConfigStorage storage = new KubernetesConfigStorage();
        Field f = KubernetesConfigStorage.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(storage, context);
        when(context.getConfigFormats()).thenReturn(Collections.singletonMap("YAML", (ConfigFormatInterface) fmt));
        return storage;
        }

    @Test
    public void configMapKeyWithExtensionIsParsedByItsFormat() throws Exception
        {
        ConfigFileFormatInterface yaml = format(".yaml");
        ConfigLayerInterface parsed = mock(ConfigLayerInterface.class);
        when(yaml.readStream(any(InputStream.class), any(ConfigLocation.class))).thenReturn(parsed);
        ConfigLocation location = mock(ConfigLocation.class);
        LayeredConfigurationInterface layeredCfg = mock(LayeredConfigurationInterface.class);

        Map<String, byte[]> data = Collections.singletonMap("app.yaml", "server:\n  port: 8080\n".getBytes(StandardCharsets.UTF_8));
        storageWith(yaml).processResourceData(new KubernetesConfigStorage.K8sResourceData(data, null), location, layeredCfg, false);

        verify(layeredCfg).add(parsed, location);
        }

    /** a parsed Secret value would lose SecretValue, redaction and wiping: Secrets are never parsed. */
    @Test
    public void secretKeyWithExtensionStaysOneSecretEntry() throws Exception
        {
        ConfigFileFormatInterface json = format(".json");
        lenient().when(context.getLogger()).thenReturn(logger);
        ConfigLocation location = mock(ConfigLocation.class, withSettings().extraInterfaces(ConfigSource.class));
        LayeredConfigurationInterface layeredCfg = mock(LayeredConfigurationInterface.class);

        Map<String, byte[]> data = Collections.singletonMap("creds.json", "{\"password\":\"s3cr3t\"}".getBytes(StandardCharsets.UTF_8));
        storageWith(json).processResourceData(new KubernetesConfigStorage.K8sResourceData(data, null), location, layeredCfg, true);

        verify(json, never()).readStream(any(), any());
        ArgumentCaptor<ConfigLayerInterface> added = ArgumentCaptor.forClass(ConfigLayerInterface.class);
        verify(layeredCfg).add(added.capture(), eq(location));
        assertTrue(added.getValue() instanceof KubernetesSecretLayer, added.getValue().getClass().getName());
        assertNotNull(added.getValue().getEntry("creds.json"));
        }

    /** the raw formats keep the whole content under the key "": a ConfigMap key would lose its name. */
    @Test
    public void rawFormatsAreNotUsedForKeys()
        {
        ConfigFileFormatInterface rawText = format(".txt", ".cfg");
        when(rawText.getFormatID()).thenReturn("text_file");
        ConfigFileFormatInterface rawBinary = format(".bin");
        when(rawBinary.getFormatID()).thenReturn("binary_file");
        List<ConfigFormatInterface> formats = Arrays.asList(rawText, rawBinary);

        assertNull(KubernetesConfigStorage.findFormatForKey("notes.txt", formats));
        assertNull(KubernetesConfigStorage.findFormatForKey("app.cfg", formats));
        assertNull(KubernetesConfigStorage.findFormatForKey("key.bin", formats));
        }

    private KubernetesConfigStorage storageWithLogger() throws Exception
        {
        KubernetesConfigStorage storage = new KubernetesConfigStorage();
        Field f = KubernetesConfigStorage.class.getDeclaredField("logger");
        f.setAccessible(true);
        f.set(storage, logger);
        return storage;
        }

    /**
     * a ConfigMap's data is plain text, as the API serves it; until 0.8.17 it was Base64-decoded, so a value with
     * ':', blanks or line breaks was dropped as "not base64", and one that happened to be valid Base64 was garbled.
     */
    @Test
    public void configMapDataIsReadAsPlainText() throws Exception
        {
        String response = "{\"apiVersion\":\"v1\",\"kind\":\"ConfigMap\","
                +"\"metadata\":{\"name\":\"app\",\"namespace\":\"default\"},"
                +"\"data\":{\"app.yaml\":\"server:\\n  port: 8080\\n\",\"plain\":\"abcd\",\"spaced\":\" x \"},"
                +"\"binaryData\":{\"blob\":\"AAEC\"}}";
        KubernetesConfigStorage.K8sResourceData parsed = storageWithLogger().parseK8sResponse(response, false);

        assertEquals("server:\n  port: 8080\n", new String(parsed.getData().get("app.yaml"), StandardCharsets.UTF_8));
        assertEquals("abcd", new String(parsed.getData().get("plain"), StandardCharsets.UTF_8), "valid Base64, but text");
        assertEquals(" x ", new String(parsed.getData().get("spaced"), StandardCharsets.UTF_8), "kept as written");
        assertArrayEquals(new byte[]{0, 1, 2}, parsed.getBinaryData().get("blob"), "binaryData stays Base64");
        assertFalse(parsed.getData().containsKey("name"), "metadata is not data");
        verify(logger, never()).warn(anyString());
        }

    @Test
    public void secretDataIsBase64() throws Exception
        {
        String response = "{\"apiVersion\":\"v1\",\"kind\":\"Secret\",\"type\":\"Opaque\","
                +"\"data\":{\"password\":\"czNjcjN0\"}}";
        KubernetesConfigStorage.K8sResourceData parsed = storageWithLogger().parseK8sResponse(response, true);

        assertEquals("s3cr3t", new String(parsed.getData().get("password"), StandardCharsets.UTF_8));
        }
}
