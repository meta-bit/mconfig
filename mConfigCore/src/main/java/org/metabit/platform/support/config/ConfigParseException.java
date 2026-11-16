/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.config;

import org.metabit.platform.support.config.impl.logging.LogSafe;

/**
 * A format could not parse the content handed to it.
 * <p>
 * Thrown by {@code ConfigFileFormatInterface.readFile()} and {@code readStream()}; caught by the calling storage, which
 * reports it with the context only it knows (configuration, schema, whether the location may hold secrets).
 * <p>
 * <b>Safe by default.</b> A parser's own message may quote the content it failed on, which can be a secret value.
 * {@link #getMessage()} therefore contains only the safe parts: format, error type, line, column and key path.
 * The parser's text and the original exception are available through {@link #getParserDetail()} and
 * {@link #getParserCause()} only; they are not the {@link #getCause()} of this exception, so logging this exception
 * as a whole does not disclose them.
 *
 * @since 0.8.17
 */
public class ConfigParseException extends ConfigCheckedException
{
    private final String                  formatId;
    private final int                     line;
    private final int                     column;
    private final String                  keyPath;
    private final String                  errorType;
    private final String                  summary;
    private final transient String        parserDetail;
    private final transient Throwable     parserCause;
    private final ConfigEvent.Kind        eventKind;
    private final String                  detailCode;
    private final ConfigEvent.Remediation remediation;
    private final String                  remediationMessage;

    private ConfigParseException(Builder b)
        {
        super(ConfigExceptionReason.INPUT_INVALID);
        this.formatId = b.formatId;
        this.line = b.line;
        this.column = b.column;
        this.keyPath = b.keyPath;
        this.errorType = (b.parserCause != null) ? b.parserCause.getClass().getSimpleName() : b.errorType;
        this.summary = b.summary;
        this.parserDetail = b.parserDetail;
        this.parserCause = b.parserCause;
        this.eventKind = b.eventKind;
        this.detailCode = b.detailCode;
        this.remediation = b.remediation;
        this.remediationMessage = b.remediationMessage;
        }

    /**
     * start a parse exception.
     *
     * @param formatId the ID of the format reporting it
     * @param summary  a fixed, content-free description, e.g. "not valid JSON"; never text from the content
     * @return builder
     */
    public static Builder builder(String formatId, String summary)
        { return new Builder(formatId, summary); }

    /**
     * the safe parts only; see the class description. Key names come from the content, so the key path is escaped
     * (CWE-117), as is the summary in case a format puts outside text into it. {@link #getKeyPath()} stays raw.
     */
    @Override
    public String getMessage()
        {
        StringBuilder sb = new StringBuilder();
        sb.append(LogSafe.text(summary));
        if (errorType != null)
            { sb.append(" (").append(errorType).append(')'); }
        if (line > 0)
            {
            sb.append(" at line ").append(line);
            if (column > 0)
                { sb.append(", column ").append(column); }
            }
        if (keyPath != null)
            { sb.append(", key ").append(LogSafe.text(keyPath)); }
        return sb.toString();
        }

    public String getFormatId()                    { return formatId; }
    /** @return 1-based line, or -1 if unknown */
    public int getLine()                           { return line; }
    /** @return 1-based column, or -1 if unknown */
    public int getColumn()                         { return column; }
    /** @return the key at the error position in mConfig form ("a/b/c"), or null if the parser cannot tell */
    public String getKeyPath()                     { return keyPath; }
    /** @return the parser's exception type (simple class name), or null */
    public String getErrorType()                   { return errorType; }
    /** @return the fixed, content-free description given by the format */
    public String getSummary()                     { return summary; }
    public ConfigEvent.Kind getEventKind()         { return eventKind; }
    public String getDetailCode()                  { return detailCode; }
    public ConfigEvent.Remediation getRemediation(){ return remediation; }
    public String getRemediationMessage()          { return remediationMessage; }

    /**
     * the parser's own message. May quote content, secret values included; not escaped.
     * For the reporting code in core only, which decides whether it may be shown.
     *
     * @return the text, or null
     */
    public String getParserDetail()                { return parserDetail; }

    /**
     * the parser's original exception. Its message is the parser detail; same restrictions.
     *
     * @return the exception, or null
     */
    public Throwable getParserCause()              { return parserCause; }

    /** builder; formats fill in what their parser provides. */
    public static final class Builder
    {
        private final String                  formatId;
        private final String                  summary;
        private int                           line   = -1;
        private int                           column = -1;
        private String                        keyPath;
        private String                        errorType;
        private String                        parserDetail;
        private Throwable                     parserCause;
        private ConfigEvent.Kind              eventKind   = ConfigEvent.Kind.FAILED_GENERIC;
        private String                        detailCode;
        private ConfigEvent.Remediation       remediation = ConfigEvent.Remediation.FIX_SYNTAX;
        private String                        remediationMessage;

        private Builder(String formatId, String summary)
            {
            this.formatId = formatId;
            this.summary = (summary != null) ? summary : "parse error";
            }

        /** 1-based line and column; values below 1 mean unknown. */
        public Builder position(int line, int column)
            {
            this.line = (line > 0) ? line : -1;
            this.column = (column > 0) ? column : -1;
            return this;
            }

        /** key at the error position, mConfig form with "/" as separator; leading "/" is removed, empty means unknown. */
        public Builder keyPath(String keyPath)
            {
            String k = (keyPath == null) ? null : (keyPath.startsWith("/") ? keyPath.substring(1) : keyPath);
            this.keyPath = (k == null || k.isEmpty()) ? null : k;
            return this;
            }

        /** the parser's exception: its type is safe to show, its message is kept as parser detail. */
        public Builder parserCause(Throwable cause)
            {
            this.parserCause = cause;
            if (cause != null && this.parserDetail == null)
                { this.parserDetail = cause.getMessage(); }
            return this;
            }

        /** the parser's text, if it is not (only) the cause's message. */
        public Builder parserDetail(String detail)
            {
            this.parserDetail = detail;
            return this;
            }

        /** error type, if there is no parser exception (e.g. an own parser). */
        public Builder errorType(String errorType)
            {
            this.errorType = errorType;
            return this;
            }

        public Builder event(ConfigEvent.Kind kind, String detailCode)
            {
            if (kind != null)
                { this.eventKind = kind; }
            this.detailCode = detailCode;
            return this;
            }

        /** remediation; the message must be fixed text, not content. */
        public Builder remediation(ConfigEvent.Remediation remediation, String remediationMessage)
            {
            if (remediation != null)
                { this.remediation = remediation; }
            this.remediationMessage = remediationMessage;
            return this;
            }

        public ConfigParseException build()
            { return new ConfigParseException(this); }
    }
}
//___EOF___
