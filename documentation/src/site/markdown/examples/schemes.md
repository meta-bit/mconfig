# Configuration Scheme Examples

This page provides various examples of configuration schemes, ranging from simple key-value pairs to complex nested structures with validation.

## 1. Simple Application Configuration

A basic scheme for a small application with a few parameters.

```json
[
  {
    "KEY": "app/name",
    "TYPE": "STRING",
    "DESCRIPTION": "The name of the application.",
    "DEFAULT": "My Awesome App"
  },
  {
    "KEY": "app/version",
    "TYPE": "STRING",
    "PATTERN": "\\d+\\.\\d+\\.\\d+",
    "DESCRIPTION": "Application version (SemVer).",
    "MANDATORY": true
  },
  {
    "KEY": "app/debug",
    "TYPE": "BOOLEAN",
    "DEFAULT": false
  }
]
```

## 2. Network and Server Settings

Using numeric ranges, enums, and list types.

```json
{
  "network-settings": {
    "NAME": "network-settings",
    "ENTRIES": [
      {
        "KEY": "server/host",
        "TYPE": "STRING",
        "DEFAULT": "localhost",
        "DESCRIPTION": "Hostname or IP address."
      },
      {
        "KEY": "server/port",
        "TYPE": "NUMBER",
        "PATTERN": "uint16",
        "DEFAULT": 8080,
        "DESCRIPTION": "Port number (0-65535)."
      },
      {
        "KEY": "server/logLevel",
        "TYPE": "ENUM",
        "PATTERN": "DEBUG|INFO|WARN|ERROR",
        "DEFAULT": "INFO"
      },
      {
        "KEY": "server/allowedIps",
        "TYPE": "MULTIPLE_STRINGS",
        "ARITY": "1..*",
        "DESCRIPTION": "A list of IP addresses that are allowed to connect."
      }
    ]
  }
}
```

## 3. Database Configuration with Secrets

Handling sensitive data and mandatory fields.

```json
[
  {
    "KEY": "db/url",
    "TYPE": "STRING",
    "MANDATORY": true,
    "DESCRIPTION": "JDBC connection URL."
  },
  {
    "KEY": "db/username",
    "TYPE": "STRING",
    "DEFAULT": "admin"
  },
  {
    "KEY": "db/password",
    "TYPE": "STRING",
    "SECRET": true,
    "DESCRIPTION": "Database password (will be redacted in logs)."
  },
  {
    "KEY": "db/poolSize",
    "TYPE": "NUMBER",
    "PATTERN": "[1, 50]",
    "DEFAULT": 10
  }
]
```

## 4. Complex Validation with Arity

Using `ENUM_SET` and arity constraints.

```json
{
  "advanced-features": {
    "ENTRIES": [
      {
        "KEY": "features/enabled",
        "TYPE": "ENUM_SET",
        "PATTERN": "LOGGING|METRICS|TRACING|AUTH",
        "ARITY": "1..3",
        "DEFAULT": ["LOGGING", "METRICS"],
        "DESCRIPTION": "Select up to 3 features to enable."
      }
    ]
  }
}
```
