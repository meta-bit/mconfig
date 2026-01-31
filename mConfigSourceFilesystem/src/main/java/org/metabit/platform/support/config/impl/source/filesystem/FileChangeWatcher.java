package org.metabit.platform.support.config.impl.source.filesystem;

import org.metabit.platform.support.config.ConfigException;
import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigLocation;
import org.metabit.platform.support.config.impl.ConfigFactoryInstanceContext;
import org.metabit.platform.support.config.impl.SourceChangeNotifier;
import org.metabit.platform.support.config.interfaces.ConfigLoggingInterface;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/// file change watcher.
/// JDK NIO provides a "WatchService"; but it watches directories only.
/// That limits its usefulness.
/// This class is for watching files for changes.
/// It records flags and waits for polling, due to the structure of mConfig
/// - the config sources have been unified.
/// It also records directories and files which do not exist yet,
/// to report when they come into existence, and then watch them.
/// detects atomic swaps using ENTRY_CREATE and ENTRY_DELETE
public class FileChangeWatcher extends TimerTask
{
    private final ConfigLoggingInterface logger;
    private final WatchService           watchService; // JDK1.7
    private final boolean               watchServiceAvailable;
    private final Map<Path, WatchKey>    directoryWatchMap;
    private final Map<Path, Path>        fileWatchMapForward; //  file to directory
    private final PathPathMultimap       fileWatchMapBackward; // directory to file
    private final Map<Path, Boolean>     fileChangedFlags;
    private final Timer                  timer;
    private final PathPathMultimap       cantWatchThis;
    private final ConfigFactoryInstanceContext ctx;
    private final Map<Path, ConfigLocation> pathToLocationMap;

    public FileChangeWatcher(ConfigFactoryInstanceContext ctx)
            throws IOException
        {
        this.ctx = ctx;
        this.logger = ctx.getLogger();
        WatchService createdWatchService = null;
        boolean watchServiceOk = false;
        try
            {
            createdWatchService = FileSystems.getDefault().newWatchService(); // may throw UnsupportedOperationException on some exotic filesystems, in theory.
            watchServiceOk = true;
            }
        catch (IOException ex)
            {
            logger.warn("file watching disabled (WatchService unavailable)", ex);
            }
        this.watchService = createdWatchService;
        this.watchServiceAvailable = watchServiceOk;
        this.directoryWatchMap = new HashMap<>();
        this.fileWatchMapForward = new HashMap<>();
        this.fileWatchMapBackward = new PathPathMultimap();
        this.fileChangedFlags = new HashMap<>();
        this.cantWatchThis = new PathPathMultimap();
        this.pathToLocationMap = new HashMap<>();
        int checkIntervalMilliseconds = ctx.getSettings().getInteger(ConfigFeature.UPDATE_CHECK_FREQUENCY_MS);
        if (watchServiceAvailable && checkIntervalMilliseconds > 0)
            {
            timer = new Timer(true); // daemon thread
            timer.scheduleAtFixedRate(this, 0, checkIntervalMilliseconds);
            }
        else
            {
            timer = null;
            if (watchServiceAvailable)
                {
                logger.info("file refresh timer turned off");
                }
            }
        }

    public synchronized void cleanup()
            throws IOException
        {
        if (timer != null)
            { timer.cancel(); }
        // this.watchMap.forEach((key,value) -> { value.cancel(); watchMap.remove(key); }); looks elegant but causes ConcurrentModificationException.
        Iterator<Map.Entry<Path, WatchKey>> it = directoryWatchMap.entrySet().iterator();
        while (it.hasNext())
            {
            Map.Entry<Path, WatchKey> entry = it.next();
            entry.getValue().cancel();
            it.remove(); // remove via iterator to avoid ConcurrentModificationException
            }
        if (watchService != null)
            {
            this.watchService.close();
            }
        fileChangedFlags.clear();
        fileWatchMapForward.clear();
        fileWatchMapBackward.clear();
        cantWatchThis.clear();
        return;
        }

    /**
     * add a file path for watching.
     *
     * @param file file path to be watched. It is *not* required to exist.
     * @param location the ConfigLocation this file belongs to.
     */
    public synchronized void addFile(final Path file, final ConfigLocation location)
        {
        if (file.toFile().isDirectory())
            throw new IllegalArgumentException("file parameter is a directory");
        // if (file.toFile().exists())

        Path parentDir = file.getParent();
        // if the directory does not exist at all, we can't put a watch on it.
        // it's not impossible, but much harder to handle. left for later.
        if (!parentDir.toFile().exists())
            {
            cantWatchThis.put(parentDir,file.toAbsolutePath());
            pathToLocationMap.put(file.toAbsolutePath(), location);
            return;
            }
        if (fileWatchMapForward.put(file, parentDir) != null)
            {
            logger.info("replacing/re-setting file watch for "+file);
            }
        if (!fileWatchMapBackward.containsKey(parentDir)) // are we watching this directory already?
            {
            addDirectory(parentDir); // on first watch on this directory, create entry.
            }
        fileWatchMapBackward.put(parentDir, file);
        fileChangedFlags.put(file, false);
        pathToLocationMap.put(file, location);
        return;
        }

    /**
     * remove a file from watching.
     *
     * @param file file path to be not-watched-anymore.
     */
    public synchronized void removeFile(final Path file)
        {
        if (file.toFile().isDirectory())
            throw new IllegalArgumentException("file parameter is a directory");
        Path parentDir = file.getParent();
        if (fileWatchMapForward.remove(file) == null)
            logger.warn("removing non-existent file watch for"+file);
        if (fileWatchMapBackward.remove(parentDir, file))
            { removeDirectory(parentDir); } // no-one watching this anymore.
        fileChangedFlags.remove(file);
        pathToLocationMap.remove(file);
        return;
        }

    /**
     * check for changes and reset.
     *
     * @param fileToCheck file to check for changes
     * @return true if it had been changed since last check, false if it hasn't. flag resets automatically
     */
    public boolean hasChanged(final Path fileToCheck)
        {
        if (!fileChangedFlags.containsKey(fileToCheck))
            {
            if (fileToCheck.toFile().isDirectory()) // for directories
                {
                logger.warn("hasChanged called with a directory argument");
                return false;
                }
            logger.warn("trying to check for changes on file "+fileToCheck+" which had not been registered for checking.");
            return false;
            }
        else
            {
            // RRR - read, reset, return
            boolean flagEntry = fileChangedFlags.get(fileToCheck); // read (with unboxing)
            fileChangedFlags.put(fileToCheck, Boolean.FALSE); // reset
            return flagEntry; // return
            }
        }

    //--------------------------------------------------------------------------
    // for testing and the like
    int howManyDirectoriesAreWatched() { synchronized(directoryWatchMap) { return directoryWatchMap.size(); } }
    int howManyDirectoriesAreWaitedForToComeIntoExistence() { synchronized(cantWatchThis) { return cantWatchThis.size(); } }

    //--------------------------------------------------------------------------
    // create directory watches, and watch keys.
    protected void addDirectory(final Path dir)
        {
        if (!watchServiceAvailable)
            {
            logger.debug("watch service unavailable; skipping directory watch for "+dir);
            return;
            }
        if (!dir.toFile().isDirectory())
            throw new IllegalArgumentException("parameter is not a directory");
        if (directoryWatchMap.get(dir) != null)
            {
            logger.warn("trying to re-watch directory "+dir);
            return;
            }
        try
            {
            WatchKey watchKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            directoryWatchMap.put(dir, watchKey);
            }
        catch (Exception e)
            {
            throw new ConfigException(e);
            }
        return;
        }

    // remove directory watches, and watch keys.
    protected void removeDirectory(final Path directory)
        {
        if (!watchServiceAvailable)
            {
            directoryWatchMap.remove(directory);
            return;
            }
        // this is where we remove watches and watch keys
        WatchKey entry = directoryWatchMap.get(directory); // directory?
        if (entry == null) // file then?
            {
            logger.warn("removal of non-existent watch failed: none registered for "+directory);
            return;
            }
        WatchKey watch;
        synchronized(this)
            {
            watch = directoryWatchMap.remove(directory); // remove from map first
            }
        watch.cancel(); // unregister with watcher service
        return;
        }

    /**
     * --- internal , but given public signature so we don't have to construct an entire inner class.
     * TimerTask run.
     */
    @Override
    public void run()
        {
        if (!watchServiceAvailable)
            {
            return;
            }
        // Snapshot the directoryWatchMap to avoid holding the lock while processing
        final List<Map.Entry<Path, WatchKey>> snapshot;
        synchronized (this)
            { snapshot = new ArrayList<>(directoryWatchMap.entrySet()); }

        for (Map.Entry<Path, WatchKey> e : snapshot)
            {
            Path key = e.getKey();
            WatchKey watchKey = e.getValue();
            if (!watchKey.isValid())
                {
                // cause: directory has been deleted.
                logger.debug("detected DELETION of \""+key+"\"");
                // 1. remove the watch.
                watchKey.cancel(); // un-register this watchKey from watch service
                // 2. remove the respective entry from the directoryWatchMap
                synchronized (this)
                    {
                    directoryWatchMap.remove(key);
                    // 3. re-add all files for this directory to the to-be-watched map.
                    Collection<Path> files = fileWatchMapBackward.internalRepresentation.remove(key);
                    if (files != null) {
                        for (Path file : files)
                            { cantWatchThis.put(key, file); }
                    }
                }
                continue; // nothing to be done any more for this watchKey.
                }
            for (WatchEvent<?> event : watchKey.pollEvents())
                {
                // Path events is the only kind (not "kind") we have here.
                WatchEvent.Kind<?> kind = event.kind();
                Path file;
                try
                    {
                    // NB: each access to "event.context()" seams to clear or modify its contents.
                    file = key.resolve((Path) event.context()); // The "resolve" is important.
                    }
                catch (ClassCastException ex)
                    {
                    logger.error("invalid WatchEvent type (should never happen), stopping", ex);
                    this.timer.cancel(); // big issue if this ever happens, reason to stop the timer.
                    continue; // skip any subsequent event handling for this.
                    }
                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) // existing file (or subdirectory) has been changed.
                    {
                    logger.debug("detected CHANGE in \""+file+"\"");
                    // check for subscribed file
                    if (!fileWatchMapForward.containsKey(file))
                        {
                        logger.debug("file without watch changed: "+file);
                        continue;
                        }
                    processFileChangeEvent(file); // modify implicit
                    }
                else if (kind == StandardWatchEventKinds.ENTRY_DELETE) // existing file or directory deleted
                    {
                    logger.debug("detected directory entry deletion: \""+file+"\"");
                    processFileChangeEvent(file); // deletion is a big change, too.
                    }
                else if (kind == StandardWatchEventKinds.ENTRY_CREATE) // new file or directory
                    {
                    logger.debug("detected directory entry creation: \""+file+"\"");
                    processFileChangeEvent(file);
                    }
                else
                    {
                    logger.warn("unknown/invalid watch event kind "+kind);
                    }
                }
            // reset watchkey to continue receiving events for the watch. important.
            watchKey.reset();
            }
        // now check the "can't watch this" set whether the directories started to exist, and if so, add the respective watches.

        // iterate over a snapshot to avoid CME; remove using iterator under lock
        final Iterator<Map.Entry<Path, Set<Path>>> it;
        synchronized (this)
            { it = new ArrayList<>(cantWatchThis.internalRepresentation.entrySet()).iterator(); }
        while (it.hasNext())
            {
            Map.Entry<Path, Set<Path>> entry = it.next();
            Path dirPath = entry.getKey();
            if (dirPath.toFile().exists()) // a directory we have been expecting has been created/found.
                {
                logger.info("previously non-existing directory detected: "+dirPath);
                Set<Path> files = entry.getValue();
                synchronized (this)
                    {
                    // remove from the live structure while holding the lock
                    cantWatchThis.internalRepresentation.remove(dirPath);
                    }
                for (Path file : files)
                    {
                    ConfigLocation location = pathToLocationMap.get(file);
                    addFile(file, location);
                    }
                addDirectory(dirPath);
                }
            }
        return;
        }

    public void processFileChangeEvent(final Path file)
        {
        synchronized(this)
            {
            if (fileChangedFlags.containsKey(file))
                {
                fileChangedFlags.put(file, Boolean.TRUE);
                // notify the subscriber model
                ConfigLocation location = pathToLocationMap.get(file);
                if (location != null && ctx.getSourceChangeNotifier() != null)
                    {
                    ctx.getSourceChangeNotifier().sendNotificationsAboutChangeInConfigLocation(location);
                    }
                }
            }
        }
}
/*
zu testen:
1. Ist die Datei-Beobachtungsliste vollständig?
Nein; das müßte mit der "nonExistentDirectoriesToWatchLater" gemacht werden;
und da reicht keine ArrayList, das muß wieder eine multimap werden, damit man darin die files mit aufnehmen kann.
Das ist noch nicht perfekt.


2. werden Änderungen all dieser Dateien erkannt? Alle? (create, delete, modify)
3. sind diese Änderungen in der entsprechenden flag-liste korrekt festgehalten?
4. werden Abfragen der Änderungen durchgeführt?
5. welche Folgen haben erfolgreiche Abfragen? Führen diese zu einem re-read?
6. kommt der re-read auch durch, so daß sich Resultate zeigen?

 */

//___EOF___
