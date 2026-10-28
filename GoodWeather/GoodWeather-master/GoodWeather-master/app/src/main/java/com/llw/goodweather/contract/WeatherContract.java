package com.llw.goodweather.contract;

import android.content.Context;

import com.llw.goodweather.api.ApiService;
import com.llw.goodweather.bean.AirQualityCurrentResponse;
import com.llw.goodweather.bean.BiYingImgResponse;
import com.llw.goodweather.bean.DailyResponse;
import com.llw.goodweather.bean.HourlyResponse;
import com.llw.goodweather.bean.LifestyleResponse;
import com.llw.goodweather.bean.MinutePrecResponse;
import com.llw.goodweather.bean.NewSearchCityResponse;
import com.llw.goodweather.bean.NowResponse;
import com.llw.goodweather.bean.WarningResponse;
import com.llw.goodweather.utils.AirQualityMapper;
import com.llw.mvplibrary.base.BasePresenter;
import com.llw.mvplibrary.base.BaseView;
import com.llw.mvplibrary.net.NetCallBack;
import com.llw.mvplibrary.net.ServiceGenerator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 天气订阅器
 *
 * @author llw
 */
public class WeatherContract {

    public static class WeatherPresenter extends BasePresenter<IWeatherView> {

        /***************** 以下为使用V7版本而新增的接口方法，后期将都会使用这些方法，请注意 ****************/

        /**
         * 搜索城市  V7版本中  需要把定位城市的id查询出来，然后通过这个id来查询详细的数据
         *
         * @param location 城市名
         */
        public void newSearchCity(String location) {//注意这里的4表示新的搜索城市地址接口
            ApiService service = ServiceGenerator.createService(ApiService.class, 4);//指明访问的地址
            service.newSearchCity(location, "exact").enqueue(newSearchCityCallback());
        }

        /** 经纬度或海外地点（不限 range=cn） */
        public void newSearchCityGlobal(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 4);
            service.lookupLocationGlobal(location).enqueue(newSearchCityCallback());
        }

        private NetCallBack<NewSearchCityResponse> newSearchCityCallback() {
            return new NetCallBack<NewSearchCityResponse>() {
                @Override
                public void onSuccess(Call<NewSearchCityResponse> call, Response<NewSearchCityResponse> response) {
                    if (getView() != null) {
                        getView().getNewSearchCityResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    if (getView() != null) {
                        getView().getDataFailed();
                    }
                }
            };
        }


        /**
         * 实况天气  V7版本
         *
         * @param location 城市名
         */
        public void nowWeather(String location) {//这个3 表示使用新的V7API访问地址
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.nowWeather(location).enqueue(new NetCallBack<NowResponse>() {
                @Override
                public void onSuccess(Call<NowResponse> call, Response<NowResponse> response) {
                    if (getView() != null) {
                        getView().getNowResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    if (getView() != null) {
                        getView().getWeatherDataFailed();
                    }
                }
            });
        }

        /**
         * 天气预报  V7版本   7d 表示天气的数据 为了和之前看上去差别小一些，这里先用七天的
         *
         * @param location 城市名
         */
        public void dailyWeather(String location) {//这个3 表示使用新的V7API访问地址
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.dailyWeather("7d", location).enqueue(new NetCallBack<DailyResponse>() {
                @Override
                public void onSuccess(Call<DailyResponse> call, Response<DailyResponse> response) {
                    if (getView() != null) {
                        getView().getDailyResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    if (getView() != null) {
                        getView().getWeatherDataFailed();
                    }
                }
            });
        }

        /**
         * 逐小时预报（未来24小时）
         *
         * @param location 城市名
         */
        public void hourlyWeather(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.hourlyWeather(location).enqueue(new NetCallBack<HourlyResponse>() {
                @Override
                public void onSuccess(Call<HourlyResponse> call, Response<HourlyResponse> response) {
                    if (getView() != null) {
                        getView().getHourlyResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    if (getView() != null) {
                        getView().getWeatherDataFailed();
                    }
                }
            });
        }

        /**
         * 当前空气质量 V1。
         */
        public void airNowWeather(String latitude, String longitude) {
            String formattedLat = AirQualityMapper.formatCoordinate(latitude);
            String formattedLon = AirQualityMapper.formatCoordinate(longitude);
            if (formattedLat == null || formattedLon == null) {
                return;
            }
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.airQualityCurrent(formattedLat, formattedLon)
                    .enqueue(new Callback<AirQualityCurrentResponse>() {
                @Override
                public void onResponse(Call<AirQualityCurrentResponse> call,
                                       Response<AirQualityCurrentResponse> response) {
                    if (getView() != null && response.isSuccessful()
                            && response.body() != null) {
                        getView().getAirNowResult(response);
                    }
                }

                @Override
                public void onFailure(Call<AirQualityCurrentResponse> call, Throwable throwable) {
                    // 附加数据，失败不影响主页核心展示
                }
            });
        }

        /**
         * 生活指数
         *
         * @param location 城市名  type中的"1,2,3,5,6,8,9,10"，表示只获取这8种类型的指数信息，同样是为了对应之前的界面UI
         */
        public void lifestyle(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.lifestyle("1,2,3,5,6,8,9,10", location).enqueue(new NetCallBack<LifestyleResponse>() {
                @Override
                public void onSuccess(Call<LifestyleResponse> call, Response<LifestyleResponse> response) {
                    if (getView() != null) {
                        getView().getLifestyleResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    // 附加数据，失败不影响主页核心展示
                }
            });
        }

        /**
         * 城市灾害预警
         *
         * @param location 城市id
         */
        public void nowWarn(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.nowWarn(location).enqueue(new NetCallBack<WarningResponse>() {
                @Override
                public void onSuccess(Call<WarningResponse> call, Response<WarningResponse> response) {
                    if (getView() != null) {
                        getView().getNowWarnResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    // 附加数据，失败不影响主页核心展示
                }
            });
        }

        /**
         * 分钟级降水
         *
         * @param location 经纬度拼接字符串，使用英文逗号分隔,经度在前纬度在后
         */
        public void getMinutePrec(String location) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.getMinutePrec(location).enqueue(new NetCallBack<MinutePrecResponse>() {
                @Override
                public void onSuccess(Call<MinutePrecResponse> call, Response<MinutePrecResponse> response) {
                    if (getView() != null) {
                        getView().getMinutePrecResult(response);
                    }
                }

                @Override
                public void onFailed() {
                    // 分钟级降水常需更高套餐，失败可忽略
                }
            });
        }


    }

    public interface IWeatherView extends BaseView {

        //天气数据获取错误返回
        void getWeatherDataFailed();


        /*                以下为V7版本新增               */

        //搜索城市返回城市id  通过id才能查下面的数据,否则会提示400  V7
        void getNewSearchCityResult(Response<NewSearchCityResponse> response);

        //实况天气
        void getNowResult(Response<NowResponse> response);

        //天气预报  7天
        void getDailyResult(Response<DailyResponse> response);

        //逐小时天气预报
        void getHourlyResult(Response<HourlyResponse> response);

        //空气质量
        void getAirNowResult(Response<AirQualityCurrentResponse> response);

        //生活指数
        void getLifestyleResult(Response<LifestyleResponse> response);

        //灾害预警
        void getNowWarnResult(Response<WarningResponse> response);

        //分钟级降水
        void getMinutePrecResult(Response<MinutePrecResponse> response);

        //错误返回
        void getDataFailed();


    }
}
