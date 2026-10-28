package com.cdy.superweather;

import android.Manifest;
import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.cdy.mvplibrary.utils.LiWindow;
import com.cdy.mvplibrary.utils.ObjectUtils;
import com.cdy.mvplibrary.view.WhiteWindmills;
import com.cdy.superweather.adapter.AreaAdapter;
import com.cdy.superweather.adapter.CityAdapter;
import com.cdy.superweather.adapter.ProvinceAdapter;
import com.cdy.superweather.adapter.WeatherForecastAdapter;
import com.cdy.superweather.bean.CityResponse;
import com.cdy.superweather.bean.LifeStyleResponse;
import com.cdy.superweather.bean.TodayResponse;
import com.cdy.superweather.bean.WeatherForecastResponse;
import com.cdy.superweather.contract.WeatherContract;
import com.cdy.superweather.utils.StatusBarUtil;
import com.cdy.superweather.utils.ToastUtils;
import com.cdy.mvplibrary.mvp.MvpActivity;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;

import static com.cdy.mvplibrary.utils.RecyclerViewAnimation.runLayoutAnimationRight;

public class MainActivity extends MvpActivity<WeatherContract.WeatherPresenter> implements WeatherContract.IWeatherView {

    @BindView(R.id.tv_info)
    TextView tvInfo;//天气状况
    @BindView(R.id.tv_temperature)
    TextView tvTemperature;//温度
    @BindView(R.id.tv_low_height)
    TextView tvLowHeight;//最高温和最低温
    @BindView(R.id.tv_city)
    TextView tvCity;//城市
    @BindView(R.id.tv_old_time)
    TextView tvOldTime;//最近更新时间
    @BindView(R.id.rv)
    RecyclerView rv;//天气预报显示列表
    @BindView(R.id.tv_comf)
    TextView tvComf;//舒适度
    @BindView(R.id.tv_trav)
    TextView tvTrav;//旅游指数
    @BindView(R.id.tv_sport)
    TextView tvSport;//运动指数
    @BindView(R.id.tv_air)
    TextView tvAir;//空气指数
    @BindView(R.id.tv_drsg)
    TextView tvDrsg;//穿衣指数
    @BindView(R.id.tv_flu)
    TextView tvFlu;//感冒指数
    @BindView(R.id.tv_cw)
    TextView tvCw;//洗车指数
    @BindView(R.id.ww_big)
    WhiteWindmills wwBig;//大风车
    @BindView(R.id.ww_small)
    WhiteWindmills wwSmall;//小风车
    @BindView(R.id.tv_wind_direction)
    TextView tvWindDirection;//风向
    @BindView(R.id.tv_wind_power)
    TextView tvWindPower;//风力
    @BindView(R.id.iv_city_select)
    ImageView ivCitySelect;//城市图标ID
    private RxPermissions rxPermissions;//权限请求框架
    //定位器
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    //标记是否已经根据定位发起过天气请求，避免百度定位与系统定位兜底重复请求
    private boolean weatherRequested = false;



    List<WeatherForecastResponse.HeWeather6Bean.DailyForecastBean> mList;//初始化数据源
    WeatherForecastAdapter mAdapter;//初始化适配器
    /**
     * 初始化天气预报数据列表
     */
    private void initList() {
        mList = new ArrayList<>();//声明为ArrayList
        mAdapter = new WeatherForecastAdapter(R.layout.item_weather_forecast_list, mList);//为适配器设置布局和数据源
        LinearLayoutManager manager = new LinearLayoutManager(context);//布局管理,默认是纵向
        rv.setLayoutManager(manager);//为列表配置管理器
        rv.setAdapter(mAdapter);//为列表配置适配器
    }

    private List<String> list;//字符串列表
    private List<CityResponse> provinceList;//省列表数据
    private List<CityResponse.CityBean> citylist;//市列表数据
    private List<CityResponse.CityBean.AreaBean> arealist;//区/县列表数据
    ProvinceAdapter provinceAdapter;//省数据适配器
    CityAdapter cityAdapter;//市数据适配器
    AreaAdapter areaAdapter;//县/区数据适配器
    String provinceTitle;//标题
    LiWindow liWindow;//自定义弹窗

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
        liWindow.showRightPopupWindow(view);
        initCityData(recyclerView,areaBack,cityBack,windowTitle);//加载城市列表数据
    }

    /**
     * 省市县数据渲染
     * @param recyclerView  列表
     * @param areaBack 区县返回
     * @param cityBack 市返回
     * @param windowTitle  窗口标题
     */
    private void initCityData(RecyclerView recyclerView,ImageView areaBack,ImageView cityBack,TextView windowTitle) {
        //初始化省数据 读取省数据并显示到列表中
        try {
            InputStream inputStream = getResources().getAssets().open("City.txt");//读取数据
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
            runLayoutAnimationRight(recyclerView);//动画展示
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
                        runLayoutAnimationRight(recyclerView);

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
                                    //根据当前城市数组位置 获取地区数据
                                    windowTitle.setText(citylist.get(position).getName());
                                    JSONObject cityJsonObj = cityArray.getJSONObject(position);
                                    JSONArray areaJsonArray = cityJsonObj.getJSONArray("area");
                                    if (arealist != null) {
                                        arealist.clear();
                                    }
                                    if(list != null){
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

                                            mPresent.todayWeather(context,arealist.get(position).getName());//今日天气
                                            mPresent.weatherForecast(context, arealist.get(position).getName());//天气预报
                                            mPresent.lifeStyle(context, arealist.get(position).getName());//生活指数
                                            liWindow.closePopupWindow();

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


    //点击事件
    @OnClick(R.id.iv_city_select)
    public void onViewClicked() {//显示城市弹窗
        showCityWindow();
    }



    //数据初始化  主线程，onCreate方法可以删除了，把里面的代码移动这个initData下面
    @Override
    public void initData(Bundle savedInstanceState) {
        //因为这个框架里面已经放入了绑定，所以这行代码可以注释掉了。
        //ButterKnife.bind(this);
        StatusBarUtil.transparencyBar(context);//透明状态栏
        initList();//天气预报列表初始化
        rxPermissions = new RxPermissions(this);//实例化这个权限请求框架，否则会报错
        permissionVersion();//权限判断
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

    //权限判断
    private void permissionVersion() {
        if (Build.VERSION.SDK_INT >= 23) {//6.0或6.0以上
            //动态权限申请
            permissionsRequest();
        } else {//6.0以下
            //发现只要权限在AndroidManifest.xml中注册过，均会认为该权限granted  提示一下即可
            ToastUtils.showShortToast(this, "你的版本在Android6.0以下，不需要动态申请权限。");
        }
    }

    //动态权限申请
    private void permissionsRequest() {//使用这个框架需要制定JDK版本，建议用1.8
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(granted -> {
                    if (granted) {//申请成功
                        //得到权限之后开始定位
                        startLocation();
                        //百度定位 AK 未配置/失效时不会回调，用系统定位兜底，保证天气数据可加载
                        startSystemLocationFallback();
                    } else {//申请失败
                        ToastUtils.showShortToast(this, "权限未开启");
                    }
                });
    }

    //定位
    private void startLocation() {
        //声明LocationClient类
        mLocationClient = new LocationClient(this);
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();

        //如果开发者需要获得当前点的地址信息，此处必须为true
        option.setIsNeedAddress(true);
        //可选，设置是否需要最新版本的地址信息。默认不需要，即参数为false
        option.setNeedNewVersionRgc(true);
        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient.setLocOption(option);
        //启动定位
        mLocationClient.start();

    }

    /**
     * 根据定位结果发起天气查询（仅第一次有效定位触发，避免百度定位与系统定位重复请求）
     *
     * @param location 区/县中文名，或 "经度,纬度"
     */
    private synchronized void requestWeatherByLocation(String location) {
        if (weatherRequested || location == null || location.trim().isEmpty()) {
            return;
        }
        weatherRequested = true;
        mPresent.todayWeather(context, location);//今日天气
        mPresent.weatherForecast(context, location);//天气预报
        mPresent.lifeStyle(context, location);//生活指数
    }

    /**
     * 系统定位兜底：百度定位在未配置有效 AK 时不会有任何回调，
     * 这里直接使用 Android 原生 LocationManager 获取最近一次已知位置
     * （模拟器 geo fix / 真机 GPS 都可用），拿到坐标后以 "经度,纬度" 发起天气请求，
     * 网络层会自动通过 GeoAPI 反查城市名。
     */
    private void startSystemLocationFallback() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return;
            }
            String[] providers = new String[]{
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    "fused"};
            //先取缓存的最近位置
            for (String provider : providers) {
                try {
                    if (!lm.isProviderEnabled(provider)) {
                        continue;
                    }
                    Location last = lm.getLastKnownLocation(provider);
                    if (last != null) {
                        requestWeatherByLocation(last.getLongitude() + "," + last.getLatitude());
                        return;
                    }
                } catch (SecurityException ignore) {
                }
            }
            //没有缓存位置则注册一次性更新
            final LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    requestWeatherByLocation(location.getLongitude() + "," + location.getLatitude());
                    try {
                        lm.removeUpdates(this);
                    } catch (Exception ignore) {
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
            for (String provider : providers) {
                try {
                    if (lm.isProviderEnabled(provider)) {
                        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper());
                    }
                } catch (SecurityException ignore) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 定位结果返回
     */
    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //优先使用百度反解出的区/县中文名；为空时退化为"经度,纬度"，交由网络层 geo 反查城市
            String district = location.getDistrict();
            if (district == null || district.trim().isEmpty()) {
                district = location.getLongitude() + "," + location.getLatitude();
            }
            requestWeatherByLocation(district);
        }
    }

    /**
     * 页面销毁时
     */
    @Override
    public void onDestroy() {
        wwBig.stop();//停止大风车
        wwSmall.stop();//停止小风车
        super.onDestroy();
    }

    //查询当天天气，请求成功后的数据返回
    @Override
    public void getTodayWeatherResult(Response<TodayResponse> response) {
        //数据返回后关闭定位
        mLocationClient.stop();
        if (response.body().getHeWeather6().get(0).getBasic() != null) {//得到数据不为空则进行数据显示
            //数据渲染显示出来
            tvTemperature.setText(response.body().getHeWeather6().get(0).getNow().getTmp());//温度
            tvCity.setText(response.body().getHeWeather6().get(0).getBasic().getLocation());//城市
            tvInfo.setText(response.body().getHeWeather6().get(0).getNow().getCond_txt());//天气状况
            tvOldTime.setText("上次更新时间：" + response.body().getHeWeather6().get(0).getUpdate().getLoc());
            tvWindDirection.setText("风向     " + response.body().getHeWeather6().get(0).getNow().getWind_dir());//风向
            tvWindPower.setText("风力     " + response.body().getHeWeather6().get(0).getNow().getWind_sc() + "级");//风力
            wwBig.startRotate();//大风车开始转动
            wwSmall.startRotate();//小风车开始转动

        } else {
            ToastUtils.showShortToast(context, response.body().getHeWeather6().get(0).getStatus());
        }
    }
    //查询天气预报，请求成功后的数据返回
    @Override
    public void getWeatherForecastResult(Response<WeatherForecastResponse> response) {
        if (("ok").equals(response.body().getHeWeather6().get(0).getStatus())) {
            //最低温和最高温
            tvLowHeight.setText(response.body().getHeWeather6().get(0).getDaily_forecast().get(0).getTmp_min() + " / " +
                    response.body().getHeWeather6().get(0).getDaily_forecast().get(0).getTmp_max() + "℃");

            if (response.body().getHeWeather6().get(0).getDaily_forecast() != null) {
                List<WeatherForecastResponse.HeWeather6Bean.DailyForecastBean> data
                        = response.body().getHeWeather6().get(0).getDaily_forecast();
                mList.clear();//添加数据之前先清除
                mList.addAll(data);//添加数据
                mAdapter.notifyDataSetChanged();//刷新列表
            } else {
                ToastUtils.showShortToast(context, "天气预报数据为空");
            }
        } else {
            ToastUtils.showShortToast(context, response.body().getHeWeather6().get(0).getStatus());
        }
    }

    //查询生活指数，请求成功后的数据返回
    @Override
    public void getLifeStyleResult(Response<LifeStyleResponse> response) {
        if(("ok").equals(response.body().getHeWeather6().get(0).getStatus())){
            List<LifeStyleResponse.HeWeather6Bean.LifestyleBean> data = response.body().getHeWeather6().get(0).getLifestyle();
            if(!ObjectUtils.isEmpty(data)){
                for (int i = 0;i<data.size();i++){
                    if(("comf").equals(data.get(i).getType())){
                        tvComf.setText("舒适度："+data.get(i).getTxt());
                    }else if(("drsg").equals(data.get(i).getType())){
                        tvDrsg.setText("穿衣指数："+data.get(i).getTxt());
                    }else if(("flu").equals(data.get(i).getType())){
                        tvFlu.setText("感冒指数："+data.get(i).getTxt());
                    }else if(("sport").equals(data.get(i).getType())){
                        tvSport.setText("运动指数："+data.get(i).getTxt());
                    }else if(("trav").equals(data.get(i).getType())){
                        tvTrav.setText("旅游指数："+data.get(i).getTxt());
                    }else if(("cw").equals(data.get(i).getType())){
                        tvCw.setText("洗车指数："+data.get(i).getTxt());
                    }else if(("air").equals(data.get(i).getType())){
                        tvAir.setText("空气指数："+data.get(i).getTxt());
                    }
                }

            }else {
                ToastUtils.showShortToast(context, "生活指数数据为空");
            }
        }else {
            ToastUtils.showShortToast(context, response.body().getHeWeather6().get(0).getStatus());
        }
    }


    //数据请求失败返回
    @Override
    public void getDataFailed() {
        ToastUtils.showShortToast(context,"网络异常");//这里的context是框架中封装好的，等同于this
    }

    //点击事件
//    @OnClick(R.id.iv_city_select)
//    public void onViewClicked(){
//
//    }
}

