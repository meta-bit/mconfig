package com.example.snippets;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.interfaces.SecretValue;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Sources of the templates in examples/snippets.md. The parts between the snippet markers
 * are shown there word for word; a test compiles this file and compares the two.
 */
class RuntimeSnippets
{
    private final Pool   connectionPool = new Pool();
    private final Log    log            = new Log();
    private final Source dataSource     = new Source();

    void environmentRead(ConfigFactory factory)
        {
        // snippet:start env-read
        Configuration net = factory.getConfig("network");
        net.getString("server/host");   // "0.0.0.0" - from SESSION, overriding the file
        // snippet:end env-read
        }

    ConfigFactory additionalDirectories()
        {
        // snippet:start container-directories
        ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
                .setFeature(ConfigFeature.ADDITIONAL_RUNTIME_DIRECTORIES,
                            List.of("HOST:/etc/demoapp", "USER:/var/run/demoapp/config"))
                .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 2000)
                .build();
        // snippet:end container-directories
        return factory;
        }

    void hotReload()
        {
        // snippet:start reload-subscribe
        try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
                .setFeature(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS, 1000)
                .build())
            {
            Configuration net = factory.getConfig("network");

            java.util.function.Consumer<ConfigLocation> onChange = location ->
                {
                log.info("configuration changed at {}", location.toLocationString());
                connectionPool.resize(net.getInteger("pool/size"));
                };

            net.subscribeToUpdates(onChange);
            // ... later, on component shutdown:
            net.unsubscribeFromUpdates(onChange);
            }
        // snippet:end reload-subscribe
        }

    void hotReloadPerKey(Configuration net)
        {
        // snippet:start reload-subscribe-key
        net.subscribeToUpdates("pool/size", location -> connectionPool.resize(net.getInteger("pool/size")));
        // snippet:end reload-subscribe-key
        }

    void secretAsChars(Configuration cfg, String user)
        {
        // snippet:start secret-chars
        SecretValue password = cfg.getSecret("db/password");
        if (password != null)
            {
            char[] chars = password.getChars();
            try
                {
                dataSource.connect(user, chars);
                }
            finally
                {
                java.util.Arrays.fill(chars, '\0');
                password.erase();          // zero the library-side buffer
                }
            }
        // snippet:end secret-chars
        }

    void secretAsBytes(Configuration cfg)
            throws GeneralSecurityException
        {
        // snippet:start secret-bytes
        SecretValue keySecret = cfg.getSecret("server/privateKey");
        byte[]      keyBytes  = keySecret.getValue();
        try
            {
            PrivateKey key = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            useKey(key);
            }
        finally
            {
            java.util.Arrays.fill(keyBytes, (byte) 0);
            keySecret.erase();
            }
        // snippet:end secret-bytes
        }

    void write(ConfigFactory factory, String secretText)
            throws ConfigCheckedException
        {
        // snippet:start write-scope
        Configuration cfg = factory.getConfig("network");

        cfg.put("server/port", 9090, ConfigScope.USER);
        cfg.flush();                                        // throws ConfigCheckedException
        // snippet:end write-scope

        // snippet:start write-scopes
        cfg.put("server/port", 9090, EnumSet.of(ConfigScope.HOST, ConfigScope.USER));
        // snippet:end write-scopes

        // snippet:start write-schema-scope
        cfg.put("server/password", secretText);             // no scope argument
        // snippet:end write-schema-scope
        }

    ConfigFactory createMissing()
        {
        // snippet:start write-create
        ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "demoApp")
                .setFeature(ConfigFeature.AUTOMATIC_CONFIG_CREATION, true)  // create the config file
                .setFeature(ConfigFeature.CREATE_MISSING_PATHS, true)       // create parent directories
                .setFeature(ConfigFeature.WRITE_SYNC, true)                 // write through immediately
                .build();
        // snippet:end write-create
        return factory;
        }

    Path whereToPut(ConfigFactory factory)
            throws ConfigCheckedException
        {
        // snippet:start write-where
        Path dir = ConfigUtil.whereToPutMyFiles(factory, ConfigScope.USER, true);   // throws ConfigCheckedException
        // snippet:end write-where
        return dir;
        }

    void adapters(Configuration cfg, Properties legacyProperties)
        {
        // snippet:start adapters
        // java.util.Properties -> Configuration (read-only view)
        Configuration fromProps = ConfigUtil.fromProperties(legacyProperties);

        // Configuration -> java.util.Properties, prefix stripped from the keys
        Properties dbProps = ConfigUtil.copyPrefixedEntriesToJavaProperties(cfg, "db.");

        // key remapping: view.getString("db.host") reads source.getString("super_db.host")
        Configuration view = ConfigUtil.remapped(cfg, "super_db.", "db.");

        // ad-hoc overlay (read-only). Prefer put(..., ConfigScope.RUNTIME) when you need to change values.
        Configuration overridden = ConfigUtil.withOverrides(cfg, Map.of("server/port", 9999));
        // snippet:end adapters
        }

    void testModeDirectories(ConfigFactoryBuilder builder, Path tempDir, Path otherDir)
        {
        // snippet:start test-directories
        builder.setFeature(ConfigFeature.TESTMODE_DIRECTORIES,
                           List.of("USER:" + tempDir, "HOST:" + otherDir));
        // snippet:end test-directories
        }

    void forbidTestMode()
        {
        // snippet:start test-forbid
        ConfigFactoryBuilder.forbidTestMode();   // static, JVM-wide; call early in main()
        // snippet:end test-forbid
        }

    private void useKey(PrivateKey key)
        {
        // stands for the application's own code
        }

    // the three types below stand for the application's own classes
    static class Pool
    {
        void resize(Integer size) { }
    }

    static class Log
    {
        void info(String format, Object argument) { }
    }

    static class Source
    {
        void connect(String user, char[] password) { }
    }
}
//___EOF___
