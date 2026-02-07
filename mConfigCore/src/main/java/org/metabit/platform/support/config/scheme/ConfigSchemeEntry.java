package org.metabit.platform.support.config.scheme;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.StringConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.scheme.impl.*;

import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>ConfigSchemeEntry class.</p>
 *
 * @version $Id: $Id
 */
public class ConfigSchemeEntry implements ConfigEntrySpecification
{
    public  ConfigEntryType                                                   type;   // from enum
    public  String                                                            key; // the name/key this is referenced by
    public  String                                                            description;
    private Map<String, String>                                               descriptions;
    public  EnumSet<ConfigEntry.ConfigEntryFlags>                             flags;
    public  Pattern                                                           validationPattern;
    public  String                                                            defaultValue;
    private EnumSet<ConfigScope>                                              scopes;
    private boolean                                                           isSecret                    = false; // contains sensitive data
    private boolean                                                           isHidden                    = false; // hidden from documentation
    private boolean                                                           hasUnknownMandatoryFeatures = false;
    private int                                                               minArity                    = -1; // -1 means use default from isMandatory()
    private int                                                               maxArity                    = 0;  // 0 means use default from type
    private RangeValidator                                                    rangeValidator;
    private EnumValidator                                                     enumValidator;
    private String                                                            enumPattern;
    private FilePathValidator                                                 filePathValidator;
    private org.metabit.platform.support.config.scheme.impl.TemporalValidator temporalValidator;
    private Map<String, Object>                                               temporalValidationFlags;
    private Map<String, Object>                                               pathValidationFlags;
    private EmailValidator                                                    emailValidator;
    private DurationValidator                                                 durationValidator;
    private SizeValidator                                                     sizeValidator;

    /**
     * <p>Constructor for ConfigSchemeEntry.</p>
     *
     * @param fullKey a {@link java.lang.String} object
     * @param type    a {@link org.metabit.platform.support.config.ConfigEntryType} object
     */
    public ConfigSchemeEntry(String fullKey, ConfigEntryType type)
        {
        this.type = type;
        this.key = fullKey;
        this.flags = EnumSet.noneOf(ConfigEntry.ConfigEntryFlags.class);
        this.scopes = EnumSet.allOf(ConfigScope.class);
        this.description = "";
        this.validationPattern = null;
        this.defaultValue = null;
        }


    public ConfigSchemeEntry()
        {
        this.flags = EnumSet.noneOf(ConfigEntry.ConfigEntryFlags.class);
        this.scopes = EnumSet.allOf(ConfigScope.class);
        this.description = "";
        } // internal use only, by the scheme parsers


    /**
     * <p>setDefault.</p>
     *
     * @param defaultStringValue a {@link java.lang.String} object
     * @return a {@link org.metabit.platform.support.config.scheme.ConfigSchemeEntry} object
     *
     * @throws org.metabit.platform.support.config.ConfigCheckedException if any.
     */
    public ConfigSchemeEntry setDefault(String defaultStringValue)
            throws ConfigCheckedException
        {
        if (defaultStringValue == null)
            {
            this.defaultValue = null;
            }
        else
            {
            if (validationPattern != null)
                {
                if (!validationPattern.matcher(defaultStringValue).matches())
                    {
                    throw new ConfigException(ConfigException.ConfigExceptionReason.DEFAULT_VALIDATOR_MISMATCH);
                    }
                }
            // Pattern already validated above
            this.defaultValue = defaultStringValue;
            }
        return this;
        }

    /**
     * <p>validateEntry.</p>
     *
     * @param entry a {@link org.metabit.platform.support.config.ConfigEntry} object
     * @return a boolean
     */
    public boolean validateEntry(final ConfigEntry entry)
        {
        if (type != entry.getType()) return false;

        // Arity validation for list types
        if (type == ConfigEntryType.MULTIPLE_STRINGS || type == ConfigEntryType.ENUM_SET)
            {
            try
                {
                java.util.List<String> list = entry.getValueAsStringList();
                int size = (list == null) ? 0 : list.size();
                int min = getMinArity();
                int max = getMaxArity();
                if (size < min) return false;
                if (max != -1 && size > max) return false;
                }
            catch (ConfigCheckedException e)
                {
                return false;
                }
            }

        // Basic type validation
        try
            {
            switch (type)
                {
                case NUMBER:
                    entry.getValueAsBigDecimal(); // Just check if it's a valid number
                    break;
                case BOOLEAN:
                    entry.getValueAsBoolean();
                    break;
                case ENUM_SET:
                    entry.getValueAsStringList();
                    break;
                case ENUM:
                case STRING:
                case URI:
                case FILEPATH:
                case DATE:
                case TIME:
                case DATETIME:
                    entry.getValueAsString();
                    break;
                default:
                    break;
                }
            }
        catch (ConfigCheckedException|NumberFormatException ex)
            {
            return false;
            }

        if (type == ConfigEntryType.URI)
            {
            try { entry.getValueAsURI(); }
            catch (ConfigCheckedException e) { return false; }
            }
        else if (type == ConfigEntryType.FILEPATH)
            {
            try { entry.getValueAsPath(); }
            catch (ConfigCheckedException e) { return false; }
            }
        else if (type == ConfigEntryType.DATE)
            {
            try { entry.getValueAsLocalDate(); }
            catch (ConfigCheckedException e) { return false; }
            }
        else if (type == ConfigEntryType.TIME)
            {
            try { entry.getValueAsLocalTime(); }
            catch (ConfigCheckedException e) { return false; }
            }
        else if (type == ConfigEntryType.DATETIME)
            {
            try { entry.getValueAsOffsetDateTime(); }
            catch (ConfigCheckedException|DateTimeParseException e)
                {
                try { entry.getValueAsLocalDateTime(); }
                catch (ConfigCheckedException|DateTimeParseException e2)
                    {
                    try { entry.getValueAsZonedDateTime(); }
                    catch (ConfigCheckedException|DateTimeParseException e3) { return false; }
                    }
                }
            }
        else if (type == ConfigEntryType.DURATION)
            {
            try { entry.getValueAsDuration(); }
            catch (ConfigCheckedException|java.time.format.DateTimeParseException e) { return false; }
            }

        if (rangeValidator != null)
            {
            if (!rangeValidator.validate(entry)) return false;
            }

        if (enumValidator != null)
            {
            if (!enumValidator.validate(entry)) return false;
            }

        if (filePathValidator != null)
            {
            if (!filePathValidator.validate(entry)) return false;
            }

        if (temporalValidator != null)
            {
            if (!temporalValidator.validate(entry)) return false;
            }

        if (emailValidator != null)
            {
            if (!emailValidator.validate(entry)) return false;
            }
        if (durationValidator != null)
            {
            if (!durationValidator.validate(entry)) return false;
            }
        if (sizeValidator != null)
            {
            if (!sizeValidator.validate(entry)) return false;
            }

        if (validationPattern != null)
            {
            try
                {
                if (!validationPattern.matcher(entry.getValueAsString()).matches())
                    return false;
                }
            catch (ConfigCheckedException cex)
                {
                return false;
                }
            }
        return true;
        }

    /**
     * Checks whether the configuration entry is marked as hidden from
     * automatic documentation (e.g. commandline --help features)
     *
     * @return true if the configuration entry is hidden, false otherwise.
     */
    @Override
    public boolean isHidden()
        {
        return false;
        }


    /**
     * <p>Setter for the field <code>scopes</code>.</p>
     *
     * @param scopes a {@link java.util.EnumSet} object
     */
    public void setScopes(EnumSet<ConfigScope> scopes) { this.scopes = scopes; }

    /** {@inheritDoc} */
    @Override
    public String toString() { return key+":"+type; }

    /**
     * <p>Getter for the field <code>key</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getKey() { return key; }

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link org.metabit.platform.support.config.ConfigEntryType} object
     */
    public ConfigEntryType getType() { return type; }

    /**
     * <p>Getter for the field <code>scopes</code>.</p>
     *
     * @return a {@link java.util.EnumSet} object
     */
    public EnumSet<ConfigScope> getScopes() { return scopes; }

    /**
     * <p>getDefault.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getDefault() { return defaultValue; }

    /**
     * <p>Getter for the field <code>flags</code>.</p>
     *
     * @return a {@link java.util.EnumSet} object
     */
    public EnumSet<ConfigEntry.ConfigEntryFlags> getFlags() { return flags; }

    /**
     * <p>Getter for the field <code>description</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getDescription() { return description; }

    @Override
    public String getDescription(Locale locale)
        {
        if (descriptions != null && locale != null)
            {
            String localized = descriptions.get(locale.getLanguage());
            if (localized != null) return localized;
            }

        if (description == null || description.isEmpty()) return description;
        try
            {
            // We search for messages.properties in standard locations
            // This is a simplified lookup for now.
            ResourceBundle bundle = ResourceBundle.getBundle(".config.messages", locale);
            if (bundle.containsKey(description))
                {
                return bundle.getString(description);
                }
            }
        catch (Exception e)
            {
            // Fallback to plain string if no bundle found or error
            }
        return description;
        }

    /**
     * <p>Getter for the field <code>validationPattern</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getValidationPattern() { return (validationPattern != null) ? this.validationPattern.pattern() : null; }

    /**
     * <p>Setter for the field <code>description</code>.</p>
     *
     * @param description a {@link java.lang.String} object
     * @return a {@link org.metabit.platform.support.config.scheme.ConfigSchemeEntry} object
     */
    public ConfigSchemeEntry setDescription(final String description)
        {
        this.description = (description != null) ? description : "";
        return this;
        }

    /**
     * <p>Set description for a specific language.</p>
     *
     * @param lang language code (e.g. "en", "de")
     * @param text description text
     * @return this entry
     */
    public ConfigSchemeEntry setDescription(String lang, String text)
        {
        if (descriptions == null)
            {
            descriptions = new HashMap<>();
            }
        descriptions.put(lang, text);
        return this;
        }

    /**
     * <p>Set multiple localized descriptions at once.</p>
     *
     * @param descriptions map of language codes to description texts
     * @return this entry
     */
    public ConfigSchemeEntry setDescriptions(Map<String, String> descriptions)
        {
        this.descriptions = descriptions;
        return this;
        }

    /**
     * <p>Setter for the field <code>flags</code>.</p>
     *
     * @param flags a {@link java.util.EnumSet} object
     * @return a {@link org.metabit.platform.support.config.scheme.ConfigSchemeEntry} object
     */
    public ConfigSchemeEntry setFlags(EnumSet<ConfigEntry.ConfigEntryFlags> flags)
        {
        if (flags != null) { this.flags = flags; }
        else { this.flags = EnumSet.noneOf(ConfigEntry.ConfigEntryFlags.class); }
        return this;
        }

    /**
     * <p>setValidator.</p>
     *
     * @param validator a {@link java.util.regex.Pattern} object
     * @return a {@link org.metabit.platform.support.config.scheme.ConfigSchemeEntry} object
     */
    public ConfigSchemeEntry setValidator(final Pattern validator)
        {
        if (validator != null) this.validationPattern = validator;
        return this;
        }

    /**
     * Set a validation pattern string (range, enum, or regex).
     *
     * @param pattern pattern string
     * @return this entry
     */
    public ConfigSchemeEntry setValidationPattern(String pattern)
        {
        if (pattern == null || pattern.isEmpty()) return this;

        String p = pattern.trim();
        if (p.equals("email") && (type == ConfigEntryType.STRING || type == ConfigEntryType.MULTIPLE_STRINGS))
            {
            this.emailValidator = new EmailValidator();
            return this;
            }
        if (p.equals("size") && type == ConfigEntryType.NUMBER)
            {
            this.sizeValidator = new SizeValidator();
            return this;
            }
        if (p.equals("duration") && type == ConfigEntryType.DURATION)
            {
            this.durationValidator = new DurationValidator();
            return this;
            }
        if (p.equals("port") && type == ConfigEntryType.NUMBER)
            {
            this.rangeValidator = new PortValidator();
            return this;
            }

        if (type == ConfigEntryType.NUMBER)
            {
            RangeValidator rv = new RangeValidator(pattern);
            if (rv.isValid())
                {
                this.rangeValidator = rv;
                return this;
                }
            }
        else if (type == ConfigEntryType.ENUM || type == ConfigEntryType.ENUM_SET)
            {
            this.enumPattern = pattern;
            this.enumValidator = new EnumValidator(pattern, type == ConfigEntryType.ENUM_SET);
            return this;
            }
        else if (type == ConfigEntryType.DATE || type == ConfigEntryType.TIME || type == ConfigEntryType.DATETIME)
            {
            // For simple string pattern, treat as regex for now. 
            // Advanced flags are handled via setTemporalValidationFlags.
            }

        // Default to regex if not a range/enum or for other types
        this.validationPattern = Pattern.compile(pattern, Pattern.UNICODE_CHARACTER_CLASS);
        return this;
        }

    @Override
    public String getValueLimitations()
        {
        if (type == ConfigEntryType.ENUM || type == ConfigEntryType.ENUM_SET)
            {
            return enumPattern;
            }
        return getValidationPattern();
        }

    @Override
    public ConfigEntry getDefaultEntry()
        {
        if (defaultValue == null) return null; // No default entry provided
        ConfigEntryMetadata meta = new ConfigEntryMetadata(null); // No source for default entry
        meta.setSpecification(this);
        return new StringConfigEntryLeaf(key, defaultValue, meta);
        }

    @Override
    public boolean isMandatory()
        {
        return flags.contains(ConfigEntry.ConfigEntryFlags.MANDATORY);
        }

    @Override
    public boolean isSecret()
        {
        return isSecret;
        }

    public ConfigSchemeEntry setSecret(boolean secret)
        {
        this.isSecret = secret;
        return this;
        }

    @Override
    public int getMinArity()
        {
        return (minArity == -1) ? ConfigEntrySpecification.super.getMinArity() : minArity;
        }

    public ConfigSchemeEntry setMinArity(int minArity)
        {
        this.minArity = minArity;
        return this;
        }

    @Override
    public int getMaxArity()
        {
        return (maxArity == 0) ? ConfigEntrySpecification.super.getMaxArity() : maxArity;
        }

    public ConfigSchemeEntry setMaxArity(int maxArity)
        {
        this.maxArity = maxArity;
        return this;
        }

    @Override
    public boolean hasUnknownMandatoryFeatures()
        {
        return hasUnknownMandatoryFeatures;
        }

    public void setHasUnknownMandatoryFeatures(boolean hasUnknownMandatoryFeatures)
        {
        this.hasUnknownMandatoryFeatures = hasUnknownMandatoryFeatures;
        }

    public void setPathValidationFlags(Map<String, Object> flags)
        {
        if (flags != null && !flags.isEmpty())
            {
            this.pathValidationFlags = flags;
            this.filePathValidator = new FilePathValidator(flags);
            }
        }

    public void setTemporalValidationFlags(Map<String, Object> flags)
        {
        if (flags != null && !flags.isEmpty())
            {
            this.temporalValidationFlags = flags;
            // System.out.println("[DEBUG_LOG] setTemporalValidationFlags for " + this.key + ": " + flags);
            this.temporalValidator = new org.metabit.platform.support.config.scheme.impl.TemporalValidator(this.type, flags);
            }
        }

    public Map<String, Object> getTemporalValidationFlags()
        {
        return temporalValidationFlags;
        }

    public Map<String, Object> getPathValidationFlags()
        {
        return pathValidationFlags;
        }

    public void setHidden(boolean hidden)
        {
        isHidden = hidden;
        }
}
//___EOF___

