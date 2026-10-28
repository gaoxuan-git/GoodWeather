package com.llw.goodweather.adapter;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.llw.goodweather.R;
import com.llw.goodweather.model.AirQualityData;
import com.llw.goodweather.utils.DateUtils;

import java.util.List;

/**
 * 3天空气质量预报适配器
 *
 * @author llw
 */
public class MoreAirFiveAdapter extends BaseQuickAdapter<AirQualityData.Daily, BaseViewHolder> {
    public MoreAirFiveAdapter(int layoutResId, @Nullable List<AirQualityData.Daily> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, AirQualityData.Daily item) {
        helper.setText(R.id.tv_date_info, DateUtils.Week(item.date))
                .setText(R.id.tv_date, DateUtils.dateSplit(item.date))
                .setText(R.id.tv_aqi, item.aqiDisplay)
                .setText(R.id.tv_category, item.category)
                .setText(R.id.tv_primary, item.primary);
    }
}
