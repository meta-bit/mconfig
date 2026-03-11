/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */
package org.metabit.platform.support.config;

import java.time.Instant;
import java.util.Map;

/**
 * if something happens which is not an Exception (interrupting program flow),
 * but not debug output for programmers either (which would go to logging).
 * These events are something in between which you may want to evaluate
 * from caller code under some circumstances and communicate accordingly.
 *
 * @version $Id: $Id
 */
public interface ConfigEvent
{
    /**
     * @return the time when this event occurred.
     */
    Instant getTimestamp();

    /**
     * @return the severity of this event.
     */
    Severity getSeverity();

    /**
     * @return the domain this event belongs to.
     */
    Domain getDomain();

    /**
     * @return the specific kind of event within the domain.
     */
    Kind getKind();

    /**
     * @return a stable machine-readable code for this specific event type.
     */
    String getDetailCode();

    /**
     * @return a human-readable message describing the event.
     */
    String getMessage();

    /**
     * @return the location where this event originated, or null if global/unknown.
     */
    ConfigLocation getLocation();

    /**
     * @return the configuration scope associated with this event, or null if not applicable.
     */
    ConfigScope getScope();

    /**
     * @return the name of the configuration this event relates to, or null if not applicable.
     */
    String getConfigName();

    /**
     * @return the key path within the configuration this event relates to, or null if not applicable.
     */
    String getKeyPath();

    /**
     * @return the format identifier associated with this event, or null if not applicable.
     */
    String getFormatId();

    /**
     * @return an unmodifiable map of additional attributes.
     */
    Map<String, String> getAttributes();

    /**
     * @return the underlying cause of this event, or null if none.
     */
    Throwable getCause();

    /**
     * @return a programmatic remediation hint.
     */
    Remediation getRemediation();

    /**
     * @return a free-form human-readable remediation message.
     */
    String getRemediationMessage();

    enum Severity
    {
        INFO,
        NOTICE,
        WARNING,
        ERROR
    }

    enum Domain
    {
        DISCOVERY,
        PARSE,
        VALIDATION,
        MERGE,
        WRITE,
        RELOAD,
        WATCHER,
        POLICY,
        OTHER
    }

    enum Kind
    {
        // DISCOVERY
        SKIPPED_UNREADABLE,
        SKIPPED_PERMISSION_DENIED,
        FORMAT_AMBIGUOUS,

        // PARSE
        FAILED_GENERIC,
        UNSUPPORTED_SYNTAX,
        TRAILING_COMMA,
        INVALID_COMMENT,
        INVALID_ENCODING,

        // VALIDATION
        SCHEMA_WARNING,
        SCHEMA_ERROR,
        DEFAULT_APPLIED,
        DEPRECATED_FIELD,

        // MERGE
        OVERRIDE_APPLIED,
        CONFLICT_RESOLVED,

        // WRITE
        REFUSED_NOT_WRITEABLE,
        BLOCKED_POLICY,
        FAILED_CONVERSION,

        // RELOAD
        APPLIED,
        NO_CHANGES,

        // WATCHER
        SOURCE_CHANGE_DETECTED,
        DEBOUNCE_SUPPRESSED,

        // POLICY
        ENFORCED,

        // OTHER
        MISC
    }

    enum Remediation
    {
        NONE,
        CHANGE_FORMAT,
        FIX_SYNTAX,
        CHECK_PERMISSIONS,
        ADJUST_SCOPE,
        RENAME_EXTENSION,
        RETRY_LATER,
        REPORT_BUG
    }
}
