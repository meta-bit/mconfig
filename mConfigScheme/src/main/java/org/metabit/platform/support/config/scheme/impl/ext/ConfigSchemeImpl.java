package org.metabit.platform.support.config.scheme.impl.ext;

import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.scheme.ConfigSchemeEntry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.source.core.DefaultLayer;
import org.metabit.platform.support.config.impl.core.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.ConfigStorageInterface;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Current implementation uses, for code simplicity, the approach to check every value in its String representation.
 * This may be replaced by a more efficient, yet complex, implementation which uses different types.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigSchemeImpl implements ConfigScheme
{
    private final Map<String, ConfigSchemeEntry> lut;

    private boolean immutable = false;

    private       boolean                    refuseUndefinedEntries; // if true, ignore/pass undefined entries
    private       boolean                    replaceDefaultLayer; // if true, ignore/pass undefined entries
    private       ConfigLoggingInterface     logger;
    private final SchemeDefaultConfigStorage schemeDefaultConfigSource;

    public ConfigSchemeImpl()
        {
        this.lut = new HashMap<>();
        this.logger = NullLogging.getSingletonInstance();
        this.schemeDefaultConfigSource = new SchemeDefaultConfigStorage();
        }


    /*
    the init is at runtime, where settings are available. -- check
     */
    /** {@inheritDoc} */
    @Override
    public void init(ConfigFactoryInstanceContext ctx)
        {
        this.logger = ctx.getLogger();
        this.refuseUndefinedEntries = ctx.getSettings().getBoolean(ConfigFeature.SCHEME_STRICT_MODE);
        this.replaceDefaultLayer = ctx.getSettings().getBoolean(ConfigFeature.SCHEME_RESETS_DEFAULTS);
        immutable = true;
        }


    /**
     * parse inputs to ConfigSchemes
     *
     * @param jsonFormattedConfigScheme input to be parsed
     * @param ctx                       context to be parsed in
     * @return parsing result
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the input is not in a valid config scheme format.
     */
    public static Map<String, ConfigScheme> fromJSON(final String jsonFormattedConfigScheme, ConfigFactoryInstanceContext ctx)
            throws ConfigCheckedException
        {
        return JsonConfigSchemeParser.parseJSON(jsonFormattedConfigScheme, ctx);
        }

    /**
     * invalid entries stop the entire thing with
     *
     * @param strings an array of Strings to be parsed for the Scheme
     * @return initialized ConfigScheme
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any of the lines provided is invalid.
     */
    public static ConfigScheme fromStrings(final String[] strings)
            throws ConfigCheckedException
        {
        ConfigSchemeImpl ct = new ConfigSchemeImpl();
        if (strings != null)
            {
            for (String schemeLine : strings)
                { ConfigSchemeImpl.singleSchemefromJSON(schemeLine); }
            }
        return ct;
        }


    /**
     * <p>singleSchemefromJSON.</p>
     *
     * @param jsonFormattedConfigScheme a {@link java.lang.String} object
     * @return a {@link org.metabit.platform.support.config.scheme.ConfigScheme} object
     */
    public static ConfigScheme singleSchemefromJSON(final String jsonFormattedConfigScheme)
        {
        ConfigSchemeImpl csi = new ConfigSchemeImpl();
        csi.addSchemeEntry(jsonFormattedConfigScheme);
        return csi; //return Collections.singletonList(ct); // or single-element list.
        }

    /**
     * create a ConfigScheme from prepared ConfigSchemeEntry instances.
     *
     * @param entries entries to combine
     * @return the config scheme
     * @throws org.metabit.platform.support.config.ConfigCheckedException if one of the parameter is invalid
     */
    public static ConfigScheme fromEntries(Set<ConfigSchemeEntry> entries)
            throws ConfigCheckedException
        {
        ConfigScheme ct = new ConfigSchemeImpl();
        for (ConfigSchemeEntry entry : entries)
            {
            ct.addSchemeEntry(entry);
            }
        return ct;
        }


    // cli = new ConfigSchemeDefaultLayer(settings);
    /** {@inheritDoc} */
    public void transferDefaults(DefaultLayer defaultLayer)
        {
        if (this.replaceDefaultLayer)
            defaultLayer.clear();


        //  @TODO CHECK, old comment was: ConfigLocationImpl lacks the correct constructor - we don't have a file "Path"; and the correct value for writeFlag would be a brownian "mu".
        ConfigSource defaultLayerSchemeLocation = new ConfigLocationImpl(ConfigScope.PRODUCT, schemeDefaultConfigSource, null, schemeDefaultConfigSource);

        for (Map.Entry<String, ConfigSchemeEntry> entry : lut.entrySet())
            {
            String key = entry.getKey();
            ConfigSchemeEntry cse = entry.getValue();
            String stringDefault = cse.getDefault();
            if (stringDefault == null) continue;

            ConfigEntryMetadata meta = new ConfigEntryMetadata(defaultLayerSchemeLocation);
            meta.setSpecification(cse);
            ConfigEntry ce = new StringConfigEntryLeaf(key, stringDefault, meta);
            defaultLayer.putEntry(key, ce);
            }
        return;
        }

    /** {@inheritDoc} */
    @Override
    public Set<String> getEntryKeys()
        {
        return Collections.unmodifiableSet(lut.keySet());
        }

    @Override
    public ConfigEntrySpecification getSpecification(String fullKey)
        {
        return lut.get(fullKey);
        }


    /** {@inheritDoc} */
    @Override
    public boolean checkConfigEntryValidity(final String fullKey, final ConfigEntry entry)
        {
        ConfigSchemeEntry matchingSchemeEntry = lut.get(fullKey);
        if (matchingSchemeEntry != null)
            {
            boolean res = matchingSchemeEntry.validateEntry(entry);
            // System.out.println("[DEBUG_LOG] ConfigSchemeImpl.checkConfigEntryValidity: " + fullKey + " -> " + res);
            return res;
            }
        else // no matching Scheme entry found for this key
            {
            if (refuseUndefinedEntries)
                {
                if (logger != null)
                    logger.debug("config entry not defined for key \""+fullKey+"\"");
                return false;
                }
            return true;
            }
        }

    @Override
    public void addSchemeEntry(final String format1)
        {
        if (immutable) {
            throw new UnsupportedOperationException("ConfigScheme is immutable and cannot be modified after init().");
        }
        throw new UnsupportedOperationException("Condensed string format support removed. Use JSON or programmatic addSchemeEntry.");
        }


    /** {@inheritDoc} */
    @Override
    public void addSchemeEntry(final String fullKey, ConfigEntryType type, final String validityScheme, final Object defaultValue, final String description, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes)
            throws ConfigCheckedException
        {
        if (immutable) {
            throw new UnsupportedOperationException("ConfigScheme is immutable and cannot be modified after init().");
        }
        // 1. check fullKey for validity.
        if (fullKey == null || fullKey.isEmpty())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.KEY_FORMAT_INVALID);
            }

        // 2. create minimal entry
        ConfigSchemeEntry tmp = new ConfigSchemeEntry(fullKey, type);

        if (validityScheme != null && !validityScheme.isEmpty())
            {
            // depending on type, parse validity Scheme and set in entry.
            switch (type)
                {
                case ENUM:
                case ENUM_SET:
                case STRING:
                case MULTIPLE_STRINGS:
                    tmp.setValidationPattern(validityScheme);
                    break;
                case NUMBER:
                    tmp.setValidationPattern(validityScheme);
                    break;
                case BOOLEAN:
                case BYTES:
                default:
                    break;
                }
            }

        if (defaultValue != null)
            {
            String stringDefault;
            switch (type)
                {
                case ENUM:
                case STRING:
                    if (!(defaultValue instanceof String))
                        { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.OBJECT_CLASS_MISMATCHING_WITH_TYPE); }
                    stringDefault = (String) defaultValue;
                    break;
                case BOOLEAN:
                    if (!(defaultValue instanceof Boolean))
                        { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.OBJECT_CLASS_MISMATCHING_WITH_TYPE); }
                    stringDefault = defaultValue.toString();
                    break;
                case NUMBER:
                    if (!(defaultValue instanceof Number))
                        { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.OBJECT_CLASS_MISMATCHING_WITH_TYPE); }
                    stringDefault = defaultValue.toString();
                    break;
                case BYTES:
                    if (defaultValue instanceof byte[])
                        {
                        byte[] value = (byte[]) defaultValue;
                        stringDefault = "base64:"+Base64.getEncoder().encodeToString(value);
                        }
                    else
                        { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.OBJECT_CLASS_MISMATCHING_WITH_TYPE); }
                    break;
                case ENUM_SET:
                case MULTIPLE_STRINGS:
                    if (defaultValue instanceof List)
                        {
                        // simplistic implementation for now
                        stringDefault = defaultValue.toString();
                        }
                    else
                        { throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.OBJECT_CLASS_MISMATCHING_WITH_TYPE); }
                    break;
                default:
                    stringDefault = defaultValue.toString();
                }
            tmp.setDefault(stringDefault);
            }

        if (description != null)
            {
            tmp.setDescription(description);
            }
        if (flags != null)
            tmp.setFlags(flags);
        if (scopes != null)
            tmp.setScopes(scopes);
        lut.put(fullKey, tmp);
        return;
        }

    /** {@inheritDoc} */
    @Override
    public void addSchemeEntry(ConfigSchemeEntry entry)
        {
        if (immutable) {
            throw new UnsupportedOperationException("ConfigScheme is immutable and cannot be modified after init().");
        }
        if (entry.hasUnknownMandatoryFeatures())
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.UNKNOWN_MANDATORY_FEATURE, entry.getKey());
            }
        lut.put(entry.getKey(), entry);
        }


}
