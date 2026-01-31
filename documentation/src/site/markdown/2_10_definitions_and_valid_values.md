# 2.10 Definitions and valid values

## 2.10.1 Configuration names

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
