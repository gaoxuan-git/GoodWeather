package com.llw.goodweather.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.llw.goodweather.MainActivity;
import com.llw.goodweather.R;
import com.llw.goodweather.bean.BiYingImgResponse;
import com.llw.goodweather.contract.SplashContract;
import com.llw.goodweather.utils.Constant;
import com.llw.goodweather.utils.SPUtils;
import com.llw.goodweather.utils.StatusBarUtil;
import com.llw.goodweather.utils.ToastUtils;
import com.llw.mvplibrary.bean.AppVersion;
import com.llw.mvplibrary.bean.Country;
import com.llw.mvplibrary.mvp.MvpActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Response;

/**
 * 欢迎页
 *
 * @author llw
 */
public class SplashActivity extends MvpActivity<SplashContract.SplashPresenter> implements SplashContract.ISplashView {

    /**
     * 权限请求框架
     */
    private RxPermissions rxPermissions;

    @Override
    public void initData(Bundle savedInstanceState) {
        //透明状态栏
        StatusBarUtil.transparencyBar(context);
        permissionVersion();//权限判断
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    /**
     * 权限判断
     */
    private void permissionVersion() {
        //6.0或6.0以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //动态权限申请
            permissionsRequest();
        } else {//6.0以下
            //发现只要权限在AndroidManifest.xml中注册过，均会认为该权限granted  提示一下即可
            ToastUtils.showShortToast(this, "你的版本在Android6.0以下，不需要动态申请权限。");
        }
    }

    /**
     * 动态权限申请  使用这个框架需要制定JDK版本，建议用1.8
     */
    private void permissionsRequest() {
        rxPermissions = new RxPermissions(this);
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        // Android 13+ 已不再支持旧版读写存储动态授权，和定位绑在一起会导致整组失败
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        String[] permissionArray = permissions.toArray(new String[0]);

        getWindow().getDecorView().post(() -> rxPermissions.request(permissionArray)
                .subscribe(granted -> {
                    if (granted || hasLocationPermission()) {
                        enterAppAfterPermission();
                    } else {
                        ToastUtils.showShortToast(this,
                                "未开启定位权限，可在应用内搜索城市查看天气");
                        enterAppAfterPermission();
                    }
                }));
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void enterAppAfterPermission() {
        mPresent.getAppInfo();
        mPresent.biying();
        // 世界国家数据入库是批量 SQLite 操作，放后台线程执行，避免首次启动主线程逐条写入触发 ANR
        new Thread(this::initCountryData).start();
    }

    /**
     * 初始化世界国家及地区数据（运行在后台线程）。
     * 首次启动需把 assets 中的 world_country.csv 批量写入 SQLite，
     * 若在主线程逐条 {@code save()} 会因大量独立事务阻塞 UI 导致 ANR。
     */
    private void initCountryData() {
        try {
            List<Country> exist = LitePal.findAll(Country.class);
            //已有数据则无需重复写入
            if (exist == null || exist.isEmpty()) {
                List<Country> countries = new ArrayList<>();
                InputStreamReader is = null;
                try {
                    is = new InputStreamReader(getAssets().open("world_country.csv"), "UTF-8");
                    BufferedReader reader = new BufferedReader(is);
                    reader.readLine();//跳过表头
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] result = line.split(",");
                        if (result.length < 2) {
                            continue;
                        }
                        Country country = new Country();
                        country.setName(result[0]);
                        country.setCode(result[1]);
                        countries.add(country);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
                //saveAll 内部使用单个事务批量提交，避免逐条 save 反复开启/提交事务
                if (!countries.isEmpty()) {
                    LitePal.saveAll(countries);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //回到主线程进入主页，无论入库成功与否都不允许卡在欢迎页
            runOnUiThread(this::goToMain);
        }
    }

    /**
     * 进入主页面
     */
    private void goToMain() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        }, 1000);
    }

    @Override
    protected SplashContract.SplashPresenter createPresent() {
        return new SplashContract.SplashPresenter();
    }

    /**
     * 获取APP最新版本信息返回
     *
     * @param response
     */
    @Override
    public void getAppInfoResult(Response<AppVersion> response) {
        if (response.body() != null) {
            AppVersion appVersion = new AppVersion();
            //应用名称
            appVersion.setName(response.body().getName());
            //应用版本 对应code
            appVersion.setVersion(response.body().getVersion());
            //应用版本名
            appVersion.setVersionShort(response.body().getVersionShort());
            //更新日志
            appVersion.setChangelog(response.body().getChangelog());
            //更新地址
            appVersion.setUpdate_url(response.body().getUpdate_url());
            //安装地址
            appVersion.setInstall_url(response.body().getInstall_url());
            //APK大小
            appVersion.setAppSize(String.valueOf(response.body().getBinary().getFsize()));

            //添加数据前先判断是否已经有数据了
            if (LitePal.find(AppVersion.class, 1) != null) {
                appVersion.update(1);
            } else {
                //保存添加数据
                appVersion.save();
            }

        }
    }

    /**
     * 必应壁纸数据返回
     *
     * @param response BiYingImgResponse
     */
    @Override
    public void getBiYingResult(Response<BiYingImgResponse> response) {
        if (response.body().getImages() != null) {
            //得到的图片地址是没有前缀的，所以加上前缀否则显示不出来
            String biyingUrl = "http://cn.bing.com" + response.body().getImages().get(0).getUrl();
            SPUtils.putString(Constant.EVERYDAY_TIP_IMG,biyingUrl,context);

        } else {
            ToastUtils.showShortToast(context, "未获取到必应的图片");
        }
    }

    @Override
    public void getDataFailed() {
        Log.d("Network Error", "网络异常");
    }



}
