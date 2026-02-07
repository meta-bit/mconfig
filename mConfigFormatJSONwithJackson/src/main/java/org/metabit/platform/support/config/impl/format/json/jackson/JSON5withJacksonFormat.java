package org.metabit.platform.support.config.impl.format.json.jackson;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

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
        return "JSON5witJackson";
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
        return JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        }
}
