# 3.3 Getting Information

## 3.3.1 Locations: Potential vs. Instantiated

mConfig makes a clear architectural distinction between *candidate / potential search locations* and *active / instantiated layers*:

### 3.3.1.1 Potential Search List (`getSearchList()`)
Where is the overall `ConfigFactory` looking across all scopes and storage providers?
`ConfigFactory.getSearchList()` returns the ordered list of all candidate `ConfigLocation` instances.

Candidate directories remain in this search list even if they do not exist on disk at initialization time. If a user or process creates one of these directories or files later while the application runs, background change checkers detect it and automatically bind the new configuration.

```java
List<ConfigLocation> searchList = configFactory.getSearchList();
System.out.println("Search list (candidate locations):");
for (ConfigLocation location : searchList) {
    System.out.println("\t" + location.getScope() + "\t" + location.getStorageName() + "\t" + location.getURI("configName", null));
}
```

### 3.3.1.2 Active Source Locations (`getSourceLocations()`)
To see which locations actually exist and contributed active data layers to a specific loaded `Configuration`:

```java
Configuration cfg = configFactory.getConfig("network");
System.out.println("Active source locations:");
for (ConfigLocation location : cfg.getSourceLocations()) {
    System.out.println("\t" + location.getScope() + "\t" + location.getURI("network", null));
}
```

### 3.3.1.3 Discovered Configurations (`listAvailableConfigurations()`)
To scan candidate locations and list all configuration files that currently exist with their metadata:

```java
Set<ConfigDiscoveryInfo> discovered = configFactory.listAvailableConfigurations();
for (ConfigDiscoveryInfo info : discovered) {
    System.out.println(info.getConfigName() + " (" + info.getFormat() + ") in " + info.getScope());
}
```

## 3.3.2 Legacy support: BLOBs, and where to put files?

Use Configuration mechanisms where possible; this enables all the features,
including schema checking, network storage, and so on.

### 3.3.2.1 Raw Files (Text and Binary)
You may need to access the entire content of a file as a single String or byte array. mConfig provides two specialized formats for this purpose: `text_file` and `binary_file`.

**Reading a text file as a single String:**
By using the `text_file` format, the entire file content is read into a single entry with an empty key `""`.

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication")
        .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, List.of("text_file"))
        .build())
    {
    Configuration cfg = factory.getConfig("myTextFile");
    String content = cfg.getString("");
    }
```

**Reading a binary file as a byte array:**
The `binary_file` format works similarly but provides the content as a byte array.

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication")
        .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, List.of("binary_file"))
        .build())
    {
    Configuration cfg = factory.getConfig("myBinaryFile");
    byte[] content = cfg.getBytes("");
    }
```

### 3.3.2.2 Hierarchical BLOBs
In modern formats like JSON and YAML, binary data can be part of the hierarchical structure.

**JSON (Base64):**
If a `ConfigSchema` defines a key as type `BYTES`, mConfig will automatically decode a Base64-encoded string in a JSON file.

**YAML (!!binary):**
YAML supports binary data natively using the `!!binary` tag. mConfig will correctly retrieve these as byte arrays.

```java
byte[] data = cfg.getBytes("server/certificate");
```

### 3.3.2.3 Legacy BLOBs
The second fallback for supporting legacy applications is that you can get
filesystem paths for a given Scope. If there are several for the same Scope,
mConfigUtil will prioritize the most specific entry, and prefer those which
exist already over those which would need to be created.

```Java
Path localPath = ConfigUtil.whereToPutMyFiles(configFactory, ConfigScope.USER, false);
```

But please, consider using regular mConfig functionality instead where possible.
