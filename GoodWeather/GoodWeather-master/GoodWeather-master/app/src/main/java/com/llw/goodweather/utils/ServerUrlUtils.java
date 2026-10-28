package com.llw.goodweather.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 实训服务端地址校验与规范化。
 */
public final class ServerUrlUtils {

    private ServerUrlUtils() {
    }

    public static String normalize(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String value = rawUrl.trim();
        if (value.isEmpty()) {
            return null;
        }

        boolean isHttp = value.regionMatches(true, 0, "http://", 0, 7);
        boolean isHttps = value.regionMatches(true, 0, "https://", 0, 8);
        if (!isHttp && !isHttps) {
            if (value.contains("://")) {
                return null;
            }
            value = "http://" + value;
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme))) {
                return null;
            }
        } catch (URISyntaxException ignored) {
            return null;
        }
        return value.endsWith("/") ? value : value + "/";
    }
}
