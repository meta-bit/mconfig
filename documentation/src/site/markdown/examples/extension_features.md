# Extension Features Example

This example demonstrates how to define and use extension features in mConfig. Extension features allow modules (like Vault, AWS, or your own custom storage) to define their own settings without modifying the core library.

## Defining an Extension Feature

To define a feature in your module, use `ConfigFeatureBase`. It is recommended to register it in a static block so it's available for string-based lookups and self-configuration.

```java
package com.example.mconfig;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureBase;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureRegistry;

public final class MyModuleFeatures {
    private MyModuleFeatures() {}

    /**
     * A custom setting for our module.
     */
    public static final ConfigFeatureInterface CUSTOM_API_URL = 
        new ConfigFeatureBase("MY_CUSTOM_API_URL", ConfigFeature.ValueType.STRING, "https://api.example.com");

    static {
        // Registering makes the feature visible to the Registry
        ConfigFeatureRegistry.register(CUSTOM_API_URL);
    }
}
```

## Using the Feature

### 1. Using the Feature Constant (Type-Safe)

If you have a compile-time dependency on the module, you can use the feature constant directly. This provides full type safety.

```java
ConfigFactory factory = ConfigFactoryBuilder.create("myOrg", "myApp")
    .setFeature(MyModuleFeatures.CUSTOM_API_URL, "https://api.test.com")
    .build();
```

### 2. Using String Names (Drop-in / Decoupled)

If you want to keep your code decoupled from the specific module, you can set the feature using its string name. mConfig will perform a case-insensitive lookup (ignoring underscores).

```java
ConfigFactory factory = ConfigFactoryBuilder.create("myOrg", "myApp")
    .setFeature("MY_CUSTOM_API_URL", "https://api.test.com")
    // These also work due to normalized lookup:
    // .setFeature("mycustomapiurl", "https://api.test.com")
    // .setFeature("My_Custom_API_URL", "https://api.test.com")
    .build();
```

### 3. Using Self-Configuration

You can also set extension features in the `mconfig.properties` file located in the `.config/metabit/mConfig/` classpath directory:

```properties
# .config/metabit/mConfig/mconfig.properties
MY_CUSTOM_API_URL = https://api.prod.com
TEST_MODE = true
```

## Implementation Tip: Accessing the Feature in a Module

Inside your `ConfigStorageInterface` or `ConfigFormatInterface` implementation, you can retrieve the value from the settings context:

```java
public void updateConfigurationLayers(ConfigFactorySettings settings) {
    // Access using the constant
    String apiUrl = (String) settings.getFeature(MyModuleFeatures.CUSTOM_API_URL);
    
    // Or use the provided helper in Settings for core types
    // String apiUrl = settings.getString(MyModuleFeatures.CUSTOM_API_URL);
}
```
