package org.metabit.platform.support.config.mockups;
   /*
    make a set of solid mockups for testing purposes.
    (whether here in the core test sources, or potentially in a separate module?)
    for the latter consider a test-jar dependency like this:

    <dependency>
        <groupId>org.metabit.platform.support.config.test</groupId>
        <artifactId>module-with-test-sources</artifactId>
        <version>${project.version}</version>
        <type>test-jar</type>
        <scope>test</scope>
    </dependency>
*/
import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;
import org.metabit.platform.support.config.source.core.InMemoryLayer;
import org.metabit.platform.support.config.source.core.InMemoryLayerSource;

/**
 * a mockup of a mConfig ConfigFactory for testing purposes.
 * It creates a single layer of in-memory runtime storage.
 * <p>
 * Adding file test storages for testing purposes is possible, but not implemented here.
 */
public class MockConfigFactory extends DefaultConfigFactory implements ConfigFactory
{
    public MockConfigFactory()
        {
        super();
        // instantiate default settings
        ConfigFactorySettings cfSettings = new ConfigFactorySettings();
        ConfigFactorySettings.initDefaults(cfSettings);
        this.ctx = new ConfigFactoryInstanceContext(cfSettings);
        init(ctx);
        // normally performed in findConfigFactory during DefaultConfigFactory construction.
        this.evaluateRequirements(ctx.getSettings());
        }

    public MockConfigFactory(ConfigFactoryInstanceContext ctx)
        {
        super();
        // check and use input
        assert (ctx != null);
        this.ctx = ctx;
        // calling super.initialize(ctx); would get all the sources and layers in the search list, which we do not want here.
        init(ctx);
        // normally performed in findConfigFactory during DefaultConfigFactory construction.
        this.evaluateRequirements(ctx.getSettings());
        }

    private static void init(ConfigFactoryInstanceContext ctx)
        {
        assert (ctx.getSearchList().getEntries().isEmpty()); // we assumed the search list is empty to start with.
        // create an in-memory-storage instance
        InMemoryLayerSource testStorageInRAM = new InMemoryLayerSource();
        /// the init call here performs important internal initialization; but it also adds our InMemoryLayerSource to the search list.
        testStorageInRAM.init(ctx); // important to call init() first!
        assert (!ctx.getSearchList().getEntries().isEmpty());
        // create a location handle for it
        ConfigLocationImpl location = new ConfigLocationImpl(ConfigScope.RUNTIME, testStorageInRAM, null, null);
        ctx.getSearchList().insertAtScopeStart(location, ConfigScope.RUNTIME);
        // create a config layer for the memory storage
        InMemoryLayer testLayer = new InMemoryLayer(ctx, location, ConfigScope.RUNTIME);
        // add this to the handle which we created above.
        location.setLayer(testLayer);
        //
        }

    // @TODO here, add a function to add file test paths, with scopes.

}
