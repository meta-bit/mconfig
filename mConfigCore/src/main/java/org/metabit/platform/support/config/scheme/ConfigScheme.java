package org.metabit.platform.support.config.scheme;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.ConfigScope;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.source.core.DefaultLayer;
import org.metabit.platform.support.config.ConfigEntry;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * a specification for a configuration.
 * fields to expect,
 * - by which name
 * - which type
 * - optional restrictions for acceptable values
 * - default value
 * - description text (or description ID, for documentation lookups)
 * - flags, e.g. "mandatory/optional"
 * - scopes for which this entry is valid
 *
 * 
 * @version $Id: $Id
 */
public interface ConfigScheme
{

    /**
     * check config entry for validity based on this Scheme.
     *
     * @param fullKey the (full) key
     * @param entry the config entry, including value.
     * @return true if it passes the test as valid, false if not;
     * threw ConfigException if the entry is inconsistent or other severe issues arise. Not to be used for invalid data.
     */
    boolean checkConfigEntryValidity(final String fullKey, final ConfigEntry entry);

    /**
     * add Scheme entries in a single-string format, described thus:
     * INVALID NEEDS REPHRASING KEY ";" DESCRIPTION ";" TYPE ("(" DEFAULT_VALUE ")"){0,1} (, VALIDITY_Scheme)* (";" (FLAG )+)*
     * in detail:
     * KEY is the mandatory string, full hierarchical key within the Configuration. For flat configs, that's the name.
     *
     * TYPE: see `ConfigEntryType` enum (case insensitive)
     * type is case insensitive.
     *
     * VALIDITY_Scheme is a pattern, format depending on type
     * ** none for boolean yet; may add more variants beyond true/false, 1/0, yes/no in the future.
     * ** for INTEGER, LOWEST_VALUE "-" HIGHEST_VALUE both may start with a "-" for negative values.
     * ** special ranges here named "uint8", "uint16", "uint32", "uint64" for unsigned,
     *    "int7", "int15", "int31", "int63" for signed integers.
     * ** for REAL, planned: LOWEST_VALUE "-" HIGHEST_VALUE (complex syntax).
     * ** for STRING, this is a Java RegExp.
     * ** for STRINGS, the same; it's expected to match *all* entries which are to be considered valid.
     *
     * DEFAULT_VALUE is what you provide; it may be checked automatically against the VALIDITY_Scheme.
     *
     * FLAG is an identifier for further checks. Defined flags so far:
     * ** MANDATORY - if this value is not set, neither in DEFAULT_VALUE nor any other config,
     * then the simplified Configuration access will throw an exception on first reading.
     *
     * DESCRIPTION is a text, reference or whatever to describe the Scheme entry. May contain whitespace and all.
     *
     *
     * test; integer (0-99), default 1, optional; just a test value.
     *
     * @param format1 string formatted Scheme entry; see above for format description.
     * @throws org.metabit.platform.support.config.ConfigCheckedException
     * -- INVALID_DEFAULT if the DEFAULT_VALUE provided does not match with the VALIDITY_Scheme
     */
    void addSchemeEntry(final String format1) throws ConfigCheckedException;

    /**
     * <p>addSchemeEntry.</p>
     *
     * @param fullKey the hierarchical key; for flat Configurations, the name.
     * @param type type of the entry.
     * @param validityScheme Scheme to check by. may be null or empty, meaning no checks are performed.
     * @param defaultValue default value. may be null.
     * @param description description. may be empty.
     * @param flags flags. null for defaults
     * @param scopes scopes for which the entry is valid. null for defaults.
     * @throws org.metabit.platform.support.config.ConfigCheckedException on format or logic errors
     */
    void addSchemeEntry(final String fullKey, ConfigEntryType type, final String validityScheme, final Object defaultValue /* or String?*/, final String description, EnumSet<ConfigEntry.ConfigEntryFlags> flags, EnumSet<ConfigScope> scopes) throws
            ConfigCheckedException;

    /**
     * <p>addSchemeEntry.</p>
     *
     * @param entry a {@link org.metabit.platform.support.config.scheme.ConfigSchemeEntry} object
     */
    void addSchemeEntry(ConfigSchemeEntry entry);

    /*
    initialize before use. internal function.
    the init is at runtime, where settings are available. -- check
     */
    /**
     * <p>init.</p>
     *
     * @param ctx a {@link org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext} object
     */
    void init(ConfigFactoryInstanceContext ctx);

    /**
     * internal function: set the default layer with the defaults found in the scheme.
     *
     * @param defaultLayer the default layer to set the defaults in
     */
    void transferDefaults(DefaultLayer defaultLayer);

    /**
     * <p>getEntryKeys.</p>
     *
     * @return a {@link java.util.Set} object
     */
    Set<String> getEntryKeys();

    /**
     * get the specification for a given key.
     *
     * @param fullKey the key to look up
     * @return the specification, or null if none is defined in this scheme.
     */
    org.metabit.platform.support.config.interfaces.ConfigEntrySpecification getSpecification(String fullKey);

    /**
     * Check if this scheme is a permissive "null" scheme that doesn't define any specific constraints.
     *
     * @return true if this is a permissive scheme, false otherwise.
     */
    default boolean isNullScheme()
        {
        return false;
        }

    //------------------------------------------------------------------------------------------------------------------
    // java 11 doesn't need the class statics workaround anymore.
    // may have to reactivate for legacy support.
    static Map<String, ConfigScheme> fromJSON(final String jsonFormattedConfigScheme, ConfigFactoryInstanceContext ctx)
            throws ConfigCheckedException {
        try {
            return ConfigSchemeFactory.create().createSchemesFromJSON(jsonFormattedConfigScheme, ctx);
        } catch (UnsupportedOperationException e) {
            throw new ConfigCheckedException(new RuntimeException("JSON scheme parsing is available in mConfigScheme module.", e));
        }
    }

    static ConfigScheme fromSchemeEntries(Set<ConfigSchemeEntry> entries) throws ConfigCheckedException {
        try {
            return ConfigSchemeFactory.create().createSchemeFromEntries(entries);
        } catch (UnsupportedOperationException e) {
            throw new ConfigCheckedException(new RuntimeException("Scheme creation from entries is available in mConfigScheme module.", e));
        }
    }


    /**
     * old workaround:
     * java doesn't permit static elements to be declared in interfaces.
     * this is an unsightly workaround.
    class statics
    {
        public static Map<String,ConfigScheme> fromJSON(final String jsonFormattedConfigScheme, ConfigFactoryInstanceContext ctx)
                throws ConfigCheckedException
            { throw new UnsupportedOperationException("JSON scheme parsing is available in mConfigScheme module."); }
        public static ConfigScheme fromSchemeEntries(Set<ConfigSchemeEntry> entries) throws ConfigCheckedException { throw new UnsupportedOperationException("Scheme creation from entries is available in mConfigScheme module."); }
    }
     */
}
