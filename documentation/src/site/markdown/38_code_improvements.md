# 3.8 Code Improvements and Best Practices

## Defaults

Replace code like
```java
    Properties defaults = new Properties();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    InputStream input = classLoader.getResourceAsStream("defaultconfig.properties");
    try {		
        defaults.load(input);
        }
    catch (IOException e) {
        e.printStackTrace();		
        }
```

with simply using mConfig.

Its module mConfigSourceJAR automatically loads
defaults from .config/CONFIGNAME.EXTENSIONS in the resource folder of your JAR -
besides all its other locations.

So, you can remove blocks like the above entirely.



## Common Pitfalls and Feature Flags

Common mistakes when starting with mConfig, and the feature flags that help avoid them:

- **Redundant Null Checks:** By default, `EXCEPTION_ON_MISSING_ENTRY` is `true`, so getters throw `ConfigException` instead of returning `null`. This removes the need for `if (value == null) value = "placeholder"` boilerplate.

Replace:
```java
String version = config.getString("version");
if (version == null) {
    version = "unknown";
}
```

With:
```java
try {
    String version = config.getString("version");
    // use version
} catch (ConfigException e) {
    // handle missing value once
}
```
 Reminder: You can provide defaults in the resource folder of your JAR.

- **Manual null handling for missing values:** Alternatively, use `EXCEPTION_ON_MISSING_ENTRY=false` and `DEFAULT_ON_MISSING_ENTRY=true` to automatically return scheme defaults instead of `null`.

- **Manual strictness checks:** Set `SCHEME_STRICT_MODE=true` to reject unknown keys at runtime.

- **Manual scope fallback loops:** Control with `FALLBACKS_ACROSS_SCOPES` (for reads) and `WRITE_FALLBACK_ACROSS_SCOPES` (for writes).

- **Custom format probing:** Configure `FILE_FORMAT_READING_PRIORITIES` and `FILE_FORMAT_WRITING_PRIORITIES` declaratively instead of hardcoding.

These features promote a declarative style, reducing boilerplate and errors.