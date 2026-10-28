package com.llw.goodweather.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.llw.goodweather.R;
import com.llw.goodweather.adapter.CommonlyCityAdapter;
import com.llw.goodweather.adapter.CommonlyCityAddAdapter;
import com.llw.goodweather.bean.NewSearchCityResponse;
import com.llw.goodweather.bean.SearchCityResponse;
import com.llw.goodweather.contract.SearchCityContract;
import com.llw.goodweather.eventbus.SearchCityEvent;
import com.llw.goodweather.utils.CodeToStringUtils;
import com.llw.goodweather.utils.Constant;
import com.llw.goodweather.utils.SPUtils;
import com.llw.goodweather.utils.StatusBarUtil;
import com.llw.goodweather.bean.ServerCitiesResponse;
import com.llw.goodweather.utils.ServerClient;
import com.llw.goodweather.utils.ToastUtils;
import com.llw.mvplibrary.bean.ResidentCity;
import com.llw.mvplibrary.mvp.MvpActivity;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 常用城市
 *
 * @author llw
 */
public class CommonlyUsedCityActivity extends MvpActivity<SearchCityContract.SearchCityPresenter>
        implements SearchCityContract.ISearchCityView {

    @BindView(R.id.edit_query)
    EditText editQuery;//输入框
    @BindView(R.id.iv_clear_search)
    ImageView ivClearSearch;//清除输入框内容的图标
    @BindView(R.id.toolbar)
    Toolbar toolbar;//标题控件
    @BindView(R.id.rv_commonly_used)
    RecyclerView rvCommonlyUsed;//常用城市列表
    @BindView(R.id.rv_search)
    RecyclerView rvSearch;//搜索城市列表
    @BindView(R.id.lay_normal)
    LinearLayout layNormal;//常用城市为空时展示的布局
    @BindView(R.id.tv_server_cities_title)
    TextView tvServerCitiesTitle;
    @BindView(R.id.rv_server_cities)
    RecyclerView rvServerCities;
    @BindView(R.id.tv_current_city)
    TextView tvCurrentCity;

    CommonlyCityAdapter mAdapter;//常用城市列表适配器
    List<NewSearchCityResponse.LocationBean> mList = new ArrayList<>();
    List<NewSearchCityResponse.LocationBean> mServerCityList = new ArrayList<>();
    CommonlyCityAddAdapter mAdapterAdd;//搜索城市列表适配器
    CommonlyCityAddAdapter mAdapterServer;//服务端推荐城市

    List<ResidentCity> cityList;//常用城市列表

    @Override
    public void initData(Bundle savedInstanceState) {
        StatusBarUtil.setStatusBarColor(context, R.color.white);//白色状态栏
        StatusBarUtil.StatusBarLightMode(context);//黑色字体
        Back(toolbar);

        updateCurrentCityHeader();

        initCityList();//初始化常用城市列表

        initQueryAddList();//初始化搜索城市列表

        initServerCityList();//服务端推荐城市

        initEdit();//初始化输入框
    }

    private void initServerCityList() {
        mAdapterServer = new CommonlyCityAddAdapter(
                R.layout.item_server_city, mServerCityList, true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rvServerCities.setLayoutManager(layoutManager);
        rvServerCities.setAdapter(mAdapterServer);
        mAdapterServer.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                addCityFromLocationBean(mServerCityList.get(position));
            }
        });
        loadServerCities();
    }

    private void loadServerCities() {
        ServerClient.getApi().cities().enqueue(new Callback<ServerCitiesResponse>() {
            @Override
            public void onResponse(Call<ServerCitiesResponse> call, Response<ServerCitiesResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getCities() == null) {
                    return;
                }
                mServerCityList.clear();
                for (ServerCitiesResponse.CityBean city : response.body().getCities()) {
                    NewSearchCityResponse.LocationBean bean = new NewSearchCityResponse.LocationBean();
                    bean.setId(city.getId());
                    bean.setName(city.getName());
                    bean.setAdm1(city.getAdm1());
                    bean.setAdm2(city.getAdm2());
                    bean.setCountry("中国");
                    mServerCityList.add(bean);
                }
                mAdapterServer.notifyDataSetChanged();
                setServerCityBarVisible(mServerCityList.size() > 0);
            }

            @Override
            public void onFailure(Call<ServerCitiesResponse> call, Throwable t) {
                // 服务端未启动时不影响本地常用城市
            }
        });
    }

    private void addCityFromLocationBean(NewSearchCityResponse.LocationBean location) {
        if (findSavedCity(location) == null) {
            ResidentCity residentCity = new ResidentCity();
            residentCity.setLocation(location.getName());
            residentCity.setParent_city(location.getAdm2());
            residentCity.setAdmin_area(location.getAdm1());
            residentCity.setCnty(location.getCountry());
            if (!residentCity.save()) {
                ToastUtils.showShortToast(context, "添加城市失败");
                return;
            }
        }
        switchToCity(location.getName(), location.getAdm2());
    }

    /** 当前界面按城市名称切换和展示，因此同名城市直接复用已有记录。 */
    private ResidentCity findSavedCity(NewSearchCityResponse.LocationBean location) {
        List<ResidentCity> savedCities = LitePal.findAll(ResidentCity.class);
        for (ResidentCity city : savedCities) {
            if (TextUtils.equals(city.getLocation(), location.getName())) {
                return city;
            }
        }
        return null;
    }

    private void switchToCity(String location, String parentCity) {
        SPUtils.putString(Constant.LOCATION, location, context);
        SPUtils.putString(Constant.CURRENT_WEATHER_CITY, location, context);
        EventBus.getDefault().post(new SearchCityEvent(location, parentCity));
        finish();
    }

    private void updateCurrentCityHeader() {
        String currentCity = getCurrentWeatherCity();
        tvCurrentCity.setText(TextUtils.isEmpty(currentCity)
                ? "当前显示城市：定位城市"
                : "当前显示城市：" + currentCity);
    }

    private String getCurrentWeatherCity() {
        String currentCity = SPUtils.getString(
                Constant.CURRENT_WEATHER_CITY, "", context);
        if (TextUtils.isEmpty(currentCity)) {
            currentCity = SPUtils.getString(Constant.LOCATION, "", context);
        }
        return currentCity;
    }

    /**
     * 初始化搜索要添加的城市列表
     */
    private void initQueryAddList() {
        mAdapterAdd = new CommonlyCityAddAdapter(R.layout.item_commonly_city_add_list, mList);
        rvSearch.setLayoutManager(new LinearLayoutManager(context));
        rvSearch.setAdapter(mAdapterAdd);

        //点击item时保存到数据库中，同时传递数据到主页面查询出天气
        mAdapterAdd.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                QueryWeather(position);
            }
        });
    }

    /**
     * 添加城市列表item，点击保存数据并发送事件
     *
     * @param position
     */
    private void QueryWeather(int position) {
        addCityFromLocationBean(mList.get(position));
    }

    /**
     * 初始化搜索输入框 ,输入后马上查询数据，不需要额外点击，同时查询到数据之后隐藏默认城市列表
     */
    private void initEdit() {
        editQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("")) {//输入后，显示清除按钮
                    ivClearSearch.setVisibility(View.VISIBLE);
                    mAdapterAdd.changTxColor(s.toString());
//                    mPresent.searchCity(context, s.toString());//开始搜索
                    mPresent.newSearchCity(s.toString());//搜索城市  V7  模糊搜索返回十条数据
                    setServerCityBarVisible(false);
                } else {//隐藏和显示控件
                    initHideOrShow();
                    setServerCityBarVisible(mServerCityList.size() > 0);
                }
            }
        });
    }

    /**
     * 根据常用城市数据来进行页面控件显示/隐藏
     */
    private void setServerCityBarVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        tvServerCitiesTitle.setVisibility(v);
        rvServerCities.setVisibility(v);
    }

    private void initHideOrShow() {
        ivClearSearch.setVisibility(View.GONE);//隐藏清除输入框内容的图标
        rvSearch.setVisibility(View.GONE);//隐藏搜索结果列表
        if (cityList != null && cityList.size() > 0) {//有数据
            rvCommonlyUsed.setVisibility(View.VISIBLE);//显示常用城市列表
            layNormal.setVisibility(View.GONE);//隐藏没有数据时的布局
        } else {//没数据
            rvCommonlyUsed.setVisibility(View.GONE);//隐藏常用城市列表
            layNormal.setVisibility(View.VISIBLE);//显示没有数据时的布局
        }
    }

    /**
     * 初始化常用城市列表数据
     */
    private void initCityList() {
        removeDuplicateSavedCities();
        //查询ResidentCity表中所有数据
        cityList = LitePal.findAll(ResidentCity.class);

        if (cityList != null && cityList.size() > 0) {
            String currentCity = getCurrentWeatherCity();
            mAdapter = new CommonlyCityAdapter(
                    R.layout.item_commonly_city_list, cityList, currentCity);

            rvCommonlyUsed.setLayoutManager(new LinearLayoutManager(context));
            rvCommonlyUsed.setAdapter(mAdapter);
            mAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                @Override
                public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                    switch (view.getId()) {
                        case R.id.tv_city_name:
                            switchToCity(cityList.get(position).getLocation(),
                                    cityList.get(position).getParent_city());
                            break;
                        case R.id.btn_delete://删除
                            ResidentCity cityToDelete = cityList.get(position);
                            String selectedCity = SPUtils.getString(
                                    Constant.LOCATION, "", context);
                            LitePal.delete(ResidentCity.class, cityToDelete.getId());//删除指定id
                            if (TextUtils.equals(selectedCity, cityToDelete.getLocation())) {
                                SPUtils.remove(Constant.LOCATION, context);
                            }
                            updateCurrentCityHeader();
                            initCityList();
                            //删除数据后判断一下显示和隐藏的控件
                            initHideOrShow();
                            break;
                    }

                }
            });

            mAdapter.notifyDataSetChanged();
        } else {
            rvCommonlyUsed.setVisibility(View.GONE);
            layNormal.setVisibility(View.VISIBLE);
        }


    }

    /** 清理旧版本已经写入的同名重复城市，只保留最早的一条。 */
    private void removeDuplicateSavedCities() {
        List<ResidentCity> savedCities = LitePal.findAll(ResidentCity.class);
        Set<String> cityNames = new HashSet<>();
        for (ResidentCity city : savedCities) {
            String name = city.getLocation();
            if (!TextUtils.isEmpty(name) && !cityNames.add(name)) {
                LitePal.delete(ResidentCity.class, city.getId());
            }
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_commonly_used_city;
    }

    @Override
    protected SearchCityContract.SearchCityPresenter createPresent() {
        return new SearchCityContract.SearchCityPresenter();
    }

    /**
     * 搜索城市天气 V7
     *
     * @param response
     */
    @Override
    public void getNewSearchCityResult(Response<NewSearchCityResponse> response) {
        dismissLoadingDialog();
        if (response.body().getCode().equals(Constant.SUCCESS_CODE)) {
            List<NewSearchCityResponse.LocationBean> data = response.body().getLocation();
            if (data != null && data.size() > 0) {
                rvCommonlyUsed.setVisibility(View.GONE);//隐藏常用城市列表
                mList.clear();
                mList.addAll(response.body().getLocation());
                mAdapterAdd.notifyDataSetChanged();
                rvSearch.setVisibility(View.VISIBLE);//显示搜索城市列表
                layNormal.setVisibility(View.GONE);
            } else {
                ToastUtils.showShortToast(context, "没有找到相关城市");
            }
        } else {
            ToastUtils.showShortToast(context, CodeToStringUtils.WeatherCode(response.body().getCode()));
        }
    }

    /**
     * 网络异常返回处理
     */
    @Override
    public void getDataFailed() {
        dismissLoadingDialog();//关闭弹窗
        ToastUtils.showShortToast(context, "网络异常");//这里的context是框架中封装好的，等同于this
    }


    @OnClick(R.id.iv_clear_search)
    public void onViewClicked() {
        editQuery.setText("");//置为空
        initHideOrShow();
    }

}
