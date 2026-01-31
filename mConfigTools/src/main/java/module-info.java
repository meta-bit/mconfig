module mConfigTools.main
    {
    requires java.base;
    requires metabit.mconfig.core;
    requires info.picocli;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.toml;
    requires com.fasterxml.jackson.dataformat.yaml;
    opens org.metabit.platform.support.config.tool to info.picocli;
    }
