# 3.7 Extensions and Advanced Use

## 3.7.1 Extensions and Advanced Features

mConfig allows you to extend its functionality by adding new configuration features without modifying the core library. This is useful for building custom storage backends or format providers.

### 3.7.1.1 ConfigFeatureBase

To create a new feature, use `ConfigFeatureBase`. It provides a standard implementation of `ConfigFeatureInterface` that defines the feature name, value type, and optional default value.

```java
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureBase;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureRegistry;

public class MyModuleFeatures {
    public static final ConfigFeatureInterface MY_SETTING = 
        new ConfigFeatureBase("MY_MODULE_SETTING", ConfigFeature.ValueType.STRING, "default_val");

    static {
        // Register the feature so it can be set by its string name
        ConfigFeatureRegistry.register(MY_SETTING);
    }
}
```

By registering the feature in the `ConfigFeatureRegistry`, you enable the library to:
1. Validate values set via string names: `builder.setFeature("MY_MODULE_SETTING", "custom_val")`.
2. Access the feature without a direct dependency on the module's feature class.
3. Support "self-configuration" via `mconfig.properties`.
4. Provide case-insensitive lookups (ignoring underscores and case).

For example, a user of your module could configure it without importing `MyModuleFeatures`:

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("ACME", "App")
    .setFeature("MY_MODULE_SETTING", "some-value");
```

### 3.7.1.2 Writing Custom Extensions

For details on implementing custom `ConfigStorageInterface` or `ConfigFormatInterface`, please refer to the developer documentation in `devdocs/library_development.md`.

For common utility adapters (Properties, Overrides, Remapping), see [3.9 Utilities and Adapters](39_utilities_and_adapters.md).

## 3.7.2 Advanced use

Why perform

* parameter validation, 
* checks for presence of mandatory settings,
* generation of parameter documentation
yourself? mConfig has a built-in facility for this and more. 

You describe all the entries your configuration can have,
and mConfig takes care of the checking. 


## 3.7.3 Data types
### 3.7.3.1 Config schema formats
#### 3.7.3.1.1 Internal format 0: single ConfigSchemaEntry
    {
    "key":"mandatory key / configuration entry name",
    "TYPE":"mandatory type",
    "DEFAULT":"optional default value, as string",
    "DESCRIPTION":"optional description, for the documentation",
    "DESCRIPTION":{"en":"English description", "de":"Deutsche Beschreibung"},
    "PATTERN":"optional validation pattern",
    "SCOPES":[ "optional", "scopes", "this", "is", "valid", "in"],
    "FLAGS":["flags"],
    "SECRET":false,
    "HIDDEN":false
    }

Multi-language descriptions are supported by providing a map of language codes to description texts. When requested via `getDescription(Locale)`, the library will first check this map, then fall back to standard `ResourceBundle` lookup (using `.config/messages.properties`), and finally fall back to the single string description if provided.

**PLAN**: For FLAGS, in the future we may choose an alternative form – each being a pair with string
key and boolean value.

#### 3.7.3.1.2 Internal format 1: ConfigSchemas, in an array. internal use only!
    [ 
    SCHEME_ENTRIES,
    GO_HERE,
    DEFINED_AS_IN_FORMAT_1
    ]

#### 3.7.3.1.3 Format 2: full single ConfigSchema
    {
     "name":"mandatory name the Configuration is identified by",
     "entries":[ ENTRIES_AS_DEFINED_IN_FORMAT_2 ],
    }

#### 3.7.3.1.4 Format 3: multiple full ConfigSchemas
    [
      FULL_CONFIG_SCHEMA_AS_DEFINED_IN_FORMAT_3
    ]

```mermaid
stateDiagram-v2
    direction LR
    [*] --> UNDECIDED
    UNDECIDED --> ARRAY : on [
    UNDECIDED --> SCHEME: on {
    SCHEME --> SCHEME_ENDED : on }
    SCHEME --> ENTRY_KEY: on string
    ENTRY  --> SCHEME: on next value

```

### 3.7.4 configure priorities
#### 3.7.4.1 File Type Priority
You can control the resolution priority between different file types (e.g. JSON, YAML) using the `FILE_TYPE_PRIORITIES` feature. This re-orders the search list within each scope.

#### 3.7.4.2 Storage Type Priority

You can control the resolution priority between different storage types (e.g. Files vs. Registry) using the `STORAGE_TYPE_PRIORITIES` feature. This re-orders the search list within each scope.

```java
ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("company", "app")
        .setFeature(ConfigFeature.STORAGE_TYPE_PRIORITIES, List.of("files", "registry", "registryjni", "JAR"));
try (ConfigFactory factory = builder.build()) 
        {
        // ...
        }
```
Default IDs include: `RAM`, `secrets`, `files`, `registry`, `registryjni`, `JAR`.

Additional directories provided via features like `ADDITIONAL_USER_DIRECTORIES` are typically **prepended** to the search list for their scope. This makes them the preferred location for creating new configuration files, while still allowing default files in standard locations to provide values (unless the same key is defined in the additional directory).
