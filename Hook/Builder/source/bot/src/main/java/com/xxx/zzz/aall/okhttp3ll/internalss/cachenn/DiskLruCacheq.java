
package com.xxx.zzz.aall.okhttp3ll.internalss.cachenn;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xxx.zzz.aall.okhttp3ll.internalss.Utilaq;
import com.xxx.zzz.aall.okhttp3ll.internalss.ioss.FileSystema;
import com.xxx.zzz.aall.okhttp3ll.internalss.platformsss.Platformq;
import com.xxx.zzz.aall.javaxlll.annotationlll.Nullableq;

import com.xxx.zzz.aall.okioss.BufferedSinkzaqds;
import com.xxx.zzz.aall.okioss.BufferedSourcezaqdfs;
import com.xxx.zzz.aall.okioss.Okiozaq;
import com.xxx.zzz.aall.okioss.Sinkzaq;
import com.xxx.zzz.aall.okioss.Sourcezaq;


public final class DiskLruCacheq implements Closeable, Flushable {
  static final String JOURNAL_FILE = "journal";
  static final String JOURNAL_FILE_TEMP = "journal.tmp";
  static final String JOURNAL_FILE_BACKUP = "journal.bkp";
  static final String MAGIC = "libcore.io.DiskLruCache";
  static final String VERSION_1 = "1";
  static final long ANY_SEQUENCE_NUMBER = -1;
  static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,120}");
  private static final String CLEAN = "CLEAN";
  private static final String DIRTY = "DIRTY";
  private static final String REMOVE = "REMOVE";
  private static final String READ = "READ";



  final FileSystema fileSystem;
  final File directory;
  private final File journalFile;
  private final File journalFileTmp;
  private final File journalFileBackup;
  private final int appVersion;
  private long maxSize;
  final int valueCount;
  private long size = 0;
  BufferedSinkzaqds journalWriter;
  final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap<>(0, 0.75f, true);
  int redundantOpCount;
  boolean hasJournalErrors;

  
  boolean initialized;
  boolean closed;
  boolean mostRecentTrimFailed;
  boolean mostRecentRebuildFailed;


  private long nextSequenceNumber = 0;


  private final Executor executor;
  private final Runnable cleanupRunnable = new Runnable() {
    public void run() {
      synchronized (DiskLruCacheq.this) {
        if (!initialized | closed) {
          return; 
        }

        try {
          trimToSize();
        } catch (IOException ignored) {
          mostRecentTrimFailed = true;
        }

        try {
          if (journalRebuildRequired()) {
            rebuildJournal();
            redundantOpCount = 0;
          }
        } catch (IOException e) {
          mostRecentRebuildFailed = true;
          journalWriter = Okiozaq.buffer(Okiozaq.blackhole());
        }
      }
    }
  };

  DiskLruCacheq(FileSystema fileSystem, File directory, int appVersion, int valueCount, long maxSize,
                Executor executor) {
    this.fileSystem = fileSystem;
    this.directory = directory;
    this.appVersion = appVersion;
    this.journalFile = new File(directory, JOURNAL_FILE);
    this.journalFileTmp = new File(directory, JOURNAL_FILE_TEMP);
    this.journalFileBackup = new File(directory, JOURNAL_FILE_BACKUP);
    this.valueCount = valueCount;
    this.maxSize = maxSize;
    this.executor = executor;
  }

  public synchronized void initialize() throws IOException {
    assert Thread.holdsLock(this);

    if (initialized) {
      return; 
    }

    
    if (fileSystem.exists(journalFileBackup)) {
      
      if (fileSystem.exists(journalFile)) {
        fileSystem.delete(journalFileBackup);
      } else {
        fileSystem.rename(journalFileBackup, journalFile);
      }
    }

    
    if (fileSystem.exists(journalFile)) {
      try {
        readJournal();
        processJournal();
        initialized = true;
        return;
      } catch (IOException journalIsCorrupt) {
        Platformq.get().log(Platformq.WARN, "DiskLruCache " + directory + " is corrupt: "
            + journalIsCorrupt.getMessage() + ", removing", journalIsCorrupt);
      }

      
      
      try {
        delete();
      } finally {
        closed = false;
      }
    }

    rebuildJournal();

    initialized = true;
  }


  public static DiskLruCacheq create(FileSystema fileSystem, File directory, int appVersion,
                                     int valueCount, long maxSize) {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize <= 0");
    }
    if (valueCount <= 0) {
      throw new IllegalArgumentException("valueCount <= 0");
    }

    
    Executor executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(), Utilaq.threadFactory("OkHttp DiskLruCache", true));

    return new DiskLruCacheq(fileSystem, directory, appVersion, valueCount, maxSize, executor);
  }

  private void readJournal() throws IOException {
    BufferedSourcezaqdfs source = Okiozaq.buffer(fileSystem.source(journalFile));
    try {
      String magic = source.readUtf8LineStrict();
      String version = source.readUtf8LineStrict();
      String appVersionString = source.readUtf8LineStrict();
      String valueCountString = source.readUtf8LineStrict();
      String blank = source.readUtf8LineStrict();
      if (!MAGIC.equals(magic)
          || !VERSION_1.equals(version)
          || !Integer.toString(appVersion).equals(appVersionString)
          || !Integer.toString(valueCount).equals(valueCountString)
          || !"".equals(blank)) {
        throw new IOException("unexpected journal header: [" + magic + ", " + version + ", "
            + valueCountString + ", " + blank + "]");
      }

      int lineCount = 0;
      while (true) {
        try {
          readJournalLine(source.readUtf8LineStrict());
          lineCount++;
        } catch (EOFException endOfJournal) {
          break;
        }
      }
      redundantOpCount = lineCount - lruEntries.size();

      
      if (!source.exhausted()) {
        rebuildJournal();
      } else {
        journalWriter = newJournalWriter();
      }
    } finally {
      Utilaq.closeQuietly(source);
    }
  }

  private BufferedSinkzaqds newJournalWriter() throws FileNotFoundException {
    Sinkzaq fileSink = fileSystem.appendingSink(journalFile);
    Sinkzaq faultHidingSink = new FaultHidingSinkq(fileSink) {
      @Override protected void onException(IOException e) {
        assert (Thread.holdsLock(DiskLruCacheq.this));
        hasJournalErrors = true;
      }
    };
    return Okiozaq.buffer(faultHidingSink);
  }

  private void readJournalLine(String line) throws IOException {
    int firstSpace = line.indexOf(' ');
    if (firstSpace == -1) {
      throw new IOException("unexpected journal line: " + line);
    }

    int keyBegin = firstSpace + 1;
    int secondSpace = line.indexOf(' ', keyBegin);
    final String key;
    if (secondSpace == -1) {
      key = line.substring(keyBegin);
      if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
        lruEntries.remove(key);
        return;
      }
    } else {
      key = line.substring(keyBegin, secondSpace);
    }

    Entry entry = lruEntries.get(key);
    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    }

    if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
      String[] parts = line.substring(secondSpace + 1).split(" ");
      entry.readable = true;
      entry.currentEditor = null;
      entry.setLengths(parts);
    } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
      entry.currentEditor = new Editor(entry);
    } else if (secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ)) {
      
    } else {
      throw new IOException("unexpected journal line: " + line);
    }
  }


  private void processJournal() throws IOException {
    fileSystem.delete(journalFileTmp);
    for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
      Entry entry = i.next();
      if (entry.currentEditor == null) {
        for (int t = 0; t < valueCount; t++) {
          size += entry.lengths[t];
        }
      } else {
        entry.currentEditor = null;
        for (int t = 0; t < valueCount; t++) {
          fileSystem.delete(entry.cleanFiles[t]);
          fileSystem.delete(entry.dirtyFiles[t]);
        }
        i.remove();
      }
    }
  }


  synchronized void rebuildJournal() throws IOException {
    if (journalWriter != null) {
      journalWriter.close();
    }

    BufferedSinkzaqds writer = Okiozaq.buffer(fileSystem.sink(journalFileTmp));
    try {
      writer.writeUtf8(MAGIC).writeByte('\n');
      writer.writeUtf8(VERSION_1).writeByte('\n');
      writer.writeDecimalLong(appVersion).writeByte('\n');
      writer.writeDecimalLong(valueCount).writeByte('\n');
      writer.writeByte('\n');

      for (Entry entry : lruEntries.values()) {
        if (entry.currentEditor != null) {
          writer.writeUtf8(DIRTY).writeByte(' ');
          writer.writeUtf8(entry.key);
          writer.writeByte('\n');
        } else {
          writer.writeUtf8(CLEAN).writeByte(' ');
          writer.writeUtf8(entry.key);
          entry.writeLengths(writer);
          writer.writeByte('\n');
        }
      }
    } finally {
      writer.close();
    }

    if (fileSystem.exists(journalFile)) {
      fileSystem.rename(journalFile, journalFileBackup);
    }
    fileSystem.rename(journalFileTmp, journalFile);
    fileSystem.delete(journalFileBackup);

    journalWriter = newJournalWriter();
    hasJournalErrors = false;
    mostRecentRebuildFailed = false;
  }


  public synchronized Snapshot get(String key) throws IOException {
    initialize();

    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (entry == null || !entry.readable) return null;

    Snapshot snapshot = entry.snapshot();
    if (snapshot == null) return null;

    redundantOpCount++;
    journalWriter.writeUtf8(READ).writeByte(' ').writeUtf8(key).writeByte('\n');
    if (journalRebuildRequired()) {
      executor.execute(cleanupRunnable);
    }

    return snapshot;
  }


  public @Nullableq
  Editor edit(String key) throws IOException {
    return edit(key, ANY_SEQUENCE_NUMBER);
  }

  synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
    initialize();

    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
        || entry.sequenceNumber != expectedSequenceNumber)) {
      return null; 
    }
    if (entry != null && entry.currentEditor != null) {
      return null; 
    }
    if (mostRecentTrimFailed || mostRecentRebuildFailed) {
      
      
      
      
      
      executor.execute(cleanupRunnable);
      return null;
    }

    
    journalWriter.writeUtf8(DIRTY).writeByte(' ').writeUtf8(key).writeByte('\n');
    journalWriter.flush();

    if (hasJournalErrors) {
      return null; 
    }

    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    }
    Editor editor = new Editor(entry);
    entry.currentEditor = editor;
    return editor;
  }


  public File getDirectory() {
    return directory;
  }


  public synchronized long getMaxSize() {
    return maxSize;
  }


  public synchronized void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
    if (initialized) {
      executor.execute(cleanupRunnable);
    }
  }


  public synchronized long size() throws IOException {
    initialize();
    return size;
  }

  synchronized void completeEdit(Editor editor, boolean success) throws IOException {
    Entry entry = editor.entry;
    if (entry.currentEditor != editor) {
      throw new IllegalStateException();
    }

    
    if (success && !entry.readable) {
      for (int i = 0; i < valueCount; i++) {
        if (!editor.written[i]) {
          editor.abort();
          throw new IllegalStateException("Newly created entry didn't create value for index " + i);
        }
        if (!fileSystem.exists(entry.dirtyFiles[i])) {
          editor.abort();
          return;
        }
      }
    }

    for (int i = 0; i < valueCount; i++) {
      File dirty = entry.dirtyFiles[i];
      if (success) {
        if (fileSystem.exists(dirty)) {
          File clean = entry.cleanFiles[i];
          fileSystem.rename(dirty, clean);
          long oldLength = entry.lengths[i];
          long newLength = fileSystem.size(clean);
          entry.lengths[i] = newLength;
          size = size - oldLength + newLength;
        }
      } else {
        fileSystem.delete(dirty);
      }
    }

    redundantOpCount++;
    entry.currentEditor = null;
    if (entry.readable | success) {
      entry.readable = true;
      journalWriter.writeUtf8(CLEAN).writeByte(' ');
      journalWriter.writeUtf8(entry.key);
      entry.writeLengths(journalWriter);
      journalWriter.writeByte('\n');
      if (success) {
        entry.sequenceNumber = nextSequenceNumber++;
      }
    } else {
      lruEntries.remove(entry.key);
      journalWriter.writeUtf8(REMOVE).writeByte(' ');
      journalWriter.writeUtf8(entry.key);
      journalWriter.writeByte('\n');
    }
    journalWriter.flush();

    if (size > maxSize || journalRebuildRequired()) {
      executor.execute(cleanupRunnable);
    }
  }


  boolean journalRebuildRequired() {
    final int redundantOpCompactThreshold = 2000;
    return redundantOpCount >= redundantOpCompactThreshold
        && redundantOpCount >= lruEntries.size();
  }


  public synchronized boolean remove(String key) throws IOException {
    initialize();

    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (entry == null) return false;
    boolean removed = removeEntry(entry);
    if (removed && size <= maxSize) mostRecentTrimFailed = false;
    return removed;
  }

  boolean removeEntry(Entry entry) throws IOException {
    if (entry.currentEditor != null) {
      entry.currentEditor.detach(); 
    }

    for (int i = 0; i < valueCount; i++) {
      fileSystem.delete(entry.cleanFiles[i]);
      size -= entry.lengths[i];
      entry.lengths[i] = 0;
    }

    redundantOpCount++;
    journalWriter.writeUtf8(REMOVE).writeByte(' ').writeUtf8(entry.key).writeByte('\n');
    lruEntries.remove(entry.key);

    if (journalRebuildRequired()) {
      executor.execute(cleanupRunnable);
    }

    return true;
  }


  public synchronized boolean isClosed() {
    return closed;
  }

  private synchronized void checkNotClosed() {
    if (isClosed()) {
      throw new IllegalStateException("cache is closed");
    }
  }


  @Override public synchronized void flush() throws IOException {
    if (!initialized) return;

    checkNotClosed();
    trimToSize();
    journalWriter.flush();
  }


  @Override public synchronized void close() throws IOException {
    if (!initialized || closed) {
      closed = true;
      return;
    }
    
    for (Entry entry : lruEntries.values().toArray(new Entry[lruEntries.size()])) {
      if (entry.currentEditor != null) {
        entry.currentEditor.abort();
      }
    }
    trimToSize();
    journalWriter.close();
    journalWriter = null;
    closed = true;
  }

  void trimToSize() throws IOException {
    while (size > maxSize) {
      Entry toEvict = lruEntries.values().iterator().next();
      removeEntry(toEvict);
    }
    mostRecentTrimFailed = false;
  }


  public void delete() throws IOException {
    close();
    fileSystem.deleteContents(directory);
  }


  public synchronized void evictAll() throws IOException {
    initialize();
    
    for (Entry entry : lruEntries.values().toArray(new Entry[lruEntries.size()])) {
      removeEntry(entry);
    }
    mostRecentTrimFailed = false;
  }

  private void validateKey(String key) {
    Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "keys must match regex [a-z0-9_-]{1,120}: \"" + key + "\"");
    }
  }


  public synchronized Iterator<Snapshot> snapshots() throws IOException {
    initialize();
    return new Iterator<Snapshot>() {

      final Iterator<Entry> delegate = new ArrayList<>(lruEntries.values()).iterator();


      Snapshot nextSnapshot;


      Snapshot removeSnapshot;

      @Override public boolean hasNext() {
        if (nextSnapshot != null) return true;

        synchronized (DiskLruCacheq.this) {
          
          if (closed) return false;

          while (delegate.hasNext()) {
            Entry entry = delegate.next();
            Snapshot snapshot = entry.snapshot();
            if (snapshot == null) continue; 
            nextSnapshot = snapshot;
            return true;
          }
        }

        return false;
      }

      @Override public Snapshot next() {
        if (!hasNext()) throw new NoSuchElementException();
        removeSnapshot = nextSnapshot;
        nextSnapshot = null;
        return removeSnapshot;
      }

      @Override public void remove() {
        if (removeSnapshot == null) throw new IllegalStateException("remove() before next()");
        try {
          DiskLruCacheq.this.remove(removeSnapshot.key);
        } catch (IOException ignored) {
          
          
        } finally {
          removeSnapshot = null;
        }
      }
    };
  }


  public final class Snapshot implements Closeable {
    private final String key;
    private final long sequenceNumber;
    private final Sourcezaq[] sources;
    private final long[] lengths;

    Snapshot(String key, long sequenceNumber, Sourcezaq[] sources, long[] lengths) {
      this.key = key;
      this.sequenceNumber = sequenceNumber;
      this.sources = sources;
      this.lengths = lengths;
    }

    public String key() {
      return key;
    }


    public @Nullableq
    Editor edit() throws IOException {
      return DiskLruCacheq.this.edit(key, sequenceNumber);
    }


    public Sourcezaq getSource(int index) {
      return sources[index];
    }


    public long getLength(int index) {
      return lengths[index];
    }

    public void close() {
      for (Sourcezaq in : sources) {
        Utilaq.closeQuietly(in);
      }
    }
  }


  public final class Editor {
    final Entry entry;
    final boolean[] written;
    private boolean done;

    Editor(Entry entry) {
      this.entry = entry;
      this.written = (entry.readable) ? null : new boolean[valueCount];
    }


    void detach() {
      if (entry.currentEditor == this) {
        for (int i = 0; i < valueCount; i++) {
          try {
            fileSystem.delete(entry.dirtyFiles[i]);
          } catch (IOException e) {
            
          }
        }
        entry.currentEditor = null;
      }
    }


    public Sourcezaq newSource(int index) {
      synchronized (DiskLruCacheq.this) {
        if (done) {
          throw new IllegalStateException();
        }
        if (!entry.readable || entry.currentEditor != this) {
          return null;
        }
        try {
          return fileSystem.source(entry.cleanFiles[index]);
        } catch (FileNotFoundException e) {
          return null;
        }
      }
    }


    public Sinkzaq newSink(int index) {
      synchronized (DiskLruCacheq.this) {
        if (done) {
          throw new IllegalStateException();
        }
        if (entry.currentEditor != this) {
          return Okiozaq.blackhole();
        }
        if (!entry.readable) {
          written[index] = true;
        }
        File dirtyFile = entry.dirtyFiles[index];
        Sinkzaq sink;
        try {
          sink = fileSystem.sink(dirtyFile);
        } catch (FileNotFoundException e) {
          return Okiozaq.blackhole();
        }
        return new FaultHidingSinkq(sink) {
          @Override protected void onException(IOException e) {
            synchronized (DiskLruCacheq.this) {
              detach();
            }
          }
        };
      }
    }


    public void commit() throws IOException {
      synchronized (DiskLruCacheq.this) {
        if (done) {
          throw new IllegalStateException();
        }
        if (entry.currentEditor == this) {
          completeEdit(this, true);
        }
        done = true;
      }
    }


    public void abort() throws IOException {
      synchronized (DiskLruCacheq.this) {
        if (done) {
          throw new IllegalStateException();
        }
        if (entry.currentEditor == this) {
          completeEdit(this, false);
        }
        done = true;
      }
    }

    public void abortUnlessCommitted() {
      synchronized (DiskLruCacheq.this) {
        if (!done && entry.currentEditor == this) {
          try {
            completeEdit(this, false);
          } catch (IOException ignored) {
          }
        }
      }
    }
  }

  private final class Entry {
    final String key;


    final long[] lengths;
    final File[] cleanFiles;
    final File[] dirtyFiles;


    boolean readable;


    Editor currentEditor;


    long sequenceNumber;

    Entry(String key) {
      this.key = key;

      lengths = new long[valueCount];
      cleanFiles = new File[valueCount];
      dirtyFiles = new File[valueCount];

      
      StringBuilder fileBuilder = new StringBuilder(key).append('.');
      int truncateTo = fileBuilder.length();
      for (int i = 0; i < valueCount; i++) {
        fileBuilder.append(i);
        cleanFiles[i] = new File(directory, fileBuilder.toString());
        fileBuilder.append(".tmp");
        dirtyFiles[i] = new File(directory, fileBuilder.toString());
        fileBuilder.setLength(truncateTo);
      }
    }


    void setLengths(String[] strings) throws IOException {
      if (strings.length != valueCount) {
        throw invalidLengths(strings);
      }

      try {
        for (int i = 0; i < strings.length; i++) {
          lengths[i] = Long.parseLong(strings[i]);
        }
      } catch (NumberFormatException e) {
        throw invalidLengths(strings);
      }
    }


    void writeLengths(BufferedSinkzaqds writer) throws IOException {
      for (long length : lengths) {
        writer.writeByte(' ').writeDecimalLong(length);
      }
    }

    private IOException invalidLengths(String[] strings) throws IOException {
      throw new IOException("unexpected journal line: " + Arrays.toString(strings));
    }


    Snapshot snapshot() {
      if (!Thread.holdsLock(DiskLruCacheq.this)) throw new AssertionError();

      Sourcezaq[] sources = new Sourcezaq[valueCount];
      long[] lengths = this.lengths.clone(); 
      try {
        for (int i = 0; i < valueCount; i++) {
          sources[i] = fileSystem.source(cleanFiles[i]);
        }
        return new Snapshot(key, sequenceNumber, sources, lengths);
      } catch (FileNotFoundException e) {
        
        for (int i = 0; i < valueCount; i++) {
          if (sources[i] != null) {
            Utilaq.closeQuietly(sources[i]);
          } else {
            break;
          }
        }
        
        
        try {
          removeEntry(this);
        } catch (IOException ignored) {
        }
        return null;
      }
    }
  }
}
