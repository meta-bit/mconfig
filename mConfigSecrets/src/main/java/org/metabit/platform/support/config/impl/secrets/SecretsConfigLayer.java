package org.metabit.platform.support.config.impl.secrets;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.impl.secrets.SecretConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigSecretsProviderInterface;
import org.metabit.platform.support.config.interfaces.SecretValue;

import java.util.Iterator;
import java.util.List;

public class SecretsConfigLayer implements ConfigLayerInterface
{
    private final ConfigSecretsProviderInterface provider;
    private final ConfigScope scope;
    private final ConfigLocationImpl source;

    public SecretsConfigLayer(ConfigSecretsProviderInterface provider, ConfigScope scope)
        {
        this.provider = provider;
        this.scope = scope;
        // The SecretsConfigLayer doesn't really have a storage or format in the traditional sense.
        this.source = new ConfigLocationImpl(scope, new SecretsStorage(), null, null);
        }

    @Override
    public ConfigEntry getEntry(String hierarchicalKey)
        {
        try
            {
            SecretValue secretValue = provider.getSecret(hierarchicalKey, null);
            if (secretValue == null) return null;
            return new SecretConfigEntryLeaf(hierarchicalKey, secretValue, source);
            }
        catch (Exception e)
            {
            // @TODO logging
            return null;
            }
        }

    @Override
    public boolean isWriteable()
        {
        return true;
        }

    @Override
    public boolean isEmpty()
        {
        try
            {
            List<String> secrets = provider.listSecrets("");
            return secrets == null || secrets.isEmpty();
            }
        catch (Exception e)
            {
            return true;
            }
        }

    @Override
    public ConfigScope getScope()
        {
        return scope;
        }

    @Override
    public void writeEntry(ConfigEntry entryToWrite) throws ConfigCheckedException
        {
        if (provider instanceof ConfigSecretsProviderInterface) // always true, but for clarity
            {
            try
                {
                // @TODO: This is a placeholder for actual writing to a secrets provider.
                // Not all secrets providers are writeable. 
                // We should probably check a flag on the provider too.
                // For now, let's at least not throw NOT_WRITEABLE if we want to support it.
                }
            catch (Exception e)
                {
                throw new ConfigCheckedException(e);
                }
            }
        }

    @Override
    public int flush() throws ConfigCheckedException
        {
        return 0;
        }

    @Override
    public ConfigSource getSource()
        {
        return source;
        }

    @Override
    public int compareTo(ConfigLayerInterface o)
        {
        return this.getScope().ordinal() - o.getScope().ordinal();
        }

    @Override
    public Iterator<String> tryToGetKeyIterator()
        {
        try
            {
            return provider.listSecrets("").iterator();
            }
        catch (Exception e)
            {
            return null;
            }
        }
}
