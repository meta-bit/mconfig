
module metabit.mconfig.modules.yamlwithjackson
    {
    requires metabit.mconfig.core;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.yaml;

    exports org.metabit.platform.support.config.impl.format.yaml.jackson;

    uses org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with org.metabit.platform.support.config.impl.format.yaml.jackson.YAMLwithJacksonFormat;
    }
