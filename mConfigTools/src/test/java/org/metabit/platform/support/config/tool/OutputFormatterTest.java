package org.metabit.platform.support.config.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputFormatterTest
{
    @Test
    void testYamlOutputContainsKey()
        {
        Map<String, Object> data = OutputFormatter.newLinkedMap();
        data.put("alpha", "value");

        String yaml = OutputFormatter.toYaml(data);
        assertTrue(yaml.contains("alpha"));
        assertTrue(yaml.contains("value"));
        }

    @Test
    void testTomlOutputContainsKey()
        {
        Map<String, Object> data = OutputFormatter.newLinkedMap();
        data.put("alpha", "value");

        String toml = OutputFormatter.toToml(data);
        assertTrue(toml.contains("alpha"));
        assertTrue(toml.contains("value"));
        }
}
