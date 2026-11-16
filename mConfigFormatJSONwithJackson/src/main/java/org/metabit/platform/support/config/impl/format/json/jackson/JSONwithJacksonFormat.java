package org.metabit.platform.support.config.impl.format.json.jackson;

import org.metabit.platform.support.config.ConfigEvent;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.ConfigParseException;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFileFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLayerInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * read and write JSON configuration files.
 * This implementation uses the popular Jackson library.
 */
public class JSONwithJacksonFormat implements ConfigFileFormatInterface
{
    protected ConfigLoggingInterface       logger;
    protected ConfigFactorySettings        settings;
    protected ConfigFactoryInstanceContext ctx;
    protected ObjectMapper                 mapper;

    @Override
    public String getFormatID()
        {
        return "JSONwithJackson";
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".json"); // maybe also .json5
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface configLoggingInterface)
        {
        this.logger = configLoggingInterface;
        this.settings = configFactorySettings;
        try
            {
            this.mapper = createObjectMapper();
            }
        catch (Throwable t)
            {
            return false;
            }
        return true;
        }

    @Override
    public void initialize(ConfigFactoryInstanceContext ctx)
        {
        this.ctx = ctx;
        }

    protected ObjectMapper createObjectMapper()
        {
        return JsonMapper.builder().build();
        }

    @Override
    public ConfigLayerInterface readFile(File file, ConfigLocation configLocation)
            throws ConfigParseException
        {
        try
            {
            return toLayer(mapper.readTree(file), configLocation, file.toPath());
            }
        catch (JacksonException ex)
            {
            throw toParseException(ex);
            }
        catch (RuntimeException e)
            {
            throw readFailure(e);
            }
        }


    @Override
    public ConfigLayerInterface readStream(InputStream inputStream, ConfigLocation configLocation)
            throws ConfigParseException
        {
        try
            {
            return toLayer(mapper.readTree(inputStream), configLocation, inputStream);
            }
        catch (JacksonException ex)
            {
            throw toParseException(ex);
            }
        catch (RuntimeException e)
            {
            throw readFailure(e);
            }
        }

    /** empty content gives an empty layer; JSON allows any value at top level, a configuration needs object or array. */
    private ConfigLayerInterface toLayer(JsonNode rootNode, ConfigLocation configLocation, Object storageInstanceHandle)
            throws ConfigParseException
        {
        if (rootNode == null || rootNode.isMissingNode())
            { rootNode = mapper.createObjectNode(); }
        if (!rootNode.isObject() && !rootNode.isArray())
            {
            throw ConfigParseException.builder(getFormatID(), "top-level value is neither object nor array")
                    .errorType(rootNode.getNodeType().name())
                    .event(ConfigEvent.Kind.FAILED_GENERIC, "JSON_UNSUPPORTED_TOPLEVEL")
                    .remediation(ConfigEvent.Remediation.FIX_SYNTAX, "Put the configuration into a JSON object.")
                    .build();
            }
        return getJsonJacksonConfigLayer(configLocation, rootNode, storageInstanceHandle);
        }

    /** name of the syntax in reports and detail codes; subclasses for other Jackson formats override it. */
    protected String syntaxName()
        {
        return "JSON";
        }

    /** a Jackson error, with position and key path; the parser's message goes in as parser detail only. */
    protected ConfigParseException toParseException(JacksonException ex)
        {
        if (ex instanceof JacksonIOException)
            { return readFailure(ex); }
        // the parser's message is only inspected here, never passed on as text
        boolean comment = "JSON".equals(syntaxName()) && (ex.getOriginalMessage() != null) && ex.getOriginalMessage().contains("'/'");
        ConfigParseException.Builder builder = ConfigParseException.builder(getFormatID(), "not valid "+syntaxName())
                .parserCause(ex)
                .keyPath(keyPathAtError(ex))
                .event(comment ? ConfigEvent.Kind.UNSUPPORTED_SYNTAX : ConfigEvent.Kind.FAILED_GENERIC,
                        comment ? "JSON_PARSE_UNSUPPORTED_SYNTAX" : syntaxName()+"_PARSE_FAILED")
                .remediation(comment ? ConfigEvent.Remediation.CHANGE_FORMAT : ConfigEvent.Remediation.FIX_SYNTAX,
                        comment ? "File may contain comments; consider using JSON5 or removing comments." : "Fix invalid "+syntaxName()+" syntax.");
        if (ex.getLocation() != null)
            { builder.position(ex.getLocation().getLineNr(), ex.getLocation().getColumnNr()); }
        return builder.build();
        }

    /** the content could not be read at all (I/O, or an unexpected failure). */
    protected ConfigParseException readFailure(RuntimeException ex)
        {
        return ConfigParseException.builder(getFormatID(), "could not read")
                .parserCause(ex)
                .event(ConfigEvent.Kind.FAILED_GENERIC, syntaxName()+"_READ_FAILED")
                .remediation(ConfigEvent.Remediation.CHECK_PERMISSIONS, "Check that the content is readable.")
                .build();
        }

    /** the JSON pointer of the parser's position when the error occurred, e.g. "/db/password"; null if unknown. */
    static String keyPathAtError(JacksonException ex)
        {
        Object processor = ex.processor();
        if (processor instanceof JsonParser)
            {
            try
                {
                return ((JsonParser) processor).streamReadContext().pathAsPointer().toString();
                }
            catch (RuntimeException ignored)
                {
                // no usable parser state; position and type are still reported
                }
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
            JsonNode rootNode = mapper.createObjectNode();
            saveJson(rootNode, fileInstance);
            // no separate flush
            return getJsonJacksonConfigLayer(configLocation, rootNode, fileWithPath);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    @Override
    public void writeFile(ConfigLayerInterface layer)
            throws org.metabit.platform.support.config.ConfigCheckedException
        {
        if (layer instanceof JSONJacksonConfigLayer)
            {
            JSONJacksonConfigLayer jsonLayer = (JSONJacksonConfigLayer) layer;
            Path path = (Path) jsonLayer.getSource().getStorageInstanceHandle();
            JsonNode rootNode = jsonLayer.getRootNode();
            try
                {
                saveJson(rootNode, path);
                }
            catch (Exception ex)
                {
                throw new org.metabit.platform.support.config.ConfigCheckedException(ex);
                }
            }
        else
            {
            throw new org.metabit.platform.support.config.ConfigCheckedException(org.metabit.platform.support.config.ConfigCheckedException.ConfigExceptionReason.INVALID_USE);
            }
        }

    private void saveJson(JsonNode rootNode, Path path)
            throws IOException
        {
        if (settings.getBoolean(ConfigFeature.WRITE_CONDENSED_FORMAT))
            {
            mapper.writeValue(path.toFile(), rootNode);
            }
        else // prettyprinted
            {
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rootNode);
            }
        }

    private JSONJacksonConfigLayer getJsonJacksonConfigLayer(ConfigLocation configLocation, JsonNode rootNode, Object storageInstanceHandle)
        {
        switch (rootNode.getNodeType())
            {
            case OBJECT:
            case ARRAY:
                return new JSONJacksonConfigLayer(settings, logger, configLocation, this, rootNode, storageInstanceHandle);
            default:
                logger.warn("JSON toplevel token type "+rootNode.getNodeType()+" not supported");
                return null;
            }
        }
}
//___EOF___
