# GoodWeather 移动互联网综合实训 — 部署与交接说明

> 项目根目录：`GoodWeather-Project/`（Windows 与 macOS 均适用）  
> 文档日期：2026-10-18  
> 包含：**Android 客户端** + **Flask 实训服务端（weather-server）**

---

## 1. 项目组成

| 目录 | 说明 |
|------|------|
| `GoodWeather\GoodWeather-master\GoodWeather-master\` | **主工程**（Android Studio 打开此目录） |
| `weather-server\` | 课设 **简易后端**（健康检查、项目信息、推荐城市列表） |
| `GoodWeather\BaiduLBS_AndroidSDK_Lib\` | 百度 LBS SDK 相关（若工程已集成可忽略） |

**架构关系：**

- 天气数据：**App 直连和风天气 API**（V7），不经过 Flask。
- Flask：设置页「实训服务端」连通性检测、`/api/cities` 等课设要求接口。
- 地图 / 定位：**百度地图 + 百度定位 SDK**。

---

## 2. 环境要求

| 组件 | 版本 / 说明 |
|------|-------------|
| Android Studio | 建议 2023.x+，自带 JDK 17 或单独安装 |
| **JDK** | **17**（`gradle.properties` 中 `org.gradle.java.home` 指向本机 JDK 17） |
| Gradle | **8.2.1**（wrapper 已配置，腾讯云镜像） |
| AGP | **8.2.2** |
| compileSdk / targetSdk | **34** |
| minSdk | 21 |
| Python | 3.x（跑 Flask） |
| 模拟器 / 真机 | 需联网；定位相关功能需位置权限 |

**打开工程：**

```
File → Open → GoodWeather-master\GoodWeather-master
Sync Gradle → Run app
```

---

## 3. 第三方 Key 与配置（必改项交接）

### 3.1 和风天气（核心）

文件：`app/src/main/java/com/llw/goodweather/utils/Constant.java`

| 常量 | 含义 | 当前示例值 |
|------|------|------------|
| `API_KEY` | 和风 API Key | `REPLACE_WITH_API_KEY` |
| `QWEATHER_API_HOST` | **专属 API Host**（控制台 → 设置 → API Host） | `nb4y3nkkjb.re.qweatherapi.com` |

**踩坑（重要）：**

- 2026 年起，新 Key 若仍走 `devapi.qweather.net` / `geoapi.qweather.net`，会 **403 Invalid Host**。
- 必须在控制台开通 **专属 Host**，并写入 `QWEATHER_API_HOST`。
- `WeatherApplication` 启动时会调用 `ServiceGenerator.configureQWeatherHost()`，Retrofit 基址才会正确。

Geo 接口已改为 **v2**：如 `/geo/v2/city/lookup`、全球坐标查询 `lookupLocationGlobal` 等（见 `ApiService` / Contract）。

### 3.2 百度地图 / 定位

文件：`app/src/main/AndroidManifest.xml`

- `com.baidu.lbsapi.API_KEY`：当前为 `REPLACE_WITH_BAIDU_API_KEY`
- 包名：`com.llw.goodweather`
- Debug 签名 SHA1（控制台需绑定）：`21:47:B2:…:F5`（以本机 `keytool -list -v -keystore ~/.android/debug.keystore` 为准）

**踩坑：**

- **错误 230**：包名、当前签名 SHA1 与百度控制台配置不匹配。当前验收环境仍存在此问题，首页依靠系统定位兜底可工作，但百度地图/定位不能视为已部署完成。
- **错误 167**：多为模拟器 / 环境定位失败，**不一定是 AK 写错**（602 才是 AK 问题）。
- 需在代码中同意百度隐私（`SplashActivity` / `MapWeatherActivity` 已用反射调用 `LocationClient.setAgreePrivacy(true)`）。
- 模拟器默认 GPS 常为 **美国（Mountain View）**，首页 / 地图可能显示海外；见下文「模拟器定位」。

### 3.3 Flask 实训服务端地址

仍在 `Constant.java`：

| 常量 | 用途 |
|------|------|
| `SERVER_BASE_URL_EMULATOR_LOCALHOST` | `http://127.0.0.1:5000/` + **`adb reverse`**（模拟器推荐） |
| `SERVER_BASE_URL_EMULATOR` | `http://10.0.2.2:5000/`（部分 Windows 环境不通，作备选） |
| `SERVER_BASE_URL_LAN` | 真机访问电脑局域网 IP，如 `http://192.168.2.14:5000/` |

**换 Wi‑Fi / 换电脑 IP 后，真机必须改 `SERVER_BASE_URL_LAN`。**

探测逻辑：`ServerClient.probeFirstReachable()` 依次请求 `/api/health`。

---

## 4. weather-server 部署

```powershell
cd "c:\Users\EDY\Desktop\安卓app\GoodWeather-Project\weather-server"
pip install -r requirements.txt
python app.py
```

- 监听：`http://0.0.0.0:5000`
- 接口：`GET /api/health`、`/api/info`、`/api/cities`

**踩坑（重要）：**

- **不要** 使用 `debug=True` 且 **不要** 开 Flask 自动重loader。  
  Windows 上会出现 **5000 端口在监听但 `/api/health` 一直超时 / 卡住**。  
  当前 `app.py` 已固定：`debug=False, use_reloader=False, threaded=True`。

**模拟器连本机 Flask：**

```powershell
adb reverse tcp:5000 tcp:5000
```

然后在 App **设置** 里测「实训服务端」；优先走 `127.0.0.1:5000`。

**真机：**

- 电脑与手机同一局域网；
- `Constant.SERVER_BASE_URL_LAN` 改为电脑 IP；
- Windows 防火墙放行 **5000** 入站。

---

## 5. Android 构建说明（已踩坑汇总）

### 5.1 Gradle / JDK

- 工程在 **中文路径** 下：`android.overridePathCheck=true` 已开启。
- ButterKnife + JDK 17：根 `build.gradle` 已为 `JavaCompile` 配置 `--add-opens`。
- `gradle.properties`：`android.nonFinalResIds=false`、`android.nonTransitiveRClass=false`（ButterKnife 兼容）。

### 5.2 依赖替换

- `rxpermissions`：使用 **JitPack** `com.github.tbruyelle:RxPermissions:0.10.2`（旧 AAR 曾导致 compileSdk 36 元数据问题）。

### 5.3 权限与启动页

- Android 13+：**不再批量申请旧版存储权限**，避免「权限未开启」直接退出。
- `SplashActivity`：`android:exported="true"`（Launcher 要求）。

### 5.4 业务逻辑修复（交接后行为预期）

| 现象 | 原因 | 处理 |
|------|------|------|
| 一直转圈 / 部分接口失败还卡住 | 分钟级降水、生活指数等可选接口失败仍占 loading | **实况 `now` 成功即 dismiss loading**；可选接口失败静默 |
| 首页仍是 Mountain View | 模拟器 GPS + 单次百度定位 | 中国 bbox 内用坐标查和风；`onResume` 海外名时刷新 |
| 地图页进页底部 0° + 长 Toast | 以前等逆地理再搜城市 | **定位回调立即 `searchCityGlobal(经纬度)`**；失败 fallback 北京；已去掉长 Toast |
| 设置里 Flask 连不上 | URL 错 / debug reloader | `ServerClient` 探测 + Flask 关闭 reloader |
| 设置页城市与主页不一致 | 定位城市、手动城市和当前展示城市共用旧 Key | 新增 `CURRENT_WEATHER_CITY`，主页确定城市后写入，设置页优先回显 |
| 常用城市重复 | 推荐或搜索结果重复写入 | 添加时复用同名记录，加载时清理旧重复项 |
| “时间”含义混在一起 | 只显示接口更新时间 | 主页和详情页新增每秒更新的 `HH:mm:ss`，接口时间独立显示为 `数据更新时间：HH:mm` |
| 详情页缺少当天天气列表 | 只显示 15 天预报 | 新增“今日24小时天气”横向列表，与 15 天列表独立请求和失败处理 |
| 夜间图标不清晰或显示 `N/A` | `151/152` 素材对比度低，部分夜间代码无映射 | `151/152` 使用方案 C 的 `icon_153`；补齐 `350/351/456/457` |
| 主页当前图标偏小、风格不一致 | 使用旧深色图标映射 | 改用 `WeatherUtil` 彩色映射，容器调整为 30dp |

---

## 6. 模拟器 / 真机操作建议

### 6.1 模拟器设国内坐标（推荐）

Android Studio 扩展控件里地图有时空白，**建议 adb：**

```powershell
adb emu geo fix 116.4074 39.9042
```

（经度在前、纬度在后）

### 6.2 手动搜城市

搜索框输入 **「北京」** 等国内城市，可绕过定位异常。

### 6.3 真机

- 开启 GPS / 网络定位；
- 和风、百度 Key 需绑定正确包名与签名 SHA1（Release 需单独配置）。

---

## 7. 日常启动 checklist

1. 启动 Flask：`python app.py`
2. （模拟器）`adb reverse tcp:5000 tcp:5000`
3. （模拟器）`adb emu geo fix 116.4074 39.9042`
4. 在主工程执行 `./gradlew testDebugUnitTest assembleDebug`（Windows 用 `gradlew.bat`）
5. Android Studio Run **GoodWeather**，首次授予定位权限
6. 核对设置页 Flask 状态、当前城市和更新频率
7. 核对主页双时间、彩色天气图标，以及详情页 24 小时和 15 天列表
8. Logcat 确认运行期间没有新增 `FATAL EXCEPTION`

---

## 8. 关键文件索引

| 文件 | 作用 |
|------|------|
| `Constant.java` | 和风 Key / Host、Flask 基址 |
| `ServerClient.java` | Flask 探测与 Retrofit |
| `WeatherApplication.java` | 和风 Host 初始化 |
| `MainActivity.java` | 首页定位、城市同步、双时间、自动刷新与彩色天气图标 |
| `SettingActivity.java` | 当前城市回显、更新频率和 Flask 状态 |
| `CommonlyUsedCityActivity.java` | 城市切换、推荐城市与重复项清理 |
| `MoreDailyActivity.java` / `MoreDailyContract.java` | 详情概要、24 小时与 15 天预报 |
| `WeatherUtil.java` | 天气代码到彩色图标资源的映射 |
| `MapWeatherActivity.java` | 地图天气页 |
| `WeatherContract.java` / `MapWeatherContract.java` | MVP 与 API 回调 |
| `mvplibrary/.../ServiceGenerator.java` | Retrofit 多基址 |
| `AndroidManifest.xml` | 百度 AK、权限、Launcher |
| `weather-server/app.py` | 实训服务端 |

---

## 9. 交接给下一位同学的建议

1. **先确认和风专属 Host + Key** 有效（浏览器或 Postman 测 Host 域名）。
2. **百度控制台** 核对包名 + SHA1 + 隐私合规。
3. **Flask 与 Constant 里 IP** 保持一致；模拟器记得 `adb reverse`。
4. 当前自动刷新基于主页 `Handler`，只在 App 前台运行，不是后台定时任务。
5. 当前业务功能在 Android 14（API 34）模拟器已验证；不要把 API 21/22、Lint 或设备测试写成已通过。
6. 官方演示账号、在线文档与本课设 **无直接关系**；以本仓库配置为准。

---

## 10. 当前验证状态

2026-10-18 最近一次复核：

- `testDebugUnitTest`：通过。
- `assembleDebug`：通过，输出 `GoodWeather2.5.apk`。
- Flask `/api/health`、`/api/info`、`/api/cities`：3/3 返回 HTTP 200。
- Android 14 模拟器：主页、设置页、城市管理、详情页及方案 C 夜间图标已人工检查；未发现新增 `FATAL EXCEPTION`。
- 未关闭：百度鉴权错误 230、`lintDebug` 7 errors/894 warnings、设备测试缺少 `androidx.test.ext:junit`、API 21/22 兼容问题。

---

## 11. 参考链接

- 和风控制台：https://console.qweather.com/
- 百度地图开放平台：https://lbsyun.baidu.com/
- 原项目 README：`GoodWeather-master/README.md`
