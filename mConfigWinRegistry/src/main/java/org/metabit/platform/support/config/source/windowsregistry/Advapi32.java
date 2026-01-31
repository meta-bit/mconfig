package org.metabit.platform.support.config.source.windowsregistry;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;
import jnr.ffi.types.u_int32_t;

/**
 * Native interface for Windows Advapi32.dll using JNR-FFI.
 */
public interface Advapi32
{
    Advapi32 INSTANCE = LibraryLoader.create(Advapi32.class).load("advapi32");

    int HKEY_CLASSES_ROOT = 0x80000000;
    int HKEY_CURRENT_USER = 0x80000001;
    int HKEY_LOCAL_MACHINE = 0x80000002;
    int HKEY_USERS = 0x80000003;

    int KEY_QUERY_VALUE = 0x0001;
    int KEY_ENUMERATE_SUB_KEYS = 0x0008;
    int KEY_WOW64_64KEY = 0x0100;
    int KEY_READ = 0x00020019 | KEY_WOW64_64KEY;

    int ERROR_SUCCESS = 0;
    int ERROR_NO_MORE_ITEMS = 259;
    int ERROR_FILE_NOT_FOUND = 2;

    int REG_NONE = 0;
    int REG_SZ = 1;
    int REG_EXPAND_SZ = 2;
    int REG_BINARY = 3;
    int REG_DWORD = 4;
    int REG_MULTI_SZ = 7;
    int REG_QWORD = 11;

    @u_int32_t
    int RegOpenKeyExA(
            int hKey,
            String lpSubKey,
            @u_int32_t int ulOptions,
            @u_int32_t int samDesired,
            PointerByReference phkResult
    );

    @u_int32_t
    int RegCloseKey(int hKey);

    @u_int32_t
    int RegQueryValueExA(
            int hKey,
            String lpValueName,
            @u_int32_t Pointer lpReserved,
            IntByReference lpType,
            byte[] lpData,
            IntByReference lpcbData
    );

    @u_int32_t
    int RegEnumKeyExA(
            int hKey,
            @u_int32_t int dwIndex,
            byte[] lpName,
            IntByReference lpcName,
            @u_int32_t Pointer lpReserved,
            byte[] lpClass,
            IntByReference lpcClass,
            Pointer lpftLastWriteTime
    );

    @u_int32_t
    int RegEnumValueA(
            int hKey,
            @u_int32_t int dwIndex,
            byte[] lpValueName,
            IntByReference lpcbValueName,
            @u_int32_t Pointer lpReserved,
            IntByReference lpType,
            byte[] lpData,
            IntByReference lpcbData
    );
}
