package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileUtil {
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String getMD5(@NotNull File file) throws IOException {
        try (var bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
            var MD5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[4096];
            int length;
            while ((length = bufferedInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return bytesToHex(MD5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true, value = "_ -> new")
    public static @NotNull String bytesToHex(byte @NotNull [] bytes) {
        var result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
