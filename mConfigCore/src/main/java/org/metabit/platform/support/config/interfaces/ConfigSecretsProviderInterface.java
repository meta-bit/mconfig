package org.metabit.platform.support.config.interfaces;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ConfigSecretsProviderInterface
    {
    String getProviderID();
    void init(Map<String,Object> config) throws Exception;
    void init(Map<String,Object> config, org.metabit.platform.support.config.Configuration contextConfig) throws Exception;
    SecretValue getSecret(String path, Map<String,Object> opts) throws Exception;
    List<String> listSecrets(String prefix) throws Exception;
    WatchHandle watchSecret(String path, Consumer<SecretValue> callback) throws Exception;
    void renewAuth() throws Exception;

    /**
     * post-initialization step: finalize configuration using the ConfigFactory instance.
     * This allows the secrets provider to perform self-configuration using any available configurations.
     *
     * @param factory the fully functional ConfigFactory instance
     */
    default void postInit(final org.metabit.platform.support.config.ConfigFactory factory)
        {
        }
    }
