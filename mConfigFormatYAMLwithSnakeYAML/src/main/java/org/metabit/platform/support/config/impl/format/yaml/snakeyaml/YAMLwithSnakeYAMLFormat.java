package org.metabit.platform.support.config.impl.format.yaml.snakeyaml;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.Node;
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
            throws ConfigParseException
        {
        try (InputStream inputStream = new FileInputStream(file))
            {
            return readStream(inputStream, configLocation, file.toPath());
            }
        catch (IOException e)
            {
            throw ConfigParseException.builder(getFormatID(), "could not read")
                    .parserCause(e)
                    .event(ConfigEvent.Kind.FAILED_GENERIC, "YAML_READ_FAILED")
                    .remediation(ConfigEvent.Remediation.CHECK_PERMISSIONS, "Check that the content is readable.")
                    .build();
            }
        }

    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation)
            throws ConfigParseException
        {
        return readStream(inputStream, configLocation, inputStream);
        }

    private ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation, Object storageInstanceHandle)
            throws ConfigParseException
        {
        try
            {
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setProcessComments(true);
            Yaml yaml = new Yaml(new Constructor(loaderOptions), new Representer(new DumperOptions()), new DumperOptions(), loaderOptions);

            Node node = yaml.compose(new InputStreamReader(inputStream));
            if (node == null) // empty content
                {
                return new YAMLSnakeYAMLConfigLayer(settings, logger, configLocation, this, new LinkedHashMap<>(), storageInstanceHandle);
                }
            return new YAMLSnakeYAMLConfigLayer(settings, logger, configLocation, this, node, storageInstanceHandle);
            }
        catch (RuntimeException e)
            {
            // SnakeYAML's message carries a snippet of the content; it goes in as parser detail only. No key path.
            ConfigParseException.Builder builder = ConfigParseException.builder(getFormatID(), "not valid YAML")
                    .parserCause(e)
                    .event(ConfigEvent.Kind.FAILED_GENERIC, "YAML_PARSE_FAILED")
                    .remediation(ConfigEvent.Remediation.FIX_SYNTAX, "Fix invalid YAML syntax.");
            if (e instanceof MarkedYAMLException && ((MarkedYAMLException) e).getProblemMark() != null)
                {
                Mark mark = ((MarkedYAMLException) e).getProblemMark();
                builder.position(mark.getLine()+1, mark.getColumn()+1); // SnakeYAML counts from 0
                }
            throw builder.build();
            }
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
