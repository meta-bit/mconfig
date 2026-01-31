package org.metabit.platform.support.config.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;

class ConfigFactoryBuilderTest
{
@Test
void testPrivateInit() throws ConfigCheckedException
    {
    ConfigFactoryBuilder configFactoryBuilder = ConfigFactoryBuilder.create("metabit", "CONFIGTEST");

    }

@Test
void testBuild() throws ConfigCheckedException
    {
    ConfigFactoryBuilder configFactoryBuilder = ConfigFactoryBuilder.create("metabit", "CONFIGTEST");

    ConfigFactory configFactory = configFactoryBuilder.build();
    }


}