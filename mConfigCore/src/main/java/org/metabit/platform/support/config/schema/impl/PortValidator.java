package org.metabit.platform.support.config.schema.impl;

/**
 * Validator for TCP/UDP ports: 0-65535 inclusive.
 * Use in scheme JSON: "validationPattern": "port"
 */
public class PortValidator extends RangeValidator {
    public PortValidator() {
        super("[0,65535]");
    }
}