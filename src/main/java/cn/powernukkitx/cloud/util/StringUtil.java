package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public final class StringUtil {
    private StringUtil() {

    }

    @Contract(pure = true)
    public static @NotNull String tryWarpPath(@NotNull String path) {
        if (path.contains(" ")) {
            return "\"" + path + "\"";
        }
        return path;
    }

    @Contract(pure = true)
    public static @NotNull String encodeFileName(@NotNull String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }

    @Contract(pure = true)
    public static @NotNull String afterLast(@NotNull String str, String splitter) {
        int index = str.lastIndexOf(splitter);
        if (index == -1) {
            return str;
        }
        return str.substring(index + splitter.length());
    }

    @Contract(pure = true)
    public static @NotNull String beforeLast(@NotNull String str, String splitter) {
        int index = str.lastIndexOf(splitter);
        if (index == -1) {
            return str;
        }
        return str.substring(0, index);
    }

    @Contract(pure = true)
    public static @NotNull String beforeFirst(@NotNull String str, String splitter) {
        int index = str.indexOf(splitter);
        if (index == -1) {
            return str;
        }
        return str.substring(0, index);
    }

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    @Contract(pure = true)
    public static String[] toStringArray(@Nullable Object obj) {
        if (obj == null) return EMPTY_STRING_ARRAY;
        if (obj instanceof String[]) return (String[]) obj;
        if (obj instanceof String) return new String[]{(String) obj};
        if (obj instanceof Object[] array) {
            String[] result = new String[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i].toString();
            }
            return result;
        }
        if (obj instanceof Collection<?> collection) {
            String[] result = new String[collection.size()];
            int i = 0;
            for (Object o : collection) {
                result[i++] = o.toString();
            }
            return result;
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass().getName() + " to String[]");
    }
}
