package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigEntry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for numeric ranges and aliases.
 */
public class RangeValidator
{
    private static final Map<String, Range> ALIASES = new HashMap<>();
    static {
        ALIASES.put("uint8",  new Range(BigDecimal.ZERO, new BigDecimal("255"), true, true, true));
        ALIASES.put("uint16", new Range(BigDecimal.ZERO, new BigDecimal("65535"), true, true, true));
        ALIASES.put("uint32", new Range(BigDecimal.ZERO, new BigDecimal("4294967295"), true, true, true));
        ALIASES.put("uint64", new Range(BigDecimal.ZERO, new BigDecimal("18446744073709551615"), true, true, true));
        
        ALIASES.put("int7",  new Range(new BigDecimal("-64"), new BigDecimal("63"), true, true, true));
        ALIASES.put("int8",  new Range(new BigDecimal("-128"), new BigDecimal("127"), true, true, true));
        ALIASES.put("int15", new Range(new BigDecimal("-16384"), new BigDecimal("16383"), true, true, true));
        ALIASES.put("int16", new Range(new BigDecimal("-32768"), new BigDecimal("32767"), true, true, true));
        ALIASES.put("int31", new Range(new BigDecimal("-1073741824"), new BigDecimal("1073741823"), true, true, true));
        ALIASES.put("int32", new Range(new BigDecimal("-2147483648"), new BigDecimal("2147483647"), true, true, true));
        ALIASES.put("int63", new Range(new BigDecimal("-4611686018427387904"), new BigDecimal("4611686018427387903"), true, true, true));
        ALIASES.put("int64", new Range(new BigDecimal("-9223372036854775808"), new BigDecimal("9223372036854775807"), true, true, true));
    }

    private static final Pattern INTERVAL_PATTERN = Pattern.compile("([\\(\\[])\\s*([-+]?[0-9]*\\.?[0-9]+)\\s*,\\s*([-+]?[0-9]*\\.?[0-9]+)\\s*([\\)\\]])");

    private final Range range;

    public RangeValidator(String pattern)
        {
        if (ALIASES.containsKey(pattern.toLowerCase()))
            {
            this.range = ALIASES.get(pattern.toLowerCase());
            }
        else
            {
            Matcher m = INTERVAL_PATTERN.matcher(pattern);
            if (m.matches())
                {
                boolean minInc = m.group(1).equals("[");
                BigDecimal min = new BigDecimal(m.group(2));
                BigDecimal max = new BigDecimal(m.group(3));
                boolean maxInc = m.group(4).equals("]");
                this.range = new Range(min, max, minInc, maxInc, false);
                }
            else
                {
                this.range = null;
                }
            }
        }

    public boolean isValid()
        {
        return range != null;
        }

    public boolean validate(ConfigEntry entry)
        {
        if (range == null) return true;
        try
            {
            BigDecimal val = entry.getValueAsBigDecimal();
            if (range.integerOnly)
                {
                if (val.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) return false;
                }
            
            int minCmp = val.compareTo(range.min);
            if (range.minInclusive) { if (minCmp < 0) return false; }
            else { if (minCmp <= 0) return false; }

            int maxCmp = val.compareTo(range.max);
            if (range.maxInclusive) { if (maxCmp > 0) return false; }
            else { if (maxCmp >= 0) return false; }

            return true;
            }
        catch (Exception e)
            {
            return false;
            }
        }

    private static class Range
    {
        final BigDecimal min;
        final BigDecimal max;
        final boolean minInclusive;
        final boolean maxInclusive;
        final boolean integerOnly;

        Range(BigDecimal min, BigDecimal max, boolean minInclusive, boolean maxInclusive, boolean integerOnly)
            {
            this.min = min;
            this.max = max;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
            this.integerOnly = integerOnly;
            }
    }
}
