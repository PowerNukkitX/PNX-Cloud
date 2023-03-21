package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ArrayUtil {
    private ArrayUtil() {
    }

    public static <T> boolean contains(@Nullable T @NotNull [] array, @Nullable T value) {
        if (value == null) {
            for (T t : array) {
                if (t == null) {
                    return true;
                }
            }
        } else {
            for (T t : array) {
                if (value.equals(t)) {
                    return true;
                }
            }
        }
        return false;
    }
}
