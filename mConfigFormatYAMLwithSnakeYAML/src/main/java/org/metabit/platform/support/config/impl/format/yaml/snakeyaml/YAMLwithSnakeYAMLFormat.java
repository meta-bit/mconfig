package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The YAMLwithSnakeYAMLFormat class is an implementation of the ConfigFileFormatInterface
 * which facilitates the handling of configuration files in YAML format. This class
 * provides methods to read, write, and validate YAML configuration files leveraging the SnakeYAML library.
 */
public class YAMLwithSnakeYAMLFormat implements ConfigFileFormatInterface
{
    private ConfigLoggingInterface logger;
    private ConfigFactorySettings  settings;

    @Override
    public String getFormatID()
        {
        return "YAMLwithSnakeYAML";
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".yaml", ".yml");
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface configLoggingInterface)
        {
        this.logger = configLoggingInterface;
        this.settings = configFactorySettings;
        return true;
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation configLocation)
        {
        try (InputStream inputStream = new FileInputStream(file))
            {
            return readStream(inputStream, configLocation, file.toPath());
            }
        catch (FileNotFoundException e)
            {
            logger.error("File not found: " + file.getAbsolutePath());
            }
        catch (IOException e)
            {
            logger.error("Error reading file " + file.getAbsolutePath() + ": " + e.getMessage(), e);
            }
        return null;
        }

    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation)
        {
        return readStream(inputStream, configLocation, inputStream);
        }

    private ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation, Object storageInstanceHandle)
        {
        try
            {
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setProcessComments(true);
            Yaml yaml = new Yaml(new Constructor(loaderOptions), new Representer(new DumperOptions()), new DumperOptions(), loaderOptions);
            
            Object data;
            Node node = yaml.compose(new InputStreamReader(inputStream));
            data = node;

            if (data == null)
                {
                return new YAMLSnakeYAMLConfigLayer(settings, logger, configLocation, this, new LinkedHashMap<>(), storageInstanceHandle);
                }
            if (data instanceof Map || data instanceof List || data instanceof Node)
                {
                return new YAMLSnakeYAMLConfigLayer(settings, logger, configLocation, this, data, storageInstanceHandle);
                }
            logger.warn("YAML data having top-level type other than Map, List or Node");
            }
        catch (Exception e)
            {
            logger.warn("Error parsing YAML: " + e.getMessage());
            }
        return null;
        }

    @Override
    public ConfigLayerInterface createFile(Path fileWithPath, ConfigLocation configLocation)
        {
        try
            {
            Path fileInstance = Files.createFile(fileWithPath);
            if (!fileInstance.toFile().canWrite())
                {
                logger.error("freshly created file not writeable?!");
                return null;
                }
            Map<String, Object> data = new LinkedHashMap<>();
            saveYaml(data, fileInstance);
            return new YAMLSnakeYAMLConfigLayer(settings, logger, configLocation, this, data, fileWithPath);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer) throws ConfigCheckedException
        {
        if (layer instanceof YAMLSnakeYAMLConfigLayer)
            {
            YAMLSnakeYAMLConfigLayer yamlLayer = (YAMLSnakeYAMLConfigLayer) layer;
            Path path = (Path) yamlLayer.getSource().getStorageInstanceHandle();
            Object data = yamlLayer.getData();
            try
                {
                saveYaml(data, path);
                }
            catch (Exception ex)
                {
                throw new ConfigCheckedException(ex);
                }
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }

    private void saveYaml(Object data, Path path) throws IOException
        {
        DumperOptions options = new DumperOptions();
        options.setProcessComments(true);
        if (settings.getBoolean(ConfigFeature.WRITE_CONDENSED_FORMAT))
            {
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
            }
        else
            {
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            }
        
        try (Writer writer = new FileWriter(path.toFile()))
            {
            if (data instanceof Node)
                {
                Serializer serializer = new Serializer(new Emitter(writer, options), new Resolver(), options, null);
                serializer.open();
                serializer.serialize((Node) data);
                serializer.close();
                }
            else
                {
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setProcessComments(true);
                Yaml yaml = new Yaml(new Constructor(loaderOptions), new Representer(options), options, loaderOptions);
                yaml.dump(data, writer);
                }
            }
        }
}
