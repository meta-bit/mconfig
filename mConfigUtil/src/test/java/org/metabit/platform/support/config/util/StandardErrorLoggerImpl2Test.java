package org.metabit.platform.support.config.util;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigCheckedException;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;


class StandardErrorLoggerImpl2Test
{
@Test
void testBuild() throws ConfigCheckedException
    {
    ConfigFactoryBuilder configFactoryBuilder= ConfigFactoryBuilder.create("metabit","CONFIGTEST");
    ConfigFactory configFactory= configFactoryBuilder.build();
    }


}