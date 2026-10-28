package com.llw.goodweather.adapter;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.llw.goodweather.R;
import com.llw.goodweather.model.AirQualityData;

import java.util.List;

/**
 * 更多空气质量之空气站点列表适配器
 *
 * @author llw
 */
public class MoreAirStationAdapter extends BaseQuickAdapter<AirQualityData.Station, BaseViewHolder> {
    public MoreAirStationAdapter(int layoutResId, @Nullable List<AirQualityData.Station> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, AirQualityData.Station item) {
        helper.setText(R.id.tv_station_name, item.name)
                .setText(R.id.tv_air_category, item.category)
                .setText(R.id.tv_aqi, item.aqiDisplay)
                .setText(R.id.tv_primary, item.primary)
                .setText(R.id.tv_pm10, item.pollutant("pm10").display)
                .setText(R.id.tv_pm25, item.pollutant("pm2p5").display)
                .setText(R.id.tv_no2, item.pollutant("no2").display)
                .setText(R.id.tv_so2, item.pollutant("so2").display)
                .setText(R.id.tv_o3, item.pollutant("o3").display)
                .setText(R.id.tv_co, item.pollutant("co").display);
    }
}
