/*
 * metabit configuration library - util API
 * (c) metabit 2018-2026, CC-BY-ND 4.0
 *
 * Almost all programs need some configuration. They may come with perfect defaults, they may be given command line
 * parameters on call, but integrating into a larger system usually requires some configuration.
 * Libraries, too, may need some configuration; even if the calling application provides it.
 *
 * The purpose of this library is to provide a generic util facility to read and write configuration data.
 * If what it supplies is sufficient for your purpose, great! And if you should need to do your own parsing, mapping - then
 * it should support you in implementing this, rather than getting in your way.
 *
 * The first goal is to find the configuration for you, with zero configuration if possible.
 * The second part is helping you parse it.
 * The third, optional part is making the access safer, providing defaults, and documentation.
 * -------------------
 *
 * minimal usage example:
 * <code>
 *     ConfigFactory cf = ConfigFactoryBuilder.getConfigFactoryWithDefaultSettings("ourCompanyName","ourApplicationName");
 *     JavaPropertyConfiguration config = configFactory.getConfiguration("test2", JavaPropertyConfiguration.class);
 * </code>
 * where the resulting JavaPropertyConfiguration is a special class that can be used as drop-in for existing java.util.Properties.
 */
package org.metabit.platform.support.config.util;