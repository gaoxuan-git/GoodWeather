package com.llw.goodweather.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.llw.goodweather.R;
import com.llw.goodweather.bean.ServerHealthResponse;
import com.llw.goodweather.bean.ServerInfoResponse;
import com.llw.goodweather.utils.Constant;
import com.llw.goodweather.utils.DisplayUtil;
import com.llw.goodweather.utils.SPUtils;
import com.llw.goodweather.utils.ServerClient;
import com.llw.goodweather.utils.ServerUrlUtils;
import com.llw.goodweather.utils.StatusBarUtil;
import com.llw.goodweather.utils.ToastUtils;
import com.llw.mvplibrary.base.BaseActivity;
import com.llw.mvplibrary.view.SwitchButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 应用设置页面
 *
 * @author llw
 */
public class SettingActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.wb_everyday)
    SwitchButton wbEveryday;
    @BindView(R.id.tv_server_address)
    TextView tvServerAddress;
    @BindView(R.id.tv_server_health)
    TextView tvServerHealth;
    @BindView(R.id.tv_server_info)
    TextView tvServerInfo;
    @BindView(R.id.tv_city_manager_value)
    TextView tvCityManagerValue;
    @BindView(R.id.tv_update_interval)
    TextView tvUpdateInterval;

    /** 更新频率选项（分钟）与文案 */
    private final int[] intervalMinutes = {
            Constant.UPDATE_INTERVAL_MANUAL,
            Constant.UPDATE_INTERVAL_1H,
            Constant.UPDATE_INTERVAL_2H,
            Constant.UPDATE_INTERVAL_4H
    };
    private final String[] intervalLabels = {
            "仅手动刷新", "每 1 小时", "每 2 小时", "每 4 小时"
    };

    @Override
    public void initData(Bundle savedInstanceState) {
        //白色状态栏
        StatusBarUtil.setStatusBarColor(context, R.color.white);
        //黑色字体
        StatusBarUtil.StatusBarLightMode(context);
        Back(toolbar);

        boolean isChecked = SPUtils.getBoolean(Constant.EVERYDAY_POP_BOOLEAN,true,context);

        wbEveryday.setChecked(isChecked);

        wbEveryday.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if(isChecked){
                    SPUtils.putBoolean(Constant.EVERYDAY_POP_BOOLEAN,true,context);
                }else {
                    SPUtils.putBoolean(Constant.EVERYDAY_POP_BOOLEAN,false,context);
                }
            }
        });

        updateServerAddress();
        loadServerStatus();

        //回显当前自动更新频率
        int interval = SPUtils.getInt(Constant.WEATHER_UPDATE_INTERVAL,
                Constant.UPDATE_INTERVAL_MANUAL, context);
        tvUpdateInterval.setText(intervalLabel(interval));
        updateCurrentCity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentCity();
    }

    /** 回显当前用于天气预报的城市；未手动选择时继续使用定位城市。 */
    private void updateCurrentCity() {
        String city = SPUtils.getString(Constant.CURRENT_WEATHER_CITY, "", context);
        if (city == null || city.trim().isEmpty()) {
            city = SPUtils.getString(Constant.LOCATION, "", context);
        }
        tvCityManagerValue.setText(city == null || city.trim().isEmpty()
                ? "定位城市"
                : city.trim());
    }

    /**
     * 城市管理：进入常用城市页，可搜索添加、切换或删除要显示天气的城市
     */
    @OnClick(R.id.lay_city_manager)
    public void onCityManager() {
        startActivity(new Intent(context, CommonlyUsedCityActivity.class));
    }

    /**
     * 天气更新频率：弹窗单选，选中后写入 SP，主界面 onResume 时按新间隔调度定时刷新
     */
    @OnClick(R.id.lay_update_interval)
    public void onUpdateInterval() {
        int current = SPUtils.getInt(Constant.WEATHER_UPDATE_INTERVAL,
                Constant.UPDATE_INTERVAL_MANUAL, context);
        int checked = 0;
        for (int i = 0; i < intervalMinutes.length; i++) {
            if (intervalMinutes[i] == current) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("天气更新频率")
                .setSingleChoiceItems(intervalLabels, checked, (dialog, which) -> {
                    int minutes = intervalMinutes[which];
                    SPUtils.putInt(Constant.WEATHER_UPDATE_INTERVAL, minutes, context);
                    tvUpdateInterval.setText(intervalLabels[which]);
                    ToastUtils.showShortToast(context,
                            minutes == Constant.UPDATE_INTERVAL_MANUAL
                                    ? "已关闭自动更新"
                                    : "已设置为" + intervalLabels[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 间隔分钟数 → 显示文案 */
    private String intervalLabel(int minutes) {
        for (int i = 0; i < intervalMinutes.length; i++) {
            if (intervalMinutes[i] == minutes) {
                return intervalLabels[i];
            }
        }
        return intervalLabels[0];
    }

    private void updateServerAddress() {
        tvServerAddress.setText(ServerClient.configuredBaseUrl(context));
    }

    @OnClick(R.id.lay_server_address)
    public void onServerAddress() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(ServerClient.configuredBaseUrl(context));
        input.setSelection(input.getText().length());

        int padding = DisplayUtil.dp2px(context, 20);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("服务端地址")
                .setView(container)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String normalized = ServerUrlUtils.normalize(
                            input.getText().toString());
                    if (normalized == null) {
                        ToastUtils.showShortToast(context,
                                "请输入有效地址，例如 10.110.61.220:5000");
                        return;
                    }
                    ServerClient.saveConfiguredBaseUrl(context, normalized);
                    updateServerAddress();
                    loadServerStatus();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void loadServerStatus() {
        tvServerHealth.setText("连接状态：检测中…");
        tvServerInfo.setText("服务信息：—");

        new Thread(() -> {
            String reachable = ServerClient.probeFirstReachable(context);
            runOnUiThread(() -> {
                if (reachable == null) {
                    tvServerHealth.setText("连接状态：无法连接");
                    tvServerInfo.setText("请确认手机与电脑在同一 Wi-Fi，电脑服务已启动");
                    return;
                }
                ServerClient.useBaseUrl(reachable);
                requestServerStatus(reachable);
            });
        }).start();
    }

    private void requestServerStatus(final String baseUrl) {
        tvServerAddress.setText(baseUrl);
        tvServerHealth.setText("连接状态：检测中…（" + baseUrl + "）");

        ServerClient.getApi().health().enqueue(new Callback<ServerHealthResponse>() {
            @Override
            public void onResponse(Call<ServerHealthResponse> call, Response<ServerHealthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ServerHealthResponse body = response.body();
                    tvServerHealth.setText("连接状态：正常（" + body.getService() + "）");
                } else {
                    tvServerHealth.setText("连接状态：异常");
                }
            }

            @Override
            public void onFailure(Call<ServerHealthResponse> call, Throwable t) {
                tvServerHealth.setText("连接状态：无法连接 " + baseUrl);
            }
        });

        ServerClient.getApi().info().enqueue(new Callback<ServerInfoResponse>() {
            @Override
            public void onResponse(Call<ServerInfoResponse> call, Response<ServerInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ServerInfoResponse body = response.body();
                    tvServerInfo.setText("服务信息：v" + body.getServerVersion() + " · " + body.getProject());
                }
            }

            @Override
            public void onFailure(Call<ServerInfoResponse> call, Throwable t) {
                tvServerInfo.setText("服务信息：获取失败");
            }
        });
    }

    @OnClick(R.id.btn_refresh_server)
    public void onRefreshServer() {
        loadServerStatus();
        ToastUtils.showShortToast(context, "正在检测服务端…");
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_setting;
    }

}
