/*
 * Copyright (c) 2018-2026 metabit GmbH.
 * Licensed under the mConfig Design Integrity License (v0.7.26 - 1.0.0-pre),
 * based on the Polyform Shield License 1.0.0.
 * See mConfigCore/LICENSE.md for details.
 */

package org.metabit.platform.support.osdetection;

/**
 * Operating Systems.
 * A list that tries to be complete, at least for practical purposes
 * from the year 2020 onward, and including some
 * historical systems, as well as many embedded ones.
 * Numerical values chosen to be compatible with "U-Boot" bootloader numeric IDs.
 * see IH_OS_* in https://github.com/u-boot/u-boot/blob/master/include/image.h
 * for values below 256.
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public enum OperatingSystem
{
    INVALID(0),
    OPENBSD(1),
    NETBSD(2),
    FREEBSD(3),
    BSD4_4(4) /* sorry: 4_4BSD is not acceptable as enum name */,
    LINUX(5),
    SVR4(6),
    ESIX(7),
    SOLARIS(8),
    IRIX(9),
    SCO(10),
    DELL(11),
    NCR(12),
    LYNXOS(13),
    VXWORKS(14),
    PSOS(15),
    QNX(16),
    U_BOOT(17),
    RTEMS(18),
    ARTOS(19),
    UNITY(20),
    INTEGRITY(21),
    OSE(22),
    PLAN9(23),
    OPENRTOS(24),
    ARM_TRUSTED_FIRMWARE(25),
    TEE(26),
    OPENSBI(27),
    EFI(28), // EFI Firmware
    ELF(20), // ELF image
    WINDOWS(256),
    MACOS(257),
    IOS(258), // Apple iOS
    ANDROID(259); //


    /** Constant <code>NAMES</code> */
    private static final String[] NAMES = {"INVALID",
            "openbsd",
            "netbsd",
            "freebsd",
            "4_4bsd",
            "linux",
            "svr4",
            "esix",
            "solaris",
            "irix",
            "sco",
            "dell",
            "ncr",
            "lynxos",
            "vxworks",
            "psos",
            "qnx",
            "u-boot",
            "rtems",
            "artos",
            "unity",
            "integrity",
            "ose",
            "plan9",
            "openrtos",
            "ARM trusted firmware",
            "TEE", // "Trusted Execution Environment"
            "OpenSBI", // RISC-V
            "EFI",
            "ELF"};

    private final int value;

    OperatingSystem(int value) { this.value = value; }

    /**
     * give the numeric value of an Operating System ID.
     *
     * @return integer value for the operating system enum ID.
     */
    public int getValue() { return value; }

    /**
     * separate lookup table and logic because the original uImage identifiers to not map well to Java identifiers.
     * especially 4_4BSD does not.
     * Also, WINDOWS, MACOS etc. don't either.
     *
     * @return OS name, from predefined list.
     */
    public String getName()
        {
        if (value == WINDOWS.value) { return "WINDOWS"; }
        else if (value == ANDROID.value) { return "ANDROID"; }
        else if (value == MACOS.value) { return "MACOS"; }
        else if (value == IOS.value) { return "iOS"; }
        else { return NAMES[value].toUpperCase(); }
        }

}
// @TODO AIX value
// see u-boot include/image.h for reference values to align with
