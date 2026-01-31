module metabit.mconfig.scheme
    {
    requires transitive metabit.mconfig.core;

    exports org.metabit.platform.support.config.scheme.impl.ext;
    exports org.metabit.library.format.json;

    provides org.metabit.platform.support.config.scheme.ConfigSchemeFactory with org.metabit.platform.support.config.scheme.impl.ext.DefaultConfigSchemeFactory;
    }
