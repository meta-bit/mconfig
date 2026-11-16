# Changelog

Condensed per release. Behaviour changes are listed one by one; check them before upgrading.

### 0.8.17
First public release since 0.8.9. It also carries the changes of 0.8.15 and 0.8.16, which were not published on their own; read all three sections.

**Behaviour changes**
* `build()` fails with `JAVA_SERVICE_CONFIGURATION_ERROR` when the application was packed into a single JAR without merging `META-INF/services` files (MLIB-156), e.g. with the Maven assembly descriptor `jar-with-dependencies`. Such packaging keeps one file per service interface, so storages, formats or components went missing without notice: bundled defaults or configuration files were not read. The check recognises it when core's own registrations are among the lost ones; fix the packaging (see "Packaging as a single JAR" in Getting Started). New feature `ALLOW_INCOMPLETE_SERVICE_REGISTRATION` starts anyway, with a WARNING.
* the INFO line at factory start lists the active storages and formats.
* `STORAGE_TYPE_ALLOW_ALL_STORAGES=false` takes effect (MLIB-138, M2): only storages covered by `STORAGE_TYPE_PRIORITIES` are used; it had no effect before. An entry covers the ID and the IDs it prefixes up to `-` or `.` (`RAM` covers `RAM-SESSION`); core's own storages are always used. Storages missing from the default list, such as `envvar`, must be listed.
* `getConfig(name, schema)` applies the requested schema when a configuration of that name is already open with a different schema (MLIB-138, M6): it returns a separate instance with that schema, closed with the factory. Before, it returned the open instance and ignored the schema.
* environment variables (`mConfigSourceEnvVar`): an underscore in a key is written doubled in the variable name, as documented (MLIB-138). The key `max_connections` is read from `<APP>_<CONFIG>_MAX__CONNECTIONS`; before, it was read from `..._MAX_CONNECTIONS`, the variable of `max/connections`, so the two keys could not be told apart. Rename variables set for keys containing an underscore. A WARNING names the variable when such a key is requested and only the old name is set (removed in 0.9). Listed keys are decoded the same way.
* `mConfigUtil` views (`ConfigUtil.remapped()`, `withOverrides()`, `fromProperties()`) are read-only without exception (MLIB-138): every `put` throws `ConfigException` with reason `NOT_WRITEABLE`; it was `UnsupportedOperationException`, and `RemappedConfiguration` wrote through to its source for `put` without a scope and for lists. `OverridingConfiguration`: an override that cannot be converted to the requested type throws `ConfigException` (`CONVERSION_FAILURE`) instead of returning `null`, and one that does not match the parent's schema throws `ConfigException` (`ARGUMENT_INVALID`); that check was documented but had no effect. `RemappedConfiguration` and `OverridingConfiguration` pass on `getSourceLocations()`, `isClosed()`, `reload()` and update subscriptions of the configuration behind them; they returned empty or fixed answers, or threw. `PropertiesConfiguration` behaves like the `Properties` behind it: a missing key still gives `null`, but a value that cannot be converted throws `ConfigException` (`CONVERSION_FAILURE`) instead of returning `null`, and `getSecret()` returns the property as a `SecretValue` instead of always `null`; the first such access is logged at INFO level, once per JVM, since the value is an unprotected String in the `Properties`.
* Windows registry storage (`mConfigWinRegistry`, part of `mConfigStandard`): it is now found on the classpath as well; its service registration was outside `META-INF/services` and took effect on the module path only. On Windows, applications on the classpath now read configuration from the registry, including the POLICY scope from `Software\Policies\<company>\<application>` under HKCU and HKLM (Group Policy), which overrides every other scope. Other systems are unaffected.
* `getBoolean()` reads `true/false`, `yes/no`, `on/off` and `1/0`, in any case and with surrounding blanks; any other value, an empty one included, throws `ConfigException` (`CONVERSION_FAILURE`). Before, it followed `Boolean.parseBoolean()`: anything but `true` was `false`, so `yes`, `on` and `1` read as `false`, and typing errors went unnoticed. Under a schema that declares the entry `BOOLEAN`, an invalid value is skipped and the next layer or the schema default applies; the schema check accepts the same literals (it rejected `yes/no/on/off/1/0` before). Without a schema, the first value found is used and the exception is thrown. New feature `LENIENT_BOOLEAN_CONVERSION` (level `COMPONENT`) restores the old behaviour in the `Configuration` getters; code converting a `ConfigEntry` itself and the ConfigMapper always convert strictly. The same literals apply to Boolean features set in `mconfig.properties` (an invalid value is logged as ERROR and not applied), in `setTestParameters()` (`ConfigException`), and to `mconfig set` on a `BOOLEAN` key (rejected; it wrote `false`).
* Kubernetes storage (`mConfigSourceKubernetes`), ConfigMaps read from the API: values arrive as written. They were Base64-decoded like a Secret's, so a value containing `:`, a blank or a line break was dropped with a "not base64" WARNING, and one that happened to be valid Base64 (such as `abcd`) was decoded into other bytes. Secret `data` and `binaryData` are Base64, as before. The Fabric8 storage and mounted directories are not affected.
* Kubernetes storage (`mConfigSourceKubernetes`): a ConfigMap key with an extension a structured format on the classpath claims, such as `app.yaml` or `db.properties`, is parsed with that format, as documented; its entries become keys of the configuration. The extension was never recognised (compared without its dot), so such a key was one string entry named `app.yaml`; code reading it that way must change. The raw formats (`.txt`, `.cfg`, `.bin`) are not used for this; such keys stay one entry named as the key. Secrets are not parsed, whatever the key's extension: each key stays one secret entry (`SecretValue`, redacted, wiped), as before.
* `mConfigFormatJsonSchema`: a JSON Schema `pattern` matches anywhere in the value, as JSON Schema defines it, unless anchored; it was checked against the whole value. A `.` in it does not match a line break, as in JSON Schema. It applies to every string value, and is no longer read as an mConfig validator keyword (`port`, `email`). A pattern on a non-string type (`integer`, `boolean`, ...) is ignored, as JSON Schema defines it. A pattern Java cannot compile fails the schema instead of dropping the entries after it silently, `x-mconfig-secret` ones included.
* `mConfigFormatJsonSchema`, `pattern` together with `format`: a value is checked when read (the JSON Schema validator, on the schema as written) and when written (`put()`, `mconfig set`, `mconfig validate`). For `uri`, `date`, `time` and `date-time`, both checks require the value to be of that type **and** to match the pattern. For any other `format` (`email`, `hostname`, ...), reading also asserts the format, while writing checks the pattern only; so a value that matches the pattern but not the format can be written and is then skipped when read. Mapping these formats onto mConfig's own validators is planned for 0.9.x.

**Security**
* `RemappedConfiguration` (`mConfigUtil`) read every value as a `String` first, secrets requested with `getSecret()` included, for a schema check that had no effect (MLIB-138). The view now hands values on as they come, so a secret stays a `SecretValue`. `OverridingConfiguration` no longer puts the override's value into the message of a failed check.
* parse errors no longer carry secret values into the log. A parser's own message may quote the content it failed on; it is now withheld for secret stores (Kubernetes Secrets), the cloud modules' bootstrap configurations, and where the schema has a `SECRET` entry at, above or below the error position (anywhere, if the parser cannot tell the position). Configuration, location, format, error type, line, column and key path are always reported. New feature `LOG_PARSE_ERROR_DETAILS` (code only) shows the parser's message regardless, for development. Failed reads of Kubernetes Secrets and Vault are reported by error type and HTTP status only, without the client's message, which could quote the response.
* API change for format and storage modules: `ConfigFileFormatInterface.readFile()` and `readStream()` return a layer or throw the new checked `ConfigParseException`; they no longer return `null` and no longer log parse errors. Storages report the exception through `ParseProblemReporter`. `LayeredConfigurationInterface` has two new methods, `getConfigSchema()` and `isBootstrapLookup()`. Third-party formats and storages must be adapted.
* outside text in log lines is escaped in all storages and format parsers (MLIB-138, A8 follow-up; CWE-117): Windows registry, ZooKeeper, Kubernetes (both), JAR/classpath, and the JSON, YAML, TOML, INI and raw-text formats, including exception messages quoting file content. 0.8.15 did this for the filesystem storage.

**Fixes**
* an invalid or `null` company name, application name or sub-path gives `ARGUMENT_INVALID` naming the argument.
* `ConfigFactoryBuilder.create()` no longer fails where the host cannot resolve its own name, e.g. in an isolated container without DNS or an `/etc/hosts` entry for it (MLIB-138). The `HOSTNAME` feature is determined when first read, from the system's host name where available, with name resolution as the last resort; `null` if none works. Factory start no longer waits for a DNS lookup. A missing `user.name` system property no longer fails the start either.
* `mConfigFormatJsonSchema`: a pattern that is invalid on its own, such as `a)|(.*`, fails the schema; wrapped for search semantics it compiled into one that matched everything. A type list such as `"type": ["integer", "null"]` failed the whole schema. Now a list containing `"string"` is read as a string, with its `pattern` (values read from files are text, and are checked as strings when read); any other list by its first type other than `null`, so `["integer", "null"]` is a number.
* YAML with SnakeYAML: `yes`, `on` (and `Yes`, `ON`, ...) read as `false`. SnakeYAML resolves them as YAML 1.1 booleans, and the layer converted them with `Boolean.valueOf()`; they now read as `true`, `no` and `off` as `false`.
* environment variables: a variable whose name has lower-case letters after the prefix, such as `MYAPP_NET_Server_Port`, is no longer listed as a key on Linux and macOS; the lookup uses the upper-case name, so the listed key could not be read.
* `OverridingConfiguration` (`mConfigUtil`): an override on a key the schema marks `SECRET` is a secret entry, redacted in `toString()` and left out of exports; it was a plain entry. List and `byte[]` overrides keep their form; they were turned into their `toString()`. The view copies the overrides when it is created, so later changes to the caller's map or arrays do not reach it.
* `PropertiesConfiguration.isEmpty()` (`mConfigUtil`) counts the `Properties` defaults, as the getters do.
* single-JAR packaging check: optional first-party modules (filesystem, JAR, environment variables, Vault, AWS, Kubernetes, ZooKeeper, Windows registry, slf4j and util loggers) are now recognised as lost when their classes are present but no service file registers them, also where core's own registration survived.
* environment variables: names are built independently of the default locale; under a Turkish locale, variables for names containing `i` were not found (MLIB-138). A lower-case variable no longer adds a layer in which nothing is found (Linux, macOS).
* `getScope()` on an entry that was not read from a source (an override or a `Properties` value in the `mConfigUtil` views) returns `null`; it threw `NullPointerException`.
* CLI: `search-paths` and `validate` set up the factory like the other commands, so they honour the sub-path, `MCONFIG_ADDITIONAL_USER_DIRECTORIES` / `MCONFIG_ADDITIONAL_RUNTIME_DIRECTORIES` and test mode; `search-paths` could list a different search list than `get` and `show` used.
* CLI, jlink runtime (`-Pjlink`, the `mconfig-tools-jlink` package): the runtime lacked `java.net.http`, which the bundled Fabric8 Kubernetes client needs, so inside a pod the CLI most likely aborted with `NoClassDefFoundError`; `jdk.crypto.ec` is added for TLS with EC keys. The profile also runs again without `clean`; `jlink` refused the existing output directory.
* CLI: `--debug` had no effect, since the bundled slf4j logger ignores `LOGLEVEL_NUMBER`. It now routes mConfig's own log output to stderr at trace level; command output on stdout is unchanged.

**Known issues**
* change detection (MLIB-129, to be fixed in 0.8.18; present in all earlier releases): where several `Configuration` instances watch the same source, only the first to check may see a change, so the others are not reloaded and their subscribers not notified; the Fabric8 Kubernetes storage stops detecting changes to a ConfigMap or Secret after its watch closed with an error, until a configuration using it is opened again; subscribers are not notified of configuration files created after they subscribed. See "Known issues" in the Updates documentation.

**Deprecated**
* `ConfigFeature.ALLOW_LOCAL_SCHEMA_OVERRIDE`: replaced in 0.9.0 by an allowlist of schema sources. It keeps working in 0.8.x.
* `ConfigFeature.DEFAULT_ON_MISSING_ENTRY`: never evaluated, setting it had no effect; removed in 0.9.0. Schema `DEFAULT` values apply anyway. Several documentation pages recommended it; corrected.

**Documentation**
* `snippets.md` at the repository root is gone; links to it must be changed. Its content is split (MLIB-139): code templates to copy are in `examples/snippets.md`, whose Java blocks are taken from compiled sources under `examples/snippets/` and compared with them on every build; API names, schema fields, settings and scopes to look up are in the documentation as "Snippets Reference" (`documentation/src/site/markdown/snippets.md`).
* "Code Improvements and Best Practices" is rewritten as "Better Practice: Replace This With That" (`38_code_improvements.md`): pairs of "instead of" and "do this", part 1 for code that does not use mConfig yet, part 2 for code that does.
* `AGENTS.md` has a "Quick Answers" block and routes to the three pages above. `.junie/guidelines.md` is removed; it was outdated, and Junie reads `AGENTS.md`.

### 0.8.16 (not published separately; released as part of 0.8.17)

**Java:** the Jackson-based modules (`mConfigFormatJSONwithJackson`, `-YAMLwithJackson`, `-TOMLwithJackson`, `mConfigFormatJsonSchema`) and everything that includes them, such as `mConfigStandard` and `mConfigTools`, need Java 17 (Jackson 3). The other modules run on Java 11.

**Behaviour changes**
* a schema without entries fails to load instead of loading empty (M5). That is what the object-keyed-by-configuration-key shape parses to; its entries went without schema, including `SECRET` protection. Discovered files are reported as ERROR and skipped.
* security (MLIB-138): the Fabric8 Kubernetes storage configures its client from the module's settings only (`KUBERNETES_API_ENDPOINT`, `KUBERNETES_NAMESPACE`, token, namespace and CA paths, `KUBERNETES_ALLOW_INSECURE_TRANSPORT`), no longer from kubeconfig or Fabric8 system properties; the token only over `https`; inactive in test mode unless bypassed.
* an unknown scope name in a `SCOPE:path` entry is skipped with a WARNING instead of being used as a plain path.
* the Fabric8 Kubernetes storage uses the JDK `HttpClient` (`kubernetes-httpclient-jdk`) instead of OkHttp, and brings Jackson 2.18.x; applications that relied on OkHttp arriving through it must declare it themselves.
* in test mode, Vault and AWS Secrets Manager sources added with `addSource()` stay inactive unless the module's `BYPASS_TEST` feature is set (`VaultFeatures.BYPASS_TEST`, `AwsFeatures.BYPASS_TEST`), as for the discovered storages. Tests reach real endpoints only when intended.
* Vault: an explicitly configured token (`builder().withToken(...)`, or `token` in `vault.properties`) is used as given; the automatic Kubernetes login only happens when no token is configured. Before, a ServiceAccount token file in the pod took precedence.
* the AWS Secrets Manager module no longer brings the AWS SDK's asynchronous Netty client (`netty-nio-client`); the module itself uses only the synchronous client. Applications that use the async SDK client must declare it themselves.

**Fixes**
* Vault and AWS Secrets Manager (MLIB-136): sources added with their `builder()` and `addSource()` now work (they read nothing before); new SPI method `ConfigStorageInterface.isInitializedWhenAddedAsSource()`, deprecated for removal from the start (stopgap until the storage SPI changes in 0.9, MLIB-155). Vault reads from KV version 2 work (wrong path before). With Kubernetes auth, Vault logs in again when a read is refused (HTTP 403).
* unavailable secret sources no longer fall back silently (MLIB-152): Vault, AWS Secrets Manager, Kubernetes and secrets providers log a WARNING and record the new config event `SOURCE_UNAVAILABLE` naming the location. A failing secrets provider is reported once, and again after it has recovered, not on every lookup.
* Vault: a read refused with HTTP 403 while the token is still valid is reported as a policy denial; no new login (each login creates another Vault token).
* scope prefixes in `SCOPE:path` entries are recognised independently of the default locale (in a Turkish locale, lower-case `session:`, `runtime:`, `policy:`, `application:` and `organization:` failed).
* `ConfigMapper.saveTo(config, file, scopes)` honours `scopes` (MLIB-138); `ConfigSchemaException` keeps the parser's message; `clone()` of three storages no longer returns `null`.
* `mConfigFormatJsonSchema` no longer registers a schema factory that cannot create schemas (with the module on the classpath, `ConfigSchemaFactory.create().createSchema()` could return `null`); it honours `x-mconfig-secret` and the other `x-mconfig-*` extensions, `pattern` and `format` (an unused code path had them; the active one ignored them).

**Dependencies**: Jackson 3.2.4, one version for the build via the Jackson BOM; `mConfigFormatJsonSchema` on Jackson 3 only, with `json-schema-validator` 3.0.8 (Jackson 2 removed); slf4j pinned to 2.0.20 for the whole build. Known CVEs in transitive dependencies fixed: ZooKeeper module with ZooKeeper 3.9.7, Netty 4.1.140.Final and Guava 33.8.0-jre; Fabric8 module with Fabric8 6.13.5 and Jackson 2.18.12; no Netty in the AWS module.

### 0.8.15 (not published separately; released as part of 0.8.17)
**Behaviour changes**
* security (MLIB-136): the Vault storage is inactive unless `address` is set (the default `http://127.0.0.1:8200` is gone). Kubernetes login only over `https` (`auth.allowInsecureTransport=true` for development); the role defaults to the application name; `auth.kubernetes.mount` is honoured, new `auth.kubernetes.tokenPath`; `VAULT_ADDR` and `~/.vault-token` are no longer read.
* security (MLIB-138): the Kubernetes storage sends the ServiceAccount token only to a verified `https` endpoint. Tests against `http` mock servers need the new `KUBERNETES_ALLOW_INSECURE_TRANSPORT`.
* self-configuration per component (MLIB-134): new file `.config/<company>/<application>/mconfig.properties`, applying only to factories of that component. Code now wins over files. Self-configuration is read in core during `build()`, so `forbidTestMode()` / `PERMIT_TEST_MODE` also cover test mode set in a file; `SelfConfigurationComponent` is removed. The global `.config/metabit/mConfig/mconfig.properties` is deprecated and will be removed before 0.9.0.
* security (MLIB-134): features have setting levels (`CODE_ONLY`, `COMPONENT`, `ANY`; new API `ConfigFeatureInterface.getSettingLevel()`); refused values are ignored with a WARNING. `MCONFIG_RUNTIME_MODULES` is refused. POLICY locations registered by the application fail at `build()` unless `ALLOW_APPLICATION_POLICY_LOCATIONS` is set in code.
* with `SCHEMA_STRICT_MODE`, the mConfigUtil adapters (`withOverrides`, `remapped`) refuse keys the schema does not define.
* the filesystem storage skips search directories and `.d` fragments whose names contain control characters (WARNING).
* CLI (MLIB-137, MLIB-150, MLIB-151): `propose-schema` is disabled until its rewrite; `kubernetes-rbac` is experimental (`-X`) and documented as a generic template, not least-privilege RBAC.

**Security**
* secret values no longer leave their own storage (MLIB-130): `ConfigMapper.saveTo()` omits them, the `mconfig` CLI shows `[REDACTED]` in every output format.
* the filesystem storage escapes outside text (file names, OS- and environment-derived paths) in log messages (CWE-117).

**Fixes**
* secrets (MLIB-136): `SecretValue.getChars()` / `getValueAsChars()` decode UTF-8 correctly; `erase()` no longer wipes the caller's array; entries from a `ConfigCursor` keep their secret flag; hex decoding in `getBytes()` is strict.
* keys not defined in the schema no longer cause a `NullPointerException`; the `mconfig` CLI loads schemas; configuration names `.` and `..` are rejected (MLIB-138).

**New**
* `mconfig get <key> --reveal` shows a single secret value in plain text; experimental CLI commands accept `-X` after the command name.
* schema loading warns about `FLAGS` values that have no effect (MLIB-146). Only `MANDATORY` is enforced; `"FLAGS": ["SECRET"]` does not protect a value, use `"SECRET": true`.

**Documentation**: where secret values may appear; self-configuration, setting levels and POLICY locations in Config Features; test mode corrections; why the Kubernetes bootstrap reads SESSION and RUNTIME; `ConfigUtil.copy…ToJavaProperties()` copies secrets as plain text by design.

### 0.8.14
**Behaviour changes**
* API: `Configuration.limitScopes()` is removed. Storages receive a `BootstrapConfigContext` in `postInit()` and read their bootstrap settings through `getRestrictedConfig()`; this removes the circular-reference risk in the Vault, AWS Secrets Manager and ZooKeeper bootstrap. Affects custom storage implementations.
* system properties feed only mConfig's own `MCONFIG_RUNTIME_*` settings (as `mconfig.runtime.*`), and only with `ALLOW_MCONFIG_RUNTIME_SETTINGS`; other properties are no longer merged.

**Security**
* path traversal hardening in `mConfigSourceFilesystem` and the schema exporter: environment-derived base paths must be absolute, `..` segments are rejected, export names are checked against an allowlist, `LOCAL_SCHEMA_DIRECTORY` must be absolute.
* secrets: improved memory safety and erasing.

**New**
* Kubernetes modules `mConfigSourceKubernetes` and `mConfigSourceKubernetesFabric8`.

**Fixes**
* change-checker threads no longer leak: daemon threads, stopped on close, none started when `UPDATE_CHECK_FREQUENCY_MS` is 0 (synchronous checks on access, not "off").
* schema validation of string values from flat sources (e.g. `.properties`); `JsonStreamParser` accepts a comma directly after a number.
* Kubernetes, Fabric8 and ZooKeeper bootstrap log invalid values instead of swallowing them; lookup messages moved from INFO to TRACE.

**Documentation**: Javadocs; `getSearchList()` vs `getSourceLocations()`. CI workflows updated to v6.

### 0.8.13
* security: replaced vulnerable dependencies; Apache Curator (ZooKeeper) downgraded from 5.7.0 to 5.6.0.
* Jackson-based format modules (JSON, YAML, TOML) upgraded to Jackson 3.x.
* build: Javadoc for Maven Central; CI workflows updated to v5.

### 0.8.12
* internal refactoring reducing the core footprint: unified configuration leaf representation, simplified core API, centralised type conversion.

### 0.8.11
* **behaviour change:** modules renamed: `mConfigStandard` to `mConfigBasic`, `mConfigFull` to `mConfigStandard`, `mConfigBase` to `mConfigBasicBinary`.
* new integration modules for HashiCorp Vault and AWS Secrets Manager.
* self-configuration: fixed type handling in `mconfig.properties`.

### 0.8.10
* **behaviour change:** the schema file extension is `.mconfig-schema.json`; `.schema.json` and `.scheme.json` are no longer discovered.
* additional initialization phase (`postInit()`) for storages, secrets providers, components and loggers.
* CLI tool (`mconfig`): better error reporting and configuration discovery.

### 0.8.9
* restored dynamic discovery of new configuration files in search paths.
* expanded default search locations on Linux and Windows.
* improved robustness of manual configuration instantiation.

### 0.8.8
* Jackson upgraded to 2.21.1 in all Jackson-based format modules.
* CLI tool and configuration writing: "Moderate Whitesmiths" indentation fixes; `show` output format fixed.
* metadata and documentation: JSON-LD module descriptions, project URL and module links.

### 0.8.7
* `ConfigEvent` mechanism for lifecycle events (parse failures, discovery skips), with capped, de-duplicated retention; `getEvents()` on `Configuration` and `ConfigFactory`.
* CLI tool (`mconfig`): comments, timestamped "monitor" mode with source tracking, better error messages, flexible argument order.
* fixes: JAR source filters by path; nesting in TOML and INI writers; hierarchical `.mconfig-schema.json` structure.
* metadata: JSON-LD descriptions in `README.md` and several modules.

### 0.8.6
* "Scheme" renamed to "Schema" throughout modules, APIs and documentation.
* new `mConfigSchema` module with SPI-based schema discovery (`ConfigSchemaProvider`) and validators for ports, e-mail addresses, durations, sizes and dates/times (with `now`).
* new `mConfigFormatJsonSchema` module for standard JSON Schema (json-schema.org); not to be confused with `mConfigSchema`.
* `ConfigSchemaExporterComponent` exports registered schemas to the local filesystem.
* legacy `*.scheme.json` and `*.schema.json` files still discovered (removed in 0.8.10).

### 0.8.5
* improved SnakeYAML module to persist comments in YAML files.
* improved mConfig TOML module to fully persist comments
* improved mConfig INI module to persist comments
* enabled write support via ConfigCursor.

### 0.8.4
* new standalone TOML module (mConfigFormatTOML).
* improved GitHub CI workflows for publication.
* miscellaneous documentation fixes and build improvements.

### 0.8.3
* fixes for publication workflow; source unchanged.

### 0.8.0
* public release moved to github.

### 0.7.x

* substantial changes to the API
* adding many modules.
* schemes, change notification, etc.
* adding support for JPMS and Java 11+; 
* public releases via maven central 

### 0.6.0
* overhaul, separate modules
* compiles separately for JDK8, and JDK9+

## 0.5.3
first public release

The first implementation goes back to 1996, when Java 1 was still fresh.