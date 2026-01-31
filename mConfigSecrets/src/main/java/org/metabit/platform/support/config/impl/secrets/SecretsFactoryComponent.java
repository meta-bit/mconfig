package org.metabit.platform.support.config.impl.secrets;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.impl.entry.SecretEntryFactory;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component that initializes secrets providers and adds them to the configuration search list.
 */
public class SecretsFactoryComponent implements ConfigFactoryComponent, SecretEntryFactory
{
    private final Map<String, ConfigSecretsProviderInterface> activeSecretsProviders = new HashMap<>();

    @Override
    public String getComponentID()
    {
        return "secrets";
    }

    @Override
    public ConfigEntry createSecretEntry(String key, SecretValue value, ConfigSource source)
    {
        return new SecretConfigEntryLeaf(key, value, source);
    }

    @Override
    public boolean initialize(ConfigFactoryInstanceContext ctx)
    {
        ConfigLoggingInterface logger = ctx.getLogger();
        ConfigFactorySettings settings = ctx.getSettings();

        // 1. initialize and activate the selected secrets providers
        String secretsProviderID = settings.getString(ConfigFeature.SECRETS_PROVIDER_ID);
        if (secretsProviderID != null && !secretsProviderID.isEmpty())
        {
            initProvider(ctx, secretsProviderID, (Map<String, Object>) settings.getObject(ConfigFeature.SECRETS_PROVIDER_CONFIG, Map.class), ConfigScope.APPLICATION);
        }

        // 2. process additional secrets providers
        List<Map<String, Object>> additionalProviders = (List<Map<String, Object>>) settings.getObject(ConfigFeature.ADDITIONAL_SECRETS_PROVIDERS, List.class);
        if (additionalProviders != null)
        {
            for (Map<String, Object> entry : additionalProviders)
            {
                String providerID = (String) entry.get("provider");
                Map<String, Object> config = (Map<String, Object>) entry.get("config");
                String scopeName = (String) entry.get("scope");
                ConfigScope scope = scopeName != null ? ConfigScope.valueOf(scopeName) : ConfigScope.APPLICATION;

                initProvider(ctx, providerID, config, scope);
            }
        }

        return true;
    }

    private void initProvider(ConfigFactoryInstanceContext ctx, String providerID, Map<String, Object> config, ConfigScope scope)
    {
        ConfigLoggingInterface logger = ctx.getLogger();
        ConfigSecretsProviderInterface provider = activeSecretsProviders.get(providerID);
        if (provider == null)
        {
            provider = ctx.getConfigSecretsProviders().get(providerID);
            if (provider != null)
            {
                try
                {
                    // Use a wrapper to prevent circularity during initialization
                    if (ctx.getFactory() != null)
                    {
                        Configuration contextConfig = new ConfigurationPhaseWrapper(ctx.getFactory().getConfig(""));
                        provider.init(config, contextConfig);
                    }
                    else
                    {
                        provider.init(config);
                    }
                    activeSecretsProviders.put(providerID, provider);
                }
                catch (Exception e)
                {
                    if (logger != null) logger.error("Failed to initialize secrets provider " + providerID, e);
                    return;
                }
            }
        }

        if (provider != null)
        {
            SecretsStorage storage = new SecretsStorage(providerID, config, scope);
            ctx.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(scope, storage, null, null), scope);
        }
        else
        {
            if (logger != null) logger.warn("Secrets provider " + providerID + " requested but not found.");
        }
    }
}
