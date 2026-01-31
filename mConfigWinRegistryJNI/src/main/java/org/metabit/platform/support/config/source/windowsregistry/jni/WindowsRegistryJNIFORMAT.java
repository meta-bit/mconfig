package org.metabit.platform.support.config.source.windowsregistry.jni;

import org.metabit.platform.support.config.ConfigEntryType;
import org.metabit.platform.support.config.impl.ConfigFactorySettings;
import org.metabit.platform.support.config.interfaces.ConfigFormatInterface;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * provides format conversion for Windows Registry access via JNI.
 */
public class WindowsRegistryJNIFORMAT implements ConfigFormatInterface
{
    @Override
    public String getFormatID()
        {
        return "winregistryjni";
        }

    @Override
    public boolean testComponent(ConfigFactorySettings configFactorySettings, ConfigLoggingInterface logger)
        {
        return true;
        }

    public Object convertRegistryValue(int type, byte[] data)
        {
        if (data == null)
            {
            return null;
            }

        switch (type)
            {
            case WindowsRegistryJNI.REG_SZ:
            case WindowsRegistryJNI.REG_EXPAND_SZ:
                return new String(data, 0, findNullTerminator(data), StandardCharsets.UTF_16LE);

            case WindowsRegistryJNI.REG_DWORD:
                if (data.length >= 4)
                    {
                    return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    }
                return 0;

            case WindowsRegistryJNI.REG_QWORD:
                if (data.length >= 8)
                    {
                    return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    }
                return 0L;

            case WindowsRegistryJNI.REG_BINARY:
                return data;

            case WindowsRegistryJNI.REG_MULTI_SZ:
                return parseMultiSz(data);

            default:
                return new String(data, StandardCharsets.UTF_16LE);
            }
        }

    public ConfigEntryType mapRegistryTypeToConfigType(int type)
        {
        switch (type)
            {
            case WindowsRegistryJNI.REG_SZ:
            case WindowsRegistryJNI.REG_EXPAND_SZ:
                return ConfigEntryType.STRING;
            case WindowsRegistryJNI.REG_DWORD:
            case WindowsRegistryJNI.REG_QWORD:
                return ConfigEntryType.NUMBER;
            case WindowsRegistryJNI.REG_BINARY:
                return ConfigEntryType.BYTES;
            case WindowsRegistryJNI.REG_MULTI_SZ:
                return ConfigEntryType.MULTIPLE_STRINGS;
            default:
                return ConfigEntryType.STRING;
            }
        }

    private int findNullTerminator(byte[] data)
        {
        for (int i = 0; i < data.length - 1; i += 2)
            {
            if (data[i] == 0 && data[i+1] == 0)
                {
                return i;
                }
            }
        return data.length;
        }

    private List<String> parseMultiSz(byte[] data)
        {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < data.length - 1; i += 2)
            {
            if (data[i] == 0 && data[i+1] == 0)
                {
                if (i > start)
                    {
                    result.add(new String(data, start, i - start, StandardCharsets.UTF_16LE));
                    }
                start = i + 2;
                // If we encounter two null terminators in a row, it's the end of the list
                if (start < data.length - 1 && data[start] == 0 && data[start+1] == 0)
                    {
                    break;
                    }
                }
            }
        return result;
        }
}
