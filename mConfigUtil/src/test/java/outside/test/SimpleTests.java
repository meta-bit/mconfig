package outside.test;

import org.junit.jupiter.api.Test;
import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;

public class SimpleTests
{

    public static final String COMPANY_NAME = "netabit";
    public static final String APPLICATION_NAME = "TESTING";

    @Test
    void testMostSimple()
        {
        try (ConfigFactory factory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME).build()) {}
        }

}
