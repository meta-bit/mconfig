# 1.3 Getting Started

## 1.3.1 Include the library in your project build

For Java, start with adding the dependency to your project build:

Maven:
```xml
<dependency>
    <groupId>org.metabit.platform.support.config</groupId>
    <artifactId>mconfigbasic</artifactId>
    <version>${mconfig.version}</version>
    <type>pom</type>
</dependency>
```

Gradle:
```gradle
implementation 'org.metabit.platform.support.config:mconfigbasic:${mconfig.version}'
```
(update version as needed)

In case you are defining an `module-info.java` in your project, add this to the `requires`
section:
```java
    requires metabit.mconfig.core;
    requires metabit.mconfig.util;
```

### Starters (Recommended)
For instant frustration-free setup, copy from [examples/starters](https://github.com/meta-bit/mconfig/blob/master/examples/starters/README.md):

- [![Maven](https://img.shields.io/badge/Maven-Minimal-blue)](https://github.com/meta-bit/mconfig/blob/master/examples/starters/maven-minimal/README.md) `mvn compile exec:java`
- [![Gradle](https://img.shields.io/badge/Gradle-Kotlin-green)](https://github.com/meta-bit/mconfig/blob/master/examples/starters/gradle-kotlin/README.md) `./gradlew run`
- [![JPMS](https://img.shields.io/badge/JPMS-Maven-orange)](https://github.com/meta-bit/mconfig/blob/master/examples/starters/jpms-maven/README.md) `mvn compile exec:java` (module-info included!)

See each README for details.

### Snippets (Quick Wins 🚀)
[![Snippets](https://img.shields.io/badge/Snippets-quick-brightblue)](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md)

Copy-paste solutions for common config pains: hardcodes → env vars → secrets → tests:
[examples/snippets.md](https://github.com/meta-bit/mconfig/blob/master/examples/snippets.md).
Names, schema fields and settings to look up: [Snippets Reference](snippets.md).
What to replace in existing code: [Better Practice](38_code_improvements.md).

## 1.3.2 Start with a simple use case

```java
    Configuration cfg = ConfigUtil.quickConfig("myCompany","myApplication","network");

    String  peerName    = cfg.getString("peer");
    int     portNumber  = cfg.getInteger("port");
    double  probability = cfg.getDouble("probability");
    //...
```

This will get you the requested values from the "network" configuration, 
wherever in the standard directories or other places they are kept.

If it's not working as expected, consult the **[Quick Verification Checklist](43_faq.md#quick-verification-checklist)** in the FAQ.

For more information on how to define the contract for your configurations (keys, types, defaults, and validation), see **[Configuration Schemas](23_configuration_schemas.md)**.

## 1.3.3 Common Pitfalls
For advice on avoiding typical mistakes and leveraging feature flags effectively, see [Code Improvements and Best Practices](38_code_improvements.md).

## 1.3.4 Resource Placement (the .config/ folder)
To bundle configurations, schemas, or library settings with your application, 
use the `.config/` resource folder in your classpath (e.g., `src/main/resources/.config/`).

*   **Configurations**: `.config/<company>/<application>/<configName>.<ext>`
*   **Schemas**: `.config/<company>/<application>/<configName>.mconfig-schema.json`
*   **Library Self-Configuration**: `.config/<company>/<application>/mconfig.properties`
    (applies only to this application's factories; the old global `.config/metabit/mConfig/mconfig.properties`
    is deprecated, see [Self-configuration per component](25_config_features.md#self-configuration-per-component))

For testing, as a starting point, 
you can create a `.config/myCompany/myApplication/network.properties` in subdirectories
of your home directory, and see its contents turn up in your code.

Regular paths would be in `/etc/myCompany/myApplication/network.properties` for Linux, `%AppData%\myCompany\myApplication\network.properties` for Windows, and so on -
and taking care of all these different locations is part of the library. Also, parsing different formats like JSON, YAML, and so on.
And much more.


The cleaner way, without shortcuts, goes like this:

```java
   try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication").build())
        {
        Configuration cfg = factory.getConfig("network");

        String  peerName    = cfg.getString("peer");
        int     portNumber  = cfg.getInteger("port");
        double  probability = cfg.getDouble("probability");
        //...
        }
```

The try-with-resources in the top line instantiates the library with all defaults;
the getConfig() line asks for a specific configuration, and then you
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

## 1.3.5 Search List and Priority

Everything should just work, by default. But in some environments, you may want to customize the search list,
or the storage types priority, or the file format priority... 

You can add extra directories to the search list, or change the priority of the storage types.

See **[2.0 Scopes](20_scopes.md)** for an overview of the hierarchy, and **[2.2 Priorities and Hierarchies](22_priorities_and_hierarchies.md)** for more technical details on the search list.


## 1.3.6 Navigating Lists with ConfigCursor

Configuration formats that support arrays (like YAML, JSON, or TOML)
may have lists/sets where the entries are not named, have no key.

So you can't access them by name. 

You can use the `ConfigCursor` to navigate through list items as individual nodes.

(Actually, you can use the ConfigCursor for all the configuration; 
it is just especially useful for navigating lists.)

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApp").build())
    {
    Configuration cfg = factory.getConfig("myConfig");
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


## 1.3.7 Module System Considerations

mConfig uses the Java Module System. The architecture is:

- `mconfigbasicbinary`: Shaded binary distribution (batteries-included)
- `mconfigbasic`: Recommended POM aggregator for most projects
- `mconfigstandard`: Extended POM aggregator for advanced usage
- `metabit.mconfig.core`: Core functionality
- `metabit.mconfig.util`: Utility functions
- Additional modules (e.g. Formats and configuration sources)

When using Java modules, you must:
1. Include `mconfigbasicbinary` (or `mconfigbasic`) as a Maven/Gradle dependency
2. Explicitly require the core modules in your `module-info.java`
3. Require any additional modules you may need

The internal modules are provided via services and do not have an API for your use,
so you don't need to "require" them in your `module-info.java`.


## 1.3.8 Packaging as a single JAR

mConfig finds its storages, formats, loggers and components with Java's `ServiceLoader`. Each module
registers them in files under `META-INF/services/`, named after the interface. Several mConfig modules
register implementations of the same interface, so several JARs contain a file of the same name, e.g.
`META-INF/services/org.metabit.platform.support.config.interfaces.ConfigStorageInterface` in
`mconfigcore`, `mconfigsourcejar` and `mconfigsourcefilesystem`.

This happens with every modular library that uses `ServiceLoader`, e.g. SLF4J 2 (its providers),
Log4j 2, JDBC (drivers) and Jackson (modules). On a regular classpath, with one JAR per library, it
causes no trouble. If you pack the application and its dependencies into **one** JAR, the files
of the same name have to be **merged**. Packaging that just unpacks all JARs into one keeps one file
per name and drops the others, depending on dependency order. The classes are all there, but
`ServiceLoader` does not see them.

For mConfig this means configuration sources, formats or components are missing: bundled defaults
(PRODUCT scope) or files in the user's and host's directories are not read, and reading a value fails
with `NO_MATCHING_ENTRY` or returns a schema default. Other libraries in the same JAR fail in their
own ways, e.g. SLF4J falling back to no logging, or a JDBC driver not being found.

### Merging the service files

**Maven, assembly plugin.** The predefined descriptor `jar-with-dependencies` does **not** merge service
files. If you use the assembly plugin, you have to use your own descriptor with the `metaInf-services`
handler, which merges them:

`src/assembly/jar-with-dependencies-merged-services.xml`:
```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0 https://maven.apache.org/xsd/assembly-2.2.0.xsd">
    <id>jar-with-dependencies-merged-services</id>
    <formats>
        <format>jar</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <containerDescriptorHandlers>
        <containerDescriptorHandler>
            <handlerName>metaInf-services</handlerName>
        </containerDescriptorHandler>
    </containerDescriptorHandlers>
    <dependencySets>
        <dependencySet>
            <outputDirectory>/</outputDirectory>
            <useProjectArtifact>true</useProjectArtifact>
            <unpack>true</unpack>
            <scope>runtime</scope>
        </dependencySet>
    </dependencySets>
</assembly>
```

`pom.xml`, instead of `<descriptorRef>jar-with-dependencies</descriptorRef>`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <descriptors>
            <descriptor>src/assembly/jar-with-dependencies-merged-services.xml</descriptor>
        </descriptors>
        <archive>
            <manifest>
                <mainClass>com.example.Main</mainClass>
            </manifest>
        </archive>
    </configuration>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
mConfig's build tests exactly this descriptor (`integrationtests/integrationtestAssembly`).

**Maven, shade plugin.** Add the `ServicesResourceTransformer`:
```xml
<transformers>
    <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
</transformers>
```

**Gradle.** With the Shadow plugin, call `mergeServiceFiles()` in the `shadowJar` task. A plain `jar` task
that adds `zipTree(...)` of the runtime classpath does not merge; with `duplicatesStrategy = EXCLUDE` it
silently keeps the first file.

**Spring Boot.** The executable JAR built by the Spring Boot plugins keeps the dependency JARs nested,
so nothing needs merging.

**`mconfigbasicbinary`** is shaded with merged service files. If you add further mConfig modules (or other
libraries) and then pack everything into one JAR, the same applies again.

### What mConfig detects

At `build()`, mConfig checks whether its core module's own registrations are present. If another module's
file has replaced core's, `build()` fails with a `ConfigException` (reason `JAVA_SERVICE_CONFIGURATION_ERROR`):

```
mConfig did not find all of its components, likely due to faulty JAR packaging, which also affects
other modular libraries. See "Packaging as a single JAR" in .../13_getting_started.md
  META-INF/services/...ConfigStorageInterface lacks ...DefaultLayerStorage, ...; present: [jar:file:/.../app.jar!/META-INF/services/...]
```

It names each service file that lacks core's registrations, and where the file that survived comes from.

The check is deliberately limited to what it can tell for certain, so it gives no false alarms. When core's
file is the one that survived, the other modules' registrations are lost without the check noticing; the same
holds for third-party modules. Which file survives depends on dependency order and can change with a version
update. So fix the packaging, rather than relying on the check, and **test the packaged artifact**: an integration
test that starts the single JAR and reads a value from your bundled defaults catches all of these cases, for
other libraries as well.

This is meant to reach developers while they set up or change the packaging, not end users: do not ship
an artifact that fails it. If you need to start anyway, e.g. to examine a faulty artifact, set the feature
`ALLOW_INCOMPLETE_SERVICE_REGISTRATION` (in code, or in your component's `mconfig.properties`). mConfig then logs
the same report as a WARNING and starts without the components concerned.
