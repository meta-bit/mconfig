This is a mConfig module for accessing environment variables for configuration purposes.

The default naming scheme is `<application name>_<config name>_<config key>.`

The environment variable names are converted to lower case.

The ConfigScope the environment variables are seen in is `SESSION`.
(more specific than USER, so they can override USER settings.)

Writing should not happen to env by default, unless the scope SESSION is actively specified.

-----

In addition to accessing environment variables,
this is a special module: It allows runtime changes to mConfig operation.
This makes it useful for debugging and development purposes;
but it could also be seen as a security risk. Because of this, you have to
activate the mConfig runtime changes feature explicitly with
`ALLOW_MCONFIG_RUNTIME_SETTINGS` set to `true`.

These settings start with "MCONFIG_RUNTIME_", and are:

* `MCONFIG_RUNTIME_DEBUG_LEVEL` with values `off|0`, `error|1`, `warn|2`, `info|3`, `debug|4`, `trace|5`,  
* `MCONFIG_RUNTIME_DEBUG_OUTPUT` with values `"stderr"`, `"stdout"`, `"quiet"`, `"file:"+filename with path`;
`"syslog"` considered for future extension.
file is appended to if exists, created if not; if access fails, that is silent.
Internally, this is handled by `InternalLogger`.

* `MCONFIG_RUNTIME_DISABLE_MODULES` with a "," separated list of module IDs to disable.
if takes precedence over the module loading
* `MCONFIG_RUNTIME_MODULES` list which is a "," separated list of module IDs and/or
module file paths. @SECURITY adding modules at runtime is a security risk;
another feature which needs to be activated explicitly in code! (Not implemented yet)

### Writing to Environment Variables
Writing to environment variables is not supported by this module and will throw a `ConfigCheckedException`. 
Environment variables are considered a read-only configuration source for the process lifetime.

```java
// This will throw ConfigCheckedException
config.put("server/port", 8080, ConfigScope.SESSION);
```

Uses `System.getenv()` and `System.getenv(name)`.