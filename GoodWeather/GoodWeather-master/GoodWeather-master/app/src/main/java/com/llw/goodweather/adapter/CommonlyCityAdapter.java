package com.llw.goodweather.adapter;

import androidx.annotation.Nullable;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.llw.goodweather.R;
import com.llw.mvplibrary.bean.ResidentCity;

import java.util.List;

/**
 * 常用城市列表适配器
 *
 * @author llw
 */
public class CommonlyCityAdapter extends BaseQuickAdapter<ResidentCity, BaseViewHolder> {
    private final String currentCity;

    public CommonlyCityAdapter(int layoutResId, @Nullable List<ResidentCity> data,
                               String currentCity) {
        super(layoutResId, data);
        this.currentCity = currentCity;
    }

    @Override
    protected void convert(BaseViewHolder helper, ResidentCity item) {
        boolean isCurrent = TextUtils.equals(currentCity, item.getLocation());
        helper.setText(R.id.tv_city_name,
                item.getLocation() + (isCurrent ? "（当前）" : ""));
        //添加点击事件
        helper.addOnClickListener(R.id.tv_city_name)
                .addOnClickListener(R.id.btn_delete);
    }
}
