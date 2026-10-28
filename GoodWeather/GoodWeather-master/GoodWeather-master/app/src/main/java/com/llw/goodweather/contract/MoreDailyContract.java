package com.llw.goodweather.contract;

import com.llw.goodweather.api.ApiService;
import com.llw.goodweather.bean.DailyResponse;
import com.llw.goodweather.bean.HourlyResponse;
import com.llw.goodweather.bean.NowResponse;
import com.llw.goodweather.bean.WorldCityResponse;
import com.llw.mvplibrary.base.BasePresenter;
import com.llw.mvplibrary.base.BaseView;
import com.llw.mvplibrary.net.NetCallBack;
import com.llw.mvplibrary.net.ServiceGenerator;

import retrofit2.Call;
import retrofit2.Response;

/**
 * 更多天气预报订阅器
 *
 * @author llw
 */
public class MoreDailyContract {

    public static class MoreDailyPresenter extends BasePresenter<IMoreDailyView> {

        /**
         * 更多天气预报  V7
         *
         * @param location 城市id
         */
        public void worldCity(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.dailyWeather("15d", location).enqueue(new NetCallBack<DailyResponse>() {
                @Override
                public void onSuccess(Call<DailyResponse> call, Response<DailyResponse> response) {
                    if (getView() != null) {
                        getView().getMoreDailyResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    if (getView() != null) {
                        getView().getDataFailed();
                    }
                }
            });
        }

        /**
         * 实况天气（用于详情页顶部概要卡片在未携带 Intent 数据时的兜底）V7
         *
         * @param location 城市id
         */
        public void nowWeather(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.nowWeather(location).enqueue(new NetCallBack<NowResponse>() {
                @Override
                public void onSuccess(Call<NowResponse> call, Response<NowResponse> response) {
                    if (getView() != null) {
                        getView().getMoreNowResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    //概要兜底请求失败时保持占位文案，不打断主列表
                }
            });
        }

        /**
         * 未来24小时逐小时天气 V7
         *
         * @param location 城市id
         */
        public void hourlyWeather(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.hourlyWeather(location).enqueue(new NetCallBack<HourlyResponse>() {
                @Override
                public void onSuccess(Call<HourlyResponse> call,
                                      Response<HourlyResponse> response) {
                    if (getView() != null) {
                        getView().getMoreHourlyResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    if (getView() != null) {
                        getView().getHourlyDataFailed();
                    }
                }
            });
        }

    }

    public interface IMoreDailyView extends BaseView {

        //更多天气预报返回数据 V7
        void getMoreDailyResult(Response<DailyResponse> response);

        //实况天气返回（详情页概要兜底）V7
        void getMoreNowResult(Response<NowResponse> response);

        //24小时逐小时天气返回 V7
        void getMoreHourlyResult(Response<HourlyResponse> response);

        //24小时天气错误返回，不影响15天预报
        void getHourlyDataFailed();

        //错误返回
        void getDataFailed();
    }
}
