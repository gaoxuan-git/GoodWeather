# GoodWeather

基于Android的天气预报系统，包含Android客户端、Flask辅助服务、部署说明、综合实训报告和界面截图。

## 完整项目下载

[下载全部原始文件的完整备份](https://github.com/gaoxuan-git/GoodWeather/releases/tag/full-backup-2026-10-28)。

完整备份包含本地目录的5600个文件条目，约713MiB，包括APK、原始压缩包、隐藏文件、构建缓存、Python虚拟环境、本机配置及原有Git历史。所有文件保留原始内容，并提供完整文件清单与SHA-256校验值。

归档使用AES-256加密，解密密码及操作步骤保存在项目所有者本地的`GoodWeather完整备份解密说明.txt`中。下载后使用GnuPG或Gpg4win解密，再解压即可恢复完整目录。

## 主要功能

- 城市定位、搜索、切换与常用城市管理。
- 当前天气、最高与最低温度、天气图标、实时时钟和数据更新时间。
- 逐小时、7天和15天天气预报。
- 空气质量、生活建议和分钟降水等扩展天气信息。
- 更新频率设置、服务地址保存与连接检测。

天气数据由客户端直接从和风天气获取。Flask提供健康检查、服务信息和推荐城市，不进行天气预测。

## 目录

| 目录或文件 | 内容 |
| --- | --- |
| `GoodWeather/GoodWeather-master/GoodWeather-master/` | 主要Android工程 |
| `GoodWeather/SuperWeather/` | 原有参考工程 |
| `weather-server/` | Flask辅助服务 |
| `文档/` | 综合实训报告、模板和设计图 |
| `验收截图/` | 应用界面截图 |
| `部署文档.md` | 安装、构建与运行说明 |
| `Windows无AndroidStudio从零部署文档.md` | Windows命令行部署说明 |
| `功能文档.md` | 项目功能要求 |

## 环境

Android工程使用JDK17、Gradle8.2.1、Android Gradle Plugin8.2.2和Android SDK34。服务端使用Python与Flask，依赖见`weather-server/requirements.txt`。

## 配置与构建

仓库内API密钥使用占位值。运行前请配置：

1. 在Android工程的`local.properties`中填写SDK路径，例如Windows使用`sdk.dir=D:/Android/Sdk`。
2. 在主工程的`app/src/main/java/com/llw/goodweather/utils/Constant.java`中填写自己的和风天气API Key，并核对API Host。
3. 使用百度定位或地图前，在`app/src/main/AndroidManifest.xml`中填写自己的百度API Key，并登记对应包名和签名。
4. 使用旧参考工程前，配置其中的天气和地图密钥。

在主Android工程目录执行：

```bash
./gradlew assembleDebug
```

Windows使用：

```powershell
.\gradlew.bat assembleDebug
```

在`weather-server`目录执行：

```bash
python -m pip install -r requirements.txt
python app.py
```

默认服务端口为5000。模拟器可以使用`adb reverse tcp:5000 tcp:5000`，再在应用设置中填写`http://127.0.0.1:5000/`；真机使用可访问的电脑局域网地址。

更多步骤和现有运行限制见部署文档。本分支提供可浏览的项目源码；包含构建缓存、本机配置、虚拟环境和APK在内的全部原始文件，见上方完整项目备份。

## 项目来源与许可

本项目在原有GoodWeather源码基础上整理和扩展。保留原项目说明及许可证，原仓库地址为<https://gitee.com/niki_lowkey/android>。许可文本见[`GoodWeather/LICENSE`](GoodWeather/LICENSE)；第三方SDK、字体及资源遵循各自许可。
