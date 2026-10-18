# TVBox 配置生成器 · Android App

基于 JS 逆向分析 `https://17nas.com/tvbox-config.php` 后,
用 Kotlin + Jetpack Compose 原生重写的 Android 应用。

> 在线版只能依赖网页,这个 App 可以完全离线工作,
> 并原生实现 TVBox 配置 URL 的 Base64 编解码算法,
> 与 17NAS 在线版**字节级兼容**。

---

## ✨ 功能

- 📺 **视频源 (Sites)**: 多源管理,可搜索/快速搜索/可筛选开关
- 🔌 **解析接口 (Parses)**: 多解析,自定义平台标识(Flags)
- 📡 **直播源 (Lives)**: M3U / TXT 双格式,可选 EPG
- 🌐 **全局设置**: 爬虫 Jar 包地址 + 背景图片地址
- 🔐 **Base64 编码**: TVBox 标准 URL 格式 `8位数字**base64`
- 📋 **JSON 编辑器**: 直接编辑/导入 JSON 配置
- 💾 **多套预设**: 通过 DataStore 持久化保存多个配置
- 📲 **离线工作**: 不依赖任何在线服务

---

## 🔍 JS 逆向分析要点

### 1. 核心数据结构

`https://17nas.com/assets/js/tvbox-config-simple.js` 的配置 schema:

```js
config = {
    spider: "",                  // 全局：爬虫/接口JAR URL
    wallpaper: "https://api.kdcc.cn",  // 全局：壁纸API
    sites: [],                   // 视频源数组
    parses: [],                  // 解析接口数组
    lives: []                    // 直播源数组
}

site = { key, name, type, api, searchable, quickSearch, filterable }
parse = { name, type, url, ext: { flag: [...] } }
live  = { name, type, url, epg }
```

### 2. Base64 编码算法 (UTF-8 安全)

逆向后的 JS 原始逻辑:

```js
function generateBase64() {
    const str = JSON.stringify(config);  // 关键 1: 紧凑 JSON, 无缩进
    const base64 = btoa(
        encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, (m, p1) =>
            String.fromCharCode('0x' + p1)
        )
    );  // 关键 2: UTF-8 → 字节 → Base64
    const digits = Array.from({length: 8}, () => Math.floor(Math.random() * 10)).join('');
    return `${digits}**${base64}`;  // 关键 3: 8位随机数字 + "**" 前缀
}
```

**Kotlin 实现** (见 [`Base64Util.kt`](app/src/main/java/com/tvbox/configgen/data/Base64Util.kt)):

```kotlin
fun encodeTvBox(json: String): String {
    val bytes = json.toByteArray(Charsets.UTF_8)
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "${randomDigits()}**$base64"
}
```

### 3. 字段顺序一致性

JSON 序列化时必须保持字段顺序,TVBox 客户端不强制要求,但保持一致便于 diff 与肉眼比对。
本项目使用 `kotlinx.serialization` 默认的字段声明顺序,正好与 JS 的 `JSON.stringify` 输出一致。

### 4. 默认平台标识

```js
const defaultFlags = ["youku", "qq", "iqiyi", "qiyi", "letv", "sohu", "tudou", "pptv", "mgtv", "wasu"];
```

完整移植到 `data/Models.kt` 中的 `DEFAULT_FLAGS` 常量。

---

## 🚀 编译

### 本地编译

```bash
cd android-app
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions 编译

推送到 `main` 分支即自动触发编译,
产物会作为 artifact 上传,在 Actions 页面下载。

工作流定义: [`.github/workflows/build.yml`](.github/workflows/build.yml)

---

## 📁 项目结构

```
android-app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tvbox/configgen/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── Models.kt         # 数据类, 对齐 JS schema
│       │   │   ├── Base64Util.kt     # ★ TVBox Base64 算法
│       │   │   └── ConfigRepository.kt  # DataStore 多预设
│       │   └── ui/
│       │       ├── ConfigViewModel.kt
│       │       ├── screens/MainScreen.kt
│       │       └── theme/Theme.kt
│       └── res/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties  (SDK 路径, 不提交)
```

---

## 📜 License

MIT