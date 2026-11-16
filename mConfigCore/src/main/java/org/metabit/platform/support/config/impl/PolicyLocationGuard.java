package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigScope;

/**
 * productive-mode guard for POLICY locations registered by the application.
 * <p>
 * POLICY is meant for administrator-controlled locations, which the storages derive from the operating
 * system (e.g. {@code /etc}, the Windows registry policy keys). Locations that the application registers
 * itself (directory settings with a {@code POLICY:} prefix, a secrets provider with scope POLICY,
 * {@code ConfigFactoryBuilder.addSource()}) must not silently become POLICY in productive mode.
 * They are allowed in test mode, or with {@link ConfigFeature#ALLOW_APPLICATION_POLICY_LOCATIONS}, which only code can set.
 * <p>
 * Internal implementation class.
 */
public final class PolicyLocationGuard
{
    private PolicyLocationGuard() { }

    /**
     * checks a location the application is about to register.
     *
     * @param settings    the factory settings
     * @param scope       the scope the location would be registered in
     * @param description what is being registered, for the error message
     * @throws ConfigException (ARGUMENT_REFUSED) for a POLICY location in productive mode without the opt-in
     */
    public static void checkApplicationLocation(ConfigFactorySettings settings, ConfigScope scope, String description)
        {
        if (scope != ConfigScope.POLICY)
            {
            return;
            }
        if (settings.getBoolean(ConfigFeature.TEST_MODE) || settings.getBoolean(ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS))
            {
            return;
            }
        throw new ConfigException(ConfigException.ConfigExceptionReason.ARGUMENT_REFUSED,
                "POLICY location refused: "+description+". POLICY is reserved for administrator-controlled locations; "
                +"to register one from the application, set ConfigFeature.ALLOW_APPLICATION_POLICY_LOCATIONS in code.");
        }
}
//___EOF___
