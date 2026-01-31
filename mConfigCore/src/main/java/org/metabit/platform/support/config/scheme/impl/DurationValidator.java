package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;

import java.time.Duration;

/**
 * Validator for durations (ISO-8601, e.g. "PT1H30M").
 * Use in scheme JSON: "type": "DURATION", "validationPattern": "duration"
 */
public class DurationValidator {
    public boolean validate(ConfigEntry entry) {
        try {
            Duration.parse(entry.getValueAsString());
            return true;
        } catch (ConfigCheckedException | java.time.format.DateTimeParseException e) {
            return false;
        }
    }
}