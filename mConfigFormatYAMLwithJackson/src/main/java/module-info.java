
module metabit.mconfig.modules.yamlwithjackson
    {
    requires metabit.mconfig.core;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    exports org.metabit.platform.support.config.impl.format.yaml.jackson;

    uses org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with org.metabit.platform.support.config.impl.format.yaml.jackson.YAMLwithJacksonFormat;
    }
