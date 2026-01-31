
module metabit.mconfig.modules.yamlwithsnakeyaml
    {
    requires metabit.mconfig.core;
    requires org.yaml.snakeyaml;

    exports org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

    uses org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

    provides org.metabit.platform.support.config.interfaces.ConfigFormatInterface
            with org.metabit.platform.support.config.impl.format.yaml.snakeyaml.YAMLwithSnakeYAMLFormat;
    }
