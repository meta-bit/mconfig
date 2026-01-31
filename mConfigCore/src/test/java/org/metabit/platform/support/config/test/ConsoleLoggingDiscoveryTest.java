package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.impl.logging.ConsoleLogging;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ServiceLoader discovery for ConsoleLogging.
 */
public class ConsoleLoggingDiscoveryTest
{
    @Test
    public void testConsoleLoggingIsDiscovered()
        {
        ServiceLoader<ConfigLoggingInterface> loaders = ServiceLoader.load(ConfigLoggingInterface.class);

        Optional<ServiceLoader.Provider<ConfigLoggingInterface>> consoleProvider = loaders.stream()
                .filter(provider -> {
                    ConfigLoggingInterface logger = provider.get();
                    return "console".equals(logger.getServiceModuleName());
                })
                .findFirst();

        assertTrue(consoleProvider.isPresent(), "ConsoleLogging Provider should be discoverable");
        ConfigLoggingInterface consoleLogger = consoleProvider.get().get();
        assertInstanceOf(ConsoleLogging.class, consoleLogger);
        }
}