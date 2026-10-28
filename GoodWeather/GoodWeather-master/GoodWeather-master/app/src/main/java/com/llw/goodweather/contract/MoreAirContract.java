package com.llw.goodweather.contract;

import com.llw.goodweather.api.ApiService;
import com.llw.goodweather.bean.AirQualityCurrentResponse;
import com.llw.goodweather.bean.AirQualityDailyResponse;
import com.llw.goodweather.bean.AirQualityStationResponse;
import com.llw.goodweather.bean.NewSearchCityResponse;
import com.llw.goodweather.utils.AirQualityMapper;
import com.llw.mvplibrary.base.BasePresenter;
import com.llw.mvplibrary.base.BaseView;
import com.llw.mvplibrary.net.ServiceGenerator;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 更多空气质量数据订阅器
 *
 * @author llw
 */

public class MoreAirContract {

    public static final int REQUEST_LOOKUP = 1;
    public static final int REQUEST_CURRENT = 2;
    public static final int REQUEST_DAILY = 3;
    public static final int REQUEST_STATION = 4;

    public static class MoreAirPresenter extends BasePresenter<IMoreAirView> {

        public void searchLocation(String cityName) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 4);
            service.newSearchCity(cityName, "exact").enqueue(new Callback<NewSearchCityResponse>() {
                @Override
                public void onResponse(Call<NewSearchCityResponse> call,
                                       Response<NewSearchCityResponse> response) {
                    if (getView() == null) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        getView().getSearchCityResult(response);
                    } else {
                        getView().getDataFailed(REQUEST_LOOKUP, "");
                    }
                }

                @Override
                public void onFailure(Call<NewSearchCityResponse> call, Throwable throwable) {
                    if (getView() != null) {
                        getView().getDataFailed(REQUEST_LOOKUP, "");
                    }
                }
            });
        }

        public void airCurrent(String latitude, String longitude) {
            String[] coordinates = formatCoordinates(latitude, longitude, REQUEST_CURRENT);
            if (coordinates == null) {
                return;
            }
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.airQualityCurrent(coordinates[0], coordinates[1])
                    .enqueue(new Callback<AirQualityCurrentResponse>() {
                @Override
                public void onResponse(Call<AirQualityCurrentResponse> call,
                                       Response<AirQualityCurrentResponse> response) {
                    if (getView() == null) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        getView().getMoreAirCurrentResult(response);
                    } else {
                        getView().getDataFailed(REQUEST_CURRENT, "");
                    }
                }

                @Override
                public void onFailure(Call<AirQualityCurrentResponse> call, Throwable throwable) {
                    if (getView() != null) {
                        getView().getDataFailed(REQUEST_CURRENT, "");
                    }
                }
            });
        }

        public void airDaily(String latitude, String longitude) {
            String[] coordinates = formatCoordinates(latitude, longitude, REQUEST_DAILY);
            if (coordinates == null) {
                return;
            }
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.airQualityDaily(coordinates[0], coordinates[1])
                    .enqueue(new Callback<AirQualityDailyResponse>() {
                @Override
                public void onResponse(Call<AirQualityDailyResponse> call,
                                       Response<AirQualityDailyResponse> response) {
                    if (getView() == null) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        getView().getMoreAirDailyResult(response);
                    } else {
                        getView().getDataFailed(REQUEST_DAILY, "");
                    }
                }

                @Override
                public void onFailure(Call<AirQualityDailyResponse> call, Throwable throwable) {
                    if (getView() != null) {
                        getView().getDataFailed(REQUEST_DAILY, "");
                    }
                }
            });
        }

        public void airStation(String stationId, String stationName) {
            ApiService service = ServiceGenerator.createService(ApiService.class, 3);
            service.airQualityStation(stationId)
                    .enqueue(new Callback<AirQualityStationResponse>() {
                @Override
                public void onResponse(Call<AirQualityStationResponse> call,
                                       Response<AirQualityStationResponse> response) {
                    if (getView() == null) {
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        getView().getMoreAirStationResult(stationName, response);
                    } else {
                        getView().getDataFailed(REQUEST_STATION, stationName);
                    }
                }

                @Override
                public void onFailure(Call<AirQualityStationResponse> call, Throwable throwable) {
                    if (getView() != null) {
                        getView().getDataFailed(REQUEST_STATION, stationName);
                    }
                }
            });
        }

        private String[] formatCoordinates(
                String latitude, String longitude, int requestType) {
            String formattedLat = AirQualityMapper.formatCoordinate(latitude);
            String formattedLon = AirQualityMapper.formatCoordinate(longitude);
            if (formattedLat != null && formattedLon != null) {
                return new String[]{formattedLat, formattedLon};
            }
            if (getView() != null) {
                getView().getDataFailed(requestType, "");
            }
            return null;
        }
    }

    public interface IMoreAirView extends BaseView {

        void getSearchCityResult(Response<NewSearchCityResponse> response);

        void getMoreAirCurrentResult(Response<AirQualityCurrentResponse> response);

        void getMoreAirDailyResult(Response<AirQualityDailyResponse> response);

        void getMoreAirStationResult(
                String stationName, Response<AirQualityStationResponse> response);

        void getDataFailed(int requestType, String contextValue);
    }
}
