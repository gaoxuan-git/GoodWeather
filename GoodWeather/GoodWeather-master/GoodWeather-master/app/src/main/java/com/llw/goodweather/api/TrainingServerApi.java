package com.llw.goodweather.api;

import com.llw.goodweather.bean.ServerCitiesResponse;
import com.llw.goodweather.bean.ServerHealthResponse;
import com.llw.goodweather.bean.ServerInfoResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * 实训 Python 服务端接口
 */
public interface TrainingServerApi {

    @GET("api/health")
    Call<ServerHealthResponse> health();

    @GET("api/info")
    Call<ServerInfoResponse> info();

    @GET("api/cities")
    Call<ServerCitiesResponse> cities();
}
