# Reader 与 Legado 文本源兼容实施计划

生成日期：2026-05-06
对应规格：`SPEC_READER_LEGADO_COMPAT.md`

## 0. 执行原则

1. 本计划只实现文本小说源兼容，保留 `bookSourceType == 0`。
2. 自动剔除音频源、漫画/图片源、文件类源。
3. WebView 当前只做字段兼容与明确失败边界，不做实际页面渲染执行。
4. 不替换现有 JSON 存储为数据库。
5. 不做 git 操作。
6. 每个阶段完成后先运行对应验证，再进入下一阶段；本次 7.2 仅更新文档，不运行 build/test。

## 0.1 当前执行状态

截至 2026-05-07，本轮真实完成状态如下：

1. 已完成 1.1/1.2：预检和样本准备。
2. 已完成 2.1-2.4：`BaseSource`、`BookSource`、`RssSource`、`SourceAnalyzer` 兼容字段，包括 `enabledCookieJar`、`loginUi`、`variableComment`、`ruleReview`。
3. 已完成 3.1-3.5：导入报告、类型过滤、批量导入过滤报告、单源保存拒绝非文本源、导入过滤测试。
4. 已完成 4.1-4.4：`CookieStore` 持久化、`Set-Cookie` 保存、请求 Cookie 注入；只有 `enabledCookieJar=true` 时启用。
5. 已完成 5.1：URL option 兼容 `method`、`charset`、`headers`、`body`、`retry`、`type`、`webView`、`webJs`、`js`。
6. 已完成 5.2：`JsExtensions` 常用扩展补齐，包括 `sha`、`decodeURI`、`time`、`random` 等。
7. 已完成 5.3：WebView 兼容边界。当前不实现 JavaFX/Chromium WebView Provider；只识别并保留 `webView`、`webJs` 字段，遇到需要 WebView 的执行路径抛明确 `NoStackTraceException`：`当前书源需要 WebView，但 reader 服务版暂未启用 WebView 执行环境`。`java.webView(...)` 同样明确失败。
8. 已完成 6.1：离线测试验证 POST body + GB2312 charset + 自定义 header。
9. 已完成 6.2：离线测试验证 chapterUrl 的 `,{"webView":true}` option 在目录解析和 `BookChapter.getAbsoluteURL()` 中不丢。
10. 已完成 6.3：离线测试验证正文规则抽取、`replaceRegex`、`@js`、`<js>`。
11. 已完成 7.1：更新 spec/plan/kb 中已实现能力与暂不支持边界。
12. 已完成 7.2：形成最终兼容说明与运行验证说明，产出 `READER_LEGADO_COMPAT_RUNBOOK.md`。
13. 已完成主 session 构建验证：`./build.sh win` 使用 Java 11 执行成功，Gradle 输出 `BUILD SUCCESSFUL`，退出码为 0；`iscc`/`candle` 缺失仍为非阻塞安装器/MSI 日志。
14. 已完成主 session 服务/API 验收：使用纯 Spring Boot 入口在 18080 临时启动，`/health`、首页 `/`、`/reader3/getSystemInfo` 正常；混合书源样本导入后仅保留 2 个文本源。
15. 已完成联网指定源验收：指定 URL 下载成功；382 个源中导入 279 个文本源，剔除 23 个音频源和 80 个图片/漫画源；`晋江APP端` 搜索、详情、目录、正文链路跑通。

说明：此处第 7.2 指 HANDOFF 中的当前执行项“形成最终兼容说明与运行验证说明”，不是下方历史计划中的“7.2 实现 JavaFX 初始化”。当前已完成构建验证、本地服务/API 验收和联网指定源主链路验收。

按用户要求，第八项缓存/导出/搜索回归，以及第九项 RSS 和替换规则后续对齐暂时不做。JavaFX WebView Provider、Chromium Provider、`sourceRegex` 资源嗅探、WebView Cookie 回写也不属于本轮继续执行内容。

## 1. 预检与样本准备

### 1.1 确认现有入口

读取并确认以下文件：

- `src/main/java/com/htmake/reader/api/YueduApi.kt`
- `src/main/java/com/htmake/reader/api/controller/BookSourceController.kt`
- `src/main/java/io/legado/app/utils/SourceAnalyzer.kt`
- `src/main/java/io/legado/app/data/entities/BookSource.kt`
- `src/main/java/io/legado/app/data/entities/BaseSource.kt`
- `src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`
- `src/main/java/io/legado/app/model/webBook/WebBook.kt`
- `src/main/java/io/legado/app/help/http/CookieStore.kt`
- `src/main/java/io/legado/app/help/JsExtensions.kt`

验收：

```bash
bash -n build.sh
```

### 1.2 准备书源样本

创建本地样本目录：

```text
src/test/resources/booksource/
```

保存以下样本：

- `894_full.json`：指定完整书源列表。
- `text_basic.json`：普通文本源样本。
- `text_cookie.json`：`enabledCookieJar=true` 文本源样本。
- `text_webview.json`：包含 `{ "webView": true }` 的文本源样本。
- `mixed_types.json`：包含文本、音频、图片/漫画源的混合样本。

说明：

- 样本文件可从指定 JSON 中抽取。
- 如果完整列表过大，测试只使用抽样文件，完整列表用于手工验收。

验收：

```bash
Get-ChildItem src/test/resources/booksource
```

## 2. 书源模型兼容

### 2.1 补齐 BaseSource 字段

修改：

- `src/main/java/io/legado/app/data/entities/BaseSource.kt`

新增属性：

```kotlin
var enabledCookieJar: Boolean?
```

要求：

- 所有实现 `BaseSource` 的实体必须补齐该字段。
- 默认值应保证旧数据不报错。

### 2.2 补齐 BookSource 字段

修改：

- `src/main/java/io/legado/app/data/entities/BookSource.kt`

新增或确认字段：

```kotlin
override var enabledCookieJar: Boolean? = false
var loginUi: String? = null
var variableComment: String? = null
var ruleReview: Any? = null
```

要求：

- `ruleReview` 第一阶段只保存/忽略，不实现评论能力。
- `loginUi`、`variableComment` 不参与业务执行，但导入时不能失败。
- `equal()` 如用于判断书源变化，应纳入 `enabledCookieJar`、`loginCheckJs`、`concurrentRate`。

### 2.3 补齐 RssSource 字段

修改：

- `src/main/java/io/legado/app/data/entities/RssSource.kt`

确认或新增：

```kotlin
override var enabledCookieJar: Boolean? = false
```

要求：

- 本轮 RSS 不接入完整改造，但模型必须能兼容后续 CookieStore 复用。

### 2.4 补齐 SourceAnalyzer 中间模型

修改：

- `src/main/java/io/legado/app/utils/SourceAnalyzer.kt`

在 `BookSourceAny` 中新增：

```kotlin
var enabledCookieJar: Boolean? = false
var variableComment: String? = null
var ruleReview: Any? = null
```

在新格式书源转换分支中赋值：

```kotlin
source.enabledCookieJar = sourceAny.enabledCookieJar
source.variableComment = sourceAny.variableComment
source.ruleReview = sourceAny.ruleReview
```

验收：

```bash
./gradlew compileKotlin
```

预期：

- 编译通过。
- 旧书源 JSON 仍能解析。

## 3. 非文本源过滤与导入报告

### 3.1 定义导入报告模型

建议新增文件：

- `src/main/java/com/htmake/reader/api/response/BookSourceImportReport.kt`

内容结构：

```kotlin
data class BookSourceImportReport(
    val imported: Int = 0,
    val skipped: Int = 0,
    val skippedByType: Map<String, Int> = emptyMap(),
    val skippedSources: List<SkippedBookSource> = emptyList()
)

data class SkippedBookSource(
    val name: String?,
    val url: String?,
    val type: Int?,
    val reason: String
)
```

如果项目已有统一响应模型，应复用现有 `ReturnData` 扩展字段，不新增重复响应系统。

### 3.2 实现类型判断工具

建议新增文件：

- `src/main/java/com/htmake/reader/utils/BookSourceTypeFilter.kt`

规则：

```kotlin
object BookSourceTypeFilter {
    fun isSupportedTextType(type: Int?): Boolean {
        return type == null || type == 0
    }

    fun reason(type: Int?): String {
        return when (type) {
            1 -> "audio source unsupported"
            2 -> "image source unsupported"
            3 -> "file source unsupported"
            else -> "unknown source type unsupported"
        }
    }

    fun bucket(type: Int?): String {
        return when (type) {
            1 -> "audio"
            2 -> "image"
            3 -> "file"
            else -> "unknown"
        }
    }
}
```

注意：

- 空类型按文本源兼容。
- 明确 `1`、`2`、`3` 跳过。

### 3.3 接入批量导入

修改：

- `src/main/java/com/htmake/reader/api/controller/BookSourceController.kt`

目标：

1. 批量解析书源后过滤。
2. 仅保存文本源。
3. 将跳过项记录进导入报告。
4. 返回数据中包含报告。

兼容要求：

- 原有成功状态保持。
- 前端旧逻辑如果只看成功状态，不应失败。
- 新报告字段可以放在 `data.report` 或同级扩展字段中，以现有响应结构为准。

### 3.4 接入单个保存

修改：

- `src/main/java/com/htmake/reader/api/controller/BookSourceController.kt`

规则：

- 单个保存 `bookSourceType != 0` 时拒绝保存。
- 返回错误信息：

```text
unsupported book source type: audio/image/file
```

验收：

```bash
./gradlew compileKotlin
```

手工验收：

- 导入混合样本后只保留文本源。
- 响应中能看到音频、图片/漫画跳过数量。

## 4. CookieStore 实现

### 4.1 设计持久化路径

建议路径：

```text
storage/{user}/cookies/book_sources.json
```

如果第一阶段拿不到用户上下文，先使用：

```text
storage/cookies/book_sources.json
```

但代码接口必须预留用户维度。

### 4.2 实现 CookieStore

修改：

- `src/main/java/io/legado/app/help/http/CookieStore.kt`

实现：

```kotlin
override fun setCookie(url: String, cookie: String?)
override fun replaceCookie(url: String, cookie: String)
override fun getCookie(url: String): String
override fun removeCookie(url: String)
fun clear()
```

要求：

- key 使用书源 key 或域名归一化结果。
- 写文件时使用临时文件替换，避免写坏 JSON。
- 读写失败写日志，不让普通请求崩溃。

### 4.3 接入响应 Set-Cookie

修改：

- `src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`
- 必要时修改 `src/main/java/io/legado/app/help/http/HttpHelper.kt` 或 OkHttp 封装位置。

行为：

1. `source.enabledCookieJar == true` 时保存响应 `Set-Cookie`。
2. `source.enabledCookieJar != true` 时不自动保存。
3. 请求 header 中显式 Cookie 仍然生效。

### 4.4 接入请求 Cookie

修改：

- `src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`

调整 `setCookie`：

- 只有 `enabledCookieJar == true` 时读取 CookieStore。
- 合并 CookieStore 和 header 中 Cookie。
- header 中 Cookie 优先级更高。

验收：

```bash
./gradlew compileKotlin
```

手工验收：

- 使用 Cookie 样本源请求两次。
- 第一次响应后文件中出现 Cookie。
- 第二次请求带上 Cookie。
- `java.getCookie` 能读取同一份 Cookie。

## 5. 文本源主链路增强

### 5.1 检查 URL option

修改：

- `src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`

确认并补齐：

- `method`
- `charset`
- `headers`
- `body`
- `retry`
- `type`
- `webView`
- `webJs`
- `js`

验收：

- GB2312/GBK POST 搜索源能正确编码关键词。
- 自定义 header 不丢失。

### 5.2 补常用 JS 扩展

修改：

- `src/main/java/io/legado/app/help/JsExtensions.kt`
- 必要时修改 `src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt`

必须确认支持：

- `ajax`
- `ajaxAll`
- `getCookie`
- `getElement`
- `getElements`
- `toNumChapter`
- `toast`
- `longToast`
- `md5Encode`
- `base64Encode`
- `base64Decode`
- `encodeURI`
- `put`
- `get`

服务端降级：

- `toast` / `longToast` 写 debug log。
- `startBrowser` 返回不支持或记录日志，不打开外部浏览器。
- 验证码相关方法返回明确不支持。

### 5.3 检查分页目录

修改：

- `src/main/java/io/legado/app/model/webBook/BookChapterList.kt`
- `src/main/java/io/legado/app/model/webBook/WebBook.kt`

确认：

- `preUpdateJs` 生效。
- `nextTocUrl` 能继续拉取分页目录。
- `isVolume`、`isVip`、`updateTime` 不导致解析失败。

### 5.4 检查分页正文

修改：

- `src/main/java/io/legado/app/model/webBook/BookContent.kt`
- `src/main/java/io/legado/app/model/webBook/WebBook.kt`

确认：

- `nextContentUrl` 能继续拼接正文。
- `replaceRegex` 顺序稳定。
- 全局替换规则仍生效。

验收：

```bash
./gradlew compileKotlin
```

手工验收：

- 普通文本源搜索、详情、目录、正文可用。
- 分页目录和分页正文样本可用。

## 6. sourceRegex 普通请求支持（历史计划，本轮不继续实现资源嗅探）

### 6.1 明确 sourceRegex 使用边界

修改：

- `src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`

规则：

- `sourceRegex` 不单独强制 WebView。
- 普通 HTTP 可以处理的资源匹配优先普通请求。
- 只有 URL option 明确 `webView=true` 时走 WebView Provider。

### 6.2 实现普通 sourceRegex 处理

目标：

- 当前请求返回正文 HTML 时，继续按普通正文规则解析。
- 如果规则期望资源 URL 嗅探，而没有 WebView，则返回明确错误：

```text
sourceRegex requires WebView resource sniffing
```

说明：

- 不要假装支持资源嗅探，否则会出现空正文。
- WebView Provider 完成后再支持资源 URL 嗅探。

验收：

- 使用含 `sourceRegex` 但不要求 WebView 的源不报错。
- 明确需要资源嗅探的源错误可读。

## 7. JavaFX WebView Provider（历史计划，本轮不执行）

状态：历史计划，仅作后续参考；本轮不执行。当前实际完成的是 WebView 字段兼容与明确失败边界，不实现 JavaFX/Chromium WebView Provider，不实现 `sourceRegex` 资源嗅探，不实现 WebView Cookie 回写。

### 7.1 新增抽象

建议新增目录：

```text
src/main/java/io/legado/app/help/webview/
```

新增文件：

- `WebViewProvider.kt`
- `WebViewRequest.kt`
- `JavaFxWebViewProvider.kt`
- `WebViewProviderFactory.kt`

接口：

```kotlin
interface WebViewProvider {
    suspend fun load(params: WebViewRequest): StrResponse
}
```

请求模型：

```kotlin
data class WebViewRequest(
    val url: String,
    val method: RequestMethod,
    val headers: Map<String, String>,
    val body: ByteArray?,
    val userAgent: String,
    val webJs: String?,
    val sourceRegex: String?,
    val timeoutMillis: Long,
    val cookieKey: String?
)
```

### 7.2 实现 JavaFX 初始化

修改/新增：

- `src/main/java/io/legado/app/help/webview/JavaFxWebViewProvider.kt`

要求：

- 能在 JavaFX Application Thread 执行。
- 如果 JavaFX 未初始化，尝试初始化。
- 初始化失败时抛出明确错误：

```text
当前书源需要 WebView，但当前运行环境无法初始化 JavaFX WebView
```

### 7.3 实现页面加载

支持：

- GET。
- 简单 POST。
- 自定义 header。
- User-Agent。
- Cookie 注入。
- 页面加载完成后延迟执行 JS。
- 无 `webJs` 时执行 `document.documentElement.outerHTML`。

超时：

- 默认 30000ms。
- 超时后清理 WebView。
- 不阻塞服务线程。

### 7.4 实现 sourceRegex 嗅探

行为：

- WebView 加载资源时匹配 `sourceRegex`。
- 命中后返回匹配 URL。
- 保存 Cookie。
- 清理 WebView。

### 7.5 接入 AnalyzeUrl

修改：

- `src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`

替换当前逻辑：

```kotlin
if (this.useWebView && useWebView) {
    throw Exception("不支持webview")
}
```

历史计划中的目标改为：

- `reader.webview.enabled=false`：返回 WebView 未启用错误。
- `reader.webview.enabled=true`：调用 `WebViewProviderFactory.get().load(...)`。
- 普通请求逻辑保持不变。

本轮实际状态：只实现 `reader.webview.enabled=false` 等价边界，即识别 WebView 字段并返回明确不支持错误；不接入 `WebViewProviderFactory`。

### 7.6 配置项

历史计划中修改配置读取位置，按项目现有配置方式接入：

```properties
# 后续 Provider 启用示例；本轮不实现
reader.webview.enabled=true
reader.webview.timeoutMillis=30000
```

如果没有统一配置类，可先在 `AppConst` 或现有配置工具中读取系统属性/环境变量。

本轮不新增上述配置入口；当前实现等价于 WebView 执行环境关闭。

验收：

```bash
./gradlew compileKotlin
```

历史计划手工验收：

- WebView 文本源能加载。
- `webJs` 能返回 HTML。
- `sourceRegex` 能命中资源 URL。
- WebView 不可用时错误明确。

## 8. 缓存、导出、搜索回归（暂缓，本轮不执行）

状态：按用户要求暂缓，本轮不执行。

### 8.1 检查调用链

读取并确认：

- `src/main/java/com/htmake/reader/api/controller/BookController.kt`
- `src/main/java/io/legado/app/model/webBook/WebBook.kt`
- 缓存书籍相关方法。
- 导出 TXT/EPUB 相关方法。
- 正文搜索相关方法。

要求：

- 缓存、导出、正文搜索最终复用同一套正文获取链路。
- 不要出现阅读接口支持 WebView/Cookie，但缓存导出仍走旧请求的问题。

### 8.2 回归现有功能

手工验证：

- 加入书架。
- 保存阅读进度。
- 获取书籍分组。
- 保存/读取书签。
- 缓存一本书。
- 导出 TXT 或 EPUB。
- 搜索已缓存正文。

## 9. RSS 和替换规则后续对齐（暂缓，本轮不执行）

状态：按用户要求暂缓，本轮不执行。

### 9.1 RSS 预留

本轮不强制实现 RSS 全面改造，但完成以下准备：

- `RssSource` 具备 `enabledCookieJar`。
- CookieStore 接口不绑定 BookSource 类型。
- `AnalyzeUrl` 能接受 `BaseSource`，供 RSS 后续复用。

### 9.2 替换规则测试接口

后续新增：

- `src/main/java/com/htmake/reader/api/controller/ReplaceRuleController.kt`
- `src/main/java/com/htmake/reader/api/YueduApi.kt`

接口建议：

```text
POST /reader3/testReplaceRule
```

输入：

```json
{
  "content": "原始正文",
  "rule": {
    "pattern": "xxx",
    "replacement": "yyy"
  }
}
```

输出：

```json
{
  "content": "替换后正文"
}
```

说明：

- 该接口列为 P2，非本轮核心阻塞项。

## 10. 测试与验证

### 10.1 编译验证

每个实现阶段至少执行：

```bash
./gradlew compileKotlin
```

最终执行：

```bash
./build.sh sync
./build.sh win
```

服务验证执行：

```bash
./build.sh serve
```

### 10.2 导入验证

使用指定完整 JSON：

```text
https://gcore.jsdelivr.net/gh/mumuceo/file01/202604/894_31e14ea5d95f46a5319bdacd1daa99b4.json
```

预期：

- 文本源导入。
- 音频源跳过。
- 漫画/图片源跳过。
- 返回导入报告。

### 10.3 文本源验证

至少选择：

- 5 个普通文本源。
- 2 个 Cookie 文本源。
- 3 个 WebView 文本源。
- 1 个分页目录源。
- 1 个分页正文源。

每个文本源验证：

1. 搜索。
2. 详情。
3. 目录。
4. 第一章正文。
5. 如有分页，验证分页拼接。

### 10.4 周边回归

验证：

- 书架保存。
- 阅读进度保存。
- 分组。
- 书签。
- 缓存。
- 导出。
- 正文搜索。
- RSS 基础接口。
- WebDAV 基础访问。
- 多用户隔离不被 CookieStore 破坏。

## 11. 推荐执行顺序

1. 预检与样本准备。已完成。
2. 书源模型兼容。已完成。
3. 非文本源过滤与导入报告。已完成。
4. CookieStore。已完成。
5. 文本源主链路增强。已完成到当前边界。
6. WebView 字段兼容与明确失败边界。已完成。
7. JavaFX/Chromium WebView Provider、`sourceRegex` 资源嗅探、WebView Cookie 回写。暂不执行。
8. 缓存、导出、搜索回归。按用户要求暂缓。
9. RSS 和替换规则后续对齐。按用户要求暂缓。
10. 最终兼容说明与运行验证说明。已完成，见 `READER_LEGADO_COMPAT_RUNBOOK.md`。
11. 最终构建验证。已完成，见 `READER_LEGADO_COMPAT_RUNBOOK.md` 的主 session 构建验证记录。
12. 本地服务/API 验收。已完成，见 `READER_LEGADO_COMPAT_RUNBOOK.md` 的主 session 服务/API 验收记录。
13. 联网指定源搜索/详情/目录/正文手工验收。已完成，见 `READER_LEGADO_COMPAT_RUNBOOK.md` 的主 session 联网指定源验收记录。

## 12. 暂停点

遇到以下情况应暂停并重新评估：

1. JavaFX WebView 在目标运行环境无法初始化。
2. CookieStore 需要跨用户隔离但当前调用链拿不到用户上下文。
3. WebView 需要引入 Chromium 才能满足样本源。
4. 导入报告返回结构会破坏前端现有逻辑。
5. 缓存/导出和阅读接口存在两套难以统一的正文获取链路。
