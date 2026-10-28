package com.llw.goodweather;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;
import com.llw.goodweather.adapter.AreaAdapter;
import com.llw.goodweather.adapter.CityAdapter;
import com.llw.goodweather.adapter.DailyAdapter;
import com.llw.goodweather.adapter.HourlyAdapter;
import com.llw.goodweather.adapter.MinutePrecAdapter;
import com.llw.goodweather.adapter.ProvinceAdapter;
import com.llw.goodweather.bean.AirQualityCurrentResponse;
import com.llw.goodweather.bean.CityResponse;
import com.llw.goodweather.bean.DailyResponse;
import com.llw.goodweather.bean.HourlyResponse;
import com.llw.goodweather.bean.LifestyleResponse;
import com.llw.goodweather.bean.MinutePrecResponse;
import com.llw.goodweather.bean.NewSearchCityResponse;
import com.llw.goodweather.bean.NowResponse;
import com.llw.goodweather.bean.WarningResponse;
import com.llw.goodweather.contract.WeatherContract;
import com.llw.goodweather.eventbus.SearchCityEvent;
import com.llw.goodweather.model.AirQualityData;
import com.llw.goodweather.ui.AboutUsActivity;
import com.llw.goodweather.ui.CommonlyUsedCityActivity;
import com.llw.goodweather.ui.MapWeatherActivity;
import com.llw.goodweather.ui.MoreAirActivity;
import com.llw.goodweather.ui.MoreDailyActivity;
import com.llw.goodweather.ui.MoreLifestyleActivity;
import com.llw.goodweather.ui.SearchCityActivity;
import com.llw.goodweather.ui.SettingActivity;
import com.llw.goodweather.ui.WallPaperActivity;
import com.llw.goodweather.ui.WarnActivity;
import com.llw.goodweather.ui.WorldCityActivity;
import com.llw.goodweather.utils.APKVersionInfoUtils;
import com.llw.goodweather.utils.AirQualityMapper;
import com.llw.goodweather.utils.AppStartUpUtils;
import com.llw.goodweather.utils.CodeToStringUtils;
import com.llw.goodweather.utils.Constant;
import com.llw.goodweather.utils.DateUtils;
import com.llw.goodweather.utils.SPUtils;
import com.llw.goodweather.utils.StatusBarUtil;
import com.llw.goodweather.utils.ToastUtils;
import com.llw.goodweather.utils.WeatherUtil;
import com.llw.mvplibrary.bean.AppVersion;
import com.llw.mvplibrary.mvp.MvpActivity;
import com.llw.mvplibrary.utils.AnimationUtil;
import com.llw.mvplibrary.utils.LiWindow;
import com.llw.mvplibrary.utils.SizeUtils;
import com.llw.mvplibrary.view.RoundProgressBar;
import com.llw.mvplibrary.view.WhiteWindmills;
import com.llw.mvplibrary.view.dialog.AlertDialog;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.LitePal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Response;

import static com.llw.mvplibrary.utils.RecyclerViewAnimation.runLayoutAnimation;
import static com.llw.mvplibrary.utils.RecyclerViewAnimation.runLayoutAnimationRight;

/**
 * 主页面
 *
 * @author cdy
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends MvpActivity<WeatherContract.WeatherPresenter>
        implements WeatherContract.IWeatherView, View.OnScrollChangeListener {

    @BindView(R.id.tv_warn)
    TextView tvWarn;//灾害预警跑马灯
    @BindView(R.id.tv_week)
    TextView tvWeek;//星期
    @BindView(R.id.tv_air_info)
    TextView tvAirInfo;//空气质量
    @BindView(R.id.tv_info)
    TextView tvInfo;//天气状况
    @BindView(R.id.iv_now_icon)
    ImageView ivNowIcon;//当前天气图标
    @BindView(R.id.tv_temperature)
    TextView tvTemperature;//温度
    @BindView(R.id.tv_temp_height)
    TextView tvTempHeight;//最高温
    @BindView(R.id.tv_temp_low)
    TextView tvTempLow;//最低温
    @BindView(R.id.tv_city)
    TextView tvCity;//城市
    @BindView(R.id.tv_current_time)
    TextView tvCurrentTime;//当前实时时间
    @BindView(R.id.tv_old_time)
    TextView tvOldTime;//天气数据更新时间
    @BindView(R.id.rv)
    RecyclerView rv;//天气预报显示列表
    @BindView(R.id.tv_uv)
    TextView tvUv;//紫外线
    @BindView(R.id.tv_comf)
    TextView tvComf;//舒适度
    @BindView(R.id.tv_trav)
    TextView tvTrav;//旅游指数
    @BindView(R.id.tv_sport)
    TextView tvSport;//运动指数
    @BindView(R.id.tv_cw)
    TextView tvCw;//洗车指数
    @BindView(R.id.tv_air)
    TextView tvAir;//空气指数
    @BindView(R.id.tv_drsg)
    TextView tvDrsg;//穿衣指数
    @BindView(R.id.tv_flu)
    TextView tvFlu;//感冒指数
    @BindView(R.id.ww_big)
    WhiteWindmills wwBig;//大风车
    @BindView(R.id.ww_small)
    WhiteWindmills wwSmall;//小风车
    @BindView(R.id.tv_wind_direction)
    TextView tvWindDirection;//风向
    @BindView(R.id.tv_wind_power)
    TextView tvWindPower;//风力
    @BindView(R.id.iv_map)
    ImageView ivMap;//地图天气
    @BindView(R.id.iv_add)
    ImageView ivAdd;//更多功能
    @BindView(R.id.bg)
    ImageView bg;//背景图
    @BindView(R.id.refresh)
    SmartRefreshLayout refresh;//刷新布局
    @BindView(R.id.iv_location)
    ImageView ivLocation;//定位图标
    @BindView(R.id.rv_hourly)
    RecyclerView rvHourly;//逐小时天气显示列表
    @BindView(R.id.rpb_aqi)
    RoundProgressBar rpbAqi;//污染指数圆环
    @BindView(R.id.tv_pm10)
    TextView tvPm10;//PM10
    @BindView(R.id.tv_pm25)
    TextView tvPm25;//PM2.5
    @BindView(R.id.tv_no2)
    TextView tvNo2;//二氧化氮
    @BindView(R.id.tv_so2)
    TextView tvSo2;//二氧化硫
    @BindView(R.id.tv_o3)
    TextView tvO3;//臭氧
    @BindView(R.id.tv_co)
    TextView tvCo;//一氧化碳
    @BindView(R.id.tv_title)
    TextView tvTitle;//标题
    @BindView(R.id.lay_slide_area)
    LinearLayout laySlideArea;//当向上滑动超过这个布局的高度时，改变Toolbar中的TextView的显示文本
    @BindView(R.id.scroll_view)
    NestedScrollView scrollView;//滑动View
    @BindView(R.id.tv_more_daily)
    TextView tvMoreDaily;//更多天气预报
    @BindView(R.id.tv_more_air)
    TextView tvMoreAir;//更多空气信息
    @BindView(R.id.tv_more_lifestyle)
    TextView tvMoreLifestyle;//更多生活建议
    @BindView(R.id.tv_precipitation)
    TextView tvPrecipitation;//降水预告
    @BindView(R.id.tv_prec_more)
    TextView tvPrecMore;//降水详情
    @BindView(R.id.rv_prec_detail)
    RecyclerView rvPrecDetail;//分钟级降水列表

    private boolean flag = true;//图标显示标识,true显示，false不显示,只有定位的时候才为true,切换城市和常用城市都为false

    //定位器
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();

    //V7 版本
    private List<DailyResponse.DailyBean> dailyListV7 = new ArrayList<>();//天气预报数据列表
    private DailyAdapter mAdapterDailyV7;//天气预报适配器
    private List<HourlyResponse.HourlyBean> hourlyListV7 = new ArrayList<>();//逐小时天气预报数据列表
    private HourlyAdapter mAdapterHourlyV7;//逐小时预报适配器

    private List<MinutePrecResponse.MinutelyBean> minutelyList = new ArrayList<>();//分钟级降水数据列表
    private MinutePrecAdapter mAdapterMinutePrec;//分钟级降水适配器

    private boolean state = false;//分钟级降水数据 收缩状态  false 收缩  true 展开

    private List<String> list;//字符串列表
    private List<CityResponse> provinceList;//省列表数据
    private List<CityResponse.CityBean> citylist;//市列表数据
    private List<CityResponse.CityBean.AreaBean> arealist;//区/县列表数据
    private ProvinceAdapter provinceAdapter;//省数据适配器
    private CityAdapter cityAdapter;//市数据适配器
    private AreaAdapter areaAdapter;//县/区数据适配器
    private String provinceTitle;//标题
    private LiWindow liWindow;//自定义弹窗

    private String district = null;//区/县  改为全局的静态变量,方便更换城市之后也能进行下拉刷新
    private String city;//市 国控站点数据  用于请求空气质量

    private String locationId = null;//城市id，用于查询城市数据  V7版本 中 才有
    private String stationName = null;//空气质量站点 查询空气质量站点才需要
    private String nowIconCode = null;//当前实况天气图标代码（传给详细界面概要卡片使用）

    private String lon = null;//经度
    private String lat = null;//纬度

    //右上角的弹窗
    private PopupWindow mPopupWindow;
    private AnimationUtil animUtil;
    private float bgAlpha = 1f;
    private boolean bright = false;
    private static final long DURATION = 500;//0.5s
    private static final float START_ALPHA = 0.7f;//开始透明度
    private static final float END_ALPHA = 1f;//结束透明度

    public boolean flagOther = false;//跳转其他页面时才为true
    public boolean searchCityData = false;//搜索城市是否传递数据回来
    private String warnBodyString = null;//灾害预警数据字符串

    private AlertDialog updateAppDialog = null;//应用更新提示弹窗

    private int OPEN_LOCATION = 9527;//进入手机定位设置页面标识

    private AlertDialog everyDayTipDialog = null;//每日提醒弹窗

    private String dialogTemp = null;//弹窗中的今日温度
    private String dialogWeatherState = null;//弹窗中的天气状态
    private int dialogWeatherStateCode = 0;//弹窗中的天气状态码
    private String dialogPrecipitation = null;//弹窗中的降水预告
    private int dialogTempHeight = 0;//弹窗中今日最高温
    private int dialogTempLow = 0;//弹窗中今日最低温


    //数据初始化
    @Override
    public void initData(Bundle savedInstanceState) {

        StatusBarUtil.transparencyBar(context);//透明状态栏
        initList();//天气预报列表初始化

        if (Constant.QWEATHER_API_HOST.contains("REPLACE_WITH")) {
            ToastUtils.showShortToast(context,
                    "请先在 Constant.java 填写和风 QWEATHER_API_HOST（控制台→设置）");
        }

        if (isOpenLocationServiceEnable()) {
            tvCity.setEnabled(false);//不可点击
            startLocation();//开始定位
        } else {
            tvCity.setEnabled(true);//可以点击
            ToastUtils.showShortToast(context, "(((φ(◎ロ◎;)φ)))，你好像忘记打开定位功能了");
            tvCity.setText("打开定位");
            loadDefaultCityWeather();
        }

        new Handler(Looper.getMainLooper()).postDelayed(this::ensureWeatherLoaded, 15000);

        //由于这个刷新框架默认是有下拉和上拉，但是上拉没有用到，为了不造成误会，我这里禁用上拉
        refresh.setEnableLoadMore(false);
        //初始化弹窗
        mPopupWindow = new PopupWindow(this);
        animUtil = new AnimationUtil();

        EventBus.getDefault().register(this);//注册

        scrollView.setOnScrollChangeListener(this);//指定当前页面，不写则滑动监听无效
    }


    /**
     * 检查APP版本
     */
    private void checkAppVersion() {
        AppVersion appVersion = LitePal.find(AppVersion.class, 1);//读取第一条数据
        Log.d("appVersion", new Gson().toJson(appVersion.getVersionShort()));

        if (AppStartUpUtils.isTodayFirstStartApp(context)) {//今天第一次打开APP
            if (!appVersion.getVersionShort().equals(APKVersionInfoUtils.getVerName(context))) {//提示更新
                //更新提示弹窗
                showUpdateAppDialog(appVersion.getInstall_url(), appVersion.getChangelog());
            }
            //设置每日提示弹窗
            setTipDialog();
        }

    }

    /**
     * 手机是否开启位置服务，如果没有开启那么App将不能使用定位功能
     */
    private boolean isOpenLocationServiceEnable() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }


    /**
     * 应用更新提示弹窗
     *
     * @param downloadUrl 下载地址
     * @param updateLog   更新日志
     */
    private void showUpdateAppDialog(String downloadUrl, String updateLog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .addDefaultAnimation()//默认弹窗动画
                .setCancelable(true)
                .setText(R.id.tv_update_info, updateLog)
                .setContentView(R.layout.dialog_update_app_tip)//载入布局文件
                .setWidthAndHeight(SizeUtils.dp2px(context, 270), ViewGroup.LayoutParams.WRAP_CONTENT)//设置弹窗宽高
                .setOnClickListener(R.id.tv_cancel, v -> {//取消
                    updateAppDialog.dismiss();
                }).setOnClickListener(R.id.tv_fast_update, v -> {//立即更新
                    //下载Apk
                    ToastUtils.showShortToast(context, "正在后台下载，下载后会自动安装");
                    downloadApk(downloadUrl);
                    updateAppDialog.dismiss();
                });
        updateAppDialog = builder.create();
        updateAppDialog.show();
    }

    /**
     * 每日提示弹窗
     */
    private void showEveryDayTipDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .addDefaultAnimation()//默认弹窗动画
                .setCancelable(false)
                .setText(R.id.tv_week, DateUtils.getWeekOfDate(new Date()))//星期
                .setText(R.id.tv_weather_state, dialogWeatherState)//天气状态
                .setText(R.id.tv_precipitation, dialogPrecipitation)//降水预告
                .setText(R.id.tv_temp_difference,
                        WeatherUtil.differenceTempTip(dialogTempHeight, dialogTempLow))//温差提示信息
                .setContentView(R.layout.dialog_everyday_tip)//载入布局文件
                .setWidthAndHeight(SizeUtils.dp2px(context, 270), ViewGroup.LayoutParams.WRAP_CONTENT)//设置弹窗宽高
                .setOnClickListener(R.id.iv_close, v -> {//关闭
                    everyDayTipDialog.dismiss();
                });
        everyDayTipDialog = builder.create();
        String imgUrl = SPUtils.getString(Constant.EVERYDAY_TIP_IMG, "", context);
        ShapeableImageView bg = everyDayTipDialog.getView(R.id.iv_dialog_bg);
        Glide.with(context).load(imgUrl).into(bg);
        //温度
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView temp = everyDayTipDialog.getView(R.id.tv_temperature);
        temp.setTypeface(typeface);
        temp.setText(dialogTemp + "℃");
        //设置天气状态图标
        ImageView weatherStateIcon = everyDayTipDialog.getView(R.id.iv_weather_state);
        WeatherUtil.changeIcon(weatherStateIcon, dialogWeatherStateCode);

        //不再弹出
        MaterialCheckBox cb = everyDayTipDialog.getView(R.id.cb_no_pop_up);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SPUtils.putBoolean(Constant.EVERYDAY_POP_BOOLEAN, false, context);
                } else {
                    SPUtils.putBoolean(Constant.EVERYDAY_POP_BOOLEAN, true, context);
                }
            }
        });

        everyDayTipDialog.show();
    }

    /**
     * 清除APK
     *
     * @param apkName
     * @return
     */
    public static File clearApk(String apkName) {
        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName);
        if (apkFile.exists()) {
            apkFile.delete();
        }
        return apkFile;
    }

    /**
     * 下载APK
     *
     * @param downloadUrl 下载地址
     */
    private void downloadApk(String downloadUrl) {
        clearApk("GoodWeather.apk");
        //下载管理器 获取系统下载服务
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        //设置运行使用的网络类型，移动网络或者Wifi都可以
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        //设置是否允许漫游
        request.setAllowedOverRoaming(true);
        //设置文件类型
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(downloadUrl));
        request.setMimeType(mimeString);
        //设置下载时或者下载完成时，通知栏是否显示
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("下载新版本");
        request.setVisibleInDownloadsUi(true);//下载UI
        //sdcard目录下的download文件夹
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GoodWeather.apk");
        //将下载请求放入队列
        downloadManager.enqueue(request);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SearchCityEvent event) {//接收
        flag = false;//切换城市得到的城市不属于定位，因此这里隐藏定位图标
        //V7版本中需要先获取到城市ID ,在结果返回值中再进行下一步的数据查询
        mPresent.newSearchCity(event.mLocation);//相应事件时
    }


    /**
     * 更换壁纸
     */
    private void updateWallpaper() {
        String imgUrl = SPUtils.getString(Constant.WALLPAPER_URL, null, context);
        if (imgUrl != null) {
            Glide.with(context).load(imgUrl).into(bg);
        } else {
            Glide.with(context).load(R.drawable.img_5).into(bg);
        }
    }

    /** 页面可见时每秒刷新当前时间，并同步处理跨日后的日期。 */
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            tvCurrentTime.setText("当前时间：" + DateUtils.getNowTime());
            tvWeek.setText(DateUtils.getDateAndWeek(now));
            clockHandler.postDelayed(this, 1_000L);
        }
    };

    private void startClock() {
        clockHandler.removeCallbacks(clockRunnable);
        clockRunnable.run();
    }

    private void stopClock() {
        clockHandler.removeCallbacks(clockRunnable);
    }


    /** 天气自动定时刷新句柄 */
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            silentRefreshCurrentCity();
            //按设置的频率持续循环调度
            int minutes = SPUtils.getInt(Constant.WEATHER_UPDATE_INTERVAL,
                    Constant.UPDATE_INTERVAL_MANUAL, context);
            if (minutes > 0) {
                autoRefreshHandler.postDelayed(this, minutes * 60_000L);
            }
        }
    };

    /**
     * 读取设置中的更新频率并调度定时刷新；间隔为 0（仅手动刷新）时不调度
     */
    private void scheduleAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        int minutes = SPUtils.getInt(Constant.WEATHER_UPDATE_INTERVAL,
                Constant.UPDATE_INTERVAL_MANUAL, context);
        if (minutes > 0) {
            autoRefreshHandler.postDelayed(autoRefreshRunnable, minutes * 60_000L);
        }
    }

    /**
     * 停止定时刷新（页面退到后台 / 销毁时调用）
     */
    private void stopAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    /**
     * 定时触发的静默刷新：不弹加载框、不切换城市，仅对当前城市重新拉取全部天气数据
     */
    private void silentRefreshCurrentCity() {
        if (locationId == null || isFinishing()) {
            return;
        }
        mPresent.nowWeather(locationId);
        mPresent.dailyWeather(locationId);
        mPresent.hourlyWeather(locationId);
        mPresent.lifestyle(locationId);
        mPresent.nowWarn(locationId);
        if (lon != null && lat != null) {
            mPresent.airNowWeather(lat, lon);
            mPresent.getMinutePrec(lon + "," + lat);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startClock();
        showLoadingDialog();//在数据请求之前放在加载等待弹窗，返回结果后关闭弹窗
        flagOther = SPUtils.getBoolean(Constant.FLAG_OTHER_RETURN, false, context);
        if (flagOther == true) {
            //取出缓存
            district = SPUtils.getString(Constant.DISTRICT, "", context);
            city = SPUtils.getString(Constant.CITY, "", context);

            //V7版本中需要先获取到城市ID ,在结果返回值中再进行下一步的数据查询
            //其他页面返回时
            mPresent.newSearchCity(district);
        } else {
            dismissLoadingDialog();
            refreshHomeLocationIfStale();
        }

        updateWallpaper();

        //按设置页配置的频率启动/调整定时刷新（从设置页返回时也会按最新值重新调度）
        scheduleAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
        stopAutoRefresh();//退到后台后停止自动刷新，回到前台 onResume 会重新调度
    }

    //绑定布局文件
    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    //绑定Presenter ，这里不绑定会报错
    @Override
    protected WeatherContract.WeatherPresenter createPresent() {
        return new WeatherContract.WeatherPresenter();
    }


    /**
     * 初始化 天气预报 和 逐小时 数据列表
     */
    private void initList() {
        /**   V7 版本   **/
        //天气预报  7天
        mAdapterDailyV7 = new DailyAdapter(R.layout.item_weather_forecast_list, dailyListV7);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(mAdapterDailyV7);
        //天气预报列表item点击事件
        mAdapterDailyV7.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                DailyResponse.DailyBean data = dailyListV7.get(position);
                showForecastWindow(data);
            }
        });

        //逐小时天气预报  24小时
        mAdapterHourlyV7 = new HourlyAdapter(R.layout.item_weather_hourly_list, hourlyListV7);
        LinearLayoutManager managerHourly = new LinearLayoutManager(context);
        managerHourly.setOrientation(RecyclerView.HORIZONTAL);//设置列表为横向
        rvHourly.setLayoutManager(managerHourly);
        rvHourly.setAdapter(mAdapterHourlyV7);
        //逐小时天气预报列表item点击事件
        mAdapterHourlyV7.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                //赋值
                HourlyResponse.HourlyBean data = hourlyListV7.get(position);
                //小时天气详情弹窗
                showHourlyWindow(data);
            }
        });

        //分钟级降水
        mAdapterMinutePrec = new MinutePrecAdapter(R.layout.item_prec_detail_list, minutelyList);
        GridLayoutManager managerMinutePrec = new GridLayoutManager(context, 2);
        managerMinutePrec.setOrientation(RecyclerView.HORIZONTAL);
        rvPrecDetail.setLayoutManager(managerMinutePrec);
        rvPrecDetail.setAdapter(mAdapterMinutePrec);
    }

    /**
     * 显示天气预报详情弹窗
     *
     * @param data
     */
    private void showForecastWindow(DailyResponse.DailyBean data) {
        liWindow = new LiWindow(context);
        final View view = LayoutInflater.from(context).inflate(R.layout.window_forecast_detail, null);
        TextView tv_datetime = view.findViewById(R.id.tv_datetime);
        TextView tv_tmp_max = view.findViewById(R.id.tv_tmp_max);//最高温
        TextView tv_tmp_min = view.findViewById(R.id.tv_tmp_min);//最低温
        TextView tv_uv_index = view.findViewById(R.id.tv_uv_index);//紫外线强度
        TextView tv_cond_txt_d = view.findViewById(R.id.tv_cond_txt_d);//白天天气状态
        TextView tv_cond_txt_n = view.findViewById(R.id.tv_cond_txt_n);//晚上天气状态
        TextView tv_wind_deg = view.findViewById(R.id.tv_wind_deg);//风向360角度
        TextView tv_wind_dir = view.findViewById(R.id.tv_wind_dir);//风向
        TextView tv_wind_sc = view.findViewById(R.id.tv_wind_sc);//风力
        TextView tv_wind_spd = view.findViewById(R.id.tv_wind_spd);//风速
        TextView tv_cloud = view.findViewById(R.id.tv_cloud);//云量  V7 新增
        TextView tv_hum = view.findViewById(R.id.tv_hum);//相对湿度
        TextView tv_pres = view.findViewById(R.id.tv_pres);//大气压强
        TextView tv_pcpn = view.findViewById(R.id.tv_pcpn);//降水量
        TextView tv_vis = view.findViewById(R.id.tv_vis);//能见度

        tv_datetime.setText(data.getFxDate() + "   " + DateUtils.Week(data.getFxDate()));//时间日期
        tv_tmp_max.setText(data.getTempMax() + "℃");
        tv_tmp_min.setText(data.getTempMin() + "℃");
        tv_uv_index.setText(data.getUvIndex());
        tv_cond_txt_d.setText(data.getTextDay());
        tv_cond_txt_n.setText(data.getTextNight());
        tv_wind_deg.setText(data.getWind360Day() + "°");
        tv_wind_dir.setText(data.getWindDirDay());
        tv_wind_sc.setText(data.getWindScaleDay() + "级");
        tv_wind_spd.setText(data.getWindSpeedDay() + "公里/小时");
        tv_cloud.setText(data.getCloud() + "%");//V7 版本中，新增 云量
        tv_hum.setText(data.getHumidity() + "%");
        tv_pres.setText(data.getPressure() + "hPa");
        tv_pcpn.setText(data.getPrecip() + "mm");
        tv_vis.setText(data.getVis() + "km");
        liWindow.showCenterPopupWindow(view, SizeUtils.dp2px(context, 300), SizeUtils.dp2px(context, 500), true);

    }

    /**
     * 显示小时详情天气信息弹窗
     *
     * @param data
     */
    private void showHourlyWindow(HourlyResponse.HourlyBean data) {
        liWindow = new LiWindow(context);
        final View view = LayoutInflater.from(context).inflate(R.layout.window_hourly_detail, null);
        TextView tv_time = view.findViewById(R.id.tv_time);//时间
        TextView tv_tem = view.findViewById(R.id.tv_tem);//温度
        TextView tv_cond_txt = view.findViewById(R.id.tv_cond_txt);//天气状态描述
        TextView tv_wind_deg = view.findViewById(R.id.tv_wind_deg);//风向360角度
        TextView tv_wind_dir = view.findViewById(R.id.tv_wind_dir);//风向
        TextView tv_wind_sc = view.findViewById(R.id.tv_wind_sc);//风力
        TextView tv_wind_spd = view.findViewById(R.id.tv_wind_spd);//风速
        TextView tv_hum = view.findViewById(R.id.tv_hum);//相对湿度
        TextView tv_pres = view.findViewById(R.id.tv_pres);//大气压强
        TextView tv_pop = view.findViewById(R.id.tv_pop);//降水概率
        TextView tv_dew = view.findViewById(R.id.tv_dew);//露点温度
        TextView tv_cloud = view.findViewById(R.id.tv_cloud);//云量

        String time = DateUtils.updateTime(data.getFxTime());

        tv_time.setText(WeatherUtil.showTimeInfo(time) + time);
        tv_tem.setText(data.getTemp() + "℃");
        tv_cond_txt.setText(data.getText());
        tv_wind_deg.setText(data.getWind360() + "°");
        tv_wind_dir.setText(data.getWindDir());
        tv_wind_sc.setText(data.getWindScale() + "级");
        tv_wind_spd.setText(data.getWindSpeed() + "公里/小时");
        tv_hum.setText(data.getHumidity() + "%");
        tv_pres.setText(data.getPressure() + "hPa");
        tv_pop.setText(data.getPop() + "%");
        tv_dew.setText(data.getDew() + "℃");
        tv_cloud.setText(data.getCloud() + "%");
        liWindow.showCenterPopupWindow(view, SizeUtils.dp2px(context, 300), SizeUtils.dp2px(context, 400), true);
    }

    //定位
    private void startLocation() {
        try {
            LocationClient.class.getMethod("setAgreePrivacy", boolean.class).invoke(null, true);
        } catch (ReflectiveOperationException ignored) {
        }
        mLocationClient = new LocationClient(this);
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();

        option.setIsNeedAddress(true);
        option.setNeedNewVersionRgc(true);
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient.setLocOption(option);
        //启动定位
        mLocationClient.start();

    }


    /**
     * 城市弹窗
     */
    private void showCityWindow() {
        provinceList = new ArrayList<>();
        citylist = new ArrayList<>();
        arealist = new ArrayList<>();
        list = new ArrayList<>();
        liWindow = new LiWindow(context);
        final View view = LayoutInflater.from(context).inflate(R.layout.window_city_list, null);
        ImageView areaBack = (ImageView) view.findViewById(R.id.iv_back_area);
        ImageView cityBack = (ImageView) view.findViewById(R.id.iv_back_city);
        TextView windowTitle = (TextView) view.findViewById(R.id.tv_title);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rv);
        liWindow.showRightPopupWindow(view);//显示弹窗
        initCityData(recyclerView, areaBack, cityBack, windowTitle);//加载城市列表数据
    }

    /**
     * 省市县数据渲染
     *
     * @param recyclerView 列表
     * @param areaBack     区县返回
     * @param cityBack     市返回
     * @param windowTitle  窗口标题
     */
    private void initCityData(RecyclerView recyclerView, ImageView areaBack, ImageView cityBack, TextView windowTitle) {
        //初始化省数据 读取省数据并显示到列表中
        try {
            //读取城市数据
            InputStream inputStream = getResources().getAssets().open("City.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer stringBuffer = new StringBuffer();
            String lines = bufferedReader.readLine();
            while (lines != null) {
                stringBuffer.append(lines);
                lines = bufferedReader.readLine();
            }

            final JSONArray Data = new JSONArray(stringBuffer.toString());
            //循环这个文件数组、获取数组中每个省对象的名字
            for (int i = 0; i < Data.length(); i++) {
                JSONObject provinceJsonObject = Data.getJSONObject(i);
                String provinceName = provinceJsonObject.getString("name");
                CityResponse response = new CityResponse();
                response.setName(provinceName);
                provinceList.add(response);
            }

            //定义省份显示适配器
            provinceAdapter = new ProvinceAdapter(R.layout.item_city_list, provinceList);
            LinearLayoutManager manager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(provinceAdapter);
            provinceAdapter.notifyDataSetChanged();
            //runLayoutAnimationRight(recyclerView);//动画展示
            provinceAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                @Override
                public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                    try {
                        //返回上一级数据
                        cityBack.setVisibility(View.VISIBLE);
                        cityBack.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                recyclerView.setAdapter(provinceAdapter);
                                provinceAdapter.notifyDataSetChanged();
                                cityBack.setVisibility(View.GONE);
                                windowTitle.setText("中国");
                            }
                        });

                        //根据当前位置的省份所在的数组位置、获取城市的数组
                        JSONObject provinceObject = Data.getJSONObject(position);
                        windowTitle.setText(provinceList.get(position).getName());
                        provinceTitle = provinceList.get(position).getName();
                        final JSONArray cityArray = provinceObject.getJSONArray("city");

                        //更新列表数据
                        if (citylist != null) {
                            citylist.clear();
                        }

                        for (int i = 0; i < cityArray.length(); i++) {
                            JSONObject cityObj = cityArray.getJSONObject(i);
                            String cityName = cityObj.getString("name");
                            CityResponse.CityBean response = new CityResponse.CityBean();
                            response.setName(cityName);
                            citylist.add(response);
                        }

                        cityAdapter = new CityAdapter(R.layout.item_city_list, citylist);
                        LinearLayoutManager manager1 = new LinearLayoutManager(context);
                        recyclerView.setLayoutManager(manager1);
                        recyclerView.setAdapter(cityAdapter);
                        cityAdapter.notifyDataSetChanged();
                        //runLayoutAnimationRight(recyclerView);

                        cityAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                            @Override
                            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                                try {
                                    //返回上一级数据
                                    areaBack.setVisibility(View.VISIBLE);
                                    areaBack.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            recyclerView.setAdapter(cityAdapter);
                                            cityAdapter.notifyDataSetChanged();
                                            areaBack.setVisibility(View.GONE);
                                            windowTitle.setText(provinceTitle);
                                            arealist.clear();
                                        }
                                    });
                                    //获取区/县的上级 市，用于请求空气质量数据API接口
                                    city = citylist.get(position).getName();
                                    //根据当前城市数组位置 获取地区数据
                                    windowTitle.setText(citylist.get(position).getName());
                                    JSONObject cityJsonObj = cityArray.getJSONObject(position);
                                    JSONArray areaJsonArray = cityJsonObj.getJSONArray("area");
                                    if (arealist != null) {
                                        arealist.clear();
                                    }
                                    if (list != null) {
                                        list.clear();
                                    }
                                    for (int i = 0; i < areaJsonArray.length(); i++) {
                                        list.add(areaJsonArray.getString(i));
                                    }
                                    Log.i("list", list.toString());
                                    for (int j = 0; j < list.size(); j++) {
                                        CityResponse.CityBean.AreaBean response = new CityResponse.CityBean.AreaBean();
                                        response.setName(list.get(j).toString());
                                        arealist.add(response);
                                    }
                                    areaAdapter = new AreaAdapter(R.layout.item_city_list, arealist);
                                    LinearLayoutManager manager2 = new LinearLayoutManager(context);

                                    recyclerView.setLayoutManager(manager2);
                                    recyclerView.setAdapter(areaAdapter);
                                    areaAdapter.notifyDataSetChanged();
                                    runLayoutAnimationRight(recyclerView);

                                    areaAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                                        @Override
                                        public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                                            showLoadingDialog();
                                            district = arealist.get(position).getName();//选中的区/县赋值给这个全局变量

                                            //V7版本中需要先获取到城市ID ,在结果返回值中再进行下一步的数据查询
                                            mPresent.newSearchCity(district);//切换城市时

                                            flag = false;//切换城市得到的城市不属于定位，因此这里隐藏定位图标
                                            liWindow.closePopupWindow();//关闭弹窗
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    /**
     * 滑动监听
     *
     * @param v          滑动视图本身
     * @param scrollX    滑动后的X轴位置
     * @param scrollY    滑动后的Y轴位置
     * @param oldScrollX 之前的X轴位置
     * @param oldScrollY 之前的Y轴位置
     */
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (scrollY > oldScrollY) {
            Log.e("onScroll", "上滑");
            //laySlideArea.getMeasuredHeight() 表示控件的绘制高度
            if (scrollY > laySlideArea.getMeasuredHeight()) {
                String tx = tvCity.getText().toString();
                if (tx.contains("定位中")) {
                    //因为存在网络异常问题，总不能你没有城市，还给你改变UI吧
                    tvTitle.setText("城市天气");
                } else {
                    //改变TextView的显示文本
                    tvTitle.setText(tx);
                }
            }
        }
        if (scrollY < oldScrollY) {
            Log.e("onScroll", "下滑");
            if (scrollY < laySlideArea.getMeasuredHeight()) {
                //改回原来的
                tvTitle.setText("城市天气");
            }
        }
    }

    /**
     * 添加点击事件
     *
     * @param view 控件
     */
    @OnClick({R.id.iv_map, R.id.iv_add, R.id.tv_warn, R.id.tv_city, R.id.tv_more_daily,
            R.id.tv_more_air, R.id.tv_more_lifestyle, R.id.tv_prec_more})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_map://地图天气
                startActivity(new Intent(context, MapWeatherActivity.class));
                break;
            case R.id.iv_add://更多功能弹窗
                showAddWindow();//更多功能弹窗
                toggleBright();//计算动画时间
                break;
            case R.id.tv_warn://灾害预警，不一定当前城市就有这个预警，如果有预警的话就可以进入查看详情
                SPUtils.putBoolean(Constant.FLAG_OTHER_RETURN, false, context);//onViewCli缓存标识
                Intent intent = new Intent(context, WarnActivity.class);
                intent.putExtra("warnBodyString", warnBodyString);
                startActivity(intent);
                break;
            case R.id.tv_city://当用户没有打开GPS定位时，则可以点击这个TextView进行重新定位
                if (isOpenLocationServiceEnable()) {//已开启定位
                    tvCity.setText("定位中");
                    startLocation();//开始定位
                } else {//未开启
                    //跳转到系统定位设置
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), OPEN_LOCATION);
                }
                break;
            case R.id.tv_more_daily://更多天气预报
                goToMore(MoreDailyActivity.class);
                break;
            case R.id.tv_more_air://更多空气质量信息
                goToMore(MoreAirActivity.class);
                break;
            case R.id.tv_more_lifestyle://更多生活建议
                goToMore(MoreLifestyleActivity.class);
                break;
            case R.id.tv_prec_more://分钟级降水详情
                if (state) {//收缩
                    AnimationUtil.collapse(rvPrecDetail, tvPrecMore);
                    state = false;
                } else {//展开
                    AnimationUtil.expand(rvPrecDetail, tvPrecMore);
                    state = true;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 进入更多数据页面
     *
     * @param clazz 要进入的页面
     */
    private void goToMore(Class<?> clazz) {
        if (locationId == null) {
            ToastUtils.showShortToast(context, "很抱歉，未获取到相关更多信息");
        } else {
            Intent intent = new Intent(context, clazz);
            intent.putExtra("locationId", locationId);
            intent.putExtra("stationName", stationName);//只要locationId不为空，则cityName不会为空,只判断一次即可
            intent.putExtra("cityName", tvCity.getText().toString());
            intent.putExtra("latitude", lat);
            intent.putExtra("longitude", lon);
            //详细界面顶部“当前天气概要”数据（显示界面的全部关键信息）
            intent.putExtra("nowTemp", tvTemperature.getText().toString());//当前温度
            intent.putExtra("nowInfo", tvInfo.getText().toString());//天气状况文字
            intent.putExtra("nowIcon", nowIconCode);//天气图标代码
            intent.putExtra("tempHigh", tvTempHeight.getText().toString());//最高温
            intent.putExtra("tempLow", tvTempLow.getText().toString());//最低温
            intent.putExtra("windDir", tvWindDirection.getText().toString());//风向
            intent.putExtra("windPower", tvWindPower.getText().toString());//风力
            //天气数据更新时间（当前时间由详细界面自己的时钟实时更新）
            if (!TextUtils.isEmpty(tvOldTime.getText())) {
                intent.putExtra("updateTime", tvOldTime.getText().toString());
            }
            startActivity(intent);
        }
    }

    /**
     * 百度 167 等失败时，用系统 GPS/网络坐标走和风 Geo（不依赖百度服务端）
     */
    @SuppressLint("MissingPermission")
    private void fallbackSystemLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            tvCity.setText("重新定位");
            tvCity.setEnabled(true);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        if (lm != null) {
            Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gps != null) {
                best = gps;
            }
            if (net != null && (best == null || net.getTime() > best.getTime())) {
                best = net;
            }
        }
        if (best != null) {
            queryWeatherByCoordinates(best.getLongitude(), best.getLatitude());
            return;
        }
        if (lm == null) {
            tvCity.setText("重新定位");
            tvCity.setEnabled(true);
            return;
        }
        ToastUtils.showShortToast(context, "百度定位不可用，正在使用系统 GPS…");
        try {
            lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    lm.removeUpdates(this);
                    queryWeatherByCoordinates(location.getLongitude(), location.getLatitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            }, null);
        } catch (Exception e) {
            tvCity.setText("重新定位");
            tvCity.setEnabled(true);
            ToastUtils.showShortToast(context, "系统定位也失败，请使用搜索城市");
        }
    }

    private void queryWeatherByCoordinates(double longitude, double latitude) {
        if (mLocationClient != null) {
            mLocationClient.stop();
        }
        tvCity.setEnabled(false);
        showLoadingDialog();
        flag = true;
        String geo = String.format(Locale.US, "%.4f,%.4f", longitude, latitude);
        mPresent.newSearchCityGlobal(geo);
        refresh.setOnRefreshListener(refreshLayout -> mPresent.newSearchCityGlobal(geo));
    }

    private static boolean isInChina(double lat, double lon) {
        return lat >= 18 && lat <= 54 && lon >= 73 && lon <= 135;
    }

    private static boolean looksLikeOverseasPlace(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        return name.contains("Mountain View") || name.matches(".*[A-Za-z].*");
    }

    private void refreshHomeLocationIfStale() {
        CharSequence shown = tvCity.getText();
        if (shown == null) {
            return;
        }
        String text = shown.toString();
        if (text.contains("Mountain View") || text.contains("定位中")) {
            if (isOpenLocationServiceEnable()) {
                tvCity.setText("定位中");
                startLocation();
            }
        }
    }

    /**
     * 定位结果返回
     */
    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                fallbackSystemLocation();
                return;
            }

            district = location.getDistrict();//获取区/县
            city = location.getCity();//获取市
            if (TextUtils.isEmpty(district)) {
                district = city;
            }

            double lat = location.getLatitude();
            double lon = location.getLongitude();
            if (isInChina(lat, lon)) {
                queryWeatherByCoordinates(lon, lat);
                return;
            }
            if (TextUtils.isEmpty(district) || looksLikeOverseasPlace(district) || looksLikeOverseasPlace(city)) {
                fallbackSystemLocation();
                return;
            }
            tvCity.setEnabled(false);//不可点击
            showLoadingDialog();
            mPresent.newSearchCity(district);//定位返回时
            refresh.setOnRefreshListener(refreshLayout -> mPresent.newSearchCity(district));
        }
    }

    /**
     * 天气预报数据访问异常返回
     */
    @Override
    public void getWeatherDataFailed() {
        refresh.finishRefresh();//关闭刷新
        dismissLoadingDialog();//关闭弹窗
        // 实况/预报已成功时，分钟降水、预警等附加接口失败不再弹窗（免费额度或未开通很常见）
        CharSequence temp = tvTemperature.getText();
        boolean hasCoreWeather = temp != null && temp.length() > 0 && !"0".equals(temp.toString().trim());
        if (!hasCoreWeather) {
            ToastUtils.showShortToast(context,
                    "天气接口失败：请检查 QWEATHER_API_HOST 与 API_KEY（和风控制台）");
        }
    }

    private void ensureWeatherLoaded() {
        if (locationId != null) {
            return;
        }
        ToastUtils.showShortToast(context, "定位超时，已尝试加载默认城市北京");
        loadDefaultCityWeather();
    }

    private void loadDefaultCityWeather() {
        fetchWeatherForLocationId("101010100", "北京", "116.41", "39.92");
    }

    private void fetchWeatherForLocationId(String id, String cityName, String longitude, String latitude) {
        tvCity.setEnabled(false);
        tvCity.setText(cityName);
        SPUtils.putString(Constant.CURRENT_WEATHER_CITY, cityName, context);
        locationId = id;
        stationName = cityName;
        lon = longitude;
        lat = latitude;
        flag = false;
        showLoadingDialog();
        mPresent.nowWeather(locationId);
        mPresent.dailyWeather(locationId);
        mPresent.hourlyWeather(locationId);
        if (lat != null && lon != null) {
            mPresent.airNowWeather(lat, lon);
        }
        mPresent.lifestyle(locationId);
        mPresent.nowWarn(locationId);
        mPresent.getMinutePrec(lon + "," + lat);
        new Handler(Looper.getMainLooper()).postDelayed(this::dismissLoadingDialog, 12000);
    }

    /**
     * 和风天气  V7  API
     * 通过定位到的地址 /  城市切换得到的地址  都需要查询出对应的城市id才行，所以在V7版本中，这是第一步接口
     *
     * @param response
     */
    @Override
    public void getNewSearchCityResult(Response<NewSearchCityResponse> response) {
        refresh.finishRefresh();//关闭刷新
        if (mLocationClient != null) {
            mLocationClient.stop();//数据返回后关闭定位
        }
        if (response.body() == null || response.body().getCode() == null) {
            tvCity.setText("查询城市失败");
            ToastUtils.showShortToast(context, "和风 Geo 无响应，请配置 QWEATHER_API_HOST");
            loadDefaultCityWeather();
            return;
        }
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            if (response.body().getLocation() != null && response.body().getLocation().size() > 0) {
                NewSearchCityResponse.LocationBean locationBean = response.body().getLocation().get(0);
                fetchWeatherForLocationId(
                        locationBean.getId(),
                        locationBean.getName(),
                        locationBean.getLon(),
                        locationBean.getLat());
                flag = true;
            } else {
                ToastUtils.showShortToast(context, "城市数据为空，已加载北京");
                loadDefaultCityWeather();
            }
        } else {
            tvCity.setText("查询城市失败");
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
            loadDefaultCityWeather();
        }
    }

    /**
     * 实况天气数据返回  V7
     *
     * @param response
     */
    @Override
    public void getNowResult(Response<NowResponse> response) {
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {//200则成功返回数据
            //根据V7版本的原则，只要是200就一定有数据，我们可以不用做判空处理，但是，为了使程序不ANR，还是要做的，信自己得永生
            NowResponse data = response.body();
            if (data != null) {
                Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
                tvTemperature.setText(data.getNow().getTemp());//温度

                dialogTemp = data.getNow().getTemp();
                dialogWeatherState = data.getNow().getText();
                dialogWeatherStateCode = Integer.parseInt(data.getNow().getIcon());

                tvTemperature.setTypeface(typeface);//使用字体
                if (flag) {
                    ivLocation.setVisibility(View.VISIBLE);//显示定位图标
                } else {
                    ivLocation.setVisibility(View.GONE);//显示定位图标
                }
                tvWeek.setText(DateUtils.getDateAndWeek(new Date()));//日期 + 星期
                tvInfo.setText(data.getNow().getText());//天气状况

                //当前天气“图片”：根据实况图标代码设置天气图标
                nowIconCode = data.getNow().getIcon();
                ivNowIcon.setImageDrawable(null);
                WeatherUtil.changeIcon(ivNowIcon, Integer.parseInt(nowIconCode.trim()));

                String time = DateUtils.updateTime(data.getUpdateTime());//截去前面的字符，保留后面所有的字符，就剩下 22:00

                tvOldTime.setText("数据更新时间：" + time);
                tvWindDirection.setText("风向     " + data.getNow().getWindDir());//风向
                tvWindPower.setText("风力     " + data.getNow().getWindScale() + "级");//风力
                wwBig.startRotate();//大风车开始转动
                wwSmall.startRotate();//小风车开始转动
                dismissLoadingDialog();// 核心数据已到，关闭 Loading（分钟降水等附加接口可能失败）
            } else {
                ToastUtils.showShortToast(context, "暂无实况天气数据");
                dismissLoadingDialog();
            }
        } else {//其他状态返回提示文字
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
            dismissLoadingDialog();
        }
    }

    /**
     * 天气预报数据返回  V7
     *
     * @param response
     */
    @Override
    public void getDailyResult(Response<DailyResponse> response) {
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            List<DailyResponse.DailyBean> data = response.body().getDaily();
            //判空处理
            if (data != null && data.size() > 0) {
                tvTempHeight.setText(data.get(0).getTempMax() + "℃");
                tvTempLow.setText(" / " + data.get(0).getTempMin() + "℃");

                dialogTempHeight = Integer.parseInt(data.get(0).getTempMax());//弹窗今日最高温
                dialogTempLow = Integer.parseInt(data.get(0).getTempMin());//弹窗今日最低温

                //添加数据之前先清除
                dailyListV7.clear();
                //添加数据
                dailyListV7.addAll(data);
                //刷新列表
                mAdapterDailyV7.notifyDataSetChanged();
                //底部动画展示
                runLayoutAnimation(rv);
            } else {
                ToastUtils.showShortToast(context, "天气预报数据为空");
            }
        } else {//异常状态码返回
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
        }
    }

    /**
     * 逐小时天气数据返回  V7
     *
     * @param response
     */
    @Override
    public void getHourlyResult(Response<HourlyResponse> response) {
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            List<HourlyResponse.HourlyBean> data = response.body().getHourly();
            if (data != null && data.size() > 0) {
                hourlyListV7.clear();
                hourlyListV7.addAll(data);
                mAdapterHourlyV7.notifyDataSetChanged();
                runLayoutAnimationRight(rvHourly);
            } else {
                ToastUtils.showShortToast(context, "逐小时预报数据为空");
            }
        } else {
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
        }
    }

    @Override
    public void getAirNowResult(Response<AirQualityCurrentResponse> response) {
        AirQualityData.Current data = AirQualityMapper.mapCurrent(response.body());
        if (data == null) {
            return;
        }
        rpbAqi.setMaxProgress(300);
        rpbAqi.setMinText("0");
        rpbAqi.setMinTextSize(32f);
        rpbAqi.setMaxText("300");
        rpbAqi.setMaxTextSize(32f);
        rpbAqi.setProgress(data.aqi);
        rpbAqi.setArcBgColor(getColor(R.color.arc_bg_color));
        rpbAqi.setProgressColor(getColor(R.color.arc_progress_color));
        rpbAqi.setFirstText(data.category);
        rpbAqi.setFirstTextSize(44f);
        rpbAqi.setSecondText(data.aqiDisplay);
        rpbAqi.setSecondTextSize(64f);
        rpbAqi.setMinTextColor(getColor(R.color.arc_progress_color));

        tvAirInfo.setText("空气" + data.category);
        tvPm10.setText(data.pollutant("pm10").display);
        tvPm25.setText(data.pollutant("pm2p5").display);
        tvNo2.setText(data.pollutant("no2").display);
        tvSo2.setText(data.pollutant("so2").display);
        tvO3.setText(data.pollutant("o3").display);
        tvCo.setText(data.pollutant("co").display);
    }

    /**
     * 生活数据返回 V7
     *
     * @param response
     */
    @Override
    public void getLifestyleResult(Response<LifestyleResponse> response) {
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            List<LifestyleResponse.DailyBean> data = response.body().getDaily();
            for (int i = 0; i < data.size(); i++) {
                switch (data.get(i).getType()) {
                    case "5":
                        tvUv.setText("紫外线：" + data.get(i).getText());
                        break;
                    case "8":
                        tvComf.setText("舒适度：" + data.get(i).getText());
                        break;
                    case "3":
                        tvDrsg.setText("穿衣指数：" + data.get(i).getText());
                        break;
                    case "9":
                        tvFlu.setText("感冒指数：" + data.get(i).getText());
                        break;
                    case "1":
                        tvSport.setText("运动指数：" + data.get(i).getText());
                        break;
                    case "6":
                        tvTrav.setText("旅游指数：" + data.get(i).getText());
                        break;
                    case "2":
                        tvCw.setText("洗车指数：" + data.get(i).getText());
                        break;
                    case "10":
                        tvAir.setText("空气指数：" + data.get(i).getText());
                        break;
                    default:
                        break;
                }
            }
        } else {
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
        }
    }

    /**
     * 灾害预警返回
     *
     * @param response
     */
    @Override
    public void getNowWarnResult(Response<WarningResponse> response) {
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            List<WarningResponse.WarningBean> data = response.body().getWarning();
            if (data != null && data.size() > 0) {
                warnBodyString = new Gson().toJson(response.body());
                tvWarn.setText(data.get(0).getTitle() + "   " + data.get(0).getText());//设置滚动标题和内容
            } else {//没有该城市预警有隐藏掉这个TextView
                tvWarn.setVisibility(View.GONE);
            }

        } else {
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
        }
    }

    /**
     * 分钟级降水返回
     *
     * @param response
     */
    @Override
    public void getMinutePrecResult(Response<MinutePrecResponse> response) {
        dismissLoadingDialog();//关闭加载弹窗
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            tvPrecipitation.setText(response.body().getSummary());
            dialogPrecipitation = response.body().getSummary();//弹窗降水预告
            if (response.body().getMinutely() != null && response.body().getMinutely().size() > 0) {
                minutelyList.clear();
                minutelyList.addAll(response.body().getMinutely());
                mAdapterMinutePrec.notifyDataSetChanged();


                checkAppVersion();//检查版本信息

            } else {
                ToastUtils.showShortToast(context, "分钟级降水数据为空");
            }
        } else {
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
        }
    }

    /**
     * 设置每日弹窗
     */
    private void setTipDialog() {
        boolean isShow = SPUtils.getBoolean(Constant.EVERYDAY_POP_BOOLEAN, true, context);
        if (isShow) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //当所有数据加载完成之后显示弹窗
                    if (everyDayTipDialog != null) {
                        return;
                    }
                    //弹出每日提醒弹窗
                    showEveryDayTipDialog();
                }
            },1000);
        }
    }


    //数据请求失败返回
    @Override
    public void getDataFailed() {
        refresh.finishRefresh();//关闭刷新
        dismissLoadingDialog();//关闭弹窗
//        ToastUtils.showShortToast(context, "网络异常");//这里的context是框架中封装好的，等同于this
    }


    /**
     * 返回Activity的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == OPEN_LOCATION) {//则是从手机定位页面返回
            if (isOpenLocationServiceEnable()) {//已打开
                tvCity.setText("重新定位");
                tvCity.setEnabled(true);//可以点击
            } else {
                ToastUtils.showShortToast(context, "有意思吗？你跳过去又不打开定位，玩呢？嗯？我也是有脾气的好伐！");
                tvCity.setText("打开定位");
                tvCity.setEnabled(false);//不可点击
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 页面销毁时
     */
    @Override
    public void onDestroy() {
        stopClock();//移除实时时钟回调
        stopAutoRefresh();//移除定时刷新回调
        wwBig.stop();//停止大风车
        wwSmall.stop();//停止小风车
        EventBus.getDefault().unregister(this);//解注
        super.onDestroy();
    }

    /**
     * 更多功能弹窗，因为区别于我原先写的弹窗
     */
    private void showAddWindow() {
        // 设置布局文件
        mPopupWindow.setContentView(LayoutInflater.from(this).inflate(R.layout.window_add, null));// 为了避免部分机型不显示，我们需要重新设置一下宽高
        mPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x0000));// 设置pop透明效果
        mPopupWindow.setAnimationStyle(R.style.pop_add);// 设置pop出入动画
        mPopupWindow.setFocusable(true);// 设置pop获取焦点，如果为false点击返回按钮会退出当前Activity，如果pop中有Editor的话，focusable必须要为true
        mPopupWindow.setTouchable(true);// 设置pop可点击，为false点击事件无效，默认为true
        mPopupWindow.setOutsideTouchable(true);// 设置点击pop外侧消失，默认为false；在focusable为true时点击外侧始终消失
        mPopupWindow.showAsDropDown(ivAdd, -100, 0);// 相对于 + 号正下面，同时可以设置偏移量
        // 设置pop关闭监听，用于改变背景透明度
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {//关闭弹窗
            @Override
            public void onDismiss() {
                toggleBright();
            }
        });
        //绑定布局中的控件
        TextView changeCity = mPopupWindow.getContentView().findViewById(R.id.tv_change_city);//切换城市
        TextView wallpaper = mPopupWindow.getContentView().findViewById(R.id.tv_wallpaper);//壁纸管理  我才是这条Gai最靓的仔
        TextView searchCity = mPopupWindow.getContentView().findViewById(R.id.tv_search_city);//城市搜索
        TextView worldCity = mPopupWindow.getContentView().findViewById(R.id.tv_world_city);//世界城市  V7
        TextView residentCity = mPopupWindow.getContentView().findViewById(R.id.tv_resident_city);//常用城市
        TextView aboutUs = mPopupWindow.getContentView().findViewById(R.id.tv_about_us);//关于我们
        TextView setting = mPopupWindow.getContentView().findViewById(R.id.tv_setting);//应用设置
        changeCity.setOnClickListener(view -> {//切换城市
            showCityWindow();
            mPopupWindow.dismiss();
        });
        wallpaper.setOnClickListener(view -> {//壁纸管理
            startActivity(new Intent(context, WallPaperActivity.class));
            mPopupWindow.dismiss();
        });
        searchCity.setOnClickListener(view -> {//城市搜索
            SPUtils.putBoolean(Constant.FLAG_OTHER_RETURN, false, context);//缓存标识
            startActivity(new Intent(context, SearchCityActivity.class));
            mPopupWindow.dismiss();
        });
        worldCity.setOnClickListener(view -> {//世界城市
            startActivity(new Intent(context, WorldCityActivity.class));
            mPopupWindow.dismiss();
        });
        residentCity.setOnClickListener(view -> {//常用城市
            SPUtils.putBoolean(Constant.FLAG_OTHER_RETURN, false, context);//缓存标识
            startActivity(new Intent(context, CommonlyUsedCityActivity.class));
            mPopupWindow.dismiss();
        });
        aboutUs.setOnClickListener(view -> {//关于我们
            startActivity(new Intent(context, AboutUsActivity.class));
            mPopupWindow.dismiss();
        });
        setting.setOnClickListener(view -> {//应用设置
            startActivity(new Intent(context, SettingActivity.class));
            mPopupWindow.dismiss();
        });
    }

    /**
     * 计算动画时间
     */
    private void toggleBright() {
        // 三个参数分别为：起始值 结束值 时长，那么整个动画回调过来的值就是从0.5f--1f的
        animUtil.setValueAnimator(START_ALPHA, END_ALPHA, DURATION);
        animUtil.addUpdateListener(new AnimationUtil.UpdateListener() {
            @Override
            public void progress(float progress) {
                // 此处系统会根据上述三个值，计算每次回调的值是多少，我们根据这个值来改变透明度
                bgAlpha = bright ? progress : (START_ALPHA + END_ALPHA - progress);
                backgroundAlpha(bgAlpha);
            }
        });
        animUtil.addEndListner(new AnimationUtil.EndListener() {
            @Override
            public void endUpdate(Animator animator) {
                // 在一次动画结束的时候，翻转状态
                bright = !bright;
            }
        });
        animUtil.startAnimator();
    }

    /**
     * 此方法用于改变背景的透明度，从而达到“变暗”的效果
     */
    private void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        // 0.0-1.0
        lp.alpha = bgAlpha;
        getWindow().setAttributes(lp);
        // everything behind this window will be dimmed.
        // 此方法用来设置浮动层，防止部分手机变暗无效
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private long timeMillis;//几点时间

    /**
     * 增加一个退出应用的提示
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - timeMillis) > 2000) {
                ToastUtils.showShortToast(context, "再按一次退出GoodWeather");
                timeMillis = System.currentTimeMillis();
            } else {
                WeatherApplication.getActivityManager().finishAll();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
