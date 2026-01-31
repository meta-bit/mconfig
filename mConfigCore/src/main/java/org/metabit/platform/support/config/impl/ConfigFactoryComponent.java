package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

/**
 * Interface for optional components that need to hook into the ConfigFactory initialization process.
 */
public interface ConfigFactoryComponent
{
    /**
     * @return a unique ID for this component.
     */
    String getComponentID();

    /**
     * Initialize the component.
     * @param ctx the factory instance context
     * @return true if successful
     */
    boolean initialize(ConfigFactoryInstanceContext ctx);

    /**
     * Called when the factory is being closed.
     */
    default void close() {}
}
