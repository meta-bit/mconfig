package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.logging.LogSafe;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

/**
 * Internal utility to record events with level filtering and capacity management.
 */
public final class EventRecorder
{
    private EventRecorder() {}

    public static void record(ConfigEvent event, ConfigFactoryInstanceContext ctx)
        {
        if (event == null || ctx == null) return;

        String levelStr = ctx.getSettings().getString(ConfigFeature.EVENTS_DETAIL_LEVEL);
        if ("OFF".equalsIgnoreCase(levelStr)) return;

        if ("FAILURES_ONLY".equalsIgnoreCase(levelStr))
            {
            boolean isFailure = event.getSeverity() == ConfigEvent.Severity.ERROR
                    || (event.getDomain() == ConfigEvent.Domain.PARSE && (event.getKind() == ConfigEvent.Kind.FAILED_GENERIC || event.getKind() == ConfigEvent.Kind.UNSUPPORTED_SYNTAX))
                    || (event.getDomain() == ConfigEvent.Domain.WRITE && event.getKind() == ConfigEvent.Kind.REFUSED_NOT_WRITEABLE)
                    || event.getKind() == ConfigEvent.Kind.SOURCE_UNAVAILABLE;
            if (!isFailure) return;
            }
        else if ("NORMAL".equalsIgnoreCase(levelStr))
            {
            // Keep high-signal items, avoid watcher churn
            boolean allow = (event.getSeverity() == ConfigEvent.Severity.ERROR
                    || event.getSeverity() == ConfigEvent.Severity.WARNING
                    || (event.getDomain() == ConfigEvent.Domain.MERGE && event.getKind() == ConfigEvent.Kind.OVERRIDE_APPLIED)
                    || (event.getDomain() == ConfigEvent.Domain.WRITE))
                    && (event.getDomain() != ConfigEvent.Domain.WATCHER);
            if (!allow) return;
            }
        else if ("VERBOSE".equalsIgnoreCase(levelStr))
            {
            // everything goes through
            }

        if (ctx.getFactory() != null)
            {
            ctx.getFactory().getEvents().add(event);
            }
        }

    /**
     * report a source that could not be read: a WARNING in the log, and a {@link ConfigEvent.Kind#SOURCE_UNAVAILABLE} event.
     * Lower-precedence sources may then deliver the value; this makes it visible.
     *
     * @param ctx        the factory context; may be null (then nothing is reported)
     * @param location   the source; may be null if the whole storage is affected
     * @param detailCode stable machine-readable code, e.g. {@code VAULT_READ_FAILED}
     * @param message    human-readable message. Outside text in it must already be escaped with
     *                   {@link org.metabit.platform.support.config.impl.logging.LogSafe#text(Object)}.
     */
    public static void recordSourceUnavailable(ConfigFactoryInstanceContext ctx, ConfigLocation location, String detailCode, String message)
        {
        if (ctx == null) return;
        String where = (location == null) ? "" : " (location "+LogSafe.text(location.toLocationString())+")";
        ConfigLoggingInterface logger = ctx.getLogger();
        if (logger != null)
            {
            logger.warn(message+where+"; lower-precedence sources may supply the values instead.");
            }
        record(ConfigEventImpl.builder()
                .severity(ConfigEvent.Severity.WARNING)
                .domain(ConfigEvent.Domain.DISCOVERY)
                .kind(ConfigEvent.Kind.SOURCE_UNAVAILABLE)
                .detailCode(detailCode)
                .message(message+where)
                .location(location)
                .scope((location == null) ? null : location.getScope())
                .remediation(ConfigEvent.Remediation.RETRY_LATER)
                .remediationMessage("Check that the source is reachable and the credentials are valid.")
                .build(), ctx);
        }

    public static void record(ConfigEvent event, LayeredConfiguration config, ConfigFactoryInstanceContext ctx)
        {
        if (event == null || config == null || ctx == null) return;

        // Factory level recording
        record(event, ctx);

        // Configuration level recording
        // We use the same level filtering as for factory for now
        String levelStr = ctx.getSettings().getString(ConfigFeature.EVENTS_DETAIL_LEVEL);
        if ("OFF".equalsIgnoreCase(levelStr)) return;

        config.getEvents().add(event);
        }
}
//___EOF___
