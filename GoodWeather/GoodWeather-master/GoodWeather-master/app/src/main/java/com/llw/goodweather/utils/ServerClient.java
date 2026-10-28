package com.llw.goodweather.utils;

import android.content.Context;
import android.os.Build;

import com.llw.goodweather.WeatherApplication;
import com.llw.goodweather.api.TrainingServerApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 连接实训 Flask 服务端
 */
public final class ServerClient {

    private static TrainingServerApi api;
    private static String activeBaseUrl;

    private ServerClient() {
    }

    public static String currentBaseUrl() {
        return resolveTrainingServerBaseUrl();
    }

    public static String configuredBaseUrl(Context context) {
        String saved = SPUtils.getString(
                Constant.SERVER_BASE_URL_CUSTOM, "", context);
        String normalized = ServerUrlUtils.normalize(saved);
        return normalized == null ? Constant.SERVER_BASE_URL_LAN : normalized;
    }

    public static void saveConfiguredBaseUrl(Context context, String baseUrl) {
        String normalized = ServerUrlUtils.normalize(baseUrl);
        if (normalized == null) {
            throw new IllegalArgumentException("Invalid server base URL");
        }
        SPUtils.putString(Constant.SERVER_BASE_URL_CUSTOM, normalized, context);
        useBaseUrl(normalized);
    }

    public static List<String> candidateBaseUrls(Context context) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        urls.add(configuredBaseUrl(context));
        if (isEmulator()) {
            // adb reverse tcp:5000 tcp:5000 后最稳定
            urls.add(Constant.SERVER_BASE_URL_EMULATOR_LOCALHOST);
            urls.add(Constant.SERVER_BASE_URL_EMULATOR);
        }
        urls.add(Constant.SERVER_BASE_URL_LAN);
        return new ArrayList<>(urls);
    }

    /** 依次探测，返回第一个能访问 /api/health 的 baseUrl */
    public static String probeFirstReachable(Context context) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build();
        for (String base : candidateBaseUrls(context)) {
            String normalized = ServerUrlUtils.normalize(base);
            if (normalized == null) {
                continue;
            }
            try {
                Request request = new Request.Builder()
                        .url(normalized + "api/health")
                        .get()
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return normalized;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public static void useBaseUrl(String baseUrl) {
        String normalized = ServerUrlUtils.normalize(baseUrl);
        if (normalized == null) {
            return;
        }
        if (!normalized.equals(activeBaseUrl)) {
            activeBaseUrl = normalized;
            reset();
        }
    }

    public static String resolveTrainingServerBaseUrl() {
        if (activeBaseUrl != null) {
            return activeBaseUrl;
        }
        Context context = WeatherApplication.getMyContext();
        return context == null
                ? Constant.SERVER_BASE_URL_LAN
                : configuredBaseUrl(context);
    }

    static boolean isEmulator() {
        String fingerprint = Build.FINGERPRINT != null ? Build.FINGERPRINT : "";
        String model = Build.MODEL != null ? Build.MODEL : "";
        String product = Build.PRODUCT != null ? Build.PRODUCT : "";
        return fingerprint.startsWith("generic")
                || fingerprint.contains("emulator")
                || fingerprint.contains("sdk_gphone")
                || model.contains("sdk_gphone")
                || model.contains("Emulator")
                || product.contains("sdk_gphone");
    }

    public static TrainingServerApi getApi() {
        if (api == null) {
            api = buildApi(resolveTrainingServerBaseUrl());
        }
        return api;
    }

    public static void reset() {
        api = null;
    }

    private static TrainingServerApi buildApi(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit.create(TrainingServerApi.class);
    }
}
