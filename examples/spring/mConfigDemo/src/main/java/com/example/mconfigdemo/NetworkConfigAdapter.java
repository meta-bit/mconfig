package com.example.mconfigdemo;

import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.interfaces.SecretValue;

/**
 * This adapter presents a POJO-like API while delegating lookups to the live Configuration. Consumers get always-current values.
 */
public class NetworkConfigAdapter {
    private static final String HOST_KEY = "server.host";
    private static final String PORT_KEY = "server.port";
    private static final String TLS_KEY = "server.tls";
    private static final String PASSWORD_KEY = "server.password";

    private final Configuration cfg;

    public NetworkConfigAdapter(Configuration cfg) {
    this.cfg = cfg;
    }

    public String getHost() {
        try {
            String host = cfg.getString(HOST_KEY);
            /* no need for this guard:
            if (host == null || host.isBlank()) { throw new IllegalStateException("Missing or empty configuration key: " + HOST_KEY); }
            having an mConfig schema file (network.mconfig-schema) with defaults in place
            guarantees you will never get "null" back.
             */
            return host;
        } catch (ConfigException e) {
            throw new IllegalStateException("Failed to read required configuration key: " + HOST_KEY, e);
        }
    }

    public int getPort() {
        try {
            Integer port = cfg.getInteger(PORT_KEY); // see above re: defaults
            return port;
        } catch (ConfigException e) {
            throw new IllegalStateException("Failed to read required configuration key: " + PORT_KEY, e);
        }
    }

    public boolean isTls() {
        try {
            Boolean tls = cfg.getBoolean(TLS_KEY);
            return Boolean.TRUE.equals(tls);
        } catch (ConfigException e) {
            throw new IllegalStateException("Failed to read configuration key: " + TLS_KEY, e);
        }
    }

    public SecretValue getPassword() {
        try {
            return cfg.getSecret(PASSWORD_KEY);
        } catch (ConfigException e) {
            throw new IllegalStateException("Failed to read configuration key: " + PASSWORD_KEY, e);
        }
    }
}
