package com.llw.goodweather.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.llw.goodweather.R;
import com.llw.goodweather.adapter.MoreAirFiveAdapter;
import com.llw.goodweather.adapter.MoreAirStationAdapter;
import com.llw.goodweather.bean.AirQualityCurrentResponse;
import com.llw.goodweather.bean.AirQualityDailyResponse;
import com.llw.goodweather.bean.AirQualityStationResponse;
import com.llw.goodweather.bean.NewSearchCityResponse;
import com.llw.goodweather.contract.MoreAirContract;
import com.llw.goodweather.model.AirQualityData;
import com.llw.goodweather.utils.AirQualityMapper;
import com.llw.goodweather.utils.Constant;
import com.llw.goodweather.utils.DateUtils;
import com.llw.mvplibrary.view.LineProgressbar;
import com.llw.goodweather.utils.StatusBarUtil;
import com.llw.goodweather.utils.ToastUtils;
import com.llw.mvplibrary.mvp.MvpActivity;
import com.llw.mvplibrary.view.RoundProgressBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import retrofit2.Response;

/**
 * 更多空气质量信息
 *
 * @author llw
 */
public class MoreAirActivity extends MvpActivity<MoreAirContract.MoreAirPresenter> implements MoreAirContract.IMoreAirView {

    @BindView(R.id.tv_title)
    TextView tvTitle;//标题
    @BindView(R.id.toolbar)
    Toolbar toolbar;//toolbar
    @BindView(R.id.rpb_aqi)
    RoundProgressBar rpbAqi;//圆环进度条
    @BindView(R.id.tv_pm10)
    TextView tvPm10;//pm10
    @BindView(R.id.tv_pm25)
    TextView tvPm25;//pm2.5
    @BindView(R.id.tv_no2)
    TextView tvNo2;//二氧化氮
    @BindView(R.id.tv_so2)
    TextView tvSo2;//二氧化硫
    @BindView(R.id.tv_o3)
    TextView tvO3;//臭氧
    @BindView(R.id.tv_co)
    TextView tvCo;//一氧化碳
    @BindView(R.id.tv_old_time)
    TextView tvOldTime;//最近更新时间
    @BindView(R.id.rv_station)
    RecyclerView rvStation;//检测站数据列表
    @BindView(R.id.tv_station_title)
    TextView tvStationTitle;
    @BindView(R.id.progress_pm10)
    LineProgressbar progressPm10;//pm10含量进度条展示
    @BindView(R.id.progress_pm25)
    LineProgressbar progressPm25;//pm2.5含量进度条展示
    @BindView(R.id.progress_no2)
    LineProgressbar progressNo2;//二氧化氮含量进度条展示
    @BindView(R.id.progress_so2)
    LineProgressbar progressSo2;//二氧化硫含量进度条展示
    @BindView(R.id.progress_o3)
    LineProgressbar progressO3;//臭氧含量进度条展示
    @BindView(R.id.progress_co)
    LineProgressbar progressCo;//一氧化碳含量进度条展示
    @BindView(R.id.rv_five_air)
    RecyclerView rvFiveAir;//未来3天空气质量列表

    private boolean currentFinished;
    private boolean dailyFinished;
    private boolean currentSucceeded;
    private boolean dailySucceeded;
    private int pendingStationCount;
    private final List<AirQualityData.Station> stationData = new ArrayList<>();

    @Override
    public void initData(Bundle savedInstanceState) {
        StatusBarUtil.transparencyBar(context);//透明状态栏
        Back(toolbar);
        String stationName = getIntent().getStringExtra("stationName");
        tvTitle.setText(stationName + " - " + getIntent().getStringExtra("cityName"));
        String latitude = getIntent().getStringExtra("latitude");
        String longitude = getIntent().getStringExtra("longitude");
        if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
            requestPrimaryAirData(latitude, longitude);
        } else {
            showLoadingDialog();
            mPresent.searchLocation(stationName);
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_more_air;
    }

    @Override
    protected MoreAirContract.MoreAirPresenter createPresent() {
        return new MoreAirContract.MoreAirPresenter();
    }

    @Override
    public void getSearchCityResult(Response<NewSearchCityResponse> response) {
        NewSearchCityResponse body = response.body();
        List<NewSearchCityResponse.LocationBean> locations =
                body == null ? null : body.getLocation();
        if (body == null || !Constant.SUCCESS_CODE.equals(body.getCode())
                || locations == null || locations.isEmpty()) {
            dismissLoadingDialog();
            ToastUtils.showShortToast(context, "未查询到相关数据");
            return;
        }
        NewSearchCityResponse.LocationBean location = locations.get(0);
        requestPrimaryAirData(location.getLat(), location.getLon());
    }

    @Override
    public void getMoreAirCurrentResult(Response<AirQualityCurrentResponse> response) {
        AirQualityData.Current data = AirQualityMapper.mapCurrent(response.body());
        currentFinished = true;
        currentSucceeded = data != null;
        if (data != null) {
            showAirBasicData(data);
            tvOldTime.setText("最近更新时间：" + DateUtils.getNowTime().substring(0, 5));
            requestStations(data.stations);
        }
        finishPrimaryRequestsIfReady();
    }

    @Override
    public void getMoreAirDailyResult(Response<AirQualityDailyResponse> response) {
        List<AirQualityData.Daily> data = AirQualityMapper.mapDaily(response.body());
        dailyFinished = true;
        dailySucceeded = !data.isEmpty();
        if (!data.isEmpty()) {
            MoreAirFiveAdapter adapter =
                    new MoreAirFiveAdapter(R.layout.item_more_air_five_list, data);
            LinearLayoutManager manager = new LinearLayoutManager(context);
            manager.setOrientation(RecyclerView.HORIZONTAL);
            rvFiveAir.setLayoutManager(manager);
            rvFiveAir.setAdapter(adapter);
        }
        finishPrimaryRequestsIfReady();
    }

    @Override
    public void getMoreAirStationResult(
            String stationName, Response<AirQualityStationResponse> response) {
        AirQualityData.Station station =
                AirQualityMapper.mapStation(stationName, response.body());
        if (station != null) {
            stationData.add(station);
        }
        finishStationRequest();
    }

    private void requestPrimaryAirData(String latitude, String longitude) {
        currentFinished = false;
        dailyFinished = false;
        currentSucceeded = false;
        dailySucceeded = false;
        showLoadingDialog();
        mPresent.airCurrent(latitude, longitude);
        mPresent.airDaily(latitude, longitude);
    }

    private void finishPrimaryRequestsIfReady() {
        if (!currentFinished || !dailyFinished) {
            return;
        }
        dismissLoadingDialog();
        if (!currentSucceeded && !dailySucceeded) {
            ToastUtils.showShortToast(context, "更多空气质量数据获取异常");
        }
    }

    private void requestStations(List<AirQualityCurrentResponse.StationRef> stations) {
        stationData.clear();
        tvStationTitle.setVisibility(View.GONE);
        rvStation.setVisibility(View.GONE);
        if (stations == null || stations.isEmpty()) {
            return;
        }
        List<AirQualityCurrentResponse.StationRef> validStations = new ArrayList<>();
        for (AirQualityCurrentResponse.StationRef station : stations) {
            if (station != null && !TextUtils.isEmpty(station.id)) {
                validStations.add(station);
            }
        }
        pendingStationCount = validStations.size();
        for (AirQualityCurrentResponse.StationRef station : validStations) {
            mPresent.airStation(station.id, station.name);
        }
    }

    private void finishStationRequest() {
        pendingStationCount--;
        if (pendingStationCount > 0) {
            return;
        }
        if (stationData.isEmpty()) {
            tvStationTitle.setVisibility(View.GONE);
            rvStation.setVisibility(View.GONE);
            return;
        }
        tvStationTitle.setVisibility(View.VISIBLE);
        rvStation.setVisibility(View.VISIBLE);
        MoreAirStationAdapter adapter =
                new MoreAirStationAdapter(R.layout.item_more_air_station_list, stationData);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setOrientation(RecyclerView.HORIZONTAL);
        rvStation.setLayoutManager(manager);
        rvStation.setOnFlingListener(null);
        new PagerSnapHelper().attachToRecyclerView(rvStation);
        rvStation.setAdapter(adapter);
    }

    private void showAirBasicData(AirQualityData.Current data) {
        rpbAqi.setMaxProgress(300);
        rpbAqi.setMinText("0");
        rpbAqi.setMinTextSize(32f);
        rpbAqi.setMaxText("300");
        rpbAqi.setMaxTextSize(32f);
        rpbAqi.setProgress(data.aqi);
        rpbAqi.setArcBgColor(getResources().getColor(R.color.arc_bg_color));
        rpbAqi.setProgressColor(getResources().getColor(R.color.arc_progress_color));
        rpbAqi.setFirstText(data.category);
        rpbAqi.setFirstTextSize(44f);
        rpbAqi.setSecondText(data.aqiDisplay);
        rpbAqi.setSecondTextSize(64f);
        rpbAqi.setMinTextColor(getResources().getColor(R.color.arc_progress_color));

        showPollutant(tvPm10, progressPm10, data.pollutant("pm10"));
        showPollutant(tvPm25, progressPm25, data.pollutant("pm2p5"));
        showPollutant(tvNo2, progressNo2, data.pollutant("no2"));
        showPollutant(tvSo2, progressSo2, data.pollutant("so2"));
        showPollutant(tvO3, progressO3, data.pollutant("o3"));
        showPollutant(tvCo, progressCo, data.pollutant("co"));
    }

    private void showPollutant(TextView textView, LineProgressbar progressbar,
                               AirQualityData.PollutantValue value) {
        textView.setText(value.display);
        progressbar.setProgress(String.valueOf(value.progress), 100);
    }

    @Override
    public void getDataFailed(int requestType, String contextValue) {
        switch (requestType) {
            case MoreAirContract.REQUEST_LOOKUP:
                dismissLoadingDialog();
                ToastUtils.showShortToast(context, "未查询到相关数据");
                break;
            case MoreAirContract.REQUEST_CURRENT:
                currentFinished = true;
                currentSucceeded = false;
                finishPrimaryRequestsIfReady();
                break;
            case MoreAirContract.REQUEST_DAILY:
                dailyFinished = true;
                dailySucceeded = false;
                finishPrimaryRequestsIfReady();
                break;
            case MoreAirContract.REQUEST_STATION:
                finishStationRequest();
                break;
            default:
                break;
        }
    }

}
