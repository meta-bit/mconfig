package org.metabit.testing.platform.support.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.Configuration;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.impl.DefaultConfigFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNull;

public class MinimalConfigBaseTest
{
    public static final String COMPANY_NAME = "metabit";
    public static final String APPLICATION_NAME = "mConfigIT";
    private static final String CONFIG_NAME     = "testconfig";


    @Test
    void minimalTest()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .build())
            { }
        }

    @Test
    void contextClassLoaderFeatureTest()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.USE_CONTEXT_CLASS_LOADER,true)
                .build())
            { }
        }


    @Test
    void testNullInsteadOfExceptionFeature()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
                .setFeature(ConfigFeature.EXCEPTION_ON_MISSING_ENTRY, false) // return null instead of throwing an exception
                .build())
            {
            Configuration cfg = factory.getConfig(CONFIG_NAME);
            String value1 = cfg.getString("undefinedEntry");
            assertNull(value1);
            }
        }



    // testing the module-based service loading in detail
    @Test
    void javaServiceLoadingTest()
            throws ClassNotFoundException
        {
        System.out.println(DefaultConfigFactory.class.getClassLoader());

        Class<?> dcfClass = ClassLoader.getSystemClassLoader().loadClass(DefaultConfigFactory.class.getCanonicalName());
        Assertions.assertNotNull(dcfClass);
        // --- this call finds the instance, so the *instance* is in the path. ---

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Assertions.assertNotNull(classLoader);
        ServiceLoader<ConfigFactory> slclis = ServiceLoader.load(ConfigFactory.class, classLoader);
        Iterator<ConfigFactory> it = slclis.iterator();
        if (it.hasNext() == false)
            {
            System.err.println("service loading failed.");
            }
        else while (it.hasNext())
            {
            ConfigFactory current = it.next();
            // what are our criteria?
            // they are to self-evaluate according to the settings we've supplied. we sort them according to that.
            System.out.println("potential ConfigFactory "+current.getClass().getCanonicalName());
            }

        // as long as anov

        return;
        }
}
