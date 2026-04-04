package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.interfaces.ConfigFactoryComponent;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.repository.ConfigSchemaRepository;
import org.metabit.platform.support.osdetection.OperatingSystem;
import org.metabit.platform.support.osdetection.PlatformDetector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Component for exporting mConfig schemas to a local filesystem.
 * This helps external tools (CLI, Editors) to validate configurations offline.
 */
public final class ConfigSchemaExporterComponent implements ConfigFactoryComponent
{
    private ConfigFactoryInstanceContext ctx;
    private ConfigLoggingInterface       logger;

    @Override
    public String getComponentID()
        {
        return "filesystem-schema-exporter";
        }

    @Override
    public boolean initialize(ConfigFactoryInstanceContext ctx)
        {
        this.ctx = ctx;
        this.logger = ctx.getLogger();
        return true;
        }

    @Override
    public void postInit(org.metabit.platform.support.config.ConfigFactory factory)
        {
        if (ctx != null && ctx.getSettings().getBoolean(ConfigFeature.EXPORT_SCHEMA_TO_LOCAL_STORAGE))
            {
            exportSchemasToLocalStorage(ctx);
            }
        }

    /**
     * Exports all registered schemas from the repository to the designated local storage.
     *
     * @param ctx the factory instance context
     */
    private void exportSchemasToLocalStorage(ConfigFactoryInstanceContext ctx)
        {
        ConfigSchemaRepository repo = ctx.getSchemaRepository();
        Path exportRoot = getExportRootDirectory(ctx);
        Path exportDir = getExportDirectory(exportRoot, ctx);
        if (exportDir == null)
            {
            return;
            }

        try
            {
            Files.createDirectories(exportDir);

            for (String name : repo.getRegisteredNames())
                {
                ConfigSchema schema = repo.getSchema(name);
                if (schema == null || schema.isNullSchema())
                    {
                    continue;
                    }

                exportSchema(name, schema, exportDir, ctx);
                }
            }
        catch (IOException e)
            {
            if (ctx.getLogger() != null)
                {
                ctx.getLogger().warn("Failed to export schemas to "+exportDir+": "+e.getMessage());
                }
            }
        }

    private void exportSchema(String name, ConfigSchema schema, Path exportDir, ConfigFactoryInstanceContext ctx)
        {
        // For now, we only export native mConfig schemas as .mconfig-schema.json
        Path file = exportDir.resolve(name+".mconfig-schema.json");

        try
            {
            String json = schema.toJSON(name, true, true);
            if (json == null)
                {
                logger.warn("Failed to export schema \""+name+"\" as JSON");
                return; // Not supported by this implementation
                }
            Files.write(file, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        catch (Exception e)
            {
            if (ctx.getLogger() != null)
                {
                ctx.getLogger().warn("Failed to export schema "+name+": "+e.getMessage());
                }
            }
        }

    /**
     * Determines the root directory where schemas should be exported.
     *
     * @param ctx the context
     * @return the resolved path, or null if it cannot be determined
     */
    private Path getExportRootDirectory(ConfigFactoryInstanceContext ctx)
        {
        String customDir = ctx.getSettings().getString(ConfigFeature.LOCAL_SCHEMA_DIRECTORY);
        if (customDir != null)
            { return Paths.get(customDir); }
        return getDefaultLocalSchemaDirectory();
        }

    private Path getExportDirectory(Path exportRoot, ConfigFactoryInstanceContext ctx)
        {
        if (exportRoot == null)
            { return null; }

        String company = normalizeSegment(ctx.getSettings().getString(ConfigFeature.COMPANY_NAME));
        String application = normalizeSegment(ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME));
        if (application == null)
            { return null; }

        if (company != null)
            {
            return exportRoot.resolve(company).resolve(application);
            }
        return exportRoot.resolve(application);
        }

    private Path getDefaultLocalSchemaDirectory()
        {
        OperatingSystem os = new PlatformDetector().getOs();
        if (os == OperatingSystem.WINDOWS)
            {
            String appData = System.getenv("APPDATA");
            if (appData != null)
                {
                return Paths.get(appData, "mconfig", "schemas");
                }
            }
        else
            {
            String home = System.getProperty("user.home");
            if (home != null)
                {
                return Paths.get(home, ".config", "mconfig", "schemas");
                }
            }
        return null;
        }

    private String normalizeSegment(String value)
        {
        if (value == null)
            { return null; }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
        }
}
//___EOF___
