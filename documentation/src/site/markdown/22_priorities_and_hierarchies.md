# 2.2 Priorities and Hierarchies


## 2.2.1 Search List and Priority
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

