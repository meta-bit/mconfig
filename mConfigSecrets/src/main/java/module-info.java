import org.metabit.platform.support.config.impl.secrets.SecretsFactoryComponent;
import org.metabit.platform.support.config.interfaces.ConfigFactoryComponent;

module metabit.mconfig.secrets
    {
    requires transitive metabit.mconfig.core;

    exports org.metabit.platform.support.config.impl.secrets;

    provides ConfigFactoryComponent with SecretsFactoryComponent;
    }
