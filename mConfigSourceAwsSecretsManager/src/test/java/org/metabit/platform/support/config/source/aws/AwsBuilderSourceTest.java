package org.metabit.platform.support.config.source.aws;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * a source from AwsSecretsManagerConfigSource.builder() brings its own storage instance; the factory must initialize it (MLIB-136).
 * Hermetic: the SDK client is built, but no request is sent.
 * The bootstrap settings come from src/test/resources/.config/awstest/builder/aws.properties.
 */
class AwsBuilderSourceTest
{
    @Test
    void builderAddedSourceIsInitialized()
        {
        AwsSecretsManagerConfigSource source = AwsSecretsManagerConfigSource.builder()
                .withSecretId("test-secret")
                .withRegion("eu-central-1")
                .build();
        AwsSecretsManagerConfigStorage storage = (AwsSecretsManagerConfigStorage) source.getStorage();

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("awstest", "builder");
        builder.setTestMode(true);
        builder.setFeature(AwsFeatures.BYPASS_TEST, true); // intentional; nothing is fetched until a read
        builder.addSource(source);
        try (ConfigFactory factory = builder.build())
            {
            assertTrue(storage.isInitialized(), "init and postInit must run on the builder's own storage instance");
            }
        }

    @Test
    void builderAddedSourceIsNotInitializedInTestModeWithoutBypass()
        {
        AwsSecretsManagerConfigSource source = AwsSecretsManagerConfigSource.builder()
                .withSecretId("test-secret")
                .withRegion("eu-central-1")
                .build();
        AwsSecretsManagerConfigStorage storage = (AwsSecretsManagerConfigStorage) source.getStorage();

        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("awstest", "builder");
        builder.setTestMode(true);
        builder.addSource(source);
        try (ConfigFactory factory = builder.build())
            {
            assertFalse(storage.isInitialized(), "test mode without BYPASS_TEST must not set up an AWS client");
            }
        }
}
//___EOF___
