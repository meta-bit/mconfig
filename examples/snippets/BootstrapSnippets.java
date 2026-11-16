package com.example.snippets;

// snippet:start bootstrap-imports
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.Configuration;
// snippet:end bootstrap-imports
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.util.ConfigUtil;

/**
 * Sources of the templates in examples/snippets.md. The parts between the snippet markers
 * are shown there word for word; a test compiles this file and compares the two.
 */
class BootstrapSnippets
{
    void bootstrap()
        {
        // snippet:start bootstrap
        try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp").build())
            {
            Configuration net = factory.getConfig("network");

            String  host = net.getString("server/host");
            Integer port = net.getInteger("server/port");
            Boolean tls  = net.getBoolean("server/tls");

            startServer(host, port, tls);
            }
        // snippet:end bootstrap
        }

    ConfigFactory subPath()
        {
        // snippet:start bootstrap-subpath
        ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp", "eu-west/v2").build();
        // snippet:end bootstrap-subpath
        return factory;
        }

    Configuration quickConfig()
        {
        // snippet:start bootstrap-quickconfig
        Configuration net = ConfigUtil.quickConfig("ACME", "demoApp", "network");
        // snippet:end bootstrap-quickconfig
        return net;
        }

    ConfigFactory lockSchema()
        {
        // snippet:start schema-lock
        ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
                .setFeature(ConfigFeature.ALLOW_LOCAL_SCHEMA_OVERRIDE, false)
                .setFeature(ConfigFeature.SCHEMA_STRICT_MODE, true)   // reject keys absent from the schema
                .build();
        // snippet:end schema-lock
        return factory;
        }

    Configuration explicitSchema(ConfigFactory factory, ConfigSchema schema)
            throws ConfigCheckedException
        {
        // snippet:start schema-explicit
        Configuration cfg = factory.getConfig("network", schema);   // throws ConfigCheckedException
        // snippet:end schema-explicit
        return cfg;
        }

    ConfigFactory runtimeSettings()
        {
        // snippet:start env-runtime-settings
        ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
                .setFeature(ConfigFeature.ALLOW_MCONFIG_RUNTIME_SETTINGS, true)
                .build();
        // snippet:end env-runtime-settings
        return factory;
        }

    private void startServer(String host, Integer port, Boolean tls)
        {
        // stands for the application's own code
        }
}
//___EOF___
