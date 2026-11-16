# 3.4 Definitions and valid values

## 3.4.1 Configuration names

Configuration names identify which configuration to access.
This needs to be portable across different storage backends and platforms
(files, environment variables, JAR resources, etc.).

These rules aim to minimize surprises and increase security.

0. `null` is not allowed and will result in an error.
1. The empty string "" is technically allowed, but reserved for internal aggregation use cases. Avoid using "" in normal application code.
2. Whitespace handling
    - Leading and trailing whitespace are actively omitted.
    - Multiple internal whitespace characters collapse to a single space.
    - on some backends, whitespace is replaced by underscores.

3. Allowed characters (portable set)
    - Letters A–Z, a–z
    - Digits 0–9
    - Dot .
    - Underscore _
    - Hyphen -
    - Space
    - In regex form: [A-Za-z0-9._- ] (spaces are allowed)

Some sources (e.g. Filesystem) may sanitize dot usage to guard against path traversal attacks.

3. Configuration names are case sensitive.

- Examples
    - Valid: "testconfig", "my.config", "my_config", "my-config", "My Config", "prod.v2"
    - invalid: "my/config", "my:config", "my*config", "conf|g", "config<>", "config\tname"

## 3.4.2 File and directory names from the filesystem

Names that come from the filesystem or the operating system, rather than from the application, are
checked as well. Since 0.8.15 the filesystem storage does not use:

- a **search directory** whose path contains control characters (line breaks, tabs, escape
  sequences, other C0/C1 controls), Unicode line or paragraph separators, or bidirectional-override
  characters. This covers directories from environment variables (`HOME`, `XDG_CONFIG_HOME`,
  `APPDATA`, …), from settings such as `ADDITIONAL_USER_DIRECTORIES`, and test-mode directories.
  The directory is not added to the search list; a WARNING names it.
- a **`.d` fragment** (a file in `<config>.d/`) whose file name contains such characters. It is
  skipped with a WARNING and a config event (`SKIPPED_UNREADABLE`, detail code
  `FS_DISCOVERY_UNSAFE_NAME`).

Such names are never legitimate for configuration, and they could forge or garble log output.
Rename the file or directory to use it. See also
[Outside text in log messages](42_logging.md#outside-text-in-log-messages).
