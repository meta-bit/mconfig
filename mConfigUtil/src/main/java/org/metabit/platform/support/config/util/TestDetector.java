package org.metabit.platform.support.config.util;

/**
 * The purpose of this class is to detect whether it is run in a test environment,
 * or not.
 * This is intended as a helper for lazy programmers, and allows to turn on
 * test mode of mConfig automatically.
 * <p>
 * Test frameworks supposed to be detected:
 * * JUnit
 * * TestNG
 * * Mockito
 * * AssertJ
 * * Spock
 * * Cucumber
 * * Aquillan
 * * JMockit
 */
public class TestDetector
{
    /**
     * try to detect whether the calling thread is started/run by a known testing
     * library
     *
     * @return true if testing lib is detected, false if not.
     */
    public static boolean isRunByTestingLibrary()
        {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace)
            {
            for (String detectionString : DETECTION_STRINGS)
                {
                if (element.getClassName().startsWith(detectionString))
                    return true;
                }
            }
        return false; // nothing detected
        }

    final static String[] DETECTION_STRINGS =
            {"junit", "org.junit.",
                    "org.testng.",
                    "org.mockito.",
                    "org.assertj.",
                    "spock.lang.",
                    "io.cucumber.",
                    "org.jboss.aquillian.",
                    "mockit."
            };
}
