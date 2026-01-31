package org.metabit.platform.support.config;

/// The scope a configuration is effective for.
///
/// This is a reduced and updated version of the original metabit MAUS concept from 1998.
///
/// @version $Id: $Id
///
/// enums have a built-in compareTo() method, which is supposed to sort by their ordinals.
/// Java enum ordinals are the enum's entries in order of declaration, starting with 0 for the first entry.
///
/// The order in ConfigScope is from broadest default scope to the most specific scope. earlier entries have lower ordinals, and lower priority.
/// later entries override earlier ones in the configuration scope hierarchy.
///
public enum ConfigScope
{
    /// **product**
    /// The "Hardcoded" defaults Floor, aka "default"
    ///
    /// This configuration scope is set for the entire product, wherever installed. used for built-in **defaults**.
    ///
    /// Keep in mind this library cannot handle versions and variants of your product/software by default.
    /// version specifics need to be handled separately, e.g. in configuration (sub)paths and/or parameters.
    PRODUCT,
    /// **organization**
    /// The "Licensee" / Corporate Identity / nonprofit organization / OU
    ///
    /// This Configuration scope is specific to an Organization, which may operate one or more clusters, one or more hosts.
    /// usually used for customer-specific settings, and licence keys.
    /// This is used e.g. for Licences, seeing this level as "Licensee".
    /// Where profile migration or roaming is an issue, this is also the level which gets migrated between hosts.
    ///
    /// NB: It refers to the organization *using* the software the mConfig library is embedded in,
    /// not the organization producing the software.
    ORGANIZATION,

    /// **cloud**
    ///  one level above CLUSTER, this is for cloud-based configurations, typically shared across multiple clusters.
    ///
    CLOUD,

    /// **cluster**
    /// ("Node Group" / Data Center)
    ///
    /// scope: in case several hosts using the same software are operated in a cluster, this setting is for the
    /// current / an entire cluster. A Cluster consists of several hosts. Cluster-wide configuration may be overridden
    /// by host-specific configuration.
    CLUSTER,

    /// **host**
    /// Host / OS Instance
    ///
    /// scope: settings specific to this host/OS instance (server, VM, container, PC, ...) - higher priority than CLUSTER; overridden by APPLICATION.
    /// often called "system-wide settings".
    ///
    /// This configuration is specific to the current host/OS installation of the application, affecting all users.
    HOST,

    /// **application**
    /// The "Installation" / Portable Root
    ///
    ///  This scope is for configuration data specific to a particular installation or instance of the
    ///  application, regardless of the user running it or the machine's global state.
    ///
    ///  It serves as a bridge between machine-wide defaults and user-specific overrides, typically
    ///  representing the "Software Vendor" or "Installation Package" level of configuration.
    ///
    ///  *Key Characteristics:*
    ///     - **Installation Specific:** Targets settings tied to the program's binary location (e.g.,
    ///       side-by-side .ini files or a {@code .config/} directory in the application folder).
    ///     - **Version Isolation:** Useful for running multiple versions of the same product on one
    ///        machine without them sharing (and potentially corrupting) the same {@link #HOST} settings.
    ///     - **Portable Mode:** Primary scope for "portable" apps where all configuration must stay
    ///        within the application's own directory structure.
    ///
    /// *Common Examples:*
    ///      - Windows: Files in the same directory as the executable/JAR, or subdirectories like {@code .config/}.
    ///      - Linux/Unix: Shared application data typically found in {@code /usr/share/<appname>} (though
    ///         often read-only) or local installation folders.
    ///       - Test Mode: Maps to {@code src/test/config/<companyName>/<applicationName>/}.
    ///
    APPLICATION,

    /// **user config data.**
    /// The "Identity" / AppData
    ///
    /// this Configuration is specific to the current/a single user, for the current application.
    /// This is one of the Scopes used most often.
    USER,

    /// **session**
    /// The "Context" / Shell / EnvVars)
    ///
    /// current process/session context, whether in shell, window manager, or the like.
    /// Examples:
    /// - environment variables
    /// - commandline options
    /// override configurations file, explicitly provided
    /// anything stored here may be lost after disconnecting/ending user session if not exported.
    SESSION,

    /// **at runtime**
    /// The "Live Instance" / Volatile
    ///
    /// runtime instance of the application.
    /// Contents of this are set by software activity.
    /// anything set here will be lost after application end/restart.
    RUNTIME,
    
    /**
     * **POLICY**
     * The "Enforced" / GPO / Mandatory
     *
     * The highest priority scope. Overrides all other scopes.
     *
     * System-wide or organizational policy settings that override all other configuration layers.
     * This scope is intended for settings enforced by IT administrators or system-level policies
     * that must not be modified by the user or the application itself at runtime.
     *
     * It represents the "Immutable Policy" level, ensuring that critical security or corporate
     * compliance settings are strictly adhered to.
     *
     * **Key Characteristics:**
     * - **Enforced Compliance:** Used for settings that are mandatory and should take precedence
     *   even over {@link #RUNTIME} or {@link #SESSION} overrides.
     * - **Read-Only:** Typically maps to storage locations that are read-only for standard users
     *   and applications (e.g. privileged registry keys or administrative configuration files).
     * - **Global Authority:** Overrides local, user, and session-specific preferences.
     *
     * *Common Examples:*
     * - Windows: Group Policy Objects (GPO) located in {@code HKLM\Software\Policies} or
     *   {@code HKCU\Software\Policies}.
     * - Linux/Unix: Specialized policy files in {@code /etc/} or central management system
     *   overrides (e.g., via SSSD or local policy dæmons).
     * - Test Mode: Can be simulated via {@code TESTMODE_DIRECTORIES} using the {@code POLICY:} prefix.
     *
     */
    POLICY;
}