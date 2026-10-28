package com.llw.goodweather.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.llw.goodweather.R;
import com.llw.goodweather.adapter.HourlyAdapter;
import com.llw.goodweather.adapter.MoreDailyAdapter;
import com.llw.goodweather.bean.DailyResponse;
import com.llw.goodweather.bean.HourlyResponse;
import com.llw.goodweather.bean.NowResponse;
import com.llw.goodweather.contract.MoreDailyContract;
import com.llw.goodweather.utils.CodeToStringUtils;
import com.llw.goodweather.utils.Constant;
import com.llw.goodweather.utils.DateUtils;
import com.llw.goodweather.utils.IconUtils;
import com.llw.goodweather.utils.RecyclerViewScrollHelper;
import com.llw.goodweather.utils.StatusBarUtil;
import com.llw.goodweather.utils.ToastUtils;
import com.llw.mvplibrary.mvp.MvpActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import retrofit2.Response;

/**
 * 更多天气预报
 *
 * @author llw
 */
public class MoreDailyActivity extends MvpActivity<MoreDailyContract.MoreDailyPresenter> implements MoreDailyContract.IMoreDailyView {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.rv)
    RecyclerView rv;
    //顶部“当前天气概要”卡片
    @BindView(R.id.tv_d_city)
    TextView tvDCity;
    @BindView(R.id.tv_d_date)
    TextView tvDDate;
    @BindView(R.id.tv_d_info)
    TextView tvDInfo;
    @BindView(R.id.tv_d_range)
    TextView tvDRange;
    @BindView(R.id.tv_d_wind)
    TextView tvDWind;
    @BindView(R.id.tv_d_time)
    TextView tvDTime;//天气数据更新时间
    @BindView(R.id.tv_d_current_time)
    TextView tvDCurrentTime;//当前实时时间
    @BindView(R.id.iv_d_icon)
    ImageView ivDIcon;
    @BindView(R.id.tv_d_temp)
    TextView tvDTemp;
    @BindView(R.id.rv_hourly)
    RecyclerView rvHourly;
    @BindView(R.id.tv_hourly_empty)
    TextView tvHourlyEmpty;

    /**
     * 数据实体
     */
    List<DailyResponse.DailyBean> mList = new ArrayList<>();
    /**
     * 适配器
     */
    MoreDailyAdapter mAdapter;
    List<HourlyResponse.HourlyBean> hourlyList = new ArrayList<>();
    HourlyAdapter hourlyAdapter;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            tvDCurrentTime.setText("当前时间：" + DateUtils.getNowTime());
            tvDDate.setText(DateUtils.getDateAndWeek(now));
            clockHandler.postDelayed(this, 1_000L);
        }
    };

    @Override
    public void initData(Bundle savedInstanceState) {
        //透明状态栏
        StatusBarUtil.transparencyBar(context);
        showLoadingDialog();
        initList();
        showSummary();
        Back(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.removeCallbacks(clockRunnable);
        clockRunnable.run();
    }

    @Override
    protected void onPause() {
        clockHandler.removeCallbacks(clockRunnable);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        clockHandler.removeCallbacks(clockRunnable);
        super.onDestroy();
    }

    /**
     * 渲染顶部“当前天气概要”卡片：数据由主界面跳转时通过 Intent 传入，
     * 与下方列表共同构成“详细界面 = 显示界面全部信息 + 当天天气列表”
     */
    private void showSummary() {
        tvDCity.setText(getIntent().getStringExtra("cityName"));
        tvDDate.setText(DateUtils.getDateAndWeek(new Date()));

        String nowTemp = getIntent().getStringExtra("nowTemp");
        tvDTemp.setText(TextUtils.isEmpty(nowTemp) ? "--" : nowTemp + "℃");

        String info = getIntent().getStringExtra("nowInfo");
        tvDInfo.setText(TextUtils.isEmpty(info) ? "" : info);

        String high = getIntent().getStringExtra("tempHigh");
        String low = getIntent().getStringExtra("tempLow");
        String range = (high == null ? "" : high) + (low == null ? "" : low);
        tvDRange.setText(TextUtils.isEmpty(range.trim()) ? "最高 -- / 最低 --" : range);

        String windDir = getIntent().getStringExtra("windDir");
        String windPower = getIntent().getStringExtra("windPower");
        StringBuilder wind = new StringBuilder();
        if (!TextUtils.isEmpty(windDir)) {
            wind.append(windDir);
        }
        if (!TextUtils.isEmpty(windPower)) {
            wind.append("    ").append(windPower);
        }
        tvDWind.setText(wind.toString());

        //天气数据更新时间由显示界面跳转时带入，当前时间由本页面独立更新
        String updateTime = getIntent().getStringExtra("updateTime");
        if (TextUtils.isEmpty(updateTime)) {
            tvDTime.setText("数据更新时间：--:--");
        } else {
            tvDTime.setText(updateTime.replace("最近更新时间：", "数据更新时间："));
        }

        String icon = getIntent().getStringExtra("nowIcon");
        if (!TextUtils.isEmpty(icon)) {
            ivDIcon.setImageResource(IconUtils.getDayIconDark(icon.trim()));
        }

        //兜底：主界面实况尚未绑定就进入时（Intent 未带当前温度），自行请求实况补全概要
        if (TextUtils.isEmpty(getIntent().getStringExtra("nowTemp"))) {
            String locId = getIntent().getStringExtra("locationId");
            if (!TextUtils.isEmpty(locId)) {
                mPresent.nowWeather(locId);
            }
        }
    }

    /**
     * 兜底实况天气返回：补全顶部概要卡片的当前温度、天气文字、图标与风力
     */
    @Override
    public void getMoreNowResult(Response<NowResponse> response) {
        if (response.body() == null || response.body().getNow() == null) {
            return;
        }
        if (!Constant.SUCCESS_CODE.equals(response.body().getCode())) {
            return;
        }
        NowResponse.NowBean now = response.body().getNow();
        tvDTemp.setText(now.getTemp() + "℃");
        tvDInfo.setText(now.getText());
        if (!TextUtils.isEmpty(now.getIcon())) {
            ivDIcon.setImageResource(IconUtils.getDayIconDark(now.getIcon().trim()));
        }
        if (TextUtils.isEmpty(getIntent().getStringExtra("windDir"))) {
            tvDWind.setText("风向  " + now.getWindDir() + "    风力  " + now.getWindScale() + "级");
        }

        //Intent 未携带更新时间时，用实况接口返回的 updateTime 兜底补全
        if (TextUtils.isEmpty(getIntent().getStringExtra("updateTime"))
                && !TextUtils.isEmpty(response.body().getUpdateTime())) {
            String time = DateUtils.updateTime(response.body().getUpdateTime());
            tvDTime.setText("数据更新时间：" + time);
        }
    }

    /**
     * 初始化列表
     */
    private void initList() {
        mAdapter = new MoreDailyAdapter(R.layout.item_more_daily_list, mList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv.setLayoutManager(linearLayoutManager);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        rv.setOnFlingListener(null);//避免抛异常
        //滚动对齐，使RecyclerView像ViewPage一样，一次滑动一项,居中
        snapHelper.attachToRecyclerView(rv);
        rv.setAdapter(mAdapter);

        hourlyAdapter = new HourlyAdapter(R.layout.item_weather_hourly_list, hourlyList);
        LinearLayoutManager hourlyLayoutManager =
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rvHourly.setLayoutManager(hourlyLayoutManager);
        rvHourly.setAdapter(hourlyAdapter);

        tvTitle.setText(getIntent().getStringExtra("cityName"));
        String locationId = getIntent().getStringExtra("locationId");
        mPresent.worldCity(locationId);
        mPresent.hourlyWeather(locationId);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_more_daily;
    }

    @Override
    protected MoreDailyContract.MoreDailyPresenter createPresent() {
        return new MoreDailyContract.MoreDailyPresenter();
    }

    /**
     * 更多预报天气返回值
     *
     * @param response
     */
    @Override
    public void getMoreDailyResult(Response<DailyResponse> response) {
        dismissLoadingDialog();
        DailyResponse body = response.body();
        if (body == null) {
            ToastUtils.showShortToast(context, "15天天气数据获取异常");
            return;
        }
        if (Constant.SUCCESS_CODE.equals(body.getCode())) {
            List<DailyResponse.DailyBean> data = body.getDaily();
            //判空处理
            if (data != null && data.size() > 0) {
                //添加数据之前先清除
                mList.clear();
                //添加数据
                mList.addAll(data);
                //刷新列表
                mAdapter.notifyDataSetChanged();

                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).getFxDate().equals(DateUtils.getNowDate())) {
                        //概要卡片高低温兜底（Intent 未携带时用今天的预报数据补全）
                        if (TextUtils.isEmpty(getIntent().getStringExtra("tempHigh"))) {
                            tvDRange.setText(data.get(i).getTempMax() + "℃ / "
                                    + data.get(i).getTempMin() + "℃");
                        }
                        //渲染完成后，定位到今天，因为和风天气预报有时候包括了昨天，有时候又不包括，搞得我很被动
                        RecyclerViewScrollHelper.scrollToPosition(rv, i);
                    }
                }

            } else {
                ToastUtils.showShortToast(context, "天气预报数据为空");
            }
        } else {//异常状态码返回
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(body.getCode()));
        }
    }

    @Override
    public void getMoreHourlyResult(Response<HourlyResponse> response) {
        HourlyResponse body = response.body();
        if (body == null || !Constant.SUCCESS_CODE.equals(body.getCode())) {
            showHourlyEmpty();
            return;
        }
        List<HourlyResponse.HourlyBean> data = body.getHourly();
        if (data == null || data.isEmpty()) {
            showHourlyEmpty();
            return;
        }
        hourlyList.clear();
        hourlyList.addAll(data);
        hourlyAdapter.notifyDataSetChanged();
        rvHourly.setVisibility(View.VISIBLE);
        tvHourlyEmpty.setVisibility(View.GONE);
    }

    @Override
    public void getHourlyDataFailed() {
        showHourlyEmpty();
    }

    private void showHourlyEmpty() {
        hourlyList.clear();
        hourlyAdapter.notifyDataSetChanged();
        rvHourly.setVisibility(View.GONE);
        tvHourlyEmpty.setVisibility(View.VISIBLE);
    }

    /**
     * 其他异常返回
     */
    @Override
    public void getDataFailed() {
        dismissLoadingDialog();
        ToastUtils.showShortToast(context, "更多天气数据获取异常");
    }

}
