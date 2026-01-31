package org.metabit.platform.support.config;

/**
 * if something happens which is not an Exception (interrupting program flow),
 * but not debug output for programmers either (which would go to logging).
 * These events are something in between which you may want to evaluate
 * under some circumstances and communicate accordingly.
 * e.g. if a config source explicitly specified by your code is not accessible.
 *
 * 
 * @version $Id: $Id
 */
public class ConfigEvent
{
}
