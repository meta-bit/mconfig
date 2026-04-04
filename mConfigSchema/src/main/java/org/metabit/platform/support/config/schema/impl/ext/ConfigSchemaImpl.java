package org.metabit.platform.support.config.schema.impl.ext;

import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaEntry;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigLocationImpl;
import org.metabit.platform.support.config.source.core.DefaultLayer;
import org.metabit.platform.support.config.impl.logging.NullLogging;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.GenericConfigEntryLeaf;

import java.util.*;

/**
 * Current implementation uses, for code simplicity, the approach to check every value in its String representation.
 * This may be replaced by a more efficient, yet complex, implementation which uses different types.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigSchemaImpl implements ConfigSchema
{
    private final Map<String, ConfigSchemaEntry> lut;

    private boolean immutable = false;

    private       boolean                    refuseUndefinedEntries; // if true, ignore/pass undefined entries
    private       boolean                    replaceDefaultLayer; // if true, ignore/pass undefined entries
    private       ConfigLoggingInterface     logger;
    private       String                     version;
    private final SchemaDefaultConfigStorage schemaDefaultConfigSource;

    public ConfigSchemaImpl()
        {
        this.lut = new HashMap<>();
        this.logger = NullLogging.getSingletonInstance();
        this.schemaDefaultConfigSource = new SchemaDefaultConfigStorage();
        }


    /*
    the init is at runtime, where settings are available. -- check
     */
    /** {@inheritDoc} */
    @Override
    public void init(ConfigFactoryInstanceContext ctx)
        {
        this.logger = ctx.getLogger();
        this.refuseUndefinedEntries = ctx.getSettings().getBoolean(ConfigFeature.SCHEMA_STRICT_MODE);
        this.replaceDefaultLayer = ctx.getSettings().getBoolean(ConfigFeature.SCHEMA_RESETS_DEFAULTS);
        immutable = true;
        }


    /**
     * parse inputs to ConfigSchemas
     *
     * @param jsonFormattedConfigSchema input to be parsed
     * @param ctx                       context to be parsed in
     * @return parsing result
     * @throws org.metabit.platform.support.config.ConfigCheckedException if the input is not in a valid config schema format.
     */
    public static Map<String, ConfigSchema> fromJSON(final String jsonFormattedConfigSchema, ConfigFactoryInstanceContext ctx)
            throws ConfigCheckedException
        {
        return JsonConfigSchemaParser.parseJSON(jsonFormattedConfigSchema, ctx);
        }

    /**
     * invalid entries stop the entire thing with
     *
     * @param strings an array of Strings to be parsed for the Scheme
     * @return initialized ConfigSchema
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any of the lines provided is invalid.
     */
    public static ConfigSchema fromStrings(final String[] strings)
            throws ConfigCheckedException
        {
        ConfigSchemaImpl ct = new ConfigSchemaImpl();
        if (strings != null)
            {
            for (String schemeLine : strings)
                { ConfigSchemaImpl.singleSchemefromJSON(schemeLine); }
            }
        return ct;
        }


    /**
     * <p>singleSchemefromJSON.</p>
     *
     * @param jsonFormattedConfigSchema a {@link java.lang.String} object
     * @return a {@link ConfigSchema} object
     */
    public static ConfigSchema singleSchemefromJSON(final String jsonFormattedConfigSchema)
        {
        ConfigSchemaImpl csi = new ConfigSchemaImpl();
        csi.addSchemaEntry(jsonFormattedConfigSchema);
        return csi; //return Collections.singletonList(ct); // or single-element list.
        }

    /**
     * create a ConfigSchema from prepared ConfigSchemaEntry instances.
     *
     * @param entries entries to combine
     * @return the config schema
     * @throws org.metabit.platform.support.config.ConfigCheckedException if one of the parameter is invalid
     */
    public static ConfigSchema fromEntries(Set<ConfigSchemaEntry> entries)
            throws ConfigCheckedException
        {
        ConfigSchema ct = new ConfigSchemaImpl();
        for (ConfigSchemaEntry entry : entries)
            {
            ct.addSchemaEntry(entry);
            }
        return ct;
        }


    // cli = new ConfigSchemeDefaultLayer(settings);
    /** {@inheritDoc} */
    public void transferDefaults(DefaultLayer defaultLayer)
        {
        if (this.replaceDefaultLayer)
            defaultLayer.clear();

        ConfigSource defaultLayerSchemaLocation = new ConfigLocationImpl(ConfigScope.PRODUCT, schemaDefaultConfigSource, null, schemaDefaultConfigSource);
        for (Map.Entry<String, ConfigSchemaEntry> entry : lut.entrySet())
            {
            String key = entry.getKey();
            ConfigSchemaEntry cse = entry.getValue();
            String stringDefault = cse.getDefault();
            if (stringDefault == null) continue;

            ConfigEntryMetadata meta = new ConfigEntryMetadata(defaultLayerSchemaLocation);
            meta.setSpecification(cse);
            ConfigEntry ce = new GenericConfigEntryLeaf(key, stringDefault, ConfigEntryType.STRING, meta);
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
        ConfigSchemaEntry matchingSchemeEntry = lut.get(fullKey);
        if (matchingSchemeEntry != null)
            {
            boolean res = matchingSchemeEntry.validateEntry(entry);
            // System.out.println("[DEBUG_LOG] ConfigSchemaImpl.checkConfigEntryValidity: " + fullKey + " -> " + res);
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
    public void addSchemaEntry(final String format1)
        {
        if (immutable) {
            throw new UnsupportedOperationException("ConfigSchema is immutable and cannot be modified after init().");
        }
        throw new UnsupportedOperationException("Condensed string format support removed. Use JSON or programmatic addSchemaEntry.");
        }


    /** {@inheritDoc} */
    @Override
    public void addSchemaEntry(final String fullKey, ConfigEntryType type, final String validitySchema, final Object defaultValue, final String description, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes)
            throws ConfigCheckedException
        {
        if (immutable) {
            throw new UnsupportedOperationException("ConfigSchema is immutable and cannot be modified after init().");
        }
        // 1. check fullKey for validity.
        if (fullKey == null || fullKey.isEmpty())
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.KEY_FORMAT_INVALID);
            }

        // 2. create minimal entry
        ConfigSchemaEntry tmp = new ConfigSchemaEntry(fullKey, type);

        if (validitySchema != null && !validitySchema.isEmpty())
            {
            // depending on type, parse validity schema and set in entry.
            switch (type)
                {
                case ENUM:
                case ENUM_SET:
                case STRING:
                case MULTIPLE_STRINGS:
                    tmp.setValidationPattern(validitySchema);
                    break;
                case NUMBER:
                    tmp.setValidationPattern(validitySchema);
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
    public void addSchemaEntry(ConfigSchemaEntry entry)
        {
        if (immutable) {
            throw new UnsupportedOperationException("ConfigSchema is immutable and cannot be modified after init().");
        }
        if (entry.hasUnknownMandatoryFeatures())
            {
            throw new ConfigException(ConfigException.ConfigExceptionReason.UNKNOWN_MANDATORY_FEATURE, entry.getKey());
            }
        lut.put(entry.getKey(), entry);
        }

    @Override
    public String getVersion()
        {
        return version;
        }

    public void setVersion(String version)
        {
        if (immutable)
            {
            throw new UnsupportedOperationException("ConfigSchema is immutable and cannot be modified after init().");
            }
        this.version = version;
        }

    @Override
    public boolean isCompatible(String version)
        {
        if (this.version == null || version == null) return true;
        try {
            String[] v1 = this.version.split("\\.");
            String[] v2 = version.split("\\.");
            return v1[0].equals(v2[0]); // Simple major version compatibility
        } catch (Exception e) {
            return true;
        }
        }

    @Override
    public String toJSON(String name, boolean filterHidden, boolean sanitizeSecrets)
        {
        return JsonConfigSchemaParser.generateJson(this, name, filterHidden, sanitizeSecrets);
        }

}
