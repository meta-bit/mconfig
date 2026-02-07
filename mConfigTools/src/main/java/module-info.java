module mConfigTools.main
    {
    requires java.base;
    requires metabit.mconfig.core;
    requires metabit.mconfig.modules.jar;
    requires metabit.mconfig.modules.filesystem;
    requires metabit.mconfig.modules.envvar;
    requires metabit.mconfig.winregistry;
    requires metabit.mconfig.format.javaproperties;
    requires metabit.mconfig.format.ini;
    requires metabit.mconfig.modules.jsonwithjackson;
    requires metabit.mconfig.modules.yamlwithjackson;
    requires metabit.mconfig.format.toml;
    requires metabit.mconfig.util;
    requires metabit.mconfig.modules.mConfigLoggingSlf4j;
    requires info.picocli;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.dataformat.toml;
    opens org.metabit.platform.support.config.tool to info.picocli;
    }
