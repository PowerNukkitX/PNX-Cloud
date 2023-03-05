package cn.powernukkitx.cloud.util;

import java.util.function.Function;

public final class NullUtil {
    private NullUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T, R> R tryDo(T obj, Function<T, R> f, R defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        return f.apply(obj);
    }
}
