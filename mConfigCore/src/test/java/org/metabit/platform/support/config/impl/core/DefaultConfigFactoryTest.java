package org.metabit.platform.support.config.impl.core;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigLocation;

import java.util.List;
import java.util.function.Consumer;

class DefaultConfigFactoryTest
{

    @Test
    void checkSearchEntries()
        {
        ConfigFactoryBuilder configFactoryBuilder = ConfigFactoryBuilder.create("metabit", "CONFIGTEST");
        ConfigFactory configFactory = configFactoryBuilder.build();

        List<ConfigLocation> searchList = configFactory.getSearchList();

        // searchList.get(0)
        Consumer<ConfigLocation> printConsumer = new Consumer<ConfigLocation>()
            {
            public void accept(ConfigLocation csle)
                {
                System.out.println(csle.toString());
                }
            };

        searchList.forEach(printConsumer);
        }
}