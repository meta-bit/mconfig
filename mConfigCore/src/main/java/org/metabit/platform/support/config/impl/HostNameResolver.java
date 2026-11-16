/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.config.impl;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * determines the host name for {@link org.metabit.platform.support.config.ConfigFeature#HOSTNAME}.
 * <p>
 * Tries the candidates in order and returns the first usable one. Sources that need no name
 * resolution come first; {@link InetAddress#getLocalHost()} is the last resort, since it fails
 * where the host cannot resolve its own name (isolated containers) and may wait for a DNS timeout.
 * Never throws: a candidate that fails is skipped, and if none works, the result is null.
 */
final class HostNameResolver
{
    private static final int MAX_HOSTNAME_LENGTH = 253; // DNS limit; anything longer is not a host name

    private final List<Supplier<String>> candidates;

    HostNameResolver(final List<Supplier<String>> candidates)
        {
        this.candidates = candidates;
        }

    /**
     * the standard order: Linux kernel host name, /etc/hostname, Windows COMPUTERNAME, then name resolution.
     *
     * @return resolver with the standard candidates
     */
    static HostNameResolver standard()
        {
        return new HostNameResolver(Arrays.asList(
                ()->readFirstLine(Paths.get("/proc/sys/kernel/hostname")),
                ()->readFirstLine(Paths.get("/etc/hostname")),
                ()->System.getenv("COMPUTERNAME"), // platform detection, like user.name; not a module setting
                HostNameResolver::resolveLocalHost));
        }

    /**
     * @return the host name, or null if it cannot be determined
     */
    String resolve()
        {
        for (Supplier<String> candidate : candidates)
            {
            try
                {
                String name = candidate.get();
                if (isUsable(name))
                    return name.trim();
                }
            catch (RuntimeException e) // e.g. SecurityException, UncheckedIOException
                {
                // try the next candidate
                }
            }
        return null;
        }

    static boolean isUsable(final String name)
        {
        if (name == null)
            return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_HOSTNAME_LENGTH)
            return false;
        if (trimmed.equals(".") || trimmed.equals("..")) // path segments once the name keys a location (MLIB-157)
            return false;
        return trimmed.chars().noneMatch(c->Character.isISOControl(c) || c == '/' || c == '\\');
        }

    private static String readFirstLine(final Path path)
        {
        if (!Files.isRegularFile(path))
            return null;
        try
            {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return lines.isEmpty() ? null : lines.get(0);
            }
        catch (java.io.IOException e)
            {
            return null;
            }
        }

    private static String resolveLocalHost()
        {
        try
            {
            return InetAddress.getLocalHost().getHostName();
            }
        catch (java.net.UnknownHostException e)
            {
            return null;
            }
        }
}
//___EOF___
