## 3.2 Priorities and Hierarchies



### 3.2.1 Search List and Priority
The `ConfigFactory` maintains an ordered **Search List** of locations. This order is crucial for two reasons:

1.  **Finding and Creating Configurations:** When looking for a configuration
    or creating a new one (e.g., via `ConfigUtil.whereToPutMyFiles`),
    mConfig searches through the list and picks the **first** matching writeable location.
    Thus, entries earlier in the search list have higher priority for discovery and creation.
2.  **Layered Overrides (Reading):** Configurations are composed of multiple layers.
    Layers from more specific scopes (e.g., `RUNTIME`) always override more
    generic scopes (e.g., `SYSTEM`).
    Within the **same scope**, however, the search list determines priority:
    layers from locations appearing **later** in the search list are added later
    to the configuration stack and thus **override** layers from locations
    appearing earlier in the same scope.

#### Storage Type Priority
@TODO this is an advanced topic, move there
You can control the resolution priority between different storage types (e.g., Files vs. Registry) using the `STORAGE_TYPE_PRIORITIES` feature. This re-orders the search list within each scope.

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
