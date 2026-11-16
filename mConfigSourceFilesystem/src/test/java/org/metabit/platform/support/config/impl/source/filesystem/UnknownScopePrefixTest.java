package org.metabit.platform.support.config.impl.source.filesystem;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigScope;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * an unknown scope name in a SCOPE:path entry is reported and skipped, not used as a plain path (T21).
 */
class UnknownScopePrefixTest
{
    private static final String MARKER = "mconfig-unknown-scope-test";

    @Test
    void misspeltScopeIsSkipped()
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "UNKNOWN_SCOPE_TEST");
        builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of("HSOT:/nonexistent/"+MARKER+"-typo", "HOST:/nonexistent/"+MARKER+"-valid"));
        try (ConfigFactory factory = builder.build())
            {
            List<ConfigLocation> searchList = factory.getSearchList();
            assertTrue(searchList.stream().noneMatch(l->String.valueOf(l.getStorageInstanceHandle()).contains(MARKER+"-typo")),
                    "the misspelt entry must not become a path: "+searchList);
            assertTrue(searchList.stream().anyMatch(l->l.getScope() == ConfigScope.HOST && String.valueOf(l.getStorageInstanceHandle()).contains(MARKER+"-valid")),
                    "valid entries still count: "+searchList);
            }
        }

    /* in a Turkish locale, "i".toUpperCase() is the dotted capital I (U+0130): "application" would not match APPLICATION */
    @Test
    void lowerCaseScopeIsRecognisedInTurkishLocale()
        {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try
            {
            ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "UNKNOWN_SCOPE_TEST");
            builder.setFeature(ConfigFeature.ADDITIONAL_USER_DIRECTORIES, List.of("application:/nonexistent/"+MARKER+"-tr"));
            try (ConfigFactory factory = builder.build())
                {
                List<ConfigLocation> searchList = factory.getSearchList();
                assertTrue(searchList.stream().anyMatch(l->l.getScope() == ConfigScope.APPLICATION && String.valueOf(l.getStorageInstanceHandle()).contains(MARKER+"-tr")),
                        "a lower-case scope name must not depend on the default locale: "+searchList);
                }
            }
        finally
            {
            Locale.setDefault(previous);
            }
        }

    @Test
    void driveLetterIsNotAScope()
        {
        assertTrue(FileConfigStorage.isDriveLetter("C"));
        assertFalse(FileConfigStorage.isDriveLetter("HOST"));
        }
}
//___EOF___
