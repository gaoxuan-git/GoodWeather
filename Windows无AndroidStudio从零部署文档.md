# GoodWeather Windows 无 Android Studio 从零部署文档

> 适用系统：Windows 10/11 64 位  
> 适用项目：GoodWeather Android 客户端 + Flask 服务端  
> 文档基线：2026-10-19  
> 目标：不安装 Android Studio，仅使用 JDK、Android SDK 命令行工具、Gradle Wrapper 和 Python，完成构建、安装、启动及联调。

---

## 1. 先说明运行方式

GoodWeather 是 Android 应用，不能像普通 Windows 软件一样直接双击运行。Windows 上需要以下任一运行载体：

- Android 真机：推荐，资源占用低，定位更接近实际环境。
- Android 命令行模拟器：不需要 Android Studio，但需要安装 Android SDK Emulator 和系统镜像。

项目由两部分组成：

| 部分 | 目录 | 作用 |
|---|---|---|
| Android 主工程 | `GoodWeather\GoodWeather-master\GoodWeather-master\` | 天气 App，包名为 `com.llw.goodweather` |
| Flask 服务端 | `weather-server\` | 健康检查、项目信息和推荐城市接口 |

不要构建 `GoodWeather\SuperWeather\`。它是早期工程，不是当前交付的主项目。

天气数据由 App 直接请求和风天气 API。Flask 服务端只负责设置页连通性和推荐城市等课设功能，因此 Flask 暂时未启动时，天气核心功能仍可能正常显示。

---

## 2. 两种部署路线

### 2.1 路线 A：直接运行现成 APK

适合只想使用和验收，不修改源码的情况。

需要安装：

- Python 3，用于运行 Flask。
- Android SDK Platform-Tools，用于通过 `adb` 安装 APK；也可以把 APK 复制到手机后手动安装，此时不需要 `adb`。

直接使用项目根目录的：

```text
GoodWeather.apk
```

完成服务端启动、APK 安装和服务地址配置即可。具体参见第 8、9、12 章。

### 2.2 路线 B：从源码完整构建

适合修改代码、重新生成 APK 或使用命令行模拟器。

需要安装：

1. JDK 17
2. Python 3
3. Android SDK Command-line Tools
4. 项目所需 Android SDK 组件

不需要安装：

- Android Studio
- 全局 Gradle

项目自带 `gradlew.bat`，会自动下载并使用 Gradle 8.2.1。

---

## 3. 当前项目构建参数

| 项目 | 当前值 |
|---|---|
| Android Gradle Plugin | 8.2.2 |
| Gradle Wrapper | 8.2.1 |
| 构建 JDK | JDK 17 |
| Java 源码兼容级别 | Java 8 |
| compileSdk / targetSdk | 34 |
| minSdk | 21 |
| 应用版本 | 2.5 |
| Debug APK 输出名 | `GoodWeather2.5.apk` |
| Flask 端口 | TCP 5000 |

建议准备至少 10 GB 可用磁盘空间。若安装模拟器镜像，建议电脑有 8 GB 以上内存，并在 BIOS/UEFI 中开启 CPU 虚拟化。

---

## 4. 约定项目路径

本文假设项目放在：

```text
D:\GoodWeather-Project
```

如果实际路径不同，请替换命令中的路径。路径包含中文或空格时必须保留双引号。

打开 PowerShell，设置两个临时变量，后续命令可直接使用：

```powershell
$ProjectRoot = "D:\GoodWeather-Project"
$AndroidProject = "$ProjectRoot\GoodWeather\GoodWeather-master\GoodWeather-master"
```

关闭 PowerShell 后变量会失效，再次操作时需重新执行。

---

## 5. 安装 JDK 17

### 5.1 使用 winget 安装

以普通用户打开 PowerShell：

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

安装结束后关闭并重新打开 PowerShell。

### 5.2 使用安装包

如果系统没有 `winget`：

1. 打开 <https://adoptium.net/temurin/releases/?version=17>
2. 选择 Windows、x64、JDK、MSI。
3. 安装时勾选设置 `JAVA_HOME` 和添加到 `PATH`。

### 5.3 验证

```powershell
java -version
javac -version
echo $env:JAVA_HOME
```

`java` 和 `javac` 都应显示 17.x。不能使用仅包含运行环境的 JRE，也不建议使用 JDK 8、11 或 21 构建本项目。

---

## 6. 安装 Python 3

### 6.1 使用 winget 安装

```powershell
winget install --id Python.Python.3.12 -e
```

### 6.2 使用安装包

从 <https://www.python.org/downloads/windows/> 下载 64 位安装包。安装首页勾选：

```text
Add python.exe to PATH
```

### 6.3 验证

关闭并重新打开 PowerShell：

```powershell
py -3 --version
```

建议使用 Python 3.9 或更高版本。

---

## 7. 安装 Android SDK 命令行环境

### 7.1 下载 Command-line Tools

1. 打开 Android 官方页面：<https://developer.android.com/studio#command-line-tools-only>
2. 找到 “Command line tools only”。
3. 下载 Windows 版 ZIP。
4. 在 PowerShell 中创建 D 盘 SDK 目录：

```powershell
New-Item -ItemType Directory -Force -Path "D:\Android\Sdk\cmdline-tools\latest"
```

5. 解压后，确保最终目录结构如下：

```text
Sdk
└─ cmdline-tools
   └─ latest
      ├─ bin
      │  ├─ sdkmanager.bat
      │  └─ avdmanager.bat
      ├─ lib
      ├─ NOTICE.txt
      └─ source.properties
```

常见错误是多套一层 `cmdline-tools`，形成：

```text
latest\cmdline-tools\bin\sdkmanager.bat
```

如果出现这种结构，应把内层 `cmdline-tools` 的内容移动到 `latest`。

### 7.2 配置当前 PowerShell

```powershell
$env:ANDROID_HOME = "D:\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path += ";$env:ANDROID_HOME\platform-tools"
$env:Path += ";$env:ANDROID_HOME\cmdline-tools\latest\bin"
$env:Path += ";$env:ANDROID_HOME\emulator"
```

### 7.3 配置永久环境变量

按 `Win + S` 搜索“环境变量”，打开“编辑系统环境变量”：

1. 点击“环境变量”。
2. 在用户变量中新增：

```text
ANDROID_HOME = D:\Android\Sdk
ANDROID_SDK_ROOT = D:\Android\Sdk
```

3. 编辑用户变量 `Path`，新增：

```text
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\cmdline-tools\latest\bin
%ANDROID_HOME%\emulator
```

4. 确认 `JAVA_HOME` 指向 JDK 17 安装目录，且 `Path` 中包含：

```text
%JAVA_HOME%\bin
```

保存后关闭并重新打开 PowerShell。

### 7.4 安装构建组件

先接受 Android SDK 许可，过程中持续输入 `y`：

```powershell
sdkmanager.bat --sdk_root="$env:ANDROID_HOME" --licenses
```

安装项目构建所需组件：

```powershell
sdkmanager.bat --sdk_root="$env:ANDROID_HOME" `
  "platform-tools" `
  "platforms;android-34" `
  "build-tools;34.0.0"
```

验证：

```powershell
sdkmanager.bat --version
adb version
```

如果只连接真机，到这里已具备构建和安装条件。

---

## 8. 启动 Flask 服务端

进入服务端目录：

```powershell
cd "$ProjectRoot\weather-server"
```

首次运行时创建虚拟环境并安装依赖：

```powershell
py -3 -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

启动服务：

```powershell
.\.venv\Scripts\python.exe app.py
```

看到以下地址说明 Flask 已监听：

```text
http://127.0.0.1:5000
http://电脑局域网IP:5000
```

保持此 PowerShell 窗口运行。再打开一个 PowerShell 验证：

```powershell
Invoke-RestMethod http://127.0.0.1:5000/api/health
Invoke-RestMethod http://127.0.0.1:5000/api/info
Invoke-RestMethod http://127.0.0.1:5000/api/cities
```

健康检查应包含：

```text
status  : ok
service : goodweather-server
```

当前 `app.py` 已关闭 Flask Debug 和自动重载。不要自行开启自动重载，否则 Windows 上可能出现 5000 端口已监听、接口却持续超时的问题。

---

## 9. 直接安装现成 APK

### 9.1 通过 adb 安装

在 Android 真机中开启：

1. 设置 → 关于手机 → 连续点击版本号约 7 次。
2. 设置 → 开发者选项 → 开启 USB 调试。
3. USB 连接电脑，并在手机弹窗中允许调试。

在 PowerShell 中确认设备：

```powershell
adb devices
```

设备状态应为 `device`，不能是 `unauthorized` 或 `offline`。

安装项目根目录的 APK：

```powershell
adb install -r "$ProjectRoot\GoodWeather.apk"
```

启动 App：

```powershell
adb shell am start -n com.llw.goodweather/.ui.SplashActivity
```

### 9.2 在手机上手动安装

也可以把 `GoodWeather.apk` 复制到手机，使用文件管理器打开。首次安装时，系统会要求允许该文件管理器“安装未知应用”。

---

## 10. 从源码构建 APK

### 10.1 生成 local.properties

进入 Android 主工程：

```powershell
cd "$AndroidProject"
```

生成本机 SDK 路径配置：

```powershell
$SdkPath = $env:ANDROID_HOME -replace "\\", "/"
"sdk.dir=$SdkPath" | Set-Content -Encoding ASCII .\local.properties
Get-Content .\local.properties
```

输出应类似：

```properties
sdk.dir=D:/Android/Sdk
```

不要把其他电脑的 `local.properties` 路径直接照搬到当前电脑。

### 10.2 检查 Gradle 环境

```powershell
.\gradlew.bat --version
```

首次执行会下载 Gradle 8.2.1，耗时取决于网络。输出中的 JVM 应为 JDK 17。

### 10.3 执行单元测试并构建

```powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug --stacktrace
```

看到以下内容即构建成功：

```text
BUILD SUCCESSFUL
```

APK 输出位置：

```text
app\build\outputs\apk\debug\GoodWeather2.5.apk
```

确认文件：

```powershell
Get-Item .\app\build\outputs\apk\debug\GoodWeather2.5.apk
```

### 10.4 安装源码构建的 APK

设备已连接时执行：

```powershell
adb install -r .\app\build\outputs\apk\debug\GoodWeather2.5.apk
adb shell am start -n com.llw.goodweather/.ui.SplashActivity
```

也可以让 Gradle 完成构建和安装：

```powershell
.\gradlew.bat installDebug
```

---

## 11. 不安装 Android Studio 创建模拟器

如果使用 Android 真机，可跳过本章。

### 11.1 检查虚拟化

打开“任务管理器 → 性能 → CPU”，确认“虚拟化”为“已启用”。如果未启用，需要进入 BIOS/UEFI 开启 Intel VT-x 或 AMD-V。

再打开“启用或关闭 Windows 功能”，勾选：

```text
Windows 虚拟机监控程序平台
```

重启电脑后继续。

### 11.2 安装模拟器与 API 34 镜像

```powershell
sdkmanager.bat --sdk_root="$env:ANDROID_HOME" `
  "emulator" `
  "system-images;android-34;google_apis;x86_64"
```

检查硬件加速：

```powershell
emulator.exe -accel-check
```

### 11.3 创建 AVD

```powershell
avdmanager.bat list device
```

创建名为 `GoodWeather_API34` 的模拟器：

```powershell
"no" | avdmanager.bat create avd `
  -n GoodWeather_API34 `
  -k "system-images;android-34;google_apis;x86_64" `
  --device "pixel_7" `
  --force
```

检查是否创建成功：

```powershell
emulator.exe -list-avds
```

### 11.4 启动模拟器

```powershell
Start-Process "$env:ANDROID_HOME\emulator\emulator.exe" `
  -ArgumentList "-avd", "GoodWeather_API34", "-no-snapshot-load"
```

等待系统进入桌面后：

```powershell
adb wait-for-device
adb devices
```

可设置北京测试坐标：

```powershell
adb emu geo fix 116.4074 39.9042
```

然后按第 9 或第 10 章安装 App。

---

## 12. 连接 App 与 Flask

### 12.1 模拟器连接电脑

推荐执行端口反向映射：

```powershell
adb reverse tcp:5000 tcp:5000
```

在 App 中进入：

```text
主页右上角“+” → 应用设置 → 实训服务端
```

服务端地址填写：

```text
127.0.0.1:5000
```

也可以不执行 `adb reverse`，直接填写 Android 官方模拟器访问宿主机的固定地址：

```text
10.0.2.2:5000
```

`adb reverse` 在模拟器重启或 adb 连接重置后可能失效，需要重新执行。

### 12.2 真机通过局域网连接电脑

手机和电脑必须连接同一个 Wi-Fi，且不能使用开启了设备隔离的访客网络。

查看电脑 IPv4 地址：

```powershell
ipconfig
```

找到当前 Wi-Fi 或以太网适配器的“IPv4 地址”，例如：

```text
192.168.1.20
```

在 App 服务端地址中填写：

```text
192.168.1.20:5000
```

App 会自动补全为 `http://192.168.1.20:5000/` 并保存。

如被 Windows 防火墙拦截，以管理员身份打开 PowerShell，添加 5000 端口入站规则：

```powershell
New-NetFirewallRule `
  -DisplayName "GoodWeather Flask TCP 5000" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 5000 `
  -Action Allow
```

如果 Windows 首次启动 Python 时弹出防火墙提示，也可以勾选“专用网络”并允许访问。

---

## 13. 第三方 API 配置

### 13.1 和风天气

配置文件：

```text
app\src\main\java\com\llw\goodweather\utils\Constant.java
```

需要确认：

```java
public static final String API_KEY = "你的和风天气Key";
public static final String QWEATHER_API_HOST = "你的专属API Host";
```

当前源码带有课程测试配置，但测试凭据可能过期或被停用。长期使用时，应在 <https://console.qweather.com/> 创建自己的 Web API Key，并使用控制台提供的专属 API Host。

只填写 Key 而继续使用旧公共 Host，可能返回 `403 Invalid Host`。

### 13.2 百度定位

百度 AK 位于：

```text
app\src\main\AndroidManifest.xml
```

百度开放平台需要同时绑定：

- 包名：`com.llw.goodweather`
- 当前 APK 签名 SHA1

首次构建后可查看 Debug 签名：

```powershell
keytool -list -v `
  -alias androiddebugkey `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -storepass android `
  -keypass android
```

如百度返回错误 230，通常是控制台中配置的包名或 SHA1 与当前 APK 不一致。天气搜索功能仍可使用，定位和地图功能则可能异常。

---

## 14. 部署验收

### 14.1 服务端

- `http://127.0.0.1:5000/api/health` 返回 `status=ok`。
- `/api/info` 和 `/api/cities` 返回 JSON。
- Flask PowerShell 窗口保持运行，无持续报错。

### 14.2 App

- App 能从启动页进入主页。
- 首次启动可以授予定位权限。
- 主页显示城市、当前温度、高低温、天气文字和图标。
- 当前时间每秒更新，数据更新时间单独显示。
- 详情页显示今日 24 小时天气和 15 天天气预报。
- 设置页服务端状态显示正常。
- 城市管理页能加载服务端推荐城市。

### 14.3 命令行检查

```powershell
adb shell pidof com.llw.goodweather
```

返回进程 ID 表示 App 正在运行。

查看当前 App 日志：

```powershell
$AppProcessId = adb shell pidof com.llw.goodweather
adb logcat --pid=$AppProcessId
```

按 `Ctrl + C` 停止日志输出。

---

## 15. 常见问题

### 15.1 `JAVA_HOME is set to an invalid directory`

确认 `JAVA_HOME` 指向 JDK 根目录，而不是 `bin`：

```text
正确：C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot
错误：C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot\bin
```

新开 PowerShell 后再次执行 `java -version` 和 `.\gradlew.bat --version`。

### 15.2 `SDK location not found`

重新生成 `local.properties`：

```powershell
cd "$AndroidProject"
$SdkPath = $env:ANDROID_HOME -replace "\\", "/"
"sdk.dir=$SdkPath" | Set-Content -Encoding ASCII .\local.properties
```

### 15.3 `License for package ... not accepted`

```powershell
sdkmanager.bat --sdk_root="$env:ANDROID_HOME" --licenses
```

对所有许可输入 `y`。

### 15.4 Gradle 或依赖下载失败

先确认浏览器能访问 Maven、JitPack 和 Gradle 镜像。再执行：

```powershell
cd "$AndroidProject"
.\gradlew.bat --stop
.\gradlew.bat assembleDebug --refresh-dependencies --stacktrace
```

项目的 Gradle Wrapper 当前使用腾讯云镜像，Android 依赖优先使用阿里云镜像，同时仍可能访问 Google Maven、Maven Central 和 JitPack。

### 15.5 `adb devices` 显示 `unauthorized`

1. 解锁手机。
2. 在手机上确认 USB 调试授权弹窗。
3. 仍无效时，在开发者选项中撤销 USB 调试授权，重新插拔数据线。

### 15.6 `no devices/emulators found`

确认：

```powershell
adb devices
emulator.exe -list-avds
```

真机需使用支持数据传输的数据线；模拟器需完全进入 Android 桌面后再安装。

### 15.7 模拟器启动失败或非常慢

执行：

```powershell
emulator.exe -accel-check
```

确认 BIOS 虚拟化和“Windows 虚拟机监控程序平台”均已启用。修改 Windows 功能后必须重启。

### 15.8 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

设备上已有相同包名、不同签名的 APK。确认可以清除该 App 的本地数据后执行：

```powershell
adb uninstall com.llw.goodweather
adb install .\app\build\outputs\apk\debug\GoodWeather2.5.apk
```

`adb uninstall` 会删除该 App 的设置和本地数据。

### 15.9 电脑能访问 Flask，手机不能访问

依次检查：

1. 手机填写的是电脑局域网 IPv4，不是 `127.0.0.1`。
2. 手机和电脑在同一局域网。
3. Flask 正在监听 `0.0.0.0:5000`。
4. Windows 防火墙已允许 TCP 5000。
5. VPN、代理和访客网络未隔离局域网设备。
6. 电脑没有休眠，运行 Flask 的窗口没有关闭。

### 15.10 5000 端口被占用

```powershell
Get-NetTCPConnection -LocalPort 5000 -ErrorAction SilentlyContinue
```

关闭占用端口的旧 Flask/Python 程序后重新运行 `app.py`。App 当前默认按 5000 端口连接，不建议只修改服务端端口。

### 15.11 天气接口返回 403 或页面一直加载

重点检查 `Constant.java` 中的：

- `API_KEY`
- `QWEATHER_API_HOST`

Key 与专属 Host 必须属于同一个和风天气账号配置。

---

## 16. 日常启动最短流程

完成首次安装后，每次开发只需：

### PowerShell 窗口 1：启动 Flask

```powershell
$ProjectRoot = "D:\GoodWeather-Project"
cd "$ProjectRoot\weather-server"
.\.venv\Scripts\python.exe app.py
```

### PowerShell 窗口 2：启动模拟器并部署

```powershell
$ProjectRoot = "D:\GoodWeather-Project"
$AndroidProject = "$ProjectRoot\GoodWeather\GoodWeather-master\GoodWeather-master"

Start-Process "$env:ANDROID_HOME\emulator\emulator.exe" `
  -ArgumentList "-avd", "GoodWeather_API34"

adb wait-for-device
adb reverse tcp:5000 tcp:5000

cd "$AndroidProject"
.\gradlew.bat installDebug
adb shell am start -n com.llw.goodweather/.ui.SplashActivity
```

使用真机时，不需要启动模拟器；连接手机后从 `adb devices`、构建和安装步骤继续即可。真机脱离 USB 使用时，要在 App 设置页填写电脑局域网 IPv4 地址。
