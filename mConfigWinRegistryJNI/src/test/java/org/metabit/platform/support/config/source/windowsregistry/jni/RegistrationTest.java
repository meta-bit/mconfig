package org.metabit.platform.support.config.source.windowsregistry.jni;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistrationTest {
    @Test
    public void testServiceRegistration() {
        ServiceLoader<ConfigStorageInterface> loader = ServiceLoader.load(ConfigStorageInterface.class);
        boolean found = StreamSupport.stream(loader.spliterator(), false)
                .anyMatch(storage -> storage instanceof WindowsRegistryJNISource);
        assertTrue(found, "WindowsRegistryJNISource should be registered as a ConfigStorageInterface service");
    }
}
