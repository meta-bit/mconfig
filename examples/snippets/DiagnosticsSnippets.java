package com.example.snippets;

import org.metabit.platform.support.config.*;

import java.util.EnumSet;
import java.util.Set;

/**
 * Sources of the templates in examples/snippets.md. The parts between the snippet markers
 * are shown there word for word; a test compiles this file and compares the two.
 */
class DiagnosticsSnippets
{
    void diagnostics(ConfigFactory factory, Configuration cfg)
        {
        // snippet:start diagnostics
        // 1. every location searched, in resolution order
        for (ConfigLocation loc : factory.getSearchList())
            {
            System.out.printf("%-12s %-10s %s%n",
                    loc.getScope(), loc.getStorage().getStorageID(), loc.toLocationString());
            }

        // 2. every configuration actually discovered
        for (ConfigDiscoveryInfo info : factory.listAvailableConfigurations())
            {
            System.out.printf("%s @ %s (%s, scope=%s, writeable=%b)%n",
                    info.getConfigName(), info.getUri(), info.getFormatID(),
                    info.getScope(), info.isWriteable());
            }

        // 3. which layers back this one configuration
        cfg.getSourceLocations().forEach(System.out::println);

        // 4. non-fatal problems the library recorded (missing files, parse issues, permissions)
        for (ConfigEvent event : factory.getEvents())
            {
            System.out.printf("[%s] %s %s: %s%n",
                    event.getSeverity(), event.getDomain(), event.getDetailCode(), event.getMessage());
            }

        // 5. every key visible in the given scopes
        Set<String> keys = cfg.getAllConfigurationKeysFlattened(EnumSet.allOf(ConfigScope.class));

        // 6. where one value comes from
        ConfigEntry entry = cfg.getConfigEntryFromFullKey("server/port", EnumSet.allOf(ConfigScope.class));
        if (entry != null)
            {
            System.out.printf("server/port: scope=%s, location=%s%n", entry.getScope(), entry.getLocation());
            }
        // snippet:end diagnostics
        }

    ConfigFactory verboseEvents()
        {
        // snippet:start diagnostics-events
        ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
                .setFeature(ConfigFeature.EVENTS_DETAIL_LEVEL, "VERBOSE")   // OFF | FAILURES_ONLY | NORMAL | VERBOSE
                .build();
        // snippet:end diagnostics-events
        return factory;
        }
}
//___EOF___
