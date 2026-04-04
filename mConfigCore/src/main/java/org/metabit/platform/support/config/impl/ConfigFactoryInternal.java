package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;

/**
 * <p>ConfigFactoryInternal interface.</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public interface ConfigFactoryInternal
{
// internal use. @IMPROVEMENT? move to sub-interface. won't hide it, but abstracts better in regard to documentation.

    /*
     * internal function, not commented.
     */
    /*
 * internal instantiation step: self-evaluate whether and how well the CF can fulfull the requirements.
 *
 * @param configFactorySettings the settings
 * @return negative if it cannot do the job at all, 0 if is barely possible; positive values indicate how many additional "good" points it can make.
 */
/**
 * <p>evaluateRequirements.</p>
 *
 * @param configFactorySettings a {@link org.metabit.platform.support.config.impl.ConfigFactorySettings} object
 * @return a int
 */
int evaluateRequirements(ConfigFactorySettings configFactorySettings);

    /*
     * internal function, not commented.
     * @param ctx internal context
     */
/**
 * <p>initialize.</p>
 *
 * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
 * @return a boolean
 */
boolean initialize(ConfigFactoryInstanceContext ctx);

    /**
     * <p>prependSearchEntry.</p>
     * @param location the location to add
     * @param scope the scope for the location
     */
    void prependSearchEntry(ConfigLocation location, ConfigScope scope);

    /**
     * <p>appendSearchEntry.</p>
     * @param location the location to add
     * @param scope the scope for the location
     */
    void appendSearchEntry(ConfigLocation location, ConfigScope scope);
    void notifyConfigurationClosed(String sanitizedConfigName);
}
