package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.interfaces.ConfigEntrySpecification;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.interfaces.LayeredConfigurationInterface;
import org.metabit.platform.support.config.schema.ConfigSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Reports a {@link ConfigParseException} for the storage that caught it: one WARNING log line and one PARSE event.
 * <p>
 * The parser's own text may quote the content it failed on. It is shown unless it may contain a secret value:
 * <ol>
 * <li>never for a location that may hold secrets regardless of the schema (secret stores such as Kubernetes Secrets,
 *     Vault, AWS Secrets Manager; the bootstrap configurations of the cloud modules);</li>
 * <li>key path known: not if the schema marks that entry, an entry below it or above it as {@code SECRET};</li>
 * <li>key path unknown: not if the schema has any {@code SECRET} entry;</li>
 * <li>always shown: configuration name, location, format, error type, line, column, key path.</li>
 * </ol>
 * {@link ConfigFeature#LOG_PARSE_ERROR_DETAILS} shows the parser's text regardless. Everything that comes from
 * outside goes through {@link LogSafe}.
 *
 * @since 0.8.17
 */
public final class ParseProblemReporter
{
    static final String DETAIL_WITHHELD = "; parser details withheld, the content may hold secret values ("
            +ConfigFeature.LOG_PARSE_ERROR_DETAILS.name()+" shows them)";

    private ParseProblemReporter() {}

    /**
     * report a parse problem found while reading a layer for a configuration.
     *
     * @param problem             the exception thrown by the format
     * @param location            where the content came from
     * @param cfg                 the configuration the layer was read for; supplies context, name and schema
     * @param locationHoldsSecrets true for secret stores (e.g. a Kubernetes Secret): never show the parser's text
     */
    public static void report(ConfigParseException problem, ConfigLocation location, LayeredConfigurationInterface cfg, boolean locationHoldsSecrets)
        {
        if (problem == null || cfg == null)
            { return; }
        boolean mayHoldSecrets = locationHoldsSecrets || cfg.isBootstrapLookup();
        report(problem, location, cfg.getContext(), cfg.getConfigName(), cfg.getConfigSchema(), mayHoldSecrets);
        }

    /**
     * report a parse problem, with the context given explicitly.
     *
     * @param problem        the exception thrown by the format
     * @param location       where the content came from; may be null
     * @param ctx            factory context; may be null (then nothing is reported)
     * @param configName     configuration name; may be null
     * @param schema         the configuration's schema; may be null
     * @param mayHoldSecrets true if the content may hold secrets regardless of the schema
     */
    public static void report(ConfigParseException problem, ConfigLocation location, ConfigFactoryInstanceContext ctx,
                              String configName, ConfigSchema schema, boolean mayHoldSecrets)
        {
        if (problem == null || ctx == null)
            { return; }
        boolean showDetail = isDetailAllowed(problem, schema, mayHoldSecrets, ctx.getSettings());
        String detail = problem.getParserDetail();

        StringBuilder msg = new StringBuilder("could not parse");
        if (configName != null)
            { msg.append(" configuration ").append(LogSafe.text(configName)); }
        if (location != null)
            { msg.append(" from ").append(LogSafe.text(location.toLocationString())); }
        if (problem.getFormatId() != null)
            { msg.append(" as ").append(LogSafe.text(problem.getFormatId())); }
        msg.append(": ").append(LogSafe.text(problem.getMessage()));
        if (detail != null && !detail.isEmpty())
            { msg.append(showDetail ? ": "+LogSafe.text(detail) : DETAIL_WITHHELD); }
        String message = msg.toString();

        ConfigLoggingInterface logger = ctx.getLogger();
        if (logger != null)
            {
            logger.warn(message);
            if (showDetail && problem.getParserCause() != null && logger.isDebugEnabled())
                { logger.debug("parser exception for the warning above", problem.getParserCause()); }
            }

        Map<String, String> attrs = new HashMap<>();
        if (problem.getLine() > 0)
            { attrs.put("line", String.valueOf(problem.getLine())); }
        if (problem.getColumn() > 0)
            { attrs.put("column", String.valueOf(problem.getColumn())); }
        if (problem.getErrorType() != null)
            { attrs.put("errorType", problem.getErrorType()); }
        EventRecorder.record(ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.WARNING)
                .domain(ConfigEvent.Domain.PARSE)
                .kind(problem.getEventKind())
                .detailCode(problem.getDetailCode())
                .message(message)
                .formatId(problem.getFormatId())
                .configName(configName)
                .keyPath(problem.getKeyPath())
                .location(location)
                .scope((location == null) ? null : location.getScope())
                .attributes(attrs)
                .cause(showDetail ? problem.getParserCause() : null)
                .remediation(problem.getRemediation())
                .remediationMessage(problem.getRemediationMessage())
                .build(), ctx);
        }

    /**
     * whether the parser's own text may be shown. See the class description for the rules.
     */
    static boolean isDetailAllowed(ConfigParseException problem, ConfigSchema schema, boolean mayHoldSecrets, ConfigFactorySettings settings)
        {
        if (settings != null && Boolean.TRUE.equals(settings.getBoolean(ConfigFeature.LOG_PARSE_ERROR_DETAILS)))
            { return true; }
        if (mayHoldSecrets)
            { return false; }
        if (schema == null || schema.isNullSchema())
            { return true; }
        String keyPath = problem.getKeyPath();
        for (String entryKey : schema.getEntryKeys())
            {
            ConfigEntrySpecification spec = schema.getSpecification(entryKey);
            if (spec == null || !spec.isSecret())
                { continue; }
            if (keyPath == null)
                { return false; } // rule 3: position unknown, the schema has a secret
            if (entryKey.equals(keyPath) || entryKey.startsWith(keyPath+"/") || keyPath.startsWith(entryKey+"/"))
                { return false; } // rule 2: at, below or above a secret entry
            }
        return true;
        }
}
//___EOF___
