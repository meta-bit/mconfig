package org.metabit.testing.platform.support.config.test;

import org.metabit.platform.support.config.*;
import org.metabit.platform.support.config.util.ConfigUtil;

import java.util.Properties;

public class TestFromJAR
{
 public static void main(String[] args)
     {
     try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
//             .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY,false) // return null instead, please.
//             .setFeature(ConfigFeature.TEST_MODE,true)
             .build())
         {
         System.out.println("mConfig search list");
         for (ConfigLocation cse : factory.getSearchList())
             { System.out.println(cse.toString()); }

         Configuration cfg = factory.getConfig(CONFIG_NAME);
         if (cfg == null)
             throw new RuntimeException("cfg instance is null");
         String testvalue = cfg.getString("dummy");
         if (testvalue == null)
             throw new RuntimeException("reading from JAR resources failed (assuming this was run from within a JAR)");
         Properties extracted = ConfigUtil.copyPrefixedEntriesToJavaProperties(cfg,"prefixed.");
         if (extracted ==  null)
             throw new RuntimeException("extracted properties is null");
         assert(! extracted.isEmpty());

        //@TODO more tests. e.g. check the layers
         }


     }

    static final String COMPANY_NAME = "metabit";
    static final String APPLICATION_NAME = "mConfigIT";
    static final String CONFIG_NAME     = "testconfig";
}
