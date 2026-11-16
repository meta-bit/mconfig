package org.metabit.platform.support.config.impl.source.kubernetes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class TokenWatcherTest
{
    private Path tempDir;
    private Path saDir;
    private Path tokenPath;
    private KubernetesConfigStorage storage;

    @BeforeEach
    void setup() throws IOException
        {
        tempDir = Files.createTempDirectory("token-watch-test");
        saDir = Files.createDirectories(tempDir.resolve("sa"));
        tokenPath = saDir.resolve("token");
        Files.writeString(tokenPath, "initial-token", StandardCharsets.UTF_8);

        storage = new KubernetesConfigStorage();
        }

    @AfterEach
    void tearDown() throws IOException
        {
        if (storage != null)
            {
            storage.exit();
            }
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignore) {}
                });
        }

    @Test
    void testTokenReload() throws Exception
        {
        ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
                .setFeature(KubernetesFeatures.BYPASS_TEST, true)
                .build();

        Field factoryCtxField = factory.getClass().getDeclaredField("ctx");
        factoryCtxField.setAccessible(true);
        ConfigFactoryInstanceContext context = (ConfigFactoryInstanceContext) factoryCtxField.get(factory);

        storage.init(context);

        // Set the path directly AFTER init() to avoid override
        Field pathField = storage.getClass().getDeclaredField("saTokenPath");
        pathField.setAccessible(true);
        pathField.set(storage, tokenPath.toAbsolutePath().toString());
        
        Field nsPathField = storage.getClass().getDeclaredField("saNamespacePath");
        nsPathField.setAccessible(true);
        nsPathField.set(storage, saDir.resolve("namespace").toAbsolutePath().toString());

        Field certPathField = storage.getClass().getDeclaredField("saCaCertPath");
        certPathField.setAccessible(true);
        certPathField.set(storage, saDir.resolve("ca.crt").toAbsolutePath().toString());

        storage.postInit(null);

        Field tokenField = storage.getClass().getDeclaredField("token");
        tokenField.setAccessible(true);
        assertArrayEquals("initial-token".getBytes(StandardCharsets.UTF_8), (byte[]) tokenField.get(storage));

        // Simulate token rotation
        Files.writeString(tokenPath, "rotated-token", StandardCharsets.UTF_8);

        // Wait for watcher to pick up change
        long start = System.currentTimeMillis();
        boolean reloaded = false;
        while (System.currentTimeMillis() - start < 5000)
            {
            byte[] currentToken = (byte[]) tokenField.get(storage);
            if (currentToken != null && java.util.Arrays.equals("rotated-token".getBytes(StandardCharsets.UTF_8), currentToken))
                {
                reloaded = true;
                break;
                }
            Thread.sleep(100);
            }

        assertTrue(reloaded, "Token was not reloaded after change");
        assertArrayEquals("rotated-token".getBytes(StandardCharsets.UTF_8), (byte[]) tokenField.get(storage));
        }

    @Test
    void testKubernetesSymlinkRotation() throws Exception
        {
        // If symlinks are not supported, we can at least test the logic by manually triggering the watcher condition
        // But let's try a simpler symlink first
        
        try {
            Path dataDir1 = Files.createDirectories(saDir.resolve("..data_dir_1"));
            Files.writeString(dataDir1.resolve("token"), "token-v1", StandardCharsets.UTF_8);
            
            Path dataSymlink = saDir.resolve("..data");
            Files.createSymbolicLink(dataSymlink, saDir.relativize(dataDir1));
            
            Files.createSymbolicLink(tokenPath, Paths.get("..data/token"));
        } catch (IOException | UnsupportedOperationException e) {
            System.err.println("Symlinks not supported in this environment, skipping symlink test: " + e.getMessage());
            return;
        }

        ConfigFactory factory = ConfigFactoryBuilder.create("metabit", "testApp")
                .setFeature(KubernetesFeatures.BYPASS_TEST, true)
                .build();

        Field factoryCtxField = factory.getClass().getDeclaredField("ctx");
        factoryCtxField.setAccessible(true);
        ConfigFactoryInstanceContext context = (ConfigFactoryInstanceContext) factoryCtxField.get(factory);

        storage.init(context);

        // Use reflection to set the path directly
        Field pathField = storage.getClass().getDeclaredField("saTokenPath");
        pathField.setAccessible(true);
        pathField.set(storage, tokenPath.toAbsolutePath().toString());

        storage.postInit(null);

        Field tokenField = storage.getClass().getDeclaredField("token");
        tokenField.setAccessible(true);
        assertArrayEquals("token-v1".getBytes(StandardCharsets.UTF_8), (byte[]) tokenField.get(storage));

        // Rotate: create new dir, update ..data symlink atomically
        Path dataDir2 = Files.createDirectories(saDir.resolve("..data_dir_2"));
        Files.writeString(dataDir2.resolve("token"), "token-v2", StandardCharsets.UTF_8);
        
        Path tmpSymlink = saDir.resolve("..data_tmp");
        Files.createSymbolicLink(tmpSymlink, saDir.relativize(dataDir2));
        Files.move(tmpSymlink, saDir.resolve("..data"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Wait for watcher
        long start = System.currentTimeMillis();
        boolean reloaded = false;
        while (System.currentTimeMillis() - start < 5000)
            {
            byte[] currentToken = (byte[]) tokenField.get(storage);
            if (currentToken != null && java.util.Arrays.equals("token-v2".getBytes(StandardCharsets.UTF_8), currentToken))
                {
                reloaded = true;
                break;
                }
            Thread.sleep(100);
            }

        assertTrue(reloaded, "Token was not reloaded after symlink rotation");
        assertArrayEquals("token-v2".getBytes(StandardCharsets.UTF_8), (byte[]) tokenField.get(storage));
        }
}
