package org.metabit.testing.platform.support.config.test.valkey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIf("isDockerAvailable")
public class TestValkeyPresence
{

    // use Valkey official image; pin a known tag in CI for stability
    @Container
    public static GenericContainer<?> valkey =
            new GenericContainer<>("valkey/valkey:latest")
                    .withExposedPorts(6379);

    private static boolean isDockerAvailable()
        {
        try
            {
            return DockerClientFactory.instance().isDockerAvailable();
            }
        catch (Exception e)
            {
            return false;
            }
        }

    private Jedis jedis;

    @BeforeEach
    void setUp()
        {
        String host = valkey.getHost();
        Integer port = valkey.getFirstMappedPort();
        jedis = new Jedis(host, port);
        // wait/verify connection
        String pong = jedis.ping();
        if (!"PONG".equals(pong))
            {
            throw new IllegalStateException("Valkey did not respond to PING");
            }
        jedis.flushAll(); // ensure clean DB for each test
        }

    @AfterEach
    void tearDown()
        {
        if (jedis != null)
            {
            jedis.close();
            }
        }

    @Test
    void simpleSetGet()
        {
        jedis.set("key:a", "1");
        String v = jedis.get("key:a");
        assertEquals("1", v);
        }

    @Test
    void incrAtomicity()
        {
        jedis.set("counter", "0");
        jedis.incr("counter");
        String v = jedis.get("counter");
        assertEquals("1", v);
        }
}
