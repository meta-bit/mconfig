package org.metabit.platform.support.config.util.ts;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigEntry;
import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.ConfigSchemaFactory;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypeScriptGeneratorTest
{
    @Test
    public void testGenerateInterface() throws Exception
        {
        ConfigSchemaFactory factory = ConfigSchemaFactory.create();
        ConfigSchema scheme = factory.createSchema();

        scheme.addSchemaEntry(factory.createEntry("server/port", ConfigEntryType.NUMBER)
                .setDescription("The port the server listens on")
                .setFlags(EnumSet.of(ConfigEntry.ConfigEntryFlags.MANDATORY)));

        scheme.addSchemaEntry(factory.createEntry("server/host", ConfigEntryType.STRING)
                .setDefault("localhost"));

        scheme.addSchemaEntry(factory.createEntry("database/type", ConfigEntryType.ENUM)
                .setValidationPattern("POSTGRES|MYSQL|SQLITE")
                .setFlags(EnumSet.of(ConfigEntry.ConfigEntryFlags.MANDATORY)));

        scheme.addSchemaEntry(factory.createEntry("features/enabled", ConfigEntryType.ENUM_SET)
                .setValidationPattern("AUTH|METRICS|LOGGING"));

        scheme.addSchemaEntry(factory.createEntry("debugMode", ConfigEntryType.BOOLEAN));

        TypeScriptGenerator generator = new TypeScriptGenerator();
        String tsInterface = generator.generateInterface(scheme, "AppConfig");

        System.out.println(tsInterface);

        assertTrue(tsInterface.contains("export interface AppConfig {"));
        assertTrue(tsInterface.contains("server: {"));
        assertTrue(tsInterface.contains("port: number;"));
        assertTrue(tsInterface.contains("/** The port the server listens on */"));
        assertTrue(tsInterface.contains("host?: string;"));
        assertTrue(tsInterface.contains("database: {"));
        assertTrue(tsInterface.contains("type: 'POSTGRES' | 'MYSQL' | 'SQLITE';"));
        assertTrue(tsInterface.contains("features: {"));
        assertTrue(tsInterface.contains("enabled?: ('AUTH' | 'METRICS' | 'LOGGING')[];"));
        assertTrue(tsInterface.contains("debugMode?: boolean;"));
        }
}
