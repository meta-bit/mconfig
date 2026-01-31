# ZooKeeper Tree Design Plan

This document outlines the plan to transition `mConfigSourceZooKeeper` from a BLOB-based design (where one ZNode contains a whole formatted configuration file) to an alternative Tree design (where each ZNode in a hierarchy represents a single configuration entry).

## 1. Core Objectives
- Map ZooKeeper ZNode hierarchy directly to mConfig configuration hierarchy.
- Example: ZNode `/mconfig/app/database/host` maps to the mConfig key `database/host`.
- Support granular updates and watches.

## 2. Architectural Changes

### 2.1 New ConfigLayer Implementation
- Create `ZooKeeperTreeConfigLayer` (implementing `ConfigLayerInterface`).
- This layer will not use standard `ConfigFormat` parsers (Jackson/Properties).
- It will wrap a Curator `CuratorCache` (or `TreeCache`) for a specific subtree.
- `getEntry(key)` will look up the corresponding path in the local cache.

### 2.2 Value Interpretation
- Since ZooKeeper stores data as `byte[]`, the tree layer needs a strategy to interpret values.
- Default: Treat everything as String (UTF-8) and rely on mConfig facade for conversion.
- Enhancement: Support `ConfigScheme` to cast types early.

### 2.3 Storage Implementation
- Modify `ZooKeeperConfigStorage.tryToReadConfigurationLayers`.
- Introduce a new feature: `ZOOKEEPER_USE_TREE_LAYOUT` (Boolean).
- If `true`, the storage will identify if a path is a "directory" (has children) or a "file" (BLOB).
- If it's a directory structure, instantiate `ZooKeeperTreeConfigLayer`.

## 3. Implementation Steps

### Phase 1: Storage Engine Enhancements
- Logic for recursive traversal and `CuratorCache` integration for trees.
- Update `listAvailableConfigurations` to distinguish between ZNodes with children and ZNodes with data.

### Phase 2: Layer Implementation
- `ZooKeeperTreeConfigLayer` implementation.
- Entry mapping (ZNode path <-> Hierarchical Key).
- Implement `tryToGetKeyIterator` for discovery.

### Phase 3: Writing Support
- Implement `writeEntry(ConfigEntry)` in the tree layer.
- Map the hierarchical key back to a ZNode path.
- Perform `client.setData()` or `client.create()` (atomic per key).

### Phase 4: Watcher Integration
- Use a `CuratorCache` on the root of the configuration tree.
- `hasChangedSincePreviousCheck` must identify exactly which subtree/key changed.

## 4. Comparison Table

| Feature | BLOB (Current) | Tree (Proposed) |
| :--- | :--- | :--- |
| **Efficiency** | High for reading whole files; Low for single key updates. | High for single key access/updates. |
| **Atomicity** | Atomic for the entire configuration. | Atomic only per individual key. |
| **Format** | Supports JSON, YAML, TOML natively. | Pure Key-Value; requires manual type interpretation. |
| **Complexity** | Low (reuses existing parsers). | Medium (requires custom tree-traversal logic). |

## 5. Lessons Learned from BLOB implementation
- Watchers on single nodes are easy, but mConfig handles (InputStream vs Path) make change detection tricky.
- `CuratorCache` is preferred over `NodeCache`/`PathChildrenCache`.
- Self-configuration startup sequence must be preserved.
