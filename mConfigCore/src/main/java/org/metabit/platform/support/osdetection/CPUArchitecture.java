/*
 * Copyright (c) metabit 2018. placed under CC-BY-ND-4.0 license.
 * Full license text available at https://tldrlegal.com/license/creative-commons-attribution-noderivatives-4.0-international-(cc-by-nd-4.0)#fulltext
 * You may: distribute, use for commercial and non-commercial purposes
 * You must: give credit, include/keep copyright, state any changes
 * You mustn't: distribute modified versions, sublicense
 */

package org.metabit.platform.support.osdetection;

/**
 * <p>CPUArchitecture class.</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public enum CPUArchitecture
{
    INVALID(0), ALPHA(1), ARM(2), I386(3), IA64(4), MIPS(5), MIPS64(6), PPC(7),
    S390(8), SH(9), SPARC(10), SPARC64(11), M68K(12), MISSING(13), MICROBLAZE(14), NIOS2(15),
    BLACKFIN(16), AVR32(17), ST200(18), SANDBOX(19), NDS32(20), OPENRISC(21), ARM64(22), ARC(23), X86_64(24);
/** Constant <code>NAMES</code> */
private static final String[] NAMES = {"INVALID", "alpha", "arm", "x86", "ia64", "mips", "mips64", "ppc" /*= powerpc */,
        "s390", "sh", "sparc", "sparc64", "m68k", "MISSING13", "microblaze", "nios2",
        "blackfin", "avr32", "st200", "sandbox", "nds32", "or1k" /* AKA openRISC 1000*/, "arm64", "arc",
        "x86_64"
};
private final        int      value;

CPUArchitecture(int value) { this.value = value; }

/**
 * <p>Getter for the field <code>value</code>.</p>
 *
 * @return a int
 */
public int getValue() { return value; }

/* the names do not map 1:1, so we need above LUT. */
/**
 * <p>getName.</p>
 *
 * @return a {@link java.lang.String} object
 */
public String getName() { return NAMES[value]; }
}
