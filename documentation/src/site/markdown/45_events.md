# Config Events

Config Events provide a mechanism to record noteworthy occurrences
during the configuration lifecycle 
that are not exceptional enough to interrupt program flow 
but may require programmatic handling or reporting.

"severity below Exception, above logging"

Example: Parsing of a discovered file fails. This happens often enough (typo),
is no reason to abort operation, but logging alone may go unnoticed by users
(invisible, no logger). 

## Key Concepts

- **Domain**: Broad category of the event (e.g., `PARSE`, `DISCOVERY`, `WRITE`).
- **Kind**: Specific classification within a domain (e.g., `UNSUPPORTED_SYNTAX`, `SKIPPED_PERMISSION_DENIED`).
- **Severity**: Importance of the event (`INFO`, `NOTICE`, `WARNING`, `ERROR`).
- **Remediation**: Programmatic hint for resolving the issue (e.g., `CHANGE_FORMAT`, `FIX_SYNTAX`).
- **Attributes**: Machine-readable metadata (e.g., `line`, `column`, `filePath`).

## Feature Flags

- `EVENTS_DETAIL_LEVEL`: Gating level for event recording.
  - `OFF`: No events recorded.
  - `FAILURES_ONLY` (Default): Only failures and blockers (e.g., parse errors).
  - `NORMAL`: Adds important informational events (e.g., merge overrides).
  - `VERBOSE`: Includes high-volume events (e.g., watcher notifications).
- `EVENTS_MAX_FACTORY`: Max events to retain at the `ConfigFactory` level (Default: 1000).
- `EVENTS_MAX_CONFIGURATION`: Max events to retain per `Configuration` (Default: 1000).
- `EVENTS_DEDUP_RECENT_LIMIT`: Size of the ring buffer used to suppress recent duplicate events (Default: 32).

## Programmatic Access

Events can be retrieved from the `ConfigFactory` or individual `Configuration` instances:

```java
List<ConfigEvent> factoryEvents = factory.getEvents();
List<ConfigEvent> configEvents = config.getEvents();
```

## Example: JSON with Comments

When `JSONwithJacksonFormat` encounters a file with `//` comments, it emits a `PARSE/UNSUPPORTED_SYNTAX` event:

```java
for (ConfigEvent ev : factory.getEvents()) {
    if (ev.getKind() == ConfigEvent.Kind.UNSUPPORTED_SYNTAX) {
        System.out.println("Hint: " + ev.getRemediationMessage());
        // Remediation: CHANGE_FORMAT
    }
}
```

## Cleanup

To clear accumulated events:

```java
factory.getEvents().clean();
```

## CLI Usage

The `mconfig` tool can display events using the `--show-events` (or `-E`) flag. 
This is particularly useful for diagnosing why certain configuration files are being ignored:

```bash
mconfig --config myapp --show-events show
```
