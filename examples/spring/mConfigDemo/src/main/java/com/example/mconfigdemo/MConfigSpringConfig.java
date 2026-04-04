package com.example.mconfigdemo;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MConfigSpringConfig
{

    @Bean(destroyMethod = "close")
    public ConfigFactory mconfigFactory()
        {
        // create factory for org=ACME app=demoApp
        return ConfigFactoryBuilder.create("ACME", "demoApp").build();
        }

    @Bean
    public org.metabit.platform.support.config.Configuration appConfiguration(ConfigFactory factory)
        {
        // obtain a live Configuration instance (no global singleton required)
        return factory.getConfig("network");
        }

    // so effective config vector is ACME:demoApp:network

    @Bean
    public NetworkConfigAdapter networkConfigAdapter(org.metabit.platform.support.config.Configuration cfg)
        {
        // adapter forwards to live Configuration
        return new NetworkConfigAdapter(cfg);
        }
}

// Note: the factory bean is closed by Spring at shutdown.
