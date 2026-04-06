package org.metabit.platform.support.config.tool;

import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReproNested
{
    public static void main(String[] args) throws Exception
        {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("key", "value");
        root.put("inner", inner);

        ObjectMapper mapper = new ObjectMapper();
        ModerateWhitesmithsPrettyPrinter pp = new ModerateWhitesmithsPrettyPrinter();
        String json = mapper.writer().with(pp).writeValueAsString(root);
        System.out.println(json);
        }
}
