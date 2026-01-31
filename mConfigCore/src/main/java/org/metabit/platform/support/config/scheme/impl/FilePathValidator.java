package org.metabit.platform.support.config.scheme.impl;

import org.metabit.platform.support.config.ConfigEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Validator for FILEPATH types.
 */
public class FilePathValidator
{
    private final boolean exists;
    private final boolean isDirectory;
    private final boolean isFile;
    private final boolean canWrite;

    public FilePathValidator(Map<String, Object> flags)
        {
        this.exists = Boolean.TRUE.equals(flags.get("EXISTS"));
        this.isDirectory = Boolean.TRUE.equals(flags.get("IS_DIRECTORY"));
        this.isFile = Boolean.TRUE.equals(flags.get("IS_FILE"));
        this.canWrite = Boolean.TRUE.equals(flags.get("CAN_WRITE"));
        }

    public boolean validate(ConfigEntry entry)
        {
        try
            {
            Path p = entry.getValueAsPath();
            if (exists && !Files.exists(p)) return false;
            if (isDirectory && !Files.isDirectory(p)) return false;
            if (isFile && !Files.isRegularFile(p)) return false;
            if (canWrite && !Files.isWritable(p)) return false;
            return true;
            }
        catch (Exception e)
            {
            return false;
            }
        }
}
