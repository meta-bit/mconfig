
module metabit.mconfig.modules.jsonwithjackson
    {
    requires metabit.mconfig.core;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports org.metabit.platform.support.config.impl.format.json.jackson;

    uses org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with org.metabit.platform.support.config.impl.format.json.jackson.JSONwithJacksonFormat,
                 org.metabit.platform.support.config.impl.format.json.jackson.JSON5withJacksonFormat;
    }