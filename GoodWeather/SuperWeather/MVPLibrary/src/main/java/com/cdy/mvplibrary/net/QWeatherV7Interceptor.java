package com.cdy.mvplibrary.net;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 和风天气 V7 兼容拦截器。
 *
 * 老教程工程 SuperWeather 原本对接的是已停服的和风 S6 接口
 * （https://free-api.heweather.net/s6/...）。为了在不改动
 * Activity / Contract / Bean / Adapter 的前提下恢复真实天气数据，
 * 本拦截器在网络层做两件事：
 * 1. 把 App 传入的中文区/县名（或经纬度）通过 GeoAPI 解析成 V7 所需的城市 ID；
 * 2. 把 V7 返回的 JSON 重新组装成 S6 的 {"HeWeather6":[...]} 结构，
 *    使既有 Gson Bean 可以照常解析。
 */
public class QWeatherV7Interceptor implements Interceptor {

    // 与 GoodWeather-master 共用的有效开发者专属 Host / Key
    private static final String HOST = "nb4y3nkkjb.re.qweatherapi.com";
    private static final String DEFAULT_KEY = "REPLACE_WITH_API_KEY";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // V7 生活指数数字类型 -> S6 字符串类型
    // 1 运动 2 洗车 3 穿衣 6 旅游 8 舒适度 9 感冒 10 空气污染扩散
    private static String mapIndexType(String v7Type) {
        if (v7Type == null) return "";
        switch (v7Type) {
            case "1": return "sport";
            case "2": return "cw";
            case "3": return "drsg";
            case "6": return "trav";
            case "8": return "comf";
            case "9": return "flu";
            case "10": return "air";
            default: return "";
        }
    }

    // GeoAPI 使用独立 Client，不会再次经过本拦截器
    private final OkHttpClient geoClient = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    private static final class Resolved {
        String id;
        String name;
        String adm2;
        String adm1;
        String country;
        String lat;
        String lon;

        Resolved(String id, String name, String adm2, String adm1,
                 String country, String lat, String lon) {
            this.id = id;
            this.name = name;
            this.adm2 = adm2;
            this.adm1 = adm1;
            this.country = country;
            this.lat = lat;
            this.lon = lon;
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url();

        // 只处理和风 V7 天气请求，其它请求原样放行
        if (!HOST.equals(url.host()) || !url.encodedPath().startsWith("/v7/")) {
            return chain.proceed(request);
        }

        String rawLocation = url.queryParameter("location");
        String key = url.queryParameter("key");
        if (key == null || key.isEmpty()) {
            key = DEFAULT_KEY;
        }

        // 1. 解析城市 ID
        Resolved resolved;
        try {
            resolved = resolveLocation(rawLocation, key);
        } catch (Exception e) {
            return s6Response(request, "城市定位解析失败");
        }
        if (resolved == null) {
            return s6Response(request, "未找到该城市");
        }

        // 2. 用城市 ID 发起真正的 V7 请求
        HttpUrl realUrl = url.newBuilder()
                .setQueryParameter("location", resolved.id)
                .build();
        Response response = chain.proceed(request.newBuilder().url(realUrl).build());
        String body = response.body() != null ? response.body().string() : "";

        // 3. 转换为 S6 结构
        String s6;
        try {
            s6 = convert(url.encodedPath(), body, resolved, rawLocation);
        } catch (Exception e) {
            return s6Response(request, "天气数据解析失败");
        }

        return response.newBuilder()
                .code(200)
                .message("OK")
                .body(ResponseBody.create(JSON, s6))
                .build();
    }

    /**
     * 中文城市名 / 经纬度 -> V7 城市 ID；纯数字 ID 直接使用。
     */
    private Resolved resolveLocation(String location, String key) throws IOException {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        location = location.trim();

        // 已经是城市 ID（如 101010100）则无需再查
        if (location.matches("\\d{6,}")) {
            return new Resolved(location, location, "", "", "", "", "");
        }

        HttpUrl geoUrl = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegments("geo/v2/city/lookup")
                .addQueryParameter("location", location)
                .addQueryParameter("key", key)
                .build();

        Request geoRequest = new Request.Builder().url(geoUrl).get().build();
        try (Response geoResponse = geoClient.newCall(geoRequest).execute()) {
            String geoBody = geoResponse.body() != null ? geoResponse.body().string() : "";
            JSONObject json = new JSONObject(geoBody);
            if (!"200".equals(json.optString("code"))) {
                return null;
            }
            JSONArray arr = json.optJSONArray("location");
            if (arr == null || arr.length() == 0) {
                return null;
            }
            // 优先选择国内城市，否则取第一个结果
            JSONObject pick = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if ("中国".equals(o.optString("country"))) {
                    pick = o;
                    break;
                }
            }
            if (pick == null) {
                pick = arr.getJSONObject(0);
            }
            return new Resolved(
                    pick.optString("id"),
                    pick.optString("name"),
                    pick.optString("adm2"),
                    pick.optString("adm1"),
                    pick.optString("country"),
                    pick.optString("lat"),
                    pick.optString("lon"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * V7 JSON -> S6 JSON。
     */
    private String convert(String path, String body, Resolved rc, String rawLocation)
            throws Exception {
        JSONObject v7 = new JSONObject(body);

        // V7 鉴权 / 参数错误时返回 {"error":{...}}，没有 code 字段
        String code = v7.optString("code", "");
        String status;
        if (code.isEmpty()) {
            JSONObject err = v7.optJSONObject("error");
            status = err != null ? ("请求失败 " + err.optInt("status", 0)) : "请求失败";
        } else {
            status = "200".equals(code) ? "ok" : ("错误码 " + code);
        }

        JSONObject item = new JSONObject();
        item.put("basic", buildBasic(rc, rawLocation));
        item.put("status", status);

        if (path.contains("/weather/now")) {
            item.put("update", buildUpdate(v7.optString("updateTime")));
            JSONObject now = v7.optJSONObject("now");
            if (now != null) {
                // now.obsTime 作为观测更新时间更精确
                item.put("update", buildUpdate(now.optString("obsTime",
                        v7.optString("updateTime"))));
                JSONObject n = new JSONObject();
                n.put("cloud", now.optString("cloud"));
                n.put("cond_code", now.optString("icon"));
                n.put("cond_txt", now.optString("text"));
                n.put("fl", now.optString("feelsLike"));
                n.put("hum", now.optString("humidity"));
                n.put("pcpn", now.optString("precip"));
                n.put("pres", now.optString("pressure"));
                n.put("tmp", now.optString("temp"));
                n.put("vis", now.optString("vis"));
                n.put("wind_deg", now.optString("wind360"));
                n.put("wind_dir", now.optString("windDir"));
                n.put("wind_sc", now.optString("windScale"));
                n.put("wind_spd", now.optString("windSpeed"));
                item.put("now", n);
            }
        } else if (path.contains("/weather/")) {
            // 多日预报：/v7/weather/7d（或 3d）
            item.put("update", buildUpdate(v7.optString("updateTime")));
            JSONArray daily = v7.optJSONArray("daily");
            JSONArray forecast = new JSONArray();
            if (daily != null) {
                for (int i = 0; i < daily.length(); i++) {
                    JSONObject d = daily.getJSONObject(i);
                    JSONObject f = new JSONObject();
                    f.put("cond_code_d", d.optString("iconDay"));
                    f.put("cond_code_n", d.optString("iconNight"));
                    f.put("cond_txt_d", d.optString("textDay"));
                    f.put("cond_txt_n", d.optString("textNight"));
                    f.put("date", d.optString("fxDate"));
                    f.put("hum", d.optString("humidity"));
                    f.put("mr", d.optString("moonrise"));
                    f.put("ms", d.optString("moonset"));
                    f.put("pcpn", d.optString("precip"));
                    f.put("pop", d.optString("pop"));
                    f.put("pres", d.optString("pressure"));
                    f.put("sr", d.optString("sunrise"));
                    f.put("ss", d.optString("sunset"));
                    f.put("tmp_max", d.optString("tempMax"));
                    f.put("tmp_min", d.optString("tempMin"));
                    f.put("uv_index", d.optString("uvIndex"));
                    f.put("vis", d.optString("vis"));
                    f.put("wind_deg", d.optString("wind360Day"));
                    f.put("wind_dir", d.optString("windDirDay"));
                    f.put("wind_sc", d.optString("windScaleDay"));
                    f.put("wind_spd", d.optString("windSpeedDay"));
                    forecast.put(f);
                }
            }
            item.put("daily_forecast", forecast);
        } else if (path.contains("/indices/")) {
            item.put("update", buildUpdate(v7.optString("updateTime")));
            JSONArray daily = v7.optJSONArray("daily");
            JSONArray lifestyle = new JSONArray();
            if (daily != null) {
                for (int i = 0; i < daily.length(); i++) {
                    JSONObject d = daily.getJSONObject(i);
                    String s6Type = mapIndexType(d.optString("type"));
                    if (s6Type.isEmpty()) {
                        continue;
                    }
                    JSONObject l = new JSONObject();
                    l.put("type", s6Type);
                    l.put("brf", d.optString("category"));
                    l.put("txt", d.optString("text"));
                    lifestyle.put(l);
                }
            }
            item.put("lifestyle", lifestyle);
        }

        JSONArray heWeather6 = new JSONArray();
        heWeather6.put(item);
        JSONObject root = new JSONObject();
        root.put("HeWeather6", heWeather6);
        return root.toString();
    }

    private JSONObject buildBasic(Resolved rc, String rawLocation) throws Exception {
        JSONObject basic = new JSONObject();
        basic.put("cid", rc.id);
        // 城市名优先用 GeoAPI 解析出的名称
        basic.put("location",
                (rc.name != null && !rc.name.isEmpty()) ? rc.name : rawLocation);
        basic.put("parent_city", rc.adm2);
        basic.put("admin_area", rc.adm1);
        basic.put("cnty", rc.country);
        basic.put("lat", rc.lat);
        basic.put("lon", rc.lon);
        basic.put("tz", "+8.00");
        return basic;
    }

    private JSONObject buildUpdate(String isoTime) throws Exception {
        JSONObject update = new JSONObject();
        String loc = formatIsoTime(isoTime);
        update.put("loc", loc);
        update.put("utc", loc);
        return update;
    }

    /**
     * "2026-10-15T20:05+08:00" -> "2026-10-15 20:05"
     */
    private String formatIsoTime(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "";
        }
        try {
            int t = iso.indexOf('T');
            if (t < 0) {
                return iso;
            }
            String date = iso.substring(0, Math.min(10, t));
            String time = iso.substring(t + 1);
            int cut = 0;
            while (cut < time.length()
                    && (Character.isDigit(time.charAt(cut)) || time.charAt(cut) == ':')) {
                cut++;
            }
            time = time.substring(0, Math.min(cut, 5));
            return date + " " + time;
        } catch (Exception e) {
            return iso;
        }
    }

    /**
     * 构造一个 S6 错误结构（HTTP 仍返回 200，保证回调进入成功分支，
     * 由页面根据 status 字段 Toast 提示）。
     */
    private Response s6Response(Request request, String status) {
        String json;
        try {
            JSONObject item = new JSONObject();
            item.put("status", status);
            JSONArray heWeather6 = new JSONArray();
            heWeather6.put(item);
            JSONObject root = new JSONObject();
            root.put("HeWeather6", heWeather6);
            json = root.toString();
        } catch (Exception e) {
            json = "{\"HeWeather6\":[{\"status\":\"网络异常\"}]}";
        }
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(JSON, json))
                .build();
    }
}
