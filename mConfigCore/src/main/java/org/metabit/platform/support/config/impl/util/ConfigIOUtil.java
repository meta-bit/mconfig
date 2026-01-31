package org.metabit.platform.support.config.impl.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Low-level IO and data transformation utility functions for mConfig core.
 * Internal implementation class.
 */
public class ConfigIOUtil
{
    /**
     * Decodes a hexadecimal string into a byte array.
     * Supports "0x" prefix and common separators like space, tab, newline, carriage return, and colon.
     *
     * @param tmpString the hexadecimal string to decode
     * @return the decoded byte array, or null if the input is null
     */
    public static byte[] hexDecode(String tmpString)
        {
        final String SEPARATORS = " \t\n\r:";
        if (tmpString == null)
            {
            return null;
            }
        // detect and trim "0x" introducer.
        if (tmpString.startsWith("0x"))
            {
            tmpString = tmpString.substring(2);
            }
        // start actual processing.
        int maxLength = tmpString.length() / 2;
        int offset = 0;
        byte[] byteBuffer = new byte[maxLength]; // upper boundary.
        int separatorCount = 0;
        boolean msbFlag = true;
        byte b = 0;
        for (int i = 0; i < tmpString.length(); i++)
            {
            int c = Character.digit(tmpString.charAt(i), 16);
            if (c < 0) // separators or invalid
                {
                if (msbFlag)
                    {
                    if (SEPARATORS.indexOf(tmpString.charAt(i)) >= 0) // valid separator
                        {
                        separatorCount++;
                        if (separatorCount > 2)
                            {
                            break; // stop the loop, sequence ended by 2 or more separators in sequence
                            }
                        continue; // separator accepted, skip and continue
                        }
                    }
                }
            else // valid hexit
                {
                if (msbFlag)
                    {
                    b = (byte) (c << 4);
                    msbFlag = false;
                    } // upper Nybble
                else
                    {
                    b |= (byte) (c);
                    msbFlag = true;
                    byteBuffer[offset++] = b;
                    separatorCount = 0;
                    }
                }
            }
        if (offset == maxLength)  // no need to trim
            {
            return byteBuffer;
            }
        // trim bytes to the number of bytes actually used
        byte[] resultBytes = new byte[offset];
        System.arraycopy(byteBuffer, 0, resultBytes, 0, offset);
        return resultBytes;
        }

    /**
     * Deletes a directory and all its contents recursively.
     *
     * @param dirToDelete the path to the directory to delete
     * @throws IOException if an I/O error occurs
     */
    public static void deleteDirectoryWithContents(final Path dirToDelete) throws IOException
        {
        if (dirToDelete == null || !Files.exists(dirToDelete))
            {
            return;
            }
        try (Stream<Path> pathStream = Files.walk(dirToDelete))
            {
            pathStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
}
