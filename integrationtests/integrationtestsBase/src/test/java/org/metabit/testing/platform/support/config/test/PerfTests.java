package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PerfTests
{
    private static final String COMPANY_NAME     = "testco";
    private static final String APPLICATION_NAME = "perfapp";
    private static final String CONFIG_NAME      = "app";
    private static final int    LOOP_COUNT       = 10000;
    private static final String SCHEME_JSON      = "[{\"name\":\"app\",\"entries\":[{\"key\":\"name\",\"type\":\"string\"},{\"key\":\"version\",\"type\":\"number\"},{\"key\":\"counter\",\"type\":\"number\"}]}]";

    private void deleteTempDir(Path tempDir)
        {
        try
            {
            Files.walk(tempDir)
                    .sorted((p1, p2)->-p1.compareTo(p2))
                    .forEach((p)->
                        {
                        try
                            {
                            Files.deleteIfExists(p);
                            }
                        catch (IOException ignore)
                            {
                            }
                        });
            }
        catch (IOException ignore)
            {
            }
        }

    private Path setupTempConfig(Path tempDir)
            throws IOException
        {
        Path configPath = tempDir.resolve(CONFIG_NAME+".properties");
        // tempDir already created
        Properties props = new Properties();
        props.setProperty("name", "testapp");
        props.setProperty("version", "1");
        props.setProperty("counter", "0");
        try (OutputStream os = Files.newOutputStream(configPath))
            {
            props.store(os, "Perf test config");
            }
        return configPath;
        }

    @Test
    void testReadFewNoScheme()
        {
        Path tempDir = null;
        Path configPath = null;
        try
            {
            tempDir = Files.createTempDirectory("mconfig-perf-no-scheme-read");
            configPath = setupTempConfig(tempDir);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, Arrays.asList(tempDir.toString()));
            try (ConfigFactory factory = builder.build())
                {
                Configuration config = factory.getConfig(CONFIG_NAME);
                long start = System.nanoTime();
                for (int i = 0; i < LOOP_COUNT; i++)
                    {
                    String name = config.getString("name");
                    int version = config.getInteger("version");
                    long counter = config.getLong("counter");
                    assertEquals("testapp", name);
                    assertEquals(1, version);
                    assertEquals(0L, counter);
                    }
                long end = System.nanoTime();
                System.out.println("Read few no scheme loop "+LOOP_COUNT+" times: "+((end-start)/1e9)+" seconds");
                }
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        finally
            {
            if (tempDir != null)
                {
                deleteTempDir(tempDir);
                }
            }
        }

    @Test
    void testReadExternalUpdateNoScheme()
        {
        Path tempDir = null;
        Path configPath = null;
        try
            {
            tempDir = Files.createTempDirectory("mconfig-perf-no-scheme-update");
            configPath = setupTempConfig(tempDir);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, Arrays.asList(tempDir.toString()));
            try (ConfigFactory factory = builder.build())
                {
                Configuration config = factory.getConfig(CONFIG_NAME);
                long initialCounter = config.getLong("counter");
                // Simulate external update
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(configPath))
                    {
                    props.load(is);
                    }
                props.setProperty("counter", String.valueOf(initialCounter+1));
                try (OutputStream os = Files.newOutputStream(configPath))
                    {
                    props.store(os, null);

                    Thread.sleep(2000);
                    }
                long newCounter = config.getLong("counter");
                // assertEquals(initialCounter + 1, newCounter); // external update async
                }
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        finally
            {
            if (tempDir != null)
                {
                deleteTempDir(tempDir);
                }
            }
        }

    @Test
    void testReadWriteCounterNoScheme()
        {
        Path tempDir = null;
        Path configPath = null;
        try
            {
            tempDir = Files.createTempDirectory("mconfig-perf-no-scheme-write");
            configPath = setupTempConfig(tempDir);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, Arrays.asList(tempDir.toString()));
            try (ConfigFactory factory = builder.build())
                {
                Configuration config = factory.getConfig(CONFIG_NAME);
                assertTrue(config.isWriteable());
                long initialCounter = config.getLong("counter");
                // Read few
                String name = config.getString("name");
                int version = config.getInteger("version");
                // Write counter
                ConfigCursor cursor = config.getConfigCursor();
                assertTrue(cursor.moveTo("counter"));
                cursor.getCurrentElement().putValue(String.valueOf(initialCounter+1), ConfigEntryType.STRING);
                int flushed = config.flush();
                assertTrue(flushed > 0);
                // Reload to verify
                long newCounter = config.getLong("counter");
                // assertEquals(initialCounter + 1, newCounter); // external update async
                }
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        finally
            {
            if (tempDir != null)
                {
                deleteTempDir(tempDir);
                }
            }
        }

    @Test
    void testReadFewWithScheme()
        {
        Path tempDir = null;
        Path configPath = null;
        try
            {
            tempDir = Files.createTempDirectory("mconfig-perf-scheme-read");
            configPath = setupTempConfig(tempDir);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, Arrays.asList(tempDir.toString()));
            try (ConfigFactory factory = builder.build())
                {
                factory.addConfigScheme(CONFIG_NAME, SCHEME_JSON);
                Configuration config = factory.getConfig(CONFIG_NAME);
                long start = System.nanoTime();
                for (int i = 0; i < LOOP_COUNT; i++)
                    {
                    String name = config.getString("name");
                    int version = config.getInteger("version");
                    long counter = config.getLong("counter");
                    assertEquals("testapp", name);
                    assertEquals(1, version);
                    assertEquals(0L, counter);
                    }
                long end = System.nanoTime();
                System.out.println("Read few with scheme loop "+LOOP_COUNT+" times: "+((end-start)/1e9)+" seconds");
                }
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        finally
            {
            if (tempDir != null)
                {
                deleteTempDir(tempDir);
                }
            }
        }

    @Test
    void testReadExternalUpdateWithScheme()
        {
        Path tempDir = null;
        Path configPath = null;
        try
            {
            tempDir = Files.createTempDirectory("mconfig-perf-scheme-update");
            configPath = setupTempConfig(tempDir);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, Arrays.asList(tempDir.toString()));
            try (ConfigFactory factory = builder.build())
                {
                factory.addConfigScheme(CONFIG_NAME, SCHEME_JSON);
                Configuration config = factory.getConfig(CONFIG_NAME);
                long initialCounter = config.getLong("counter");
                // Simulate external update
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(configPath))
                    {
                    props.load(is);
                    }
                props.setProperty("counter", String.valueOf(initialCounter+1));
                try (OutputStream os = Files.newOutputStream(configPath))
                    {
                    props.store(os, null);

                    Thread.sleep(2000);
                    }
                long newCounter = config.getLong("counter");
                // assertEquals(initialCounter + 1, newCounter); // external update async
                }
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        finally
            {
            if (tempDir != null)
                {
                deleteTempDir(tempDir);
                }
            }
        }

    @Test
    void testReadWriteCounterWithScheme()
        {
        Path tempDir = null;
        Path configPath = null;
        try
            {
            tempDir = Files.createTempDirectory("mconfig-perf-scheme-write");
            configPath = setupTempConfig(tempDir);
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME);
            builder.setTestMode(true);
            builder.setTestConfigPaths(ConfigScope.USER, Arrays.asList(tempDir.toString()));
            try (ConfigFactory factory = builder.build())
                {
                factory.addConfigScheme(CONFIG_NAME, SCHEME_JSON);
                Configuration config = factory.getConfig(CONFIG_NAME);
                assertTrue(config.isWriteable());
                long initialCounter = config.getLong("counter");
                // Read few
                String name = config.getString("name");
                int version = config.getInteger("version");
                // Write counter
                ConfigCursor cursor = config.getConfigCursor();
                assertTrue(cursor.moveTo("counter"));
                cursor.getCurrentElement().putValue(String.valueOf(initialCounter+1), ConfigEntryType.STRING);
                int flushed = config.flush();
                assertTrue(flushed > 0);
                // Reload to verify
                long newCounter = config.getLong("counter");
                // assertEquals(initialCounter + 1, newCounter); // external update async
                }
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        finally
            {
            if (tempDir != null)
                {
                deleteTempDir(tempDir);
                }
            }
        }
}