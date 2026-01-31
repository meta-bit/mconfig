# 2.9 Getting Information

## 2.9.1 Locations

So, where is my specific configuration looking for its contents?

To see where a Configuration instance gets its contents from, you can use `getSourceLocations()` like this:
```java
 System.out.println("config locations: ");
 for (ConfigLocation location : cfg.getSourceLocations())
     System.out.println("\t" + location.getScope() + "\t" + location.getURI("entry", null));
```

And where is the overall mConfig factory instance looking, in general?
There is `getSearchList()` from ConfigFactory.
```Java
 System.out.println("searchList: ");
 for (ConfigLocation location : searchList)
     System.out.println("\t" + location.getScope() + "\t" + location.getURI("entry", null));
```

Depending on your use case, you'll want different parts of the ConfigLocation entries.
Above is just a little text dump.

## 2.9.2 Legacy support: BLOBs, and where to put files?

Use Configuration mechanisms where possible; this enables all the features,
including scheme checking, network storage, and so on.

### 2.9.2.1 Raw Files (Text and Binary)
You may need to access the entire content of a file as a single String or byte array. mConfig provides two specialized formats for this purpose: `text_file` and `binary_file`.

**Reading a text file as a single String:**
By using the `text_file` format, the entire file content is read into a single entry with an empty key `""`.

```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("myCompany", "myApplication")
        .setFeature(ConfigFeature.FILE_FORMAT_READING_PRIORITIES, List.of("text_file"))
        .build())
    {
    Configuration cfg = factory.getConfiguration("myTextFile");
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
    Configuration cfg = factory.getConfiguration("myBinaryFile");
    byte[] content = cfg.getBytes("");
    }
```

### 2.9.2.2 Hierarchical BLOBs
In modern formats like JSON and YAML, binary data can be part of the hierarchical structure.

**JSON (Base64):**
If a `ConfigScheme` defines a key as type `BYTES`, mConfig will automatically decode a Base64-encoded string in a JSON file.

**YAML (!!binary):**
YAML supports binary data natively using the `!!binary` tag. mConfig will correctly retrieve these as byte arrays.

```java
byte[] data = cfg.getBytes("server/certificate");
```

### 2.9.2.3 Legacy BLOBs
The second fallback for supporting legacy applications is that you can get
filesystem paths for a given Scope. If there are several for the same Scope,
mConfigUtil will prioritize the most specific entry, and prefer those which
exist already over those which would need to be created.

```Java
Path localPath = ConfigUtil.whereToPutMyFiles(configFactory, ConfigScope.USER, false);
```

But please, consider using regular mConfig functionality instead where possible.
