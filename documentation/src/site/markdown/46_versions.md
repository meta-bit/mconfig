# 4.5 Versions

0.8.17
first public release since 0.8.13; includes the internal steps 0.8.14 to 0.8.16, which were not published on their own.
new on Maven Central: Kubernetes storage modules (API and Fabric8).
security: secret values never leave their own storage (`saveTo()`, CLI); Vault sends credentials only to a configured
`https` address; Kubernetes tokens only over verified `https`; POLICY locations from the application need an explicit opt-in.
self-configuration per component (`.config/<company>/<app>/mconfig.properties`), feature setting levels.
Vault: KV version 2 reads, re-login on expired tokens; unavailable secret sources are reported (`SOURCE_UNAVAILABLE`).
schemas without entries fail to load instead of loading empty. Jackson 3.2.
See the CHANGELOG for the behaviour changes.

0.8.13
Jackson 3.x for the JSON, YAML and TOML modules; vulnerable dependencies replaced.
first release with the Vault and AWS Secrets Manager modules on Maven Central.

0.8.12
smaller core: unified configuration leaf representation, simplified core API.

0.8.11
renamed mConfigFull to mConfigStandard, mConfigStandard to mConfigBasic, and mConfigBase to mConfigBasicBinary.
new integration modules for HashiCorp Vault and AWS Secrets Manager.

0.8.5 
comprehensive documentation overhaul and restructuring
full comment preservation for TOML, INI, and (partial) YAML; separate TOML module
write support via ConfigCursor
updated examples and starters

0.8.0 
public release on github and maven central
formats: .properties, .json, .yaml, .toml, .ini
sources: JAR, filesystem, environment variables, ZooKeeper, Windows registry
dynamic configuration updates, config schemas, input validation, secrets,
packaged in multiple modules, mConfigStandard and mConfigBasic as POM collections.
.deb generated in mConfigTools, in addition to .jar. 
user-requested code smell pattern finds its place in mConfigCheese.

0.5.3 
public release
layered configurations, .properties format, raw files, OS-specific defaults,
config schema basics. packaged as a single JAR. JPMS support.

0.4.2
first public release, 2018-03-29
released on maven central and metabit repository + website