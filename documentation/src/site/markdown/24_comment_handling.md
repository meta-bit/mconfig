# 2.4 Comment Handling in mConfig

Of the possible configuration sources, files are the ones that support comments. 
Comments in configuration files are an important source of information for users. They can be used to explain configuration options, document decisions, provide context, or include relevant links.

mConfig aims to keep comments intact in configuration files to ensure that human-readable information is not lost during programmatic updates.

## Core Principles

- **Write-Preservation**: Writing to a configuration file should not erase existing comments.
- **Read-Write Symmetry**: Maintaining the original structure and documentation of the file is a priority.
- **Human-Centric**: Comments are for humans. They should not be used for machine processing or "shebang" style logic.

## Programmatic Comments

While mConfig prioritizes preserving existing comments, it also supports adding comments programmatically:

1.  **Description from Scheme**: When an entry is first created, mConfig can automatically include the description defined in the `ConfigScheme` as a comment. This is controlled by the `ConfigFeature.DESCRIPTION_ON_CREATE` feature.
2.  **Explicit Comments**: Developers can add comments to a `ConfigEntry` using the `setComment(String)` method. This is useful for documenting automated changes (e.g., "# AUTO-REDUCED: Disk space was below 5%").

## Configuration Features

The behavior of comment handling is governed by several `ConfigFeature` flags:

- **`ConfigFeature.COMMENTS_WRITING`** (default: `false`): When set to `true`, mConfig will write programmatic comments to the configuration. Even when `false`, the library still attempts to preserve *existing* comments for supported formats.
- **`ConfigFeature.COMMENTS_READING`** (default: `false`): Enables reading comments from files into the `ConfigEntry` objects. Note that determining exactly which lines belong to which key is heuristic-based and may not be perfect for all formats.
- **`ConfigFeature.DESCRIPTION_ON_CREATE`** (default: `false`): When `true`, uses the `description` field from the `ConfigSchemeEntry` to populate comments for new entries.

## Format Support Matrix

Preservation of comments depends on the underlying format module. Some high-level libraries (like Jackson) may strip comments by design.

| File Format | mConfig Module | Preserve Comments |
| :--- | :--- | :--- |
| `.properties` | `mConfigFormatJavaProperties` | Yes |
| `INI` | `mConfigFormatINI` | Yes |
| `TOML` | `mConfigFormatTOML` | Yes |
| `YAML` | `mConfigFormatYAMLwithSnakeYAML` | Partially |
| `TOML` | `mConfigFormatTOMLwithJackson` | No |
| `YAML` | `mConfigFormatYAMLwithJackson` | No |
| `JSON` | `mConfigFormatJSONwithJackson` | No |
| `JSON5` | `mConfigFormatJSONwithJackson` | No |

## Implementation Details

### Multi-line Comments
- **Representation**: Represented as a single `String` with `\n` line separators.
- **Markers**: On reading, format-specific markers (e.g., `#`, `!`, `;`) are preserved.
- **Writing**: mConfig ensures each line is correctly prefixed with the format's primary marker if it's missing.

### Heuristics and Association
- **Leading Comments**: Consecutive comment lines immediately preceding a key are associated with that key.
- **Global Headers**: Comment blocks at the top of a file (or between entries) separated by empty lines are treated as "Global Headers". They are preserved and rewritten at the top of the file but are not attached to any specific `ConfigEntry`.
- **Inline Comments**: Comments on the same line as a value are appended to the entry's comment string on a best-effort basis.

### Priority and Merging
When writing an entry, comments are merged based on the following priority (top to bottom):
1.  **Description from Scheme**: If `DESCRIPTION_ON_CREATE=true` and the entry is new.
2.  **Existing Comments**: Preserved from the original file.
3.  **Programmatic Comments**: Added via `ConfigEntry.setComment()`.

*Duplicate lines are automatically filtered during the merge process.*
