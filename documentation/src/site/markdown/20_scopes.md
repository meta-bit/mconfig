# 2.0 Scopes

mConfig uses the concept of hierarchical **Scopes** to manage configuration precedence.
This is a core API-level concept that determines which configuration values "win" when the same key is defined in multiple places.

You can think of it as a layered fall-back hierarchy.

## The Scope Hierarchy

The software may have generic defaults, which can be overridden by settings of the individual local installation.
These, in turn, can be overridden by user-based settings, session-specific environment variables, or even enforced by system policies.

**Scope order (from lowest to highest priority):**

`PRODUCT` → `ORGANIZATION` → `CLOUD` → `CLUSTER` → `HOST` → `APPLICATION` → `USER` → `SESSION` → `RUNTIME` → `POLICY`

| Priority | Scope            | Intended Use                                            | Typical Sources                      |
|:---------|:-----------------|:--------------------------------------------------------|:-------------------------------------|
| 10       | **PRODUCT**      | Defaults shipped with the software.                     | ConfigSchema defaults, JAR resources |
| 9        | **ORGANIZATION** | Company-level defaults and licensing.                   | Registry, network services           |
| 8        | **CLOUD**        | Cloud-based configurations, shared across clusters.     | Cloud providers, network services    |
| 7        | **CLUSTER**      | Cluster-wide settings.                                  | ZooKeeper, network services          |
| 6        | **HOST**         | System-wide settings for one host.                      | `/etc/`, `%ProgramData%`, Registry   |
| 5        | **APPLICATION**  | Installation-specific settings (portable/side-by-side). | Filesystem near the binary           |
| 4        | **USER**         | Per-user overrides and preferences.                     | `~/.config/`, `%AppData%`, Registry  |
| 3        | **SESSION**      | Shell/session overrides.                                | Environment variables, command-line  |
| 2        | **RUNTIME**      | In-memory changes during program execution.             | RAM layer (`config.put(...)`)        |
| 1        | **POLICY**       | Enforced overrides (highest priority).                  | GPO, Registry, policy files          |

## Why Scopes Matter

Understanding scopes is essential for both developing and deploying applications with mConfig:

1.  **For Developers**: Scopes allow you to provide sensible defaults in the `PRODUCT` scope while allowing any value to be overridden by the environment or the user without changing your code.
2.  **For System Administrators**: Scopes like `HOST` and `POLICY` provide a standard way to enforce settings across an entire machine or organization, ensuring security and compliance.
3.  **For Users**: The `USER` scope ensures that personal preferences are preserved regardless of system-wide updates or changes.
4.  **For CI/CD and Testing**: `SESSION` and `RUNTIME` scopes are perfect for passing temporary configuration or mocking settings during automated tests.

## Resolution Logic

When you request a configuration value, mConfig searches through the layers from the highest priority scope down to the lowest. The first scope that contains the requested key provides the value.

Within the **same scope**, mConfig may have multiple sources (e.g., several directories in the `HOST` scope). In this case, the order is determined by the **Search List**. See [Priorities and Hierarchies](22_priorities_and_hierarchies.md) for more details on how ties are broken and how search paths are constructed.

Calling code can also limit which scopes are considered during resolution, allowing for fine-grained control over configuration precedence.
 