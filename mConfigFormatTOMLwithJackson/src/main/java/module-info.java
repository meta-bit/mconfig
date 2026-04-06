import org.metabit.platform.support.config.impl.format.toml.jackson.TOMLwithJacksonFormat;

module metabit.mconfig.modules.tomlwithjackson
    {
    requires metabit.mconfig.modules.jsonwithjackson;
    requires transitive metabit.mconfig.core;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.toml;

    exports org.metabit.platform.support.config.impl.format.toml.jackson;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with TOMLwithJacksonFormat;
    }
