package org.metabit.platform.support.config;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.source.core.InMemoryLayer;
import org.metabit.platform.support.config.source.core.InMemoryLayerSource;
 
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that the Hierarchy defined by ConfigScope is adhered to.
 * PRODUCT < ORGANIZATION < CLUSTER < HOST < APPLICATION < USER < SESSION < RUNTIME < POLICY
 * This is an unit test, so it uses the TEST_MODE directories.
 */
class ConfigScopeHierarchyTest
{
    private ConfigFactoryBuilder builder;

    @TempDir
    Path tempBaseDir;

    @BeforeEach
    void setUp()
        {
        builder = ConfigFactoryBuilder.create("metabit", "HIERARCHY_TEST");
        // We must permit test mode before enabling it
        builder.setFeature(ConfigFeature.PERMIT_TEST_MODE, true);
        builder.setFeature(ConfigFeature.TEST_MODE, true);
        }

    @Test
    @DisplayName("Verify full scope hierarchy precedence")
    void testFullHierarchyPrecedence()
            throws IOException, ConfigCheckedException
        {
        // Verification of the Enum order itself (as used in LayeredConfiguration.add)
        assertTrue(ConfigScope.PRODUCT.ordinal() < ConfigScope.ORGANIZATION.ordinal());
        assertTrue(ConfigScope.ORGANIZATION.ordinal() < ConfigScope.CLUSTER.ordinal());
        assertTrue(ConfigScope.CLUSTER.ordinal() < ConfigScope.HOST.ordinal());
        assertTrue(ConfigScope.HOST.ordinal() < ConfigScope.APPLICATION.ordinal());
        assertTrue(ConfigScope.APPLICATION.ordinal() < ConfigScope.USER.ordinal());
        assertTrue(ConfigScope.USER.ordinal() < ConfigScope.SESSION.ordinal());
        assertTrue(ConfigScope.SESSION.ordinal() < ConfigScope.RUNTIME.ordinal());
        assertTrue(ConfigScope.RUNTIME.ordinal() < ConfigScope.POLICY.ordinal());

        // Now verify it in LayeredConfiguration
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        LayeredConfiguration layeredCfg = new LayeredConfiguration("test", null, ctx, null);
        InMemoryLayerSource ramStorage = new InMemoryLayerSource();

        // Add layers in mixed order
        for (ConfigScope scope : ConfigScope.values())
            {
            ConfigLocationImpl loc = new ConfigLocationImpl(scope, ramStorage, null, null);
            InMemoryLayer layer = new InMemoryLayer(ctx, loc, scope);
            layer.putEntry("key", new StringConfigEntryLeaf("key", "value_at_" + scope.name(), new ConfigEntryMetadata(loc)));
            layeredCfg.add(layer, loc);
            }

        // Higher scopes must win
        assertEquals("value_at_POLICY", layeredCfg.getString("key"));

        // Verify fallback
        EnumSet<ConfigScope> scopes = EnumSet.allOf(ConfigScope.class);
        scopes.remove(ConfigScope.POLICY);
        assertEquals("value_at_RUNTIME", layeredCfg.getConfigEntryFromFullKey("key", scopes).getValueAsString());
        }


    @Test
    @DisplayName("Verify newest addition wins within same scope")
    void testSameScopeNewestWins() throws IOException, ConfigCheckedException 
        {
        // In mConfigCore, we don't have FileConfigStorage or any file formats available,
        // so we test using InMemoryLayer for the precedence logic in LayeredConfiguration.
        
        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        
        // We manually create a LayeredConfiguration to test its internal 'add' logic
        LayeredConfiguration layeredCfg = new LayeredConfiguration("test", null, ctx, null);
        
        InMemoryLayerSource ramStorage = new InMemoryLayerSource();
        ConfigLocationImpl loc1 = new ConfigLocationImpl(ConfigScope.RUNTIME, ramStorage, null, null);
        ConfigLocationImpl loc2 = new ConfigLocationImpl(ConfigScope.RUNTIME, ramStorage, null, null);
        
        InMemoryLayer layer1 = new InMemoryLayer(ctx, loc1, ConfigScope.RUNTIME);
        layer1.putEntry("key", new StringConfigEntryLeaf("key", "first", new ConfigEntryMetadata(loc1)));
        
        InMemoryLayer layer2 = new InMemoryLayer(ctx, loc2, ConfigScope.RUNTIME);
        layer2.putEntry("key", new StringConfigEntryLeaf("key", "second", new ConfigEntryMetadata(loc2)));
        
        // Later added layer should win
        layeredCfg.add(layer1, loc1);
        layeredCfg.add(layer2, loc2);
        
        assertEquals("second", layeredCfg.getString("key"), 
            "The last added layer in the same scope should take precedence.");
    }
}
