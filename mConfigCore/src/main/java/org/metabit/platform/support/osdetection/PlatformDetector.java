/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.osdetection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>PlatformDetector class.</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class PlatformDetector
{
static Map<Pattern, OperatingSystem> operatingSystemMap = new HashMap<>();

static
    {
    operatingSystemMap.put(Pattern.compile(".*(?:n[iu]x|aix).*", Pattern.DOTALL|Pattern.CASE_INSENSITIVE), OperatingSystem.LINUX);
    operatingSystemMap.put(Pattern.compile(".*android.*", Pattern.DOTALL|Pattern.CASE_INSENSITIVE), OperatingSystem.ANDROID);
    operatingSystemMap.put(Pattern.compile("Win.*", Pattern.DOTALL|Pattern.CASE_INSENSITIVE), OperatingSystem.WINDOWS);
/*
    "win"               // Windows -- but warning, "darwin" gives a false positive here.
    "mac" "macos", "mac os", "darwin"              // mac OS
    "nix","nux","aix"   // Unix variants
    "sunos", "solaris"             // Solaris
 */
    }

OperatingSystem os;
CPUArchitecture cpuArchitecture;
int             wordWidth;


/**
 * <p>Constructor for PlatformDetector.</p>
 */
public PlatformDetector()
    {
    detectCurrentPlatform();
    }

private static CPUArchitecture getArchFromName(String javaOSarch)
    {
    // x86
    throw new UnsupportedOperationException("not implemented yet");
    }

private static OperatingSystem getOSFromName(String javaOSname)
    {
    for (Map.Entry<Pattern, OperatingSystem> entry : operatingSystemMap.entrySet())
        {
        Matcher m = entry.getKey().matcher(javaOSname);
        if (m.matches())
            return entry.getValue();
        }
    return OperatingSystem.INVALID;
    }

/**
 * <p>checkOSandDetectBitType.</p>
 *
 * @return a int
 */
public static int checkOSandDetectBitType()
    {
    String osArch = System.getProperty("os.arch");
    if (osArch.indexOf("64") >= 0)
        return 64;
    else if (osArch.indexOf("i386") >= 0)
        return 32;
    else if (osArch.indexOf("x86") >= 0)    // standard value for 32 bit OS.
        return 32;
    else if (osArch.indexOf("i686") >= 0)
        return 32;
    else
        return -2;
    // oriented on the output of *ix `uname -m`
    // x86  - x86_64 - amd64
    }

/**
 * <p>Getter for the field <code>os</code>.</p>
 *
 * @return a {@link org.metabit.platform.support.osdetection.OperatingSystem} object
 */
public OperatingSystem getOs()
    {
    return os;
    }

void detectCurrentPlatform()
    {
    String javaOSname = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    String javaOSarch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    String javaOSversion = System.getProperty("os.version").toLowerCase(Locale.ENGLISH);
    os = getOSFromName(javaOSname);
    // add more on demand needed; this version has been minimized.
    // cpuArchitecture = getArchFromName(javaOSarch);
    // setVersionFromVersionString(javaOSversion);
    }
}
