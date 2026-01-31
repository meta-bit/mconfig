package org.metabit.platform.support.config.source.windowsregistry.jni;

/**
 * Native interface for Windows Registry using JNI.
 */
public class WindowsRegistryJNI
{
    public static final int HKEY_CLASSES_ROOT = 0x80000000;
    public static final int HKEY_CURRENT_USER = 0x80000001;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;
    public static final int HKEY_USERS = 0x80000003;

    public static final int KEY_QUERY_VALUE = 0x0001;
    public static final int KEY_ENUMERATE_SUB_KEYS = 0x0008;
    public static final int KEY_WOW64_64KEY = 0x0100;
    public static final int KEY_READ = 0x00020019 | KEY_WOW64_64KEY;

    public static final int ERROR_SUCCESS = 0;
    public static final int ERROR_NO_MORE_ITEMS = 259;
    public static final int ERROR_FILE_NOT_FOUND = 2;

    public static final int REG_NONE = 0;
    public static final int REG_SZ = 1;
    public static final int REG_EXPAND_SZ = 2;
    public static final int REG_BINARY = 3;
    public static final int REG_DWORD = 4;
    public static final int REG_MULTI_SZ = 7;
    public static final int REG_QWORD = 11;

    static
        {
        try
            {
            loadLibrary();
            }
        catch (Exception e)
            {
            // Logging may not be available yet, so we use stderr as a fallback for this critical failure
            System.err.println("CRITICAL: Failed to load mConfigWinRegistryJNI native library: " + e.getMessage());
            }
        }

    private static void loadLibrary() throws Exception
        {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("win"))
            {
            return;
            }

        String libName = "mConfigWinRegistryJNI.dll";

        // 1. Try to load from system library path first (allows user override via java.library.path or system folders)
        try
            {
            System.loadLibrary("mConfigWinRegistryJNI");
            return;
            }
        catch (UnsatisfiedLinkError e)
            {
            // Not found on library path, continue with extraction logic
            }

        String resourcePath = "/META-INF/natives/windows-x64/" + libName;

        // We use a versioned temp directory to avoid collisions and allow updates
        String version = WindowsRegistryJNI.class.getPackage().getImplementationVersion();
        if (version == null)
            {
            version = "dev";
            }

        java.nio.file.Path tempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "mConfig", version);
        java.nio.file.Files.createDirectories(tempDir);
        java.nio.file.Path tempFile = tempDir.resolve(libName);

        // 2. Check if the DLL already exists in the versioned temp directory
        // If it exists, we forgo extraction to allow users to provide their own (e.g. signed) DLL
        if (java.nio.file.Files.exists(tempFile))
            {
            try
                {
                System.load(tempFile.toAbsolutePath().toString());
                return;
                }
            catch (UnsatisfiedLinkError e)
                {
                // If loading fails (e.g. corrupted file or incompatible signature), we attempt to overwrite it
                }
            }

        // 3. Extraction logic
        try (java.io.InputStream is = WindowsRegistryJNI.class.getResourceAsStream(resourcePath))
            {
            if (is == null)
                {
                throw new java.io.FileNotFoundException("Native library not found in JAR: " + resourcePath);
                }

            java.nio.file.Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

        System.load(tempFile.toAbsolutePath().toString());
        }

    public native int openKey(int hKey, String subKey, int samDesired);

    public native void closeKey(int hKey);

    public native RegistryValue enumValue(int hKey, int index);

    public native String enumKey(int hKey, int index);

    public static class RegistryValue
    {
        public int type;
        public byte[] data;
        public String name;
    }
}