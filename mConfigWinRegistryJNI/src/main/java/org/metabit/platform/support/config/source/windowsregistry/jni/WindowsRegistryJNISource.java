package org.metabit.platform.support.config.source.windowsregistry.jni;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.impl.*;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.interfaces.*;
import org.metabit.platform.support.config.scheme.ConfigScheme;
import org.metabit.platform.support.config.source.core.InMemoryLayer;
import org.metabit.platform.support.osdetection.OperatingSystem;
import org.metabit.platform.support.osdetection.PlatformDetector;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * accesses the Windows Registry as config source via JNI, if on a Windows OS.
 */
public class WindowsRegistryJNISource implements ConfigStorageInterface
{
    private ConfigLoggingInterface           logger;
    private Integer hkcu;
    private Integer hklm;
    private String  basePath;
    private WindowsRegistryJNIFORMAT winRegistryFormat;
    private ConfigFactoryInstanceContext ctx;
    private WindowsRegistryJNI jni;

    private static class RegistryLocation
        {
        final int    hive;
        final String path;

        RegistryLocation(int hive, String path)
            {
            this.hive = hive;
            this.path = path;
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegistryLocation that = (RegistryLocation) o;
            return hive == that.hive && java.util.Objects.equals(path, that.path);
            }

        @Override
        public int hashCode()
            {
            return java.util.Objects.hash(hive, path);
            }

        @Override
        public String toString()
            {
            String hiveStr = "UNKNOWN";
            if (hive == WindowsRegistryJNI.HKEY_CURRENT_USER) hiveStr = "HKCU";
            else if (hive == WindowsRegistryJNI.HKEY_LOCAL_MACHINE) hiveStr = "HKLM";
            return hiveStr + "\\" + path;
            }
        }

    @Override
    public String getStorageName()        { return "WindowsRegistryJNI";  }

    @Override
    public String getStorageID()         { return "registryjni"; }


    /**
     * test whether initializing a config source for use would work.
     *
     * @param settings settings to use
     * @param logger   logger to use
     * @return true if successful and the config source is usable, false if there was a problem and it should not be used.
     */
    @Override
    public boolean test(ConfigFactorySettings settings, ConfigLoggingInterface logger)
        {
        return new PlatformDetector().getOs() == OperatingSystem.WINDOWS;
        }

    /**
     * initialize the config source.
     *
     * @param ctx reference to the ConfigFactoryInstanceContext object in use
     * @return true if OK, false if there was an issue, and the config source should be removed from future use.
     */
    @Override
    public boolean init(ConfigFactoryInstanceContext ctx)
        {
        this.ctx = ctx;
        this.logger = ctx.getLogger();
        if (new PlatformDetector().getOs() != OperatingSystem.WINDOWS)
            {
            return true; // Silently disable on non-Windows
            }

        this.jni = new WindowsRegistryJNI();
        this.winRegistryFormat = new WindowsRegistryJNIFORMAT();
        this.basePath = ctx.getSettings().getString(ConfigFeature.REGISTRY_BASE_PATH);
        if (this.basePath == null)
            {
            String companyName = ctx.getSettings().getString(ConfigFeature.COMPANY_NAME);
            String applicationName = ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME);
            if (companyName == null || companyName.trim().isEmpty())
                {
                this.basePath = "Software\\" + applicationName;
                }
            else
                {
                this.basePath = "Software\\" + companyName + "\\" + applicationName;
                }
            }

        this.hkcu = WindowsRegistryJNI.HKEY_CURRENT_USER;
        this.hklm = WindowsRegistryJNI.HKEY_LOCAL_MACHINE;

        String companyName2 = ctx.getSettings().getString(ConfigFeature.COMPANY_NAME);
        String applicationName2 = ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME);
        String gpoPath;
        if (companyName2 == null || companyName2.trim().isEmpty())
            {
            gpoPath = "Software\\Policies\\" + applicationName2;
            }
        else
            {
            gpoPath = "Software\\Policies\\" + companyName2 + "\\" + applicationName2;
            }

        // 1. POLICY Scope: Explicitly for GPO (Mandatory/Read-Only)
        ctx.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.POLICY, this, winRegistryFormat, new RegistryLocation(hkcu, gpoPath)), ConfigScope.POLICY);
        ctx.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.POLICY, this, winRegistryFormat, new RegistryLocation(hklm, gpoPath)), ConfigScope.POLICY);

        // 2. USER Scope: Standard HKCU (Mutable)
        ctx.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.USER, this, winRegistryFormat, new RegistryLocation(hkcu, basePath)), ConfigScope.USER);

        // 3. HOST Scope: Standard HKLM (Hardware Defaults)
        ctx.getSearchList().insertAtScopeEnd(new ConfigLocationImpl(ConfigScope.HOST, this, winRegistryFormat, new RegistryLocation(hklm, basePath)), ConfigScope.HOST);

        return true;
        }

    @Override
    public void exit()
        {
        }

    @Override
    public ConfigStorageInterface clone()
            throws CloneNotSupportedException
        {
        return (ConfigStorageInterface) super.clone();
        }

    @Override
    public boolean isGenerallyWriteable()
        {
        return false; 
        }

    @Override
    public URI getURIforConfigLocation(ConfigLocation configLocation, String key, String optionalFragment)
        {
        if (configLocation.getStorage() != this)
            throw new ConfigException(ConfigException.ConfigExceptionReason.CODE_LOGIC_ERROR);
        String hive = "invalid";
        Object handle = configLocation.getStorageInstanceHandle();
        int hiveInt = 0;
        if (handle instanceof RegistryLocation)
            {
            hiveInt = ((RegistryLocation) handle).hive;
            }
        else if (handle instanceof Integer)
            {
            hiveInt = (Integer) handle;
            }

        if (hkcu.equals(hiveInt))
            hive = "HKCU";
        else if (hklm.equals(hiveInt))
            hive = "HKLM";

        String keyString = (key == null) ? "" : URLEncoder.encode(key, StandardCharsets.UTF_8);
        String locationString = URLEncoder.encode(configLocation.toLocationString(), StandardCharsets.UTF_8);
        String uristring = String.format("mconfig:winregistryjni/%s/%s/%s", hive, locationString, keyString);
        if (optionalFragment != null)
            uristring += "#"+URLEncoder.encode(optionalFragment, StandardCharsets.UTF_8);
        return URI.create(uristring);
        }

    @Override
    public void tryToReadConfigurationLayers(String sanitizedConfigName, ConfigLocation possibleSource, LayeredConfigurationInterface layeredCfg)
        {
        Object handle = possibleSource.getStorageInstanceHandle();
        int hive;
        String path;

        if (handle instanceof RegistryLocation)
            {
            hive = ((RegistryLocation) handle).hive;
            path = ((RegistryLocation) handle).path;
            }
        else if (handle instanceof Integer)
            {
            hive = (Integer) handle;
            path = basePath;
            }
        else
            {
            return;
            }

        String fullPath = path;
        if (sanitizedConfigName != null && !sanitizedConfigName.isEmpty())
            {
            fullPath += "\\" + sanitizedConfigName;
            }

        int hKey = jni.openKey(hive, fullPath, WindowsRegistryJNI.KEY_READ);
        if (hKey == 0)
            {
            return;
            }

        try
            {
            ConfigLayerInterface layer = readKeyToLayer(hKey, possibleSource);
            if (layer != null)
                {
                layeredCfg.add(layer, possibleSource);
                }
            }
        finally
            {
            jni.closeKey(hKey);
            }
        }

    private ConfigLayerInterface readKeyToLayer(int hKey, ConfigLocation location)
        {
        InMemoryLayer layer = new InMemoryLayer(ctx, location, location.getScope());
        ConfigEntryMetadata meta = new ConfigEntryMetadata((ConfigSource) location);

        // Enumerate values
        int index = 0;
        while (true)
            {
            WindowsRegistryJNI.RegistryValue regValue = jni.enumValue(hKey, index);
            if (regValue == null)
                {
                break;
                }
            
            String key = regValue.name;
            Object value = winRegistryFormat.convertRegistryValue(regValue.type, regValue.data);
            ConfigEntryType configType = winRegistryFormat.mapRegistryTypeToConfigType(regValue.type);
            
            TypedConfigEntryLeaf entry = new TypedConfigEntryLeaf(key, value, configType, meta);
            layer.putEntry(key, entry);
            index++;
            }

        // Recursively enumerate subkeys
        index = 0;
        while (true)
            {
            String subKeyName = jni.enumKey(hKey, index);
            if (subKeyName == null)
                {
                break;
                }
            
            int hSubKey = jni.openKey(hKey, subKeyName, WindowsRegistryJNI.KEY_READ);
            if (hSubKey != 0)
                {
                try
                    {
                    readSubKeyToLayer(hSubKey, subKeyName, layer, meta);
                    }
                finally
                    {
                    jni.closeKey(hSubKey);
                    }
                }
            index++;
            }

        return layer.isEmpty() ? null : layer;
    }

    private void readSubKeyToLayer(int hKey, String prefix, InMemoryLayer layer, ConfigEntryMetadata meta)
        {
        // Enumerate values in subkey
        int index = 0;
        while (true)
            {
            WindowsRegistryJNI.RegistryValue regValue = jni.enumValue(hKey, index);
            if (regValue == null)
                {
                break;
                }
            
            String key = prefix + "/" + regValue.name;
            Object value = winRegistryFormat.convertRegistryValue(regValue.type, regValue.data);
            ConfigEntryType configType = winRegistryFormat.mapRegistryTypeToConfigType(regValue.type);
            
            TypedConfigEntryLeaf entry = new TypedConfigEntryLeaf(key, value, configType, meta);
            layer.putEntry(key, entry);
            index++;
            }

        // Recursively enumerate sub-subkeys
        index = 0;
        while (true)
            {
            String subKeyName = jni.enumKey(hKey, index);
            if (subKeyName == null)
                {
                break;
                }
            
            int hSubKey = jni.openKey(hKey, subKeyName, WindowsRegistryJNI.KEY_READ);
            if (hSubKey != 0)
                {
                try
                    {
                    readSubKeyToLayer(hSubKey, prefix + "/" + subKeyName, layer, meta);
                    }
                finally
                    {
                    jni.closeKey(hSubKey);
                    }
                }
            index++;
            }
        }

    @Override
    public ConfigLayerInterface tryToCreateConfiguration(String configName, ConfigLocation location, ConfigScheme configScheme, LayeredConfiguration layeredConfiguration)
        {
        return null;
        }

    @Override
    public void tryToReadBlobConfigurations(String sanitizedConfigName, ConfigLocation location, BlobConfiguration blobConfig)
        {
        }

    @Override
    public Set<ConfigDiscoveryInfo> listAvailableConfigurations(ConfigLocation location)
        {
        Set<ConfigDiscoveryInfo> configurations = new HashSet<>();
        if (new PlatformDetector().getOs() != OperatingSystem.WINDOWS)
            {
            return configurations;
            }

        Object handle = location.getStorageInstanceHandle();
        int hive;
        String path;

        if (handle instanceof RegistryLocation)
            {
            hive = ((RegistryLocation) handle).hive;
            path = ((RegistryLocation) handle).path;
            }
        else if (handle instanceof Integer)
            {
            hive = (Integer) handle;
            path = basePath;
            }
        else
            {
            return configurations;
            }

        // We list subkeys under the path as potential configuration names
        int hKey = jni.openKey(hive, path, WindowsRegistryJNI.KEY_READ);
        if (hKey == 0)
            {
            return configurations;
            }

        try
            {
            int index = 0;
            while (true)
                {
                String subKeyName = jni.enumKey(hKey, index);
                if (subKeyName == null)
                    {
                    break;
                    }

                URI uri = getURIforConfigLocation(location, subKeyName, null);
                configurations.add(new ConfigDiscoveryInfo(subKeyName, location.getScope(), uri, winRegistryFormat.getFormatID(), false));
                index++;
                }
            }
        finally
            {
            jni.closeKey(hKey);
            }

        return configurations;
        }

    @Override
    public boolean hasChangedSincePreviousCheck(Object storageInstanceHandle)
        {
        return true;
        }

    @Override
    public void triggerChangeCheck(Object storageInstanceHandle)
        {
        }
}

/*
tricky how to implement.
the commandline execution like https://stackoverflow.com/questions/69493490/how-to-read-integer-value-from-windows-registry-using-java
i've done before, and might use again.


    com.sun.deploy.association.utility.WinRegistryWrapper
needs the deploy.jar from JDK, and it is string-only.
https://stackoverflow.com/questions/62289/read-write-to-windows-registry-using-java

external code, one file only:
    https://github.com/apache/npanday/tree/trunk/components/dotnet-registry/src/main/java/npanday/registry
but according to https://stackoverflow.com/questions/62289/read-write-to-windows-registry-using-java
not stable anymore. read that page anyhow


External dependencies: we try to do without.
Otherwise, see
    https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/win32/Advapi32Util.java
    com.sun.jna.platform.win32.Advapi32Util
    https://code.dblock.org/2010/03/23/jna-accessing-windows-registry-from-java.html

Hm. Dynamic code, checking whether advapi is present, falling back to commandline otherwise?


 */
