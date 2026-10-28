package com.cdy.superweather.api;

import com.cdy.superweather.bean.LifeStyleResponse;
import com.cdy.superweather.bean.TodayResponse;
import com.cdy.superweather.bean.WeatherForecastResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * API服务接口
 */
public interface ApiService {

    /**
     * 当天天气查询
     * https://free-api.heweather.net/s6/weather/now?key=REPLACE_WITH_API_KEY&location=深圳
     *   将地址进一步拆分，将可变的一部分放在注解@GET的地址里面，其中
     *   /s6/weather/now?key=REPLACE_WITH_API_KEY 这一部分在这个接口中又是不变的，变的是location的值
     *   所以将location的参数放入@Query里面，因为是使用的GET请求，所以里面的内容会拼接到地址后面，并且自动会加上 & 符号
     *   Call是retrofit2框架里面的，这个框架是对OKHttp的进一步封装，会让你的使用更加简洁明了，里面放入之前通过接口返回
     *   的JSON字符串生成返回数据实体Bean,Retrofit支持Gson解析实体类,所以，后面的返回值就不用做解析了。
     *   getTodayWeather是这个接口的方法名。这样说应该很清楚了吧
     * @param location  区/县
     * @return
     */
    // 和风天气 V7 接口（S6 已停服）。城市名/经纬度由 QWeatherV7Interceptor 自动解析为城市 ID。
    @GET("/v7/weather/now?key=REPLACE_WITH_API_KEY")
    Call<TodayResponse> getTodayWeather(@Query("location") String location);

    /**
     * 未来多日天气预报（V7 /v7/weather/7d，实际可返回天数由订阅决定）
     */
    @GET("/v7/weather/7d?key=REPLACE_WITH_API_KEY")
    Call<WeatherForecastResponse> getWeatherForecast(@Query("location") String location);

    /**
     * 生活指数（V7 /v7/indices/1d，type 依次：
     * 8 舒适度 3 穿衣 9 感冒 1 运动 6 旅游 2 洗车 10 空气污染扩散）
     */
    @GET("/v7/indices/1d?key=REPLACE_WITH_API_KEY&type=8,3,9,1,6,2,10")
    Call<LifeStyleResponse> getLifestyle(@Query("location") String location);

}


