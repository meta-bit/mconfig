package org.metabit.platform.support.config.source.hardcoded.provider;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.metabit.platform.support.config.schema.ConfigSchema;
import org.metabit.platform.support.config.schema.provider.ConfigSchemaProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ConfigSchemaProvider that scans the classpath for schemes.
 * Logic moved from JARConfigSource.
 */
public class ClasspathConfigSchemaProvider implements ConfigSchemaProvider
{
    private static final String CONFIG_RESOURCE_PATH_PREFIX = ".config/";

    @Override
    public Map<String, ConfigSchema> discoverSchemas(ConfigFactoryInstanceContext ctx)
        {
        Map<String, ConfigSchema> discovered = new HashMap<>();
        ConfigLoggingInterface logger = ctx.getLogger();
        ClassLoader classLoader = ctx.getClassLoader();

        scanResources(classLoader, logger, CONFIG_RESOURCE_PATH_PREFIX, (name, is)->
            {
            if (name.endsWith(".mconfig-schema.json") || name.endsWith(".schema.json") || name.endsWith(".scheme.json"))
                {
                String configName = extractConfigNameFromPath(name);
                if (configName != null)
                    {
                    try
                        {
                        String json = readStreamToString(is);
                        Map<String, ConfigSchema> schemas = ConfigSchema.fromJSON(json, ctx);
                        if (schemas.containsKey(""))
                            {
                            discovered.put(configName, schemas.get(""));
                            logger.debug("Discovered schema for "+configName+" in "+name);
                            }
                        else
                            {
                            discovered.putAll(schemas);
                            logger.debug("Discovered multiple schemas in "+name);
                            }
                        }
                    catch (ConfigCheckedException|IOException e)
                        {
                        logger.error("Failed to parse discovered schema "+name, e);
                        }
                    }
                }
            });

        return discovered;
        }

    private void scanResources(ClassLoader classLoader, ConfigLoggingInterface logger, String resourcePath, ResourceProcessor processor)
        {
        if (!resourcePath.endsWith("/"))
            {
            resourcePath += "/";
            }

        try
            {
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            while (resources.hasMoreElements())
                {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("jar".equalsIgnoreCase(protocol))
                    {
                    scanJarResources(url, resourcePath, processor);
                    }
                else if ("file".equalsIgnoreCase(protocol))
                    {
                    scanDirectoryResources(new File(url.toURI()), resourcePath, processor);
                    }
                }
            }
        catch (Exception e)
            {
            logger.warn("Failed to scan resources at "+resourcePath+": "+e.getMessage());
            }
        }

    private void scanJarResources(URL jarUrl, String resourcePath, ResourceProcessor processor)
            throws IOException
        {
        String jarPath = jarUrl.getPath();
        if (jarPath.startsWith("file:"))
            {
            jarPath = jarPath.substring(5);
            }
        int bang = jarPath.indexOf('!');
        if (bang != -1)
            {
            jarPath = jarPath.substring(0, bang);
            }
        jarPath = java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8);

        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath))
            {
            Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
                {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(resourcePath) && !entry.isDirectory())
                    {
                    try (InputStream is = jarFile.getInputStream(entry))
                        {
                        processor.process(name, is);
                        }
                    }
                }
            }
        }

    private void scanDirectoryResources(File dir, String resourcePath, ResourceProcessor processor)
            throws IOException
        {
        File[] files = dir.listFiles();
        if (files != null)
            {
            for (File file : files)
                {
                if (file.isDirectory())
                    {
                    scanDirectoryResources(file, resourcePath+file.getName()+"/", processor);
                    }
                else
                    {
                    try (InputStream is = new java.io.FileInputStream(file))
                        {
                        processor.process(resourcePath+file.getName(), is);
                        }
                    }
                }
            }
        }

    private String extractConfigNameFromPath(String name)
        {
        String fileName = name;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash != -1)
            {
            fileName = name.substring(lastSlash+1);
            }
        if (fileName.endsWith(".mconfig-schema.json"))
            {
            return fileName.substring(0, fileName.length()-".mconfig-schema.json".length());
            }
        if (fileName.endsWith(".schema.json"))
            {
            return fileName.substring(0, fileName.length()-".schema.json".length());
            }
        if (fileName.endsWith(".scheme.json"))
            {
            return fileName.substring(0, fileName.length()-".scheme.json".length());
            }
        return null;
        }

    private String readStreamToString(InputStream is)
            throws IOException
        {
        byte[] buffer = new byte[4096];
        int bytesRead;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((bytesRead = is.read(buffer)) != -1)
            {
            baos.write(buffer, 0, bytesRead);
            }
        return baos.toString(StandardCharsets.UTF_8);
        }

    private interface ResourceProcessor
    {
        void process(String name, InputStream inputStream)
                throws IOException;
    }
}
