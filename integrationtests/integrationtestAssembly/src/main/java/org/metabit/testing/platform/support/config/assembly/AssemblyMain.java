package org.metabit.testing.platform.support.config.assembly;

import org.metabit.platform.support.config.ConfigFactory;
import org.metabit.platform.support.config.ConfigFactoryBuilder;
import org.metabit.platform.support.config.ConfigFeature;

/**
 * the "application" packaged into the single JARs; started by AssemblyPackagingIT with {@code java -jar}.
 * <p>
 * No argument: reads a value from its bundled defaults (JAR source, PRODUCT scope) and prints it.
 * Argument {@code allow}: sets ALLOW_INCOMPLETE_SERVICE_REGISTRATION and only builds the factory.
 */
public final class AssemblyMain
{
    private AssemblyMain() { }

    public static void main(String[] args)
        {
        ConfigFactoryBuilder builder = ConfigFactoryBuilder.create("metabit", "assemblyIT");
        boolean allow = (args.length > 0) && "allow".equals(args[0]);
        if (allow)
            {
            builder.setFeature(ConfigFeature.ALLOW_INCOMPLETE_SERVICE_REGISTRATION, true);
            }
        try (ConfigFactory factory = builder.build())
            {
            if (allow)
                {
                System.out.println("started");
                return;
                }
            System.out.println("port="+factory.getConfig("network").getInteger("server/port"));
            }
        }
}
//___EOF___
