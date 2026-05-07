# Reader 与 Legado 文本源兼容 Spec

生成日期：2026-05-06

## 1. 背景

`reader` 是 `legado` 移动端阅读能力的服务端/网页化简化版本。当前目标不是完整追齐移动端 App，而是让 `reader` 能稳定兼容指定书源列表中的文本小说源：

```text
https://gcore.jsdelivr.net/gh/mumuceo/file01/202604/894_31e14ea5d95f46a5319bdacd1daa99b4.json
```

该书源列表经抽样和统计后具有以下特征：

- 总书源数约 382 个。
- 文本小说源约 279 个。
- 音频源约 23 个。
- 漫画/图片源约 80 个。
- `enabledCookieJar=true` 的源约 122 个。
- 使用 `sourceRegex` 的源约 16 个。
- 规则中包含 `@js`、`<js>`、`java.*` 调用的源数量较多。
- 部分源包含 `{ "webView": true }` 或依赖 WebView 获取页面/资源。

## 2. 目标

本次兼容工作的目标是：

1. 仅保留并支持文本小说源，即 `bookSourceType == 0`。
2. 自动剔除音频、漫画/图片、文件类和未知类型源。
3. 提升文本源导入、搜索、发现、详情、目录、正文的成功率。
4. 支持文本源常见的 Cookie、Header、POST、编码、JS 规则、分页目录、分页正文。
5. 兼容 WebView 相关字段的识别和保留，并在实际需要 WebView 执行时给出明确不支持错误。
6. 对无法支持的源给出明确错误或导入报告，而不是静默失败。

### 2.1 当前实现状态

截至 2026-05-07，本轮已完成：

1. 预检和书源样本准备。
2. `BaseSource`、`BookSource`、`RssSource`、`SourceAnalyzer` 兼容 `enabledCookieJar`、`loginUi`、`variableComment`、`ruleReview` 等字段。
3. 导入报告、类型过滤、批量导入过滤报告、单源保存拒绝非文本源，以及导入过滤测试。
4. `CookieStore` 持久化、响应 `Set-Cookie` 保存、请求 Cookie 注入；仅在 `enabledCookieJar=true` 时启用。
5. URL option 兼容 `method`、`charset`、`headers`、`body`、`retry`、`type`、`webView`、`webJs`、`js`。
6. `JsExtensions` 常用扩展补齐，包括 `sha`、`decodeURI`、`time`、`random` 等。
7. WebView 字段兼容与明确失败边界：识别并保留 `webView`、`webJs` 字段；当前不实现 JavaFX/Chromium WebView Provider。遇到需要 WebView 的执行路径时抛出明确 `NoStackTraceException`：`当前书源需要 WebView，但 reader 服务版暂未启用 WebView 执行环境`。`java.webView(...)` 同样明确失败。
8. 离线回归测试覆盖：导入过滤、WebView 边界、POST body + GB2312 charset + 自定义 header、chapterUrl 的 `,{"webView":true}` option 保留、正文 `content`/`replaceRegex`/`@js`/`<js>`。

按用户要求，缓存/导出/搜索回归、RSS 后续对齐、替换规则后续对齐暂缓，不作为本轮继续实施内容。

## 3. 非目标

以下能力暂不实现：

1. 音频源阅读或播放。
2. 漫画/图片源阅读体验。
3. 文件类源支持。
4. 人工验证码。
5. 扫码登录。
6. 需要用户交互的网页登录。
7. WebRTC、Service Worker、复杂视频播放。
8. 完整 Chromium 级别反爬兼容。
9. 完整迁移移动端 Android WebView 逻辑。
10. 把移动端 App 的 UI、通知、横竖屏、权限、ContentProvider 等能力搬到服务端。
11. 一次性替换现有 JSON 文件存储为数据库。
12. 将移动端完整阅读 UI 配置、TTS 前台服务、通知、系统文件选择器迁移到服务端。

### 3.1 整体能力对齐蓝图

本 Spec 的核心是书源兼容，但整体方向仍需与 `legado` 移动端当前能力保持同一语义。对齐原则如下：

1. 只对齐 `reader` 已经具备入口或产品形态合理的能力。
2. 服务端特有能力保留服务端形态，不强行改成移动端语义。
3. 移动端依赖 Android 的能力只抽取可在 JVM 服务端运行的核心逻辑。
4. 优先保证文本小说源主链路，其次再补 RSS、替换规则、缓存、导出等周边能力。

#### 3.1.1 书源与解析引擎

对齐目标：

- 书源字段兼容移动端常见 JSON。
- 搜索、发现、详情、目录、正文的规则执行语义尽量一致。
- 支持 Cookie、Header、POST、编码、`@js`、`<js>`、`java.*`。
- 识别并保留 WebView 相关字段；实际 WebView 渲染执行暂不支持。
- `sourceRegex` 资源嗅探依赖 WebView，暂不实现。

本轮状态：

- 属于本轮核心范围。
- 只支持文本小说源。
- 音频、漫画/图片、文件类源自动剔除。

#### 3.1.2 书架、进度、分组、书签

`reader` 已有书架、阅读进度、分组、书签等 API。移动端使用 Room/SQLite 管理书籍、章节、阅读记录、书签和排序状态。

对齐目标：

- 保持 `reader` 现有多用户书架模型。
- 保证导入新书源后，书籍详情、目录、进度保存不破坏旧数据。
- 后续可补齐移动端更完整的阅读记录字段，但不作为本轮前置条件。

本轮要求：

- 书源兼容改造不得破坏现有书架、进度、分组、书签数据结构。
- Cookie 或 WebView 失败不得污染书籍进度。
- 换源、搜索其他来源等现有文本能力应继续可用。

#### 3.1.3 RSS

`reader` 已有 RSS 源管理和 RSS 内容接口。移动端 RSS 源模型同样包含 Cookie、Header、登录检测、并发限速等能力。

对齐目标：

- RSS 源后续复用同一套 CookieStore、Header、并发限速和 JS 扩展。
- RSS 文章、正文、已读/收藏状态可在后续阶段补齐。

本轮状态：

- RSS 不作为指定书源 JSON 兼容的阻塞项。
- CookieStore 设计必须预留 RSS 复用能力。
- RSS 对齐已按用户要求暂缓，不在本轮继续实施。

#### 3.1.4 替换规则

`reader` 已有替换规则保存、批量保存、删除、批量删除、获取等能力。移动端额外有替换规则测试能力。

对齐目标：

- 正文获取后应用替换规则的顺序保持稳定。
- 后续补 `/reader3/testReplaceRule` 或等价测试接口。

本轮要求：

- 文本源正文规则中的 `replaceRegex` 必须继续生效。
- 全局替换规则不得因 WebView 或 Cookie 改造失效。

#### 3.1.5 本地书

`reader` 已有本地书导入、上传、刷新、导出等能力。移动端本地书格式支持更广。

对齐目标：

- 第一优先级保持 TXT、EPUB、UMD 等当前可用文本格式稳定。
- MOBI、PDF 后续单独评估。

本轮状态：

- 本地书不是本轮书源兼容的改造对象。
- 不因过滤音频/漫画网络书源而影响本地书导入。

#### 3.1.6 缓存、导出、全文搜索

`reader` 已有缓存书籍、缓存信息、删除缓存、导出 TXT/EPUB、正文搜索等服务端能力。移动端缓存更贴近离线阅读体验。

对齐目标：

- 缓存和导出应复用同一套正文获取链路。
- WebView、Cookie、`sourceRegex` 支持后，缓存和导出也能读取同样正文。
- 全文搜索优先基于已缓存正文，不应触发不可控的大量联网请求。

本轮要求：

- 改造 `AnalyzeUrl` 和正文链路时，不应只修阅读接口而遗漏缓存/导出调用路径。
- 当前缓存、导出、全文搜索回归已按用户要求暂缓，不在本轮继续实施。

#### 3.1.7 WebDAV、多用户、本地文件库

`reader` 是服务端产品，已有多用户、本地文件管理和 WebDAV Server；移动端主要是 WebDAV Client 语义。

对齐目标：

- 保留 `reader` 多用户和 WebDAV Server 形态。
- Cookie、缓存、书架数据必须考虑用户隔离。
- 不把 WebDAV Server 能力迁移为移动端语义。

本轮要求：

- CookieStore 设计必须预留用户维度。
- WebView 和普通 HTTP 请求不得跨用户共享登录态。

#### 3.1.8 存储演进

当前 `reader` 主要使用 JSON 文件和本地目录。移动端使用 Room/SQLite。

对齐目标：

- 短期保持 JSON 存储，避免一次性大迁移。
- 新增 Cookie、导入报告、兼容状态等数据时，应有清晰文件布局和版本兼容。
- 中长期可评估 SQLite/H2，但不作为本轮目标。

本轮要求：

- 新增持久化文件不得破坏现有 storage 布局。
- 写文件应采用原子写或临时文件替换，避免并发写损坏。

## 4. 书源导入规格

### 4.1 类型过滤

导入书源时按 `bookSourceType` 过滤：

```text
bookSourceType == 0  -> 导入
bookSourceType == 1  -> 跳过，原因 audio source unsupported
bookSourceType == 2  -> 跳过，原因 image source unsupported
bookSourceType == 3  -> 跳过，原因 file source unsupported
其他或空值          -> 默认按文本源处理；解析失败时跳过
```

说明：

- 当前指定书源列表中主要类型为 `0`、`1`、`2`。
- 移动端部分历史源可能缺失 `bookSourceType`，为兼容旧文本源，空值不应直接剔除。
- 过滤逻辑应放在书源导入/保存入口，不放入底层 `BookSource.fromJsonArray`，避免污染通用解析能力。

### 4.2 导入报告

批量导入接口应返回导入报告。报告至少包含：

```json
{
  "imported": 279,
  "skipped": 103,
  "skippedByType": {
    "audio": 23,
    "image": 80,
    "file": 0,
    "unknown": 0
  },
  "skippedSources": [
    {
      "name": "source name",
      "url": "source url",
      "type": 2,
      "reason": "image source unsupported"
    }
  ]
}
```

兼容要求：

- 旧前端如果只依赖原有成功/失败字段，不应被破坏。
- 新增报告字段应作为扩展字段返回。
- 单个书源保存接口如果遇到非文本源，应返回可读错误，不应写入启用列表。

## 5. 书源模型兼容规格

`BookSource` 需要兼容指定书源列表中的常见字段。

### 5.1 顶层字段

需要识别并保存：

- `bookSourceName`
- `bookSourceGroup`
- `bookSourceUrl`
- `bookSourceType`
- `bookUrlPattern`
- `customOrder`
- `enabled`
- `enabledExplore`
- `enabledCookieJar`
- `concurrentRate`
- `header`
- `loginUrl`
- `loginUi`
- `loginCheckJs`
- `bookSourceComment`
- `variableComment`
- `lastUpdateTime`
- `respondTime`
- `weight`
- `exploreUrl`
- `searchUrl`

第一阶段不要求完整使用 `loginUi`、`variableComment`，但导入和保存时不应导致解析失败。

### 5.2 规则字段

需要兼容：

- `ruleSearch`
- `ruleExplore`
- `ruleBookInfo`
- `ruleToc`
- `ruleContent`
- `ruleReview`

其中 `ruleReview` 第一阶段不实现评论能力，但应允许为空对象或未知对象存在，不应导致整源导入失败。

### 5.3 正文字段

`ruleContent` 需要支持：

- `content`
- `nextContentUrl`
- `replaceRegex`
- `webJs`
- `sourceRegex`
- `imageStyle`

`imageStyle` 对文本阅读影响较小，第一阶段仅保存，不参与核心正文渲染。

## 6. 文本源主链路规格

文本源应支持以下链路：

```text
导入书源
  -> 搜索或发现
  -> 获取书籍详情
  -> 获取目录
  -> 获取章节正文
  -> 处理分页正文
  -> 应用替换规则
  -> 返回网页阅读器
```

### 6.1 搜索

应支持：

- GET 搜索 URL。
- POST 搜索 URL。
- URL option 中的 `method`、`charset`、`headers`、`body`。
- `{{key}}`、`{{page}}`。
- `@js:` 和 `<js>` 搜索 URL。
- 搜索规则中的 CSS、XPath、JsonPath、正则和 JS 组合。

### 6.2 发现

应支持：

- 普通字符串发现配置。
- JSON 数组形式发现配置。
- `@js:` 生成发现分类。
- 发现页分页。

不保证：

- 发现配置中依赖人工选择源变量后才能工作的模板源。

### 6.3 详情

应支持：

- `ruleBookInfo.init`。
- 书名、作者、分类、简介、封面、最新章节、目录 URL、字数。
- 详情 URL 相对路径转绝对路径。
- `canReName` 字段保存和基础解析。

### 6.4 目录

应支持：

- 普通目录规则。
- `preUpdateJs`。
- `nextTocUrl` 分页目录。
- `isVolume`。
- `isVip` 字段保存。
- 章节更新时间字段 `updateTime`。

### 6.5 正文

应支持：

- 普通正文规则。
- `nextContentUrl` 分页正文。
- `replaceRegex`。
- `webJs`。
- `sourceRegex`。
- 章节 URL 相对路径转绝对路径。

当前状态：

- `sourceRegex` 字段可被模型保留，但 WebView 资源嗅探暂不实现。
- 需要 WebView 的正文执行路径不会尝试渲染页面，而是返回明确不支持错误。

## 7. JS 扩展规格

指定书源列表大量使用 `@js`、`<js>` 和 `java.*`。第一阶段需要保证常用扩展可用。

### 7.1 必须支持

- `java.ajax`
- `java.ajaxAll`
- `java.getCookie`
- `java.getElement`
- `java.getElements`
- `java.toNumChapter`
- `java.toast`
- `java.longToast`
- `java.md5Encode`
- `java.base64Encode`
- `java.base64Decode`
- `java.encodeURI`
- `java.put`
- `java.get`

### 7.2 服务端降级

移动端 UI 行为在服务端降级：

```text
java.toast / java.longToast -> 写 debug log，不弹 UI
java.startBrowser          -> 返回不支持提示，不打开浏览器
验证码相关调用             -> 返回明确不支持
```

## 8. Cookie 规格

当前 `CookieStore` 为空实现，必须补齐。

### 8.1 存储范围

Cookie 应至少按以下维度隔离：

```text
用户标识 + 书源 key
```

如果暂时无法接入用户上下文，第一阶段可先按书源 key 隔离，但需要预留用户维度。

### 8.2 行为

当 `enabledCookieJar == true`：

1. 请求前从 `CookieStore` 读取 Cookie。
2. 合并 URL option/header 中手写 Cookie。
3. 请求后保存响应 `Set-Cookie`。
4. JS 中 `java.getCookie` 能读取同一份 Cookie。
5. WebView Cookie 写回依赖后续 WebView Provider，当前暂不实现。

当 `enabledCookieJar != true`：

1. 不自动保存响应 Cookie。
2. 仍允许请求 header 中显式 Cookie 生效。

## 9. WebView 兼容规格

### 9.1 当前技术路线

当前只实现 WebView 字段兼容与明确失败边界，不实现实际页面渲染执行：

1. URL option 中的 `{ "webView": true }` 会被识别和保留。
2. `webJs` 字段会被识别和保留。
3. 章节 URL 中的 `,{"webView":true}` option 在目录解析和 `BookChapter.getAbsoluteURL()` 中不丢失。
4. 执行路径实际需要 WebView 时，抛出明确 `NoStackTraceException`：`当前书源需要 WebView，但 reader 服务版暂未启用 WebView 执行环境`。
5. `java.webView(...)` 同样明确失败，不会假装返回页面内容。

### 9.2 当前不支持能力

当前不支持：

- JavaFX WebView Provider。
- Chromium WebView Provider。
- WebView 页面加载、JS 注入和 DOM 渲染。
- `sourceRegex` 资源 URL 嗅探。
- WebView Cookie 写回 `CookieStore`。
- 人工验证码。
- 扫码登录。
- 需要用户点击、输入、选择的网页登录。
- Service Worker。
- WebRTC。
- 音视频播放。
- 漫画源图片阅读体验。
- 复杂 Chrome-only API。

### 9.3 后续可选项

以下能力仅作为后续评估项，不属于本轮已实现内容：

1. JavaFX WebView Provider。
2. Chromium Provider。
3. Docker/Linux headless WebView 支持。
4. `sourceRegex` 资源嗅探。
5. WebView Cookie 写回。

## 10. 配置规格

后续如启用实际 WebView Provider，可再评估新增配置：

```properties
reader.source.import.onlyText=true
reader.webview.enabled=true
reader.webview.timeoutMillis=30000
```

默认行为：

- `reader.source.import.onlyText=true`
- `reader.webview.enabled=false`
- `reader.webview.timeoutMillis=30000`

当前实现等价于 WebView 执行环境未启用；遇到 WebView 源时返回明确错误，不应回退成错误正文。

## 11. 错误处理

错误信息应可区分：

- 非文本源被跳过。
- 书源 JSON 解析失败。
- 规则解析失败。
- 请求超时。
- Cookie 不可用。
- WebView 未启用。
- WebView 初始化失败。
- WebView 加载超时。
- 当前源需要交互式登录，服务端不支持。

控制器返回给前端的错误应简洁，debug log 保留详细堆栈。

## 12. 验收标准

使用指定书源列表验收。

### 12.1 导入验收

1. 批量导入不崩溃。
2. 只导入文本源。
3. 音频源被跳过。
4. 漫画/图片源被跳过。
5. 返回导入报告。
6. 被跳过源有明确名称、URL、类型、原因。

### 12.2 普通文本源验收

至少选择 5 个普通文本源，验证：

1. 搜索有结果。
2. 可打开详情。
3. 可拉取目录。
4. 可读取第一章正文。
5. 分页正文可继续拼接。

### 12.3 Cookie 文本源验收

至少选择 2 个 `enabledCookieJar=true` 的文本源，验证：

1. 首次请求后 Cookie 被保存。
2. 后续请求带上 Cookie。
3. `java.getCookie` 可读取 Cookie。
4. 不影响未启用 CookieJar 的源。

### 12.4 WebView 边界验收

当前验收目标不是 WebView 页面可加载，而是字段兼容和失败边界明确：

1. `webView`、`webJs` 字段导入和保存不丢失。
2. chapterUrl 的 `,{"webView":true}` option 在目录解析和绝对 URL 转换中不丢失。
3. 需要 WebView 执行时返回 `当前书源需要 WebView，但 reader 服务版暂未启用 WebView 执行环境`。
4. `java.webView(...)` 明确失败。

### 12.5 已覆盖离线回归测试

本轮已覆盖：

1. 导入过滤。
2. WebView 明确失败边界。
3. POST body + GB2312 charset + 自定义 header。
4. chapterUrl option 保留。
5. 正文 `content`、`replaceRegex`、`@js`、`<js>`。

### 12.6 构建验收

至少执行：

```bash
./build.sh sync
./build.sh win
```

如果仅验证后端改动，也应至少执行：

```bash
./build.sh serve
```

## 13. 实施阶段

### 阶段 1：导入过滤与模型兼容

1. 补 `enabledCookieJar` 等模型字段。
2. 允许 `ruleReview`、`loginUi`、`variableComment` 存在。
3. 在导入/保存入口过滤非文本源。
4. 返回导入报告。

### 阶段 2：CookieStore

1. 实现 Cookie 持久化。
2. 接入普通 HTTP 请求。
3. 接入 JS `java.getCookie`。
4. 保留用户隔离扩展点。

### 阶段 3：普通文本源兼容增强

1. 检查 POST、charset、header、body。
2. 补常用 JS 扩展。
3. 修正分页目录和分页正文边界。
4. 完善错误信息。

### 阶段 4：WebView 字段兼容与明确失败边界

1. 识别并保留 `webView`、`webJs` 字段。
2. 保证 chapterUrl option 不丢失。
3. 需要 WebView 执行时返回明确不支持错误。
4. JavaFX/Chromium WebView Provider、`sourceRegex` 资源嗅探、WebView Cookie 写回暂不实现。

### 阶段 5：样本回归

1. 固化指定 JSON 的导入样本。
2. 建立文本源样本清单。
3. 建立 Cookie 源样本清单。
4. 建立 WebView 源样本清单。
5. 执行构建和运行验证。

### 阶段 6：周边能力回归（暂缓）

1. 回归书架、阅读进度、分组、书签。
2. 回归缓存书籍、导出 TXT/EPUB、正文搜索。
3. 回归 RSS 源列表和 RSS 正文基础接口。
4. 回归 WebDAV、本地文件库、多用户隔离。

### 阶段 7：后续能力对齐（暂缓）

1. 增加替换规则测试接口。
2. RSS 复用 CookieStore 和并发限速。
3. 缓存/导出统一复用 WebView 与 Cookie 后的正文链路。
4. 评估 CookieStore 的用户隔离持久化布局。
5. 评估 SQLite/H2 等服务端存储演进，不在本轮直接迁移。

## 14. 优先级

### P0：必须完成

1. 非文本源过滤。
2. 书源模型兼容。
3. CookieStore 基础实现。
4. 普通 HTTP 文本源搜索、详情、目录、正文回归。
5. WebView 源明确可用或明确报错。

### P1：应完成

1. WebView 字段兼容与明确失败边界。
2. `webJs` 字段保留；实际 WebView 执行暂缓。
3. 常用 `java.*` 扩展补齐。
4. 缓存/导出路径复用新正文链路（暂缓）。
5. 导入报告前端展示。

### P2：后续增强

1. 替换规则测试 API。
2. RSS Cookie/限速对齐。
3. 更完整的多用户 Cookie 隔离。
4. Chromium Provider 评估。
5. 服务端存储升级评估。

## 15. 风险

1. JavaFX WebView 在无图形环境下可能不可用。
2. 部分 WebView 源实际依赖人工登录或验证码，即使启用 WebView 也无法服务端自动兼容。
3. Cookie 隔离如果没有接入用户上下文，短期可能存在多用户串 Cookie 风险。
4. 复杂 `java.*` 扩展可能仍有遗漏，需要通过样本补齐。
5. 过滤非文本源可能改变用户预期，因此必须返回导入报告。
6. 缓存、导出、正文阅读如果各自走不同获取链路，可能出现阅读可用但导出失败的问题。
7. 一次性改造过多周边能力会扩大回归面，因此必须先完成文本源主链路。

## 16. 后续扩展

若第一阶段稳定后，可再评估：

1. Chromium Provider。
2. Docker/Linux headless WebView 支持。
3. 更完整的登录态管理。
4. 书源兼容测试自动化。
5. 按用户隔离的 Cookie 文件布局。
6. RSS 已读/收藏状态对齐。
7. 本地 MOBI/PDF 支持可行性。
8. SQLite/H2 等服务端持久化方案。
