package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigEntry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for sizes with units (e.g., "10MB", "1.5GiB") parsed to bytes.
 * Supports decimal (KB=1000, MB=1e6) and binary (KiB=1024, MiB=1MiB).
 * Units case-insensitive. Bare number assumes bytes.
 * Use in scheme JSON: "type": "NUMBER", "validationPattern": "size"
 */
public class SizeValidator {
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(B?)([KMGT]I?B?)?$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, BigDecimal> DECIMAL_MULTIPLIERS = new HashMap<>();
    private static final Map<String, BigDecimal> BINARY_MULTIPLIERS = new HashMap<>();

    static {
        DECIMAL_MULTIPLIERS.put("B", BigDecimal.ONE);
        DECIMAL_MULTIPLIERS.put("KB", BigDecimal.TEN.pow(3));
        DECIMAL_MULTIPLIERS.put("MB", BigDecimal.TEN.pow(6));
        DECIMAL_MULTIPLIERS.put("GB", BigDecimal.TEN.pow(9));
        DECIMAL_MULTIPLIERS.put("TB", BigDecimal.TEN.pow(12));

        BINARY_MULTIPLIERS.put("B", BigDecimal.ONE);
        BINARY_MULTIPLIERS.put("KIB", BigDecimal.valueOf(1024));
        BINARY_MULTIPLIERS.put("MIB", BigDecimal.valueOf(1024).pow(2));
        BINARY_MULTIPLIERS.put("GIB", BigDecimal.valueOf(1024).pow(3));
        BINARY_MULTIPLIERS.put("TIB", BigDecimal.valueOf(1024).pow(4));
    }

    public boolean validate(ConfigEntry entry) {
        try {
            String s = entry.getValueAsString().trim().toUpperCase();
            Matcher m = SIZE_PATTERN.matcher(s);
            if (!m.matches()) return false;

            BigDecimal num = new BigDecimal(m.group(1));
            if (num.signum() < 0) return false; // non-negative

            String unit = (m.group(3) != null ? m.group(3) : "B").toUpperCase();
            BigDecimal multiplier;
            if (unit.endsWith("I") || unit.endsWith("IB")) {
                multiplier = BINARY_MULTIPLIERS.get(unit);
            } else {
                multiplier = DECIMAL_MULTIPLIERS.get(unit);
            }
            if (multiplier == null) return false;

            BigDecimal bytes = num.multiply(multiplier);
            // Could chain with RangeValidator if needed
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}