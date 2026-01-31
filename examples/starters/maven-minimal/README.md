# mConfig Maven Minimal Starter

## Quick Start

1. Copy all files from this directory to a new folder (e.g., `my-project`).
2. Open terminal in that folder.
3. Run:
   ```
   mvn clean compile exec:java
   ```
4. Expected output:
   ```
   Peer: localhost
   Port: 8080
   Probability: 0.5
   Available configs: 1 found
   ```
   (Plus logging if DEBUG enabled.)

## Features Included
- `mConfigStandard` (filesystem, JAR).
- `mConfigLoggingSlf4j` + Logback (hints on issues).
- Sample config loads from classpath `.config/myco/myapp/network.properties`.
- Ready for JPMS (add `module-info.java` if needed).

## Logging
Create `src/main/resources/logback.xml`:
```xml
&lt;configuration&gt;
  &lt;logger name="metabit.config" level="DEBUG"/&gt;
&lt;/configuration&gt;
```

## Customize
- Update `mconfig.version` in `pom.xml`.
- Add schemes, formats, env vars.

See main mConfig docs for more!