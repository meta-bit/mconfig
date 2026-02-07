# Simple Configuration Loading

This example shows the minimal setup for loading a configuration and reading a few values.

## 1. Create a factory and configuration
```java
try (ConfigFactory factory = ConfigFactoryBuilder.create("ACME", "ourApp").build())
    {
    Configuration cfg = factory.getConfiguration("network");

    String host = cfg.getString("server/host");
    int port = cfg.getInteger("server/port");
    boolean tls = cfg.getBoolean("server/tls");
    }
```

## 2. Provide defaults (recommended)
Add defaults in your resources so the configuration works out of the box:
`src/main/resources/.config/ACME/ourApp/network.properties`

Example contents:
```properties
server.host=127.0.0.1
server.port=8080
server.tls=false
```

## 3. Optional: add a scheme
Add a scheme next to the config file to define types and defaults:
`src/main/resources/.config/ACME/ourApp/network.scheme.json`

```json
[
  { "KEY": "server/host", "TYPE": "STRING", "DEFAULT": "127.0.0.1" },
  { "KEY": "server/port", "TYPE": "NUMBER", "DEFAULT": 8080 },
  { "KEY": "server/tls", "TYPE": "BOOLEAN", "DEFAULT": false }
]
```

Related docs:
- [Getting Started](../13_getting_started.md)
- [Configuration Schemes](../23_configuration_schemes.md)
