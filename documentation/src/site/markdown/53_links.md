# 5.4 Links
## 5.4.1 Requirements and Dependencies

mConfig in its Java incarnation requires the Java JRE, version 8 or higher at core.
Most of its modules require Java JRE 11 and up.

Additional dependencies are introduced only by its optional modules.

* mConfigLoggingSlf4j depends on slf4j-api from the org.slf4j package, see slf4j.org

## 5.4.2 References

mConfig is an original work by metabit, J.Wilkes.

Platform specific information was gathered from these sources:

* Linux
  * [XDG Base Directory Specification (freedesktop.org)](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html) - defines `$XDG_CONFIG_HOME` (`~/.config/`) and `$XDG_CONFIG_DIRS` (`/etc/xdg/`).
  * [Filesystem Hierarchy Standard (FHS)](https://refspecs.linuxfoundation.org/fhs.shtml) - defines `/etc`, `/etc/opt`, and `/usr/local/etc` system configuration directories.
* Windows
  * [Microsoft Learn: KNOWNFOLDERID (Known Folders)](https://learn.microsoft.com/en-us/windows/win32/shell/knownfolderid) - defines `FOLDERID_RoamingAppData` (`%APPDATA%`), `FOLDERID_LocalAppData` (`%LOCALAPPDATA%`), and `FOLDERID_ProgramData` (`%ProgramData%`).
  * [Microsoft Learn: Structure of the Registry](https://learn.microsoft.com/en-us/windows/win32/sysinfo/structure-of-the-registry) - defines `HKEY_CURRENT_USER\Software` (user), `HKEY_LOCAL_MACHINE\Software` (host), and `Software\Policies` (GPO/policy).
* MacOS
  * [Apple Developer: File System Basics & Standard Directories](https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/FileSystemOverview/FileSystemOverview.html) - defines `~/Library/Application Support/`, `~/Library/Preferences/`, `/Library/Application Support/`, and `/Library/Preferences/`.
* Android
  * [Android Developers: Data and file storage overview](https://developer.android.com/training/data-storage) - defines app-specific internal storage (`Context.getFilesDir()` / `/data/data/<package>/files/`) and scoped external storage (`Context.getExternalFilesDir()`).
