# Java Platform Module System (JPMS)

Java 9 introduced the Java Platform Module System (JPMS), often referred to as "Java Modules".
JPMS adds strong encapsulation and explicit dependencies between modules.

mConfig is modular and uses JPMS so you can choose only the parts you need.
If your application uses `module-info.java`, you should explicitly declare the
mConfig modules you depend on.

## Why this matters
- **Stronger encapsulation:** internal packages are hidden unless exported.
- **Explicit dependencies:** missing `requires` entries cause compile-time errors.
- **Service loading:** mConfig uses the service loader to discover formats and sources.

## Minimal `module-info.java`
If you only use the core and utility APIs:
```java
module my.app {
    requires metabit.mconfig.core;
    requires metabit.mconfig.util;
}
```

## Typical setup
If you use the standard bundle:
```java
module my.app {
// ...
requires metabit.mconfig.core;
requires metabit.mconfig.scheme;
requires metabit.mconfig.secrets;
requires metabit.mconfig.util;
requires metabit.mconfig.modules.jar;
requires metabit.mconfig.modules.filesystem;
requires metabit.mconfig.format.javaproperties;
// ...
}
```

## Notes on auto-discovery
Most format and source modules are discovered via the service loader.
You do not need to `requires` them directly unless you reference their classes.
But they must be on the classpath, and visible to the service loader.

Related docs:
- [Getting Started](../13_getting_started.md)
- [mConfig Modules](mconfig-modules.md)
