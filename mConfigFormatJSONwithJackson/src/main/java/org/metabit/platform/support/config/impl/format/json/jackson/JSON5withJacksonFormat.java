package org.metabit.platform.support.config.impl.format.json.jackson;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * JSON5 support via Jackson.
 * This extends the existing JSON support and enables JSON5-specific features.
 */
public class JSON5withJacksonFormat extends JSONwithJacksonFormat
{
    @Override
    public String getFormatID()
        {
        return "JSON5withJackson";
        }

    @Override
    public List<String> getFilenameExtensions()
        {
        return List.of(".json5");
        }

    /**
     * Creates an ObjectMapper with JSON5 features enabled.
     * @return configured ObjectMapper
     */
    protected ObjectMapper createObjectMapper()
        {
        JsonMapper.Builder builder = JsonMapper.builder();
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_JAVA_COMMENTS);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_YAML_COMMENTS);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_COMMA);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_SINGLE_QUOTES);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS);
        builder.enable(tools.jackson.core.json.JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS);
        return builder.build();
        }
}
