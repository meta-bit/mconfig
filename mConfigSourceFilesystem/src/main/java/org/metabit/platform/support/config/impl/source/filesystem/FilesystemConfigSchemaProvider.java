package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.provider.ConfigSchemaProvider;
import org.metabit.platform.support.osdetection.OperatingSystem;
import org.metabit.platform.support.osdetection.PlatformDetector;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FilesystemConfigSchemaProvider implements ConfigSchemaProvider
{

    @Override
    public Map<String, ConfigSchema> discoverSchemas(ConfigFactoryInstanceContext ctx)
        {
        if (!ctx.getSettings().getBoolean(ConfigFeature.ALLOW_LOCAL_SCHEMA_OVERRIDE))
            {
            return Collections.emptyMap();
            }

        Map<String, ConfigSchema> discovered = new HashMap<>();
        List<Path> searchPaths = getSearchPaths(ctx);
        String company = normalizeSegment(ctx.getSettings().getString(ConfigFeature.COMPANY_NAME));
        String application = normalizeSegment(ctx.getSettings().getString(ConfigFeature.APPLICATION_NAME));

        for (Path path : searchPaths)
            {
            for (Path dir : getSchemaDirectories(path, company, application))
                {
                if (Files.isDirectory(dir))
                    {
                    File[] files = dir.toFile().listFiles((d, name)->name.endsWith(".mconfig-schema.json"));
                    if (files != null)
                        {
                        for (File file : files)
                            {
                            try
                                {
                                String json = new String(Files.readAllBytes(file.toPath()));
                                Map<String, ConfigSchema> schemas = ConfigSchema.fromJSON(json, ctx);
                                String configName = extractConfigName(file.getName());

                                if (schemas.containsKey(""))
                                    {
                                    discovered.put(configName, schemas.get(""));
                                    }
                                else
                                    {
                                    discovered.putAll(schemas);
                                    }
                                }
                            catch (Exception e)
                                {
                                ctx.getLogger().error("Failed to load schema from "+file.getAbsolutePath(), e);
                                }
                            }
                        }
                    }
                }
            }
        return discovered;
        }

    private List<Path> getSearchPaths(ConfigFactoryInstanceContext ctx)
        {
        List<Path> paths = new ArrayList<>();

        // Custom directory from features
        String customDir = ctx.getSettings().getString(ConfigFeature.LOCAL_SCHEMA_DIRECTORY);
        if (customDir != null)
            {
            paths.add(Paths.get(customDir));
            }

        // Standard OS-specific locations
        OperatingSystem os = new PlatformDetector().getOs();
        if (os == OperatingSystem.WINDOWS)
            {
            String appData = System.getenv("APPDATA");
            if (appData != null) paths.add(Paths.get(appData, "mconfig", "schemas"));
            String programData = System.getenv("PROGRAMDATA");
            if (programData != null) paths.add(Paths.get(programData, "mconfig", "schemas"));
            }
        else
            {
            // Linux/Unix/Mac
            String home = System.getProperty("user.home");
            if (home != null) paths.add(Paths.get(home, ".config", "mconfig", "schemas"));
            paths.add(Paths.get("/etc/mconfig/schemas"));
            paths.add(Paths.get("/usr/local/share/mconfig/schemas"));
            }
        return paths;
        }

    private List<Path> getSchemaDirectories(Path root, String company, String application)
        {
        if (application == null)
            {
            return Collections.emptyList();
            }

        LinkedHashSet<Path> dirs = new LinkedHashSet<>();
        if (company != null)
            {
            dirs.add(root.resolve(company).resolve(application));
            }
        dirs.add(root.resolve(application));
        return new ArrayList<>(dirs);
        }

    private String extractConfigName(String filename)
        {
        if (filename.endsWith(".mconfig-schema.json"))
            {
            return filename.substring(0, filename.length()-".mconfig-schema.json".length());
            }
        return filename;
        }

    private String normalizeSegment(String value)
        {
        if (value == null)
            {
            return null;
            }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
        }
}
