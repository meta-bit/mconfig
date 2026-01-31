module metabit.mconfig.secrets
    {
    requires transitive metabit.mconfig.core;

    exports org.metabit.platform.support.config.impl.secrets;

    provides org.metabit.platform.support.config.impl.ConfigFactoryComponent with org.metabit.platform.support.config.impl.secrets.SecretsFactoryComponent;
    }
