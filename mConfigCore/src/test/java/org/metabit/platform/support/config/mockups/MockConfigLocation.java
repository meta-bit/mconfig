package org.metabit.platform.support.config.mockups;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;

import java.net.URI;
import java.nio.file.Path;

/**
 * Mock implementation of ConfigLocation for testing purposes.
 */
public class MockConfigLocation implements ConfigLocation
    {
    private final ConfigScope scope;
    private final String description;

    public MockConfigLocation(ConfigScope scope, String description)
        {
        this.scope = scope;
        this.description = description;
        }

    @Override public String toLocationString() { return description; }
    @Override public URI getURI(String key, String optionalFragment) { return null; }
    @Override public ConfigScope getScope() { return scope; }
    @Override public boolean isWriteable() { return false; }
    @Override public ConfigStorageInterface getStorage() { return null; }
    @Override public Object getStorageInstanceHandle() { return null; }
    @Override public ConfigLocation derive(Path file) { return null; }
    @Override public ConfigLocation derive(URI uri) { return null; }
    @Override public String toString() { return description; }
    }
