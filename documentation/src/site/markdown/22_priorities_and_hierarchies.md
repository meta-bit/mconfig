# 2.2 Priorities and Hierarchies


## 2.2.1 Search List and Priority
The `ConfigFactory` maintains an ordered **Search List** of locations. This order is crucial for two reasons:

1.  **Finding and Creating Configurations:** When looking for a configuration
    or creating a new one (e.g., via `ConfigUtil.whereToPutMyFiles`),
    mConfig searches through the list and picks the **first** matching writeable location.
    Thus, entries earlier in the search list have higher priority for discovery and creation.
2.  **Layered Overrides (Reading):** Configurations are composed of multiple layers.
    Layers from more specific [Scopes](20_scopes.md) (e.g., `RUNTIME`) always override more
    generic scopes (e.g., `SYSTEM`).
    Within the **same scope**, however, the search list determines priority:
    layers from locations appearing **later** in the search list are added later
    to the configuration stack and thus **override** layers from locations
    appearing earlier in the same scope.

## 2.2.2 Potential (Candidate) Locations vs. Instantiated Layers

An important architectural aspect of mConfig is the separation between candidate search locations and active configuration layers:

- **Candidate Locations (`ConfigLocation` via `ConfigFactory.getSearchList()`):**
  The factory's search list contains all potential `ConfigLocation`s evaluated 
   by active storage providers (e.g., standard filesystem paths across scopes). 
   Candidate directories are retained in the search list even if they do not 
   exist on disk at initialization time. When a missing directory or file
   is created at runtime, storages that watch their locations (such as the
   filesystem storage) detect it and bind the new source.
- **Active Locations (`ConfigLocation` via `Configuration.getSourceLocations()`):**
  Represents only the locations that currently exist and contributed
  active data layers to a specific `Configuration` handle.

- **Active Layers (`ConfigLayerInterface`):**
  The concrete in-memory representations of parsed configuration entries
  contributing to the configuration stack. These are internal; library code
  reaches them via `LayeredConfiguration.getLayers()`, which is not part of
  the public `Configuration` API.

- **Discovered Configurations (`ConfigDiscoveryInfo` via `ConfigFactory.listAvailableConfigurations()`):**
  Scans all candidate locations in `getSearchList()` and returns metadata for configuration files that currently exist.

