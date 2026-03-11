module metabit.mconfig.format.jsonschema {
    requires transitive metabit.mconfig.core;
    requires com.networknt.schema;
    requires com.fasterxml.jackson.databind;

    exports org.metabit.platform.support.config.impl.format.jsonschema;
}
