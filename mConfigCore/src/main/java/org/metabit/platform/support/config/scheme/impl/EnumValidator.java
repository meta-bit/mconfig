package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for ENUM and ENUM_SET types.
 */
public class EnumValidator
{
    private final Set<String> validOptions;
    private final boolean isSet;

    public EnumValidator(String pattern, boolean isSet)
        {
        this.isSet = isSet;
        this.validOptions = new HashSet<>(Arrays.asList(pattern.split("\\|")));
        }

    public boolean validate(ConfigEntry entry)
        {
        try
            {
            if (isSet)
                {
                List<String> values = entry.getValueAsStringList();
                if (values == null || values.isEmpty()) return true;
                for (String val : values)
                    {
                    if (!validOptions.contains(val)) return false;
                    }
                return true;
                }
            else
                {
                String val = entry.getValueAsString();
                return validOptions.contains(val);
                }
            }
        catch (ConfigCheckedException e)
            {
            return false;
            }
        }
}
