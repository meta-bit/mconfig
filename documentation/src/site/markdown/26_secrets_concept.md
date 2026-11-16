# Security Model

The mConfig modules function as a library, 
running within the application’s context and process environment.


mConfig operates with the application's inherent access rights 
and does not attempt to "guard" secrets from the application itself. 
Instead, it delegates the primary security boundary to the underlying
execution environment (e.g., OS, Kubernetes, or Cloud IAM roles), 
maintaining the same level of security as the host process.

## What mConfig protects against, and what not

mConfig prevents **accidental disclosure** of secret values: in logs, `toString()`, tool output,
exports and diagnostic dumps (see [Where secret values may appear](#where-secret-values-may-appear)).

mConfig does **not** protect secrets in **process memory**. Whoever can read the memory of the
process - a heap dump, a debugger, an agent attached to the JVM, other code running in the same
process, the operating-system user the process runs as - can read the secrets the application has
loaded. Erasing (see [Memory wiping](#memory-wiping)) shortens the time a value is held, which
limits what a later heap dump contains; it is no protection against an attacker with access to the
running process. Protecting secrets from the process itself takes handles instead of values, such as
keys kept in an HSM or a KMS.

# Secrets

## Bridging Modern and Legacy security

Ideally, an application never handles raw secrets, but operates via handles only. This is the
model of the JCE (Java Cryptography Extension) and of Hardware Security Modules (HSMs): keys stay in
a hardware enclave and are used in a controlled manner, without ever entering application memory.
Many common tasks, however - sending a bearer token to an API, a password to a database driver -
require the actual secret value.

mConfig acts as bridge for both:

* High Security: Manage IDs and handles for HSM/KMS providers.
* Practical Utility: fetch and provide plaintext tokens or passwords for legacy interfaces, with
  the exposure limited as described below.

## mConfig Secrets Handling

mConfig secrets are treated differently than standard configuration to reduce their exposure footprint:

* **Masking**: secret values are replaced by `[REDACTED]` in logs, `toString()` and tool output.
* **Transport**: the network sources use TLS. The Vault and Kubernetes modules send the Kubernetes
  ServiceAccount token only to an `https` address, unless insecure transport is explicitly allowed
  for development. Other credentials, such as a static Vault token, go to the address you
  configure: configure an `https` address.
* **Memory**: secret values are handed out as `SecretValue`, a mutable byte buffer that the
  application can wipe with `erase()`, instead of an immutable `String`.
* **Late-bound decryption**: in the configuration, a secret is a reference. It is resolved -
  fetched, and decrypted where its source keeps it encrypted - only when the application requests
  the value (`getSecret()`), and the visible bytes can be erased right after use.
  *Status:* this is the model the secrets handling is built for. The storages shipped today still
  fetch their secrets when the configuration is built; fetching on request (lazy fetch) is
  scheduled for 0.9.1.

Strings are immutable and stay on the heap until garbage collected, so a password kept as a
`String` cannot be wiped. A `SecretValue` can. This works only as far as the value was never a
`String` along the way: values read from text formats (`.properties`, JSON, YAML, …) pass through
the format parser as `String`s, and environment variables are `String`s in the JVM from the start.
The buffer reduces the exposure; it does not remove it.

## Dynamic Updates
Vault and AWS Secrets Manager are polled for changes (`pollInterval`; default 60 s for Vault, 5 min
for AWS, where API calls have costs; `0` disables polling). The Kubernetes module watches the
ConfigMaps and Secrets it reads. Changed values replace the old ones without a process restart, so
rotated credentials are picked up.

### Memory wiping
When a `Configuration` is closed via `close()`, mConfig calls `erase()` on its configuration
layers. The in-memory and default layers and the environment-variable, Vault, AWS Secrets Manager
and Kubernetes storages wipe the secret buffers they hold. **Layers read from files do not wipe
yet**; complete wipe-on-close is planned for 0.9.1.

With the storages shipped with mConfig, each `getSecret()` returns a new copy, so wiping it with
`erase()` after use affects only the application's copy, never the stored value. A third-party
secrets provider (see [Where secrets come from](35_handling_secrets.md#356-where-secrets-come-from))
should do the same.

# Secret and Sensitive

mConfig distinguishes two kinds of confidentiality. They are related, but they are protected
for different reasons and therefore handled differently.

* **SECRET** — cryptographic secrets and credentials: private keys, symmetric keys, API tokens,
  passwords, license IDs. If one leaks, a system is compromised.
* **SENSITIVE** — information that must not become visible where it does not belong, typically
  personal data under GDPR and comparable regimes: names, e-mail addresses, postal addresses,
  user IDs, IP addresses. If one leaks, a person is harmed and a reporting obligation may follow.

The two overlap: **most SECRET entries are also SENSITIVE, but far from every SENSITIVE entry is
SECRET.** A per-user API token is both a credential and personal data. A customer's e-mail address
is personal data and no credential at all. The two markers are therefore independent, not a
severity scale — an entry may carry either, both, or neither, and when both are set the
protections add up.

## The distinction in one sentence

> **SECRET minimises exposure in _time_. SENSITIVE minimises exposure in _scope_.**

A credential is dangerous for as long as it exists in readable form, so mConfig wraps it, keeps it
out of the JVM String pool, and wipes it once it is no longer needed. Personal data is not made
safe by wiping it — the application legitimately needs it for its normal working life — but it
must not spread into logs, exports, diagnostic dumps, or documentation.

This is why SENSITIVE deliberately does **not** use the `SecretValue` machinery. Forcing personal
data through a wipeable byte buffer would be counterproductive: the application would convert it
straight back into a `String` to use it, and nothing would be gained.

## Confidentiality never hides the location

What gets suppressed is the *value*. Where the value is *stored* always remains traceable.

mConfig can tell you, for any entry, which scope and which concrete location it came from —
`getScope()`, `getLocation()` and `getURI()` on a `ConfigEntry` answer this regardless of whether
the entry is SECRET, SENSITIVE, or neither. That property is deliberate and is kept intact by
every confidentiality mechanism in the library.

This matters most precisely for the data that is most protected:

* **Data subjects can exercise their rights.** Finding, correcting, and erasing personal data
  (GDPR Art. 15-17) is only possible if one can determine where the data is held. A redaction
  that also hid the storage location would obstruct the very obligations SENSITIVE exists to serve.
* **Owners keep authority over their data.** If a value comes from "somewhere" and its origin
  cannot be determined, part of the ownership over it is lost.
* **Credentials can be rotated and revoked.** Knowing which store holds a secret is a
  precondition for replacing or removing it.

So a redacted log line or diagnostic dump still names the key, the scope, and the storage location
of a protected entry — it withholds only the value itself. Location traceability across a
ten-tier hierarchy of heterogeneous sources is a distinguishing property of mConfig, and
confidentiality markers do not trade it away.

## Where secret values may appear

Secrets are often stored in plain text on purpose: in a configuration file readable only by the
service account, in the registry, in a Kubernetes Secret. Their protection is the access control of
that storage — file permissions, ACLs, the login to a network service. mConfig respects this and
draws one line:

* **The storage the value came from is the value's home.** When a secret is replaced with a new
  value in the same scope, mConfig writes the new value to the same location, if that location is
  writeable, and replaces it there. The other secrets stored there stay as they are.
* **Every other context is outside that access control.** A newly created file
  (`ConfigMapper.saveTo()`), console output, logs, diagnostic dumps, event messages: whoever reads
  these was not necessarily allowed to read the original storage. Secret values do not go there.

In practice:

| Path                                                                       | Secret value                                                                                              |
|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| writing back to the source location (`put()`, `mconfig set`)               | kept intact; this is its home                                                                             |
| `ConfigMapper.saveTo()` to a new file                                      | left out; the omitted keys are reported as a WARNING config event (`EXPORT_SECRETS_OMITTED`)              |
| CLI output (`get`, `show`, `monitor`, `validate`, `set` echo), all formats | replaced by the marker `[REDACTED]`                                                                       |
| `toString()`, logs                                                         | replaced by the marker `[REDACTED]`                                                                       |
| parse error reports (log line, PARSE event)                                | the parser's quote of the content is left out where it may be a secret; see [Parse errors](#parse-errors) |
| `mconfig get <key> --reveal`                                               | shown: an explicit request for one value                                                                  |
| `getSecret()`, typed getters, `ConfigUtil.copy…ToJavaProperties()`         | returned to the application: an in-process request                                                        |

Key, scope and location stay visible in all of these.

**A string marker, in every output format.** A redacted value is shown as the string
`[REDACTED]` — in text, CSV, JSON, YAML and TOML alike — and never as `null` or by omitting the key
from tool output. A `null` reads as "not set", which is wrong exactly when someone is checking
whether a secret is configured; and if such output is ever re-used as a configuration file, a
`null` silently falls back to a lower-priority value, while a marker fails visibly. TOML has no
`null` anyway. The JSON, YAML and TOML output of `mconfig get` additionally carries
`"secret": true`.

**Explicit reveal for a single value.** Seeing a secret in plain text takes an explicit request for
exactly that value: `mconfig get <key> --reveal`. Listing and monitoring commands have no such
option. Whoever can read the source may see the value; the tool does not add a second gate, but it
does not hand out values unasked either.

**Files written by `saveTo()` leave secrets out** rather than writing a marker: a marker written
into a configuration file would become the value when the file is read back. To copy a
configuration including its secrets, copy the original storage under its own access control.

This follows established practice: summary views redact with a string marker and keep the key
(Spring Boot Actuator shows `******`, Terraform `<sensitive>`, CI logs `***`, `kubectl describe
secret` only byte counts), while plain text is given out only on an explicit request for the value,
guarded by the access control of the source (`kubectl get secret`, the Vault and AWS command line
tools). Consistent behaviour with these tools is intended: no surprises for operators who know them.

### Parse errors

When content cannot be parsed, no value has been read yet, so there is nothing to redact - but the
parser's own message often quotes the content it failed on, e.g. `Unrecognized token 's3cr3t'` or
the YAML line with a `^` marker below it. Since 0.8.17 mConfig always reports configuration,
location, format, error type, line, column and the key at the error position (if the parser can tell
it), and adds the parser's message only where the content cannot be a secret value.

The message is **left out** in these cases:

| Case                                                                                                                                                   | Why the content may be secret                                       | Schema needed? |
|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|----------------|
| the schema marks the entry at the error position `SECRET`                                                                                              | declared secret                                                     | yes            |
| the schema marks an entry above or below the error position `SECRET` (e.g. error in `db`, secret `db/password`)                                        | the broken structure may contain the secret                         | yes            |
| the parser cannot tell the position (SnakeYAML; errors before the first key), and the schema has any `SECRET` entry                                    | the error may be anywhere, the secret included                      | yes            |
| the content comes from a **Kubernetes Secret**                                                                                                         | secret by its source                                                | no             |
| the configuration is a **bootstrap configuration of a cloud module** (`vault`, `aws`, `zookeeper`, `kubernetes`: read through `getRestrictedConfig()`) | holds credentials such as a Vault token, typically without a schema | no             |

In all other cases - no schema, or a schema without `SECRET` entries near the error - the parser's
message is shown, escaped like all outside text. A secret in a file whose schema does not mark it
`SECRET` is not recognised; mark it.

Reading from a secret store can fail outside the format parsers too, e.g. when a client library cannot
handle a response, and its message may then quote the response. Such failures are reported without
the message:

- **Vault**: error type, plus the HTTP status where the client knows it.
- **Kubernetes Secrets**: error type only. An HTTP error answer of the API server (e.g. 403) is
  reported with its status and the server's error text, which describes the request, not the Secret.
- **AWS Secrets Manager**: with the AWS SDK's message, which carries the service's error text, not
  the secret; a secret that is not a flat JSON object falls back to the raw value without a report.

[`LOG_PARSE_ERROR_DETAILS`](25_config_features.md), set in code, adds the parser's message in every
case. It is meant for development: with it, a broken line holding a secret value goes into the log.

## Comparison

| Aspect                     | SECRET                                     | SENSITIVE                                                  |
|----------------------------|--------------------------------------------|------------------------------------------------------------|
| Reason for protection      | security: compromise of a system           | privacy: harm to a person, regulatory obligation           |
| Typical content            | keys, tokens, passwords, license IDs       | names, e-mail and postal addresses, user IDs, IP addresses |
| Value representation       | changed: `SecretValue`, avoids `String`    | unchanged: ordinary typed value                            |
| Access                     | `getSecret()`, then `erase()` after use    | ordinary `getString()` and other typed getters             |
| Lifetime in memory         | as short as possible                       | the application's normal working lifetime                  |
| Logs and `toString()`      | redacted                                   | redacted                                                   |
| Export, diagnostics, dumps | value never in plain text                  | value suppressed                                           |
| Storage location           | always traceable                           | always traceable                                           |
| Rotation                   | meaningful; sources may rotate credentials | not a meaningful concept                                   |
| Erasure request            | not applicable                             | applicable (e.g. GDPR Art. 17)                             |
| Primary audience           | security / threat model                    | data protection officer, legal, data inventory             |

## Where the marking comes from

A **SECRET** marking has two possible origins:

1. the configuration schema, via `"SECRET": true`, and
2. the configuration source itself — Vault, AWS Secrets Manager and the Kubernetes modules
   deliver entries that are secret by their nature, with no schema involved.

A **SENSITIVE** marking currently has one origin: the configuration schema. No storage backend
carries a notion of personal data today.

## Storage suitability

Not every storage place is a fit for every kind of information. A storage module may decline to
hold SECRET data, and may equally decline to hold SENSITIVE data, on the grounds that *this storage
place is not safe for the type of information being handled*.

The rule for secrets: **a secret is only ever written to a location that is acceptable for
secrets, and never moved to a place less protected than its home.** If no acceptable location
exists, the write is refused rather than moved further.

The mechanism is the normal write path. A write stays in the scope it was made for and goes through
that scope's locations in priority order. A storage declines a location by not offering a writeable
layer there, or by not creating one; the write then moves on to the next location of the scope.
A storage can also refuse a single entry when it is written - the entry tells whether it is
secret. Such a refusal currently ends the write with an error instead of moving on, which is the
safe behaviour under the rule above.

The storages shipped today do not decline secrets: they treat a secret like any other value of the
location, protected by the location's own access control.

Suitability is decided per storage type. Finer granularity — different handling for different
sub-paths within one storage, such as branches of the Windows registry — is a possible later
refinement and is not part of the model today.

## Implementation status

Be aware of what is in place and what is not:

| Marker      | Status                                                                                                                                                 |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SECRET`    | implemented, via the schema field `"SECRET": true` and via secret-aware sources                                                                        |
| `SENSITIVE` | **defined but not yet enforced**: it can be described here and reasoned about, but marking an entry does not yet cause redaction or export suppression |

Do not rely on `SENSITIVE` for compliance until it is enforced. Until then, an entry whose
disclosure would be damaging should be marked `SECRET`, accepting the stricter handling.

## Next Steps
For practical examples on how to use secrets in your application, see [Handling Secrets](35_handling_secrets.md).
For the schema fields that carry these markings, see [Configuration Schemas](23_configuration_schemas.md).