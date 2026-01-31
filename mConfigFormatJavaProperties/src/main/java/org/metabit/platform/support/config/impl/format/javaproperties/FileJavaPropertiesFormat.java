package org.metabit.platform.support.config.impl.format.javaproperties;

import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigSource;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class FileJavaPropertiesFormat implements ConfigFileFormatInterface
{
    private ConfigLoggingInterface logger;
    private ConfigFactorySettings  settings;

    @Override
    public String getFormatID()
        {
        return "properties";
        }


    //@TODO this is more of an init, than test.
    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger)
        {
        this.logger = logger;
        this.settings = configFactorySettings;
        return true;
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".properties");
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation location)
        {
        JavaPropertiesConfigLayer instance = new JavaPropertiesConfigLayer(settings, location, this, file.toPath());
        Properties properties = instance.internalGetProperties();
        try
            {
            properties.load(new FileReader(file));
            }
        catch (FileNotFoundException ex)
            {
            logger.error("previously existing Java properties file denied its existence when reading"); //
            return null;
            }
        catch (IOException ex)
            {
            logger.warn("IOException when reading Java Properties");
            return null;
            }
        // the source we set is derived from the location
        // instance.setSource(new ConfigLocationImpl(location, instance, this, file.toPath()));
        // instance.setWriteable(file.canWrite());
        // this is done by the instances themselves instead! instance.setWriteable(file.canWrite());
        return instance;
        }

    /**
     * @param inputStream the input stream to read Java properties from
     * @param location the location of the configuration
     * @return the configuration layer instance
     */
    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation location)
        {
        JavaPropertiesConfigLayer instance = new JavaPropertiesConfigLayer(settings, location, this, null);
        Properties properties = instance.internalGetProperties();
        try
            {
            properties.load(inputStream);
            }
        catch (FileNotFoundException ex)
            {
            logger.error("previously existing Java properties file denied its existence when reading"); //
            return null;
            }
        catch (IOException ex)
            {
            logger.warn("IOException when reading Java Properties");
            return null;
            }
        //instance.setSource(new ConfigLocationImpl(location, instance, this, null));
        //instance.setWriteable(false); // can't write streams
        return instance;
        }

    /**
     * @param fileWithPath the path to the file to create
     * @param location the location of the configuration
     * @return the configuration layer instance
     */
    @Override
    public ConfigLayerInterface createFile(Path fileWithPath, ConfigLocation location)
        {
        try
            {
            Path fileInstance = Files.createFile(fileWithPath);
            if (! fileInstance.toFile().canWrite())
                {
                logger.error("freshly created file not writeable?!");
                return null;
                }
            JavaPropertiesConfigLayer instance = new JavaPropertiesConfigLayer(settings, location, this, fileInstance);
            // nothing to load() / read from freshly created file.
            return instance;
            }
        catch (IOException e)
            {
            logger.error("config creation failed at filesystem level",e);
            return null;
            }
        }

    public void writePropertiesToFile(ConfigSource source, final Properties props)
            throws ConfigCheckedException
        {
        try
            {
            Path path = (Path) source.getStorageInstanceHandle();
            FileWriter writer = new FileWriter(path.toFile());
            String timestamp = ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT );
            props.store(writer,"written by mConfig " + timestamp);
            }
        catch (Exception ex)
            {
            throw new ConfigCheckedException(ex);
            }
        return;
        }
    @Override
    public void writeFile(ConfigLayerInterface layer) throws ConfigCheckedException
        {
        if (layer instanceof JavaPropertiesConfigLayer)
            {
            JavaPropertiesConfigLayer propsLayer = (JavaPropertiesConfigLayer) layer;
            writePropertiesToFile(propsLayer.getSource(), propsLayer.internalGetProperties());
            }
        else
            {
            throw new ConfigCheckedException(ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }
}
