package org.metabit.platform.support.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.metabit.platform.support.config.ConfigFeature.SUB_PATH;
import static org.metabit.platform.support.config.ConfigFeature.UPDATE_CHECK_SCOPES;

class ConfigFeatureTest
{

    @Test
    public void testEnumSetConversion()
        {
        assertEquals(UPDATE_CHECK_SCOPES.getType(), ConfigFeature.ValueType.STRINGLIST);
        assertTrue(UPDATE_CHECK_SCOPES.isStringListType());

        // this is how the full EnumSet is converted to an array of Strings.
        List<String> dummy0 = Arrays.stream(ConfigScope.values()).map(Enum::name).collect(Collectors.toList());

        @SuppressWarnings("unchecked") List<String> scopesAsStrings = (List<String>) UPDATE_CHECK_SCOPES.getDefault();
        EnumSet<ConfigScope> checkedScopes = EnumSet.noneOf(ConfigScope.class);
        scopesAsStrings.forEach(s->checkedScopes.add(ConfigScope.valueOf(s)));

        return;
        }

    @Test
    public void testSubPathParameterCheck()
        {
        assertEquals(SUB_PATH.getType(), ConfigFeature.ValueType.STRING);
        assertTrue(SUB_PATH.isStringType());

        ConfigFactoryBuilder configFactoryBuilder = ConfigFactoryBuilder.create("metabit", "CONFIGTEST");
        // expected to be OK
        configFactoryBuilder.setFeature(SUB_PATH, "minimal");
        configFactoryBuilder.setFeature(SUB_PATH, "replacement");
        configFactoryBuilder.setFeature(SUB_PATH, "two/level");
        configFactoryBuilder.setFeature(SUB_PATH, "three/separate/levels");
        configFactoryBuilder.setFeature(SUB_PATH, "subpath with spaces");
        configFactoryBuilder.setFeature(SUB_PATH, "subpath with spaces/and another level");
        configFactoryBuilder.setFeature(SUB_PATH, ""); // reset to empty
        // expected to fail checks

        ConfigException cex;
        cex = assertThrows(ConfigException.class, ()->configFactoryBuilder.setFeature(SUB_PATH, "/starting with slash"));
        cex = assertThrows(ConfigException.class, ()->configFactoryBuilder.setFeature(SUB_PATH, "starting with slash/"));
        cex = assertThrows(ConfigException.class, ()->configFactoryBuilder.setFeature(SUB_PATH, "//"));
        cex = assertThrows(ConfigException.class, ()->configFactoryBuilder.setFeature(SUB_PATH, "///"));
        cex = assertThrows(ConfigException.class, ()->configFactoryBuilder.setFeature(SUB_PATH, "../goingup"));
        cex = assertThrows(ConfigException.class, ()->configFactoryBuilder.setFeature(SUB_PATH, "file://honest/mistake"));

        return;
        }
}