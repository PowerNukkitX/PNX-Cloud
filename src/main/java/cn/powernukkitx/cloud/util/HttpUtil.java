package cn.powernukkitx.cloud.util;

import okhttp3.OkHttpClient;

public final class HttpUtil {
    private HttpUtil() {
        throw new UnsupportedOperationException();
    }

    public static final OkHttpClient client = new OkHttpClient.Builder().build();
}
