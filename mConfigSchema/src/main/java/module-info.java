import org.metabit.platform.support.config.schema.ConfigSchemaFactory;
import org.metabit.platform.support.config.schema.impl.ext.DefaultConfigSchemaFactory;

module metabit.mconfig.schema
    {
    requires transitive metabit.mconfig.core;

    exports org.metabit.platform.support.config.schema.impl.ext;
    exports org.metabit.library.format.json;

    provides ConfigSchemaFactory with DefaultConfigSchemaFactory;
    }
