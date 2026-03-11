package org.metabit.platform.support.config.impl;

import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigFeature;

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
                    || (event.getDomain() == ConfigEvent.Domain.WRITE && event.getKind() == ConfigEvent.Kind.REFUSED_NOT_WRITEABLE);
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
