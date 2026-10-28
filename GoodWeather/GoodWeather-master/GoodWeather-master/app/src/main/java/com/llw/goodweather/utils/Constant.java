package com.llw.goodweather.utils;

/**
 * 统一管理缓存中对应的KEY
 *
 * @author llw
 */
public class Constant {

    /**
     * 和风天气 API Key（凭据 ID: C5B8URT3KY，本工程接口使用 key 参数）
     */
    public static final String API_KEY = "REPLACE_WITH_API_KEY";

    /**
     * 和风「专属 API Host」（控制台 → 设置 → API Host，形如 h2a9cf3mhs.xy.qweatherapi.com）
     * 2026 年起 devapi.qweather.net / geoapi.qweather.net 对新 Key 会返回 403 Invalid Host，必须填此项。
     */
    public static final String QWEATHER_API_HOST = "nb4y3nkkjb.re.qweatherapi.com";

    /** 模拟器 + adb reverse tcp:5000 tcp:5000（推荐） */
    public static final String SERVER_BASE_URL_EMULATOR_LOCALHOST = "http://127.0.0.1:5000/";

    /** 模拟器访问宿主机（部分 Windows 环境不通，作备选） */
    public static final String SERVER_BASE_URL_EMULATOR = "http://10.0.2.2:5000/";

    /** 用户在设置页保存的实训服务端地址 */
    public static final String SERVER_BASE_URL_CUSTOM = "serverBaseUrlCustom";

    /** 真机访问的默认局域网地址；换网络后可在设置页直接修改 */
    public static final String SERVER_BASE_URL_LAN = "http://10.110.61.220:5000/";
    /**
     * 更新APP的用户Id
     */
    public static final String UPDATE_USER_ID = "5e8c37e90d81cc0db2645c1c";
    /**
     * 更新APP的用户Token
     */
    public static final String UPDATE_API_TOKEN = "REPLACE_WITH_UPDATE_API_TOKEN";
    /**
     * 返回成功码
     */
    public static final String SUCCESS_CODE = "200";
    /**
     * 市
     */
    public static final String CITY = "city";
    /**
     * 区/县
     */
    public static final String DISTRICT = "district";
    /**
     * 通过搜索接口得到的城市id,在V7中所有数据通过id来查询
     */
    public static final String LOCATION_ID = "locationId";
    /**
     * 每日图片开关
     */
    public static final String EVERYDAY_IMG = "everyday_img";
    /**
     * 图片列表开关
     */
    public static final String IMG_LIST = "img_list";
    /**
     * 手动定义开关
     */
    public static final String CUSTOM_IMG = "custom_img";
    /**
     * 选中的本地背景图片
     */
    public static final String IMG_POSITION = "img_position";
    /**
     * 启动相册标识
     */
    public final static int SELECT_PHOTO = 2;
    /**
     * 手动上传图片地址
     */
    public static final String CUSTOM_IMG_PATH = "custom_img_path";
    /**
     * 跳转页面的标识
     */
    public static final String FLAG_OTHER_RETURN = "flag_other_return";

    /** 用户手动选择的城市，用于城市切换流程。 */
    public static final String LOCATION = "location";
    /** 主界面当前实际展示天气的城市。 */
    public static final String CURRENT_WEATHER_CITY = "currentWeatherCity";
    /**
     * App首次启动
     */
    public static final String APP_FIRST_START = "appFirstStart";
    /**
     * 今日启动APP的时间
     */
    public static final String START_UP_APP_TIME = "startAppTime";

    /**
     * 成功
     */
    public static final String SUCCESS = "success";

    /**
     * 壁纸地址
     */
    public static final String WALLPAPER_URL = "wallpaperUrl";
    /**
     * 壁纸类型  1  壁纸列表  2  每日一图  3  手动上传  4  默认壁纸
     */
    public static final String WALLPAPER_TYPE = "wallpaperType";

    /**
     * 每日提示弹窗的背景图
     */
    public static final String EVERYDAY_TIP_IMG = "everydayTipImg";

    /**
     * 每日提示弹窗是否弹出
     */
    public static final String EVERYDAY_POP_BOOLEAN = "everydayPopBoolean";

    /**
     * 天气自动更新频率（SP key），值为 int，单位：分钟；0 表示仅手动下拉刷新
     */
    public static final String WEATHER_UPDATE_INTERVAL = "weatherUpdateInterval";
    /** 仅手动刷新 */
    public static final int UPDATE_INTERVAL_MANUAL = 0;
    /** 每 1 小时自动更新 */
    public static final int UPDATE_INTERVAL_1H = 60;
    /** 每 2 小时自动更新 */
    public static final int UPDATE_INTERVAL_2H = 120;
    /** 每 4 小时自动更新 */
    public static final int UPDATE_INTERVAL_4H = 240;
}
