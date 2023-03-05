package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.NotNull;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public final class ZipUtil {
    private ZipUtil() {
        throw new UnsupportedOperationException();
    }

    public static void decompress(@NotNull Path compressedFilePath, Path decompressDirPath) throws IOException {
        var pathStr = compressedFilePath.toString();
        if (pathStr.endsWith(".zip") || pathStr.endsWith(".jar")) {
            decompressZip(compressedFilePath, decompressDirPath);
        } else if (pathStr.endsWith(".tar.gz")) {
            decompressTarGz(compressedFilePath, decompressDirPath);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + compressedFilePath);
        }
    }

    public static void decompressTarGz(Path compressedFilePath, Path decompressDirPath) throws IOException {
        if (!Files.exists(compressedFilePath)) {
            throw new IllegalArgumentException("The compressed file does not exist");
        }
        if (!Files.exists(decompressDirPath)) {
            Files.createDirectories(decompressDirPath);
        }
        if (!Files.isDirectory(decompressDirPath)) {
            throw new IllegalArgumentException("The decompress directory does not exist");
        }
        try (var tis = new TarInputStream(new BufferedInputStream(new GZIPInputStream(
                Files.newInputStream(compressedFilePath))))) {
            BufferedOutputStream dest;
            TarEntry entry;
            byte[] data = new byte[4096];
            while ((entry = tis.getNextEntry()) != null) {
                int count;
                if (entry.isDirectory()) {
                    Files.createDirectories(decompressDirPath.resolve(entry.getName()));
                    continue;
                } else {
                    int di = entry.getName().lastIndexOf('/');
                    if (di != -1) {
                        Files.createDirectories(decompressDirPath.resolve(entry.getName().substring(0, di)));
                    }
                }
                var fos = Files.newOutputStream(decompressDirPath.resolve(entry.getName()));
                dest = new BufferedOutputStream(fos);
                while ((count = tis.read(data)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
    }

    public static void decompressZip(Path compressedFilePath, Path decompressDirPath) throws IOException {
        if (!Files.exists(compressedFilePath)) {
            throw new IllegalArgumentException("The compressed file does not exist");
        }
        if (!Files.exists(decompressDirPath)) {
            Files.createDirectories(decompressDirPath);
        }
        if (!Files.isDirectory(decompressDirPath)) {
            throw new IllegalArgumentException("The decompress directory does not exist");
        }
        try (var zis = new java.util.zip.ZipInputStream(new BufferedInputStream(Files.newInputStream(compressedFilePath)))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                if (entry.isDirectory()) {
                    Files.createDirectories(decompressDirPath.resolve(entry.getName()));
                    continue;
                } else {
                    int di = entry.getName().lastIndexOf('/');
                    if (di != -1) {
                        Files.createDirectories(decompressDirPath.resolve(entry.getName().substring(0, di)));
                    }
                }
                var fos = Files.newOutputStream(decompressDirPath.resolve(entry.getName()));
                var dest = new BufferedOutputStream(fos);
                byte[] data = new byte[4096];
                while ((count = zis.read(data)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
    }
}
