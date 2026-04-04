package org.metabit.platform.support.config.interfaces;

import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;

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
     * post-initialization step: finalize configuration using the ConfigFactory instance.
     * This allows the component to perform tasks that require a fully functional ConfigFactory.
     *
     * @param factory the fully functional ConfigFactory instance
     */
    default void postInit(final org.metabit.platform.support.config.ConfigFactory factory)
        {
        }

    /**
     * Called when the factory is being closed.
     */
    default void close() {}
}
