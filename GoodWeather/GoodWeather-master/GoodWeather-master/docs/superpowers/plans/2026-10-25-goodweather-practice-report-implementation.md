# GoodWeather Practice Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate a template-compliant GoodWeather comprehensive practice report in Markdown and Word formats, including verified project content, diagrams, code excerpts, screenshots, conclusion, and references.

**Architecture:** Use one canonical Markdown report as the content baseline. Generate three diagrams as PNG assets in a task-local temporary directory, then use docx-js to create a single-section A4 Word document with explicit page breaks and template-prescribed Chinese typography. Validate content counts, DOCX structure, rendered pages, and consistency between both deliverables.

**Tech Stack:** Markdown, JavaScript, docx-js, Graphviz, image-size, LibreOffice, Poppler, shell validation

---

### Task 1: Establish the Verified Content Baseline

**Files:**
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/模版.md`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/项目说明.md`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/项目需求核对报告.md`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/部署文档.md`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/GoodWeather/GoodWeather-master/GoodWeather-master/app/src/main/java/com/llw/goodweather/MainActivity.java`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/GoodWeather/GoodWeather-master/GoodWeather-master/app/src/main/java/com/llw/goodweather/ui/SettingActivity.java`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/GoodWeather/GoodWeather-master/GoodWeather-master/app/src/main/java/com/llw/goodweather/utils/ServerClient.java`
- Read: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/weather-server/app.py`

- [ ] **Step 1: Extract the fixed template fields**

Record the following unchanged values for both outputs:

```text
课程类别：实训实习(见习)类
课程ID：05039
课程名称：综合实训(软件技术)
实践学期：26秋
专业：软件技术
实践起止日期：2026.10.15-11.20
未知个人字段：待填写
指导教师区域：此项不写
```

- [ ] **Step 2: Lock the verified project facts**

Use only facts supported by the current files:

```text
项目：GoodWeather 天气预报系统
客户端：Android Java，MVP，Retrofit/OkHttp/Gson
外部天气数据：和风天气 API
辅助服务端：Flask /api/health、/api/info、/api/cities
本地存储：LitePal、SharedPreferences
主要功能：定位与城市选择、实时天气、逐小时预报、7/15天预报、空气质量、生活指数、灾害预警、分钟降水、更新频率、可配置服务端地址
验证环境：Android 14（API 34）
```

- [ ] **Step 3: Lock the limitations**

```text
百度 LBS 鉴权可能返回 230
lintDebug：7 errors、920 warnings
Android 设备测试缺少 androidx.test.ext:junit
API 21/22 存在启动兼容风险
DateUtils.updateTime 对空或短字符串缺少兜底
```

### Task 2: Create the Canonical Markdown Report

**Files:**
- Create: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.md`

- [ ] **Step 1: Reproduce the template front matter**

Create the cover information, student information, assessment information, material directory, practice assessment form, student report summary area, signature area, and evaluation page in the exact template order. Use Markdown tables for all fixed forms and write `待填写` in every unknown personal field.

- [ ] **Step 2: Write the abstract and keywords**

Write an approximately 300-Chinese-character abstract covering purpose, method, result, conclusion, and innovation. Use these keywords:

```text
关键词：Android；天气预报；MVP；移动应用；Flask
```

- [ ] **Step 3: Write the body**

Use this exact heading structure:

```text
一、项目概述
（一）实践背景
（二）实践目的与意义
（三）实践内容
二、需求分析
（一）功能需求
（二）非功能需求
（三）运行环境
三、系统总体设计
（一）总体架构
（二）功能模块设计
（三）数据流与存储设计
四、系统详细设计与实现
（一）启动与权限处理
（二）城市定位和切换
（三）天气数据请求与展示
（四）详细预报页面
（五）设置与自动更新
（六）局域网服务端连接
五、系统测试与部署
（一）测试环境与方法
（二）功能测试结果
（三）部署方法
（四）问题分析与改进
```

The body must contain 4500–6000 Chinese characters and cite code, diagrams, and screenshots in context.

- [ ] **Step 4: Add the conclusion**

Write a standalone conclusion covering practical process, achieved functions, engineering skills learned, verified results, limitations, and future improvements.

- [ ] **Step 5: Add all 15 references**

Copy the 15 entries under “实际参考文献” from the template without renumbering or deleting the duplicate entry.

- [ ] **Step 6: Validate the Markdown structure**

Run:

```bash
grep -c '^\\[[0-9][0-9]*\\]' /Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.md
grep -nE '请输入|请填写正确|内容为小四号|举例如下|TBD|TODO' /Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.md
```

Expected:

```text
15
No matches from the placeholder scan
```

### Task 3: Generate the Required Technical Diagrams

**Files:**
- Create temporarily: `/tmp/goodweather-practice-report/system-architecture.png`
- Create temporarily: `/tmp/goodweather-practice-report/weather-flow.png`
- Create temporarily: `/tmp/goodweather-practice-report/server-address-flow.png`

- [ ] **Step 1: Check Graphviz**

Run:

```bash
dot -V
```

Expected: Graphviz version output.

- [ ] **Step 2: Render the system architecture diagram**

Render a left-to-right diagram containing:

```text
用户 → Android App
Android App → 和风天气 API
Android App → Flask 辅助服务
Android App → LitePal
Android App → SharedPreferences
Flask 辅助服务 → 健康检查/项目信息/推荐城市
```

- [ ] **Step 3: Render the weather business flow**

Render:

```text
启动 → 权限判断 → 定位或手动城市 → Geo 城市解析 → 并行请求天气数据 → 页面展示 → 手动/定时刷新
```

- [ ] **Step 4: Render the server address flow**

Render:

```text
读取保存地址 → URL 规范化 → 候选地址去重 → /api/health 逐个探测 → 选择可达地址 → Retrofit 重建 → 显示服务信息
```

- [ ] **Step 5: Inspect all diagram dimensions**

Run:

```bash
file /tmp/goodweather-practice-report/*.png
```

Expected: three valid PNG images with readable landscape dimensions.

### Task 4: Create the Word Report

**Files:**
- Create temporarily: `/tmp/goodweather-practice-report/generate-report.js`
- Create: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.docx`

- [ ] **Step 1: Check Node.js and task-local dependencies**

Run:

```bash
node --version
cd /tmp/goodweather-practice-report
npm init -y
npm install docx image-size
```

Expected: Node.js version output and successful installation.

- [ ] **Step 2: Implement the DOCX generator**

The generator must:

```text
Use exactly one A4 section
Use SimSun for body and SimHei for headings
Set body to 12 pt with 1.5 line spacing
Set Heading1 to 18 pt with 24 pt before/after
Set Heading2 to 10.5 pt with 18 pt before/after
Set Heading3 to 12 pt with 12 pt before/after
Set abstract body to 14 pt SimSun
Build all template forms as fixed-width tables with cantSplit rows
Insert explicit page breaks before abstract, conclusion, and references
Insert the three generated diagrams and four existing verification screenshots
Insert figure numbers and captions
Insert selected real code excerpts in bordered monospaced blocks
Add centered page numbers in the footer
Write the final file to the required document directory
```

- [ ] **Step 3: Generate and sanitize the DOCX**

Run:

```bash
node /tmp/goodweather-practice-report/generate-report.js
python /Users/bytedance/.trae-cn/plugins/trae-remote-official/office-plugin-mac/0.1.0/skills/docx/scripts/sanitize.py /Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.docx
```

Expected: the DOCX exists and sanitize reports no unrepaired structural problem.

### Task 5: Render and Inspect the Word Document

**Files:**
- Create temporarily: `/tmp/goodweather-practice-report/rendered/GoodWeather综合实训报告.pdf`
- Create temporarily: `/tmp/goodweather-practice-report/rendered/page-*.png`

- [ ] **Step 1: Convert DOCX to PDF**

Run:

```bash
mkdir -p /tmp/goodweather-practice-report/rendered
soffice --headless --convert-to pdf --outdir /tmp/goodweather-practice-report/rendered /Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.docx
```

Expected: PDF conversion succeeds.

- [ ] **Step 2: Render all pages**

Run:

```bash
pdftoppm -png -r 110 /tmp/goodweather-practice-report/rendered/GoodWeather综合实训报告.pdf /tmp/goodweather-practice-report/rendered/page
```

Expected: one PNG per page.

- [ ] **Step 3: Visually inspect representative and boundary pages**

Inspect the cover, assessment forms, abstract, each diagram page, screenshot pages, conclusion, and references. Confirm no clipped text, black image backgrounds, split table rows, overlapping content, unreadable diagrams, or unintended blank pages.

- [ ] **Step 4: Correct and regenerate if needed**

Adjust only the generator’s spacing, image dimensions, table widths, or page breaks; regenerate, sanitize, and rerender until all checks pass.

### Task 6: Final Consistency Checks and Delivery

**Files:**
- Verify: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.md`
- Verify: `/Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.docx`

- [ ] **Step 1: Extract Word text**

Run:

```bash
pandoc /Users/bytedance/Downloads/安卓app/GoodWeather-Project/文档/GoodWeather综合实训报告.docx -t plain -o /tmp/goodweather-practice-report/docx.txt
```

Expected: extraction succeeds.

- [ ] **Step 2: Check required content in both outputs**

Confirm both contain:

```text
国家开放大学软件学院实践类课程综合考核材料
基于 Android 的 GoodWeather 天气预报系统设计与实现
摘   要
关键词
一、项目概述
五、系统测试与部署
总结
参考文献
```

- [ ] **Step 3: Check counts**

Confirm:

```text
Unknown personal fields use 待填写
15 references
At least 3 diagrams
At least 4 code excerpts or screenshots
Body exceeds 2500 Chinese characters
```

- [ ] **Step 4: Deliver**

Provide `computer://` links to both final files and briefly report page count, validation outcome, and intentionally retained personal-information placeholders.
