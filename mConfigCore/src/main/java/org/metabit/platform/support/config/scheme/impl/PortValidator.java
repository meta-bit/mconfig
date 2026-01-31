package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.scheme.impl.RangeValidator;

import java.math.BigDecimal;

/**
 * Validator for TCP/UDP ports: 0-65535 inclusive.
 * Use in scheme JSON: "validationPattern": "port"
 */
public class PortValidator extends RangeValidator {
    public PortValidator() {
        super("[0,65535]");
    }
}