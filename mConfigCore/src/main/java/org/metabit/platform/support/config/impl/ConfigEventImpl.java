package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable implementation of {@link ConfigEvent}.
 */
public final class ConfigEventImpl implements ConfigEvent
{
    private final Instant             timestamp;
    private final Severity            severity;
    private final Domain              domain;
    private final Kind                kind;
    private final String              detailCode;
    private final String              message;
    private final ConfigLocation      location;
    private final ConfigScope         scope;
    private final String              configName;
    private final String              keyPath;
    private final String              formatId;
    private final Map<String, String> attributes;
    private final Throwable           cause;
    private final Remediation         remediation;
    private final String              remediationMessage;

    private ConfigEventImpl(Builder builder)
        {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.severity = Objects.requireNonNull(builder.severity, "severity must not be null");
        this.domain = Objects.requireNonNull(builder.domain, "domain must not be null");
        this.kind = Objects.requireNonNull(builder.kind, "kind must not be null");
        this.detailCode = builder.detailCode;
        this.message = builder.message;
        this.location = builder.location;
        this.scope = builder.scope;
        this.configName = builder.configName;
        this.keyPath = builder.keyPath;
        this.formatId = builder.formatId;
        this.attributes = builder.attributes != null ? Collections.unmodifiableMap(new HashMap<>(builder.attributes)) : Collections.emptyMap();
        this.cause = builder.cause;
        this.remediation = builder.remediation != null ? builder.remediation : Remediation.NONE;
        this.remediationMessage = builder.remediationMessage;
        }

    @Override public Instant getTimestamp() { return timestamp; }
    @Override public Severity getSeverity() { return severity; }
    @Override public Domain getDomain() { return domain; }
    @Override public Kind getKind() { return kind; }
    @Override public String getDetailCode() { return detailCode; }
    @Override public String getMessage() { return message; }
    @Override public ConfigLocation getLocation() { return location; }
    @Override public ConfigScope getScope() { return scope; }
    @Override public String getConfigName() { return configName; }
    @Override public String getKeyPath() { return keyPath; }
    @Override public String getFormatId() { return formatId; }
    @Override public Map<String, String> getAttributes() { return attributes; }
    @Override public Throwable getCause() { return cause; }
    @Override public Remediation getRemediation() { return remediation; }
    @Override public String getRemediationMessage() { return remediationMessage; }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigEventImpl that = (ConfigEventImpl) o;
        return Objects.equals(severity, that.severity) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(kind, that.kind) &&
                Objects.equals(detailCode, that.detailCode) &&
                Objects.equals(message, that.message) &&
                Objects.equals(location, that.location) &&
                Objects.equals(scope, that.scope) &&
                Objects.equals(configName, that.configName) &&
                Objects.equals(keyPath, that.keyPath) &&
                Objects.equals(formatId, that.formatId) &&
                Objects.equals(attributes, that.attributes) &&
                Objects.equals(remediation, that.remediation) &&
                Objects.equals(remediationMessage, that.remediationMessage);
        // timestamp and cause are excluded from equality to help with de-duplication of recurring events
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(severity, domain, kind, detailCode, message, location, scope, configName, keyPath, formatId, attributes, remediation, remediationMessage);
        }

    @Override
    public String toString()
        {
        return "ConfigEvent[" + severity + "][" + domain + "/" + kind + "] " + message + (location != null ? " @ " + location.toLocationString() : "");
        }

    public static Builder builder()
        {
        return new Builder();
        }

    public static final class Builder
    {
        private Instant             timestamp;
        private Severity            severity;
        private Domain              domain;
        private Kind                kind;
        private String              detailCode;
        private String              message;
        private ConfigLocation      location;
        private ConfigScope         scope;
        private String              configName;
        private String              keyPath;
        private String              formatId;
        private Map<String, String> attributes;
        private Throwable           cause;
        private Remediation         remediation;
        private String              remediationMessage;

        private Builder() {}

        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder severity(Severity severity) { this.severity = severity; return this; }
        public Builder domain(Domain domain) { this.domain = domain; return this; }
        public Builder kind(Kind kind) { this.kind = kind; return this; }
        public Builder detailCode(String detailCode) { this.detailCode = detailCode; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder location(ConfigLocation location) { this.location = location; return this; }
        public Builder scope(ConfigScope scope) { this.scope = scope; return this; }
        public Builder configName(String configName) { this.configName = configName; return this; }
        public Builder keyPath(String keyPath) { this.keyPath = keyPath; return this; }
        public Builder formatId(String formatId) { this.formatId = formatId; return this; }
        public Builder attributes(Map<String, String> attributes) { this.attributes = attributes; return this; }
        public Builder cause(Throwable cause) { this.cause = cause; return this; }
        public Builder remediation(Remediation remediation) { this.remediation = remediation; return this; }
        public Builder remediationMessage(String remediationMessage) { this.remediationMessage = remediationMessage; return this; }

        public ConfigEventImpl build()
            {
            return new ConfigEventImpl(this);
            }
    }
}
