package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for email addresses (STRING or MULTIPLE_STRINGS).
 * Simple validation matching most common formats.
 * Not full RFC 5322 compliant (no quoted local-parts, IP literals, comments).
 * Use in scheme JSON: "validationPattern": "email"
 */
public class EmailValidator {
    private final Pattern pattern;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$",
        Pattern.CASE_INSENSITIVE
    );

    public EmailValidator() {
        this.pattern = EMAIL_PATTERN;
    }

    public boolean validate(ConfigEntry entry) {
        try {
            if (entry.getType() == ConfigEntryType.MULTIPLE_STRINGS) {
                List<String> emails = entry.getValueAsStringList();
                if (emails == null) return false;
                for (String email : emails) {
                    if (!pattern.matcher(email).matches()) return false;
                }
                return true;
            } else {
                String email = entry.getValueAsString();
                return pattern.matcher(email).matches();
            }
        } catch (ConfigCheckedException e) {
            return false;
        }
    }
}