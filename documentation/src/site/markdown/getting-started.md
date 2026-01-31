# 2.1 Getting Started

## 2.1.1 Include the library in your project build

For Java, start with adding the dependency to your project build:

Maven:
```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mConfigStandard</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

Gradle:
```gradle
implementation 'org.metabit.platform.support.config:mConfigStandard:${mconfig.version}'
```
(update version as needed)

In case you are defining an `module-info.java` in your project, add this to the `requires`
section:
```java
    requires metabit.mconfig.core;
    requires metabit.mconfig.util;
```

### Starters (Recommended)
For instant frustration-free setup, copy from [examples/starters](../../../examples/starters):

- [![Maven](https://img.shields.io/badge/Maven-Minimal-blue)](../../../examples/starters/maven-minimal) `mvn compile exec:java`
- [![Gradle](https://img.shields.io/badge/Gradle-Kotlin-green)](../../../examples/starters/gradle-kotlin) `./gradlew run`
- [![JPMS](https://img.shields.io/badge/JPMS-Maven-orange)](../../../examples/starters/jpms-maven) `mvn compile exec:java` (module-info included!)

See each README for details.

### Snippets (Quick Wins 🚀)
[![Snippets](https://img.shields.io/badge/Snippets-quick-brightblue)](snippets.md)

Copy-paste solutions for common config pains: hardcodes → env vars → secrets → tests.

## 2.1.2 Start with a simple use case

```java
    Config cfg = ConfigUtil.quickConfig("myCompany","myApplication","network");

    String  peerName    = cfg.getString("peer");
    int     portNumber  = cfg.getInteger("port");
    double  probability = cfg.getDouble("probability");
    //...
```

This will get you the requested values from the "network" configuration, 
wherever in the standard directories or other places they are kept.

If it's not working as expected, consult the **[Quick Verification Checklist](4_FAQ.md#quick-verification-checklist)** in the FAQ.

For more information on how to define the contract for your configurations (keys, types, defaults, and validation), see **[Configuration Schemes](configuration-schemes.md)**.

## 2.1.3 Common Pitfalls
For advice on avoiding typical mistakes and leveraging feature flags effectively, see [Code Improvements and Best Practices](4_4_Code_Improvements.md#common-pitfalls).

## 2.1.4 Resource Placement (the .config/ folder)
To bundle configurations, schemes, or library settings with your application, 
use the `.config/` resource folder in your classpath (e.g., `src/main/resources/.config/`).

*   **Configurations**: `.config/<company>/<application>/<configName>.<ext>`
*   **Schemes**: `.config/<company>/<application>/<configName>.scheme.json`
*   **Library Self-Configuration**: `.config/metabit/mConfig/mconfig.properties`

For testing, as a starting point, 
you can create a .config/myCompany/myApplication/network.properties in subdirectories
of your home directory, and see its contents turn up in your code.

Regular paths would be in /etc/myCompany/myApplication/network.properties for Linux, %AppData%\myCompany\myApplication\network.properties for Windows, and so on -
and taking care of all these different locations is part of the library. Also, parsing different formats like JSON, YAML, and so on.
And much more.


The cleaner way, without shortcuts, goes like this:

```java
   try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication").build())
        {
        Config cfg = factory.getConfiguration("network");

        String  peerName    = cfg.getString("peer");
        int     portNumber  = cfg.getInteger("port");
        double  probability = cfg.getDouble("probability");
        //...
        }
```

The try-with-resources in the top line instantiates the library with all defaults;
the getConfiguration() line asks for a specific configuration, and then you
can get config entries, typesafe.

You can also list all available configuration names:

```java
   try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication").build())
        {
        Set<ConfigDiscoveryInfo> info = factory.listAvailableConfigurations();
        for (ConfigDiscoveryInfo c : info)
            {
            System.out.println("Found: " + c.getConfigName() + " in scope " + c.getScope() + " URL: " + c.getUri());
            }
        }
```

**Caveat**:
When you use the values stored in your local variables, you miss out on receiving changes. 
It is preferable to use the mConfig objects directly; they'll have the latest values.

## 2.1.5 Search List and Priority

Everything should just work, by default. But in some environments, you may want to customize the search list,
or the storage types priority, or the file format priority... 

You can add extra directories to the search list, or change the priority of the storage types.

see [3_2_priorities_and_hierarchies.md](3_2_priorities_and_hierarchies.md) for details.


## 2.1.6 Navigating Lists with ConfigCursor

Configuration formats that support arrays (like YAML, JSON, or TOML)
may have lists/sets where the entries are not named, have no key.

So you can't access them by name. 

You can use the `ConfigCursor` to navigate through list items as individual nodes.

(Actually, you can use the ConfigCursor for all the configuration; 
it is just especially useful for navigating lists.)

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApp").build())
    {
    Config cfg = factory.getConfig("myConfig");
    ConfigCursor cursor = cfg.getConfigCursor();

    if (cursor.moveTo("servers") && cursor.isOnList())
        {
        cursor.enter(); // Move into the array
        while (cursor.moveNext())
            {
            // Now positioned on a list item.
            // Items can be accessed as unnamed entries via virtual keys (indices).
            ConfigEntry item = cursor.getCurrentElement();
            System.out.println("Server: " + item.getValueAsString());
            }
        cursor.leave(); // Move back up to the "servers" node
        }
    }
```


## 2.1.7 Module System Considerations

mConfig uses the Java Module System. The architecture is:

- `mConfigBase`: Aggregator module that bundles some standard dependencies
- `metabit.mconfig.core`: Core functionality
- `metabit.mconfig.util`: Utility functions
- Additional modules (e.g. Formats and configuration sources)

When using Java modules, you must:
1. Include `mConfigBase` as a Maven/Gradle dependency
2. Explicitly require the core modules in your `module-info.java`
3. Require any additional modules you may need

The internal modules are provided via services and do not have an API for your use,
so you don't need to "require" them in your `module-info.java`.
