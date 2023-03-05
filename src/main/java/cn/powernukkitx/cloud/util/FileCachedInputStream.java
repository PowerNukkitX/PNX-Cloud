package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileCachedInputStream extends BufferedInputStream {
    private static final Set<@NotNull String> cachingFiles = ConcurrentHashMap.newKeySet();
    private final File file;
    private final RandomAccessFile cacheFile;
    private final int expectSize;

    private boolean close = false;

    public static boolean isCaching(@NotNull String path) {
        return cachingFiles.contains(path);
    }

    public static boolean isCaching(@NotNull File file) {
        return cachingFiles.contains(file.getAbsolutePath());
    }

    public FileCachedInputStream(@NotNull InputStream in, @NotNull File file, int expectSize) throws FileNotFoundException {
        super(in);
        this.file = file;
        this.cacheFile = new RandomAccessFile(checkFile(file), "rw");
        this.expectSize = expectSize;
        cachingFiles.add(file.getAbsolutePath());
    }

    public FileCachedInputStream(@NotNull InputStream in, int size, @NotNull File file, int expectSize) throws FileNotFoundException {
        super(in, size);
        this.file = file;
        this.cacheFile = new RandomAccessFile(checkFile(file), "rw");
        this.expectSize = expectSize;
        cachingFiles.add(file.getAbsolutePath());
    }

    @Contract("_ -> param1")
    private static @NotNull File checkFile(@NotNull File file) {
        if (file.exists()) {
            if (!file.isFile()) {
                throw new IllegalArgumentException("File is not a file");
            }
            if (!file.canWrite()) {
                throw new IllegalArgumentException("File is not writable");
            }
        } else {
            try {
                if (!file.getParentFile().exists()) {
                    if (!file.getParentFile().mkdirs()) {
                        throw new IOException("Failed to create parent directories");
                    }
                }
                if (!file.createNewFile()) {
                    throw new IllegalArgumentException("File is not writable");
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("File is not writable");
            }
        }
        return file;
    }

    @Override
    public synchronized int read() throws IOException {
        var tmp = super.read();
        this.cacheFile.write(tmp);
        return tmp;
    }

    @Override
    public synchronized int read(byte @NotNull [] b, int off, int len) throws IOException {
        var tmp = super.read(b, off, len);
        this.cacheFile.write(b, off, len);
        return tmp;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        var tmp = super.skip(n);
        this.cacheFile.seek(this.cacheFile.getFilePointer() + n);
        return tmp;
    }

    @Override
    public synchronized int available() throws IOException {
        return super.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (!close) {
                cachingFiles.remove(file.getAbsolutePath());
                if (cacheFile.length() >= expectSize) {
                    cacheFile.close();
                } else {
                    cacheFile.close();
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
                close = true;
            }
        }
    }
}
