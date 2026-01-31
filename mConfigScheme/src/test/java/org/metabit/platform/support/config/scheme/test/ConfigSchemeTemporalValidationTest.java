package org.metabit.platform.support.config.scheme.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.impl.entry.ConfigEntryMetadata;
import org.metabit.platform.support.config.impl.entry.TypedConfigEntryLeaf;
import org.metabit.platform.support.config.scheme.ConfigScheme;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigSchemeTemporalValidationTest
{

    @Test
    public void testTemporalValidation()
            throws ConfigCheckedException
        {
        String json = "["+
                "  { \"KEY\": \"startDate\", \"TYPE\": \"DATE\", \"MANDATORY\": { \"AFTER\": \"2020-01-01\" } },"+
                "  { \"KEY\": \"startTime\", \"TYPE\": \"TIME\" },"+
                "  { \"KEY\": \"eventTime\", \"TYPE\": \"DATETIME\", \"MANDATORY\": { \"REQUIRE_OFFSET\": true } }"+
                "]";

        ConfigFactorySettings settings = new ConfigFactorySettings();
        ConfigFactoryInstanceContext ctx = new ConfigFactoryInstanceContext(settings);
        Map<String, ConfigScheme> schemes = org.metabit.platform.support.config.scheme.impl.ext.JsonConfigSchemeParser.parseJSON(json, ctx);
        ConfigScheme scheme = schemes.get("");

        // Test DATE
        ConfigEntry validDate = new TypedConfigEntryLeaf("startDate", LocalDate.of(2021, 1, 1), ConfigEntryType.DATE, new ConfigEntryMetadata(null));
        assertTrue(scheme.checkConfigEntryValidity("startDate", validDate));

        ConfigEntry invalidDate = new TypedConfigEntryLeaf("startDate", LocalDate.of(2019, 1, 1), ConfigEntryType.DATE, new ConfigEntryMetadata(null));
        assertFalse(scheme.checkConfigEntryValidity("startDate", invalidDate));

        // Test DATETIME with REQUIRE_OFFSET
        ConfigEntry validDateTime = new TypedConfigEntryLeaf("eventTime", OffsetDateTime.parse("2026-01-14T22:00:00Z"), ConfigEntryType.DATETIME, new ConfigEntryMetadata(null));
        assertTrue(scheme.checkConfigEntryValidity("eventTime", validDateTime));

        ConfigEntry invalidDateTime = new TypedConfigEntryLeaf("eventTime", "2026-01-14T22:00:00", ConfigEntryType.DATETIME, new ConfigEntryMetadata(null));
        assertFalse(scheme.checkConfigEntryValidity("eventTime", invalidDateTime), "Should fail because REQUIRE_OFFSET is true but value has no offset");
        }
}
