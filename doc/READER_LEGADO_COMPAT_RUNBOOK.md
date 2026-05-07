# Reader 与 Legado 文本源兼容运行验收说明

生成日期：2026-05-07

## 1. 文档目的

本文面向后续运行、构建验证和手工验收。当前范围仅覆盖 Legado 文本小说源兼容，不覆盖音频、漫画/图片、文件类源，也不覆盖真实 WebView Provider。

## 2. 当前已支持能力

截至本文生成时，本轮已形成以下兼容能力：

1. 文本源导入和过滤报告
   - 批量导入时仅保留 `bookSourceType == 0` 的文本小说源。
   - 音频、漫画/图片、文件类和明确未知的非文本源会被剔除。
   - 批量导入报告包含导入数量、跳过数量、按类型统计和跳过原因。
   - 单源保存遇到非文本源会明确拒绝保存。

2. CookieStore
   - 支持 Cookie 持久化。
   - 支持响应 `Set-Cookie` 保存。
   - 支持请求前注入 Cookie。
   - 仅在书源 `enabledCookieJar == true` 时启用自动 CookieJar 行为。
   - 请求 header 中显式 Cookie 仍可生效。

3. URL option
   - 已兼容 `method`、`charset`、`headers`、`body`、`retry`、`type`、`webView`、`webJs`、`js`。
   - 已通过离线回归覆盖 POST body、GB2312 charset 和自定义 header。
   - chapterUrl 中的 `,{"webView":true}` option 在目录解析和绝对 URL 转换中会保留。

4. 常用 JS 扩展
   - 已补齐常见 `java.*`/JS 扩展能力，包括 `ajax`、`ajaxAll`、`getCookie`、`getElement`、`getElements`、`toNumChapter`、`toast`、`longToast`、`md5Encode`、`base64Encode`、`base64Decode`、`encodeURI`、`put`、`get`。
   - 另已补齐常见辅助函数，例如 `sha`、`decodeURI`、`time`、`random` 等。
   - 服务端不适合执行的 UI 行为按服务端语义降级或明确失败。

5. WebView 字段识别与明确失败边界
   - 可识别并保留 `webView`、`webJs` 字段。
   - 可保留章节 URL 中的 `,{"webView":true}` option。
   - 当前不执行真实 WebView 渲染。
   - 当书源实际需要 WebView 执行时，会明确失败：`当前书源需要 WebView，但 reader 服务版暂未启用 WebView 执行环境`。
   - `java.webView(...)` 同样明确失败，不会假装返回页面内容。

6. 离线回归测试覆盖
   - 非文本源导入过滤和导入报告。
   - WebView 明确失败边界。
   - POST body + GB2312 charset + 自定义 header。
   - chapterUrl option 保留。
   - 正文 `content`、`replaceRegex`、`@js`、`<js>`。

## 3. 明确不支持或暂缓边界

以下能力不属于本轮已实现范围：

1. 音频源阅读或播放。
2. 漫画/图片源阅读体验。
3. 文件类源支持。
4. JavaFX WebView Provider。
5. Chromium WebView Provider。
6. WebView 页面加载、JS 注入、DOM 渲染。
7. `sourceRegex` 资源嗅探。
8. WebView Cookie 回写 CookieStore。
9. 人工验证码、扫码登录、需要用户交互的网页登录。
10. WebRTC、Service Worker、复杂 Chrome-only API。
11. 缓存、导出、搜索回归：按用户要求暂缓，不能视为本轮完成。
12. RSS 后续对齐：按用户要求暂缓。
13. 替换规则后续对齐：按用户要求暂缓。

## 4. 构建命令

Windows 构建使用：

```bash
./build.sh win
```

当前 `build.sh` 会选择 Java 11，以兼容项目使用的 Gradle 6.1.1。不要使用 JDK 26 作为本项目当前 Gradle 构建 JDK，否则会触发 Gradle/Groovy 兼容错误。

如需同步前端资源，可先执行：

```bash
./build.sh sync
```

本 runbook 只说明命令；本轮文档任务没有执行构建。

## 5. 运行命令

Windows 桌面打包产物可运行：

```text
build/reader/reader.exe
```

Jar 运行示例：

```bash
java -jar ./build/libs/reader-2.5.4.jar --reader.app.showUI=true --reader.server.port=8080
```

注意：直接运行 jar 时，需要保证前端资源已通过 `./build.sh sync` 同步到后端资源目录。否则访问页面时可能出现 `Resource not found`。

如果只需要启动服务和 Web API，不需要 JavaFX 桌面壳，建议使用纯 Spring Boot 入口：

```bash
java -Dloader.main=com.htmake.reader.ReaderApplicationKt -cp ./build/libs/reader-2.5.4.jar org.springframework.boot.loader.PropertiesLauncher --spring.profiles.active=prod --reader.server.port=8080 --reader.app.showUI=false
```

说明：`java -jar` 默认入口是 `ReaderUIApplicationKt`，会初始化 JavaFX；在无 GUI 或 OpenJFX native cache 不可写的环境中，可能无法作为 headless 服务验收入口。

## 6. 生成物位置

后续主 session 执行构建后，应重点确认以下产物：

```text
build/reader/reader.exe
build/reader-2.5.4-windows.zip
build/libs/reader-2.5.4.jar
```

这些路径是预期产物位置，不代表本文档任务已经重新生成。

## 7. 已知非阻塞警告

后续构建或运行验证中可能看到以下已知警告：

1. `iscc` 缺失会导致 Inno Setup 安装器生成失败日志。
2. `candle` 缺失会导致 WiX MSI 生成失败日志。
3. 上述安装器/MSI 失败日志可在 Gradle 已 `BUILD SUCCESSFUL` 且退出码为 0 时视为非阻塞。
4. Java 11 下测试或运行可能出现 Netty illegal reflective access 警告。
5. JDK 26 会触发 Gradle 6.1.1/Groovy 兼容错误，应使用 `build.sh` 选择的 Java 11。

## 8. 最小手工验收步骤

后续主 session 完成构建后，建议按以下最小步骤验收：

1. 启动服务
   - Windows：运行 `build/reader/reader.exe`。
   - 或运行 jar：`java -jar ./build/libs/reader-2.5.4.jar --reader.app.showUI=true --reader.server.port=8080`。

2. 访问端口
   - 浏览器访问 `http://localhost:8080`。
   - 如端口被占用，运行时改用其他 `--reader.server.port`。

3. 导入样本或指定书源
   - 使用指定书源列表：

```text
https://gcore.jsdelivr.net/gh/mumuceo/file01/202604/894_31e14ea5d95f46a5319bdacd1daa99b4.json
```

   - 或使用本地样本验证导入路径。

4. 确认非文本源剔除
   - 导入报告中应能看到音频、漫画/图片、文件类源的跳过统计和原因。
   - 最终保存的书源列表不应包含已识别的非文本源。

5. 验证普通文本源主链路
   - 选择普通文本源搜索书名。
   - 打开搜索结果详情。
   - 拉取目录。
   - 打开第一章正文。
   - 如样本包含分页目录或分页正文，确认能继续拼接。

6. 验证 Cookie 文本源
   - 选择 `enabledCookieJar=true` 的文本源。
   - 执行至少两次请求。
   - 确认首次响应后的 Cookie 可被后续请求复用。

7. 验证 WebView 边界
   - 导入包含 `webView` 或 `webJs` 的文本源。
   - 确认字段导入/保存不丢失。
   - 触发需要 WebView 执行的路径。
   - 预期返回明确不支持错误：`当前书源需要 WebView，但 reader 服务版暂未启用 WebView 执行环境`。

## 9. 7.2 文档子任务未执行的验证

7.2 文档子任务只新增/更新文档，当时没有执行以下验证；后续主 session 验证见下方记录：

1. 未运行 `./build.sh sync`。
2. 未运行 `./build.sh win`。
3. 未运行 Gradle build/test。
4. 未运行 npm/yarn。
5. 未启动服务。
6. 未做浏览器/API 验收。
7. 未做 git 操作。

## 10. 主 session 构建验证记录

2026-05-07 主 session 已执行：

```powershell
& "C:\Program Files\Git\bin\bash.exe" -lc "./build.sh win"
```

结果：

1. 使用 Java 11：`/c/Program Files/Java/jdk-11.0.30/bin/java`。
2. Gradle 结果：`BUILD SUCCESSFUL in 12s`。
3. 退出码：0。
4. 测试任务：`test` 为 `UP-TO-DATE`，未失败。
5. Windows EXE 已生成：`build/reader/reader.exe`。
6. Windows zip 已生成：`build/reader-2.5.4-windows.zip`。

仍然存在的非阻塞日志：

1. `iscc` 缺失导致 Inno Setup installer 生成失败日志。
2. `candle` 缺失导致 WiX MSI 生成失败日志。

这些日志出现在 `BUILD SUCCESSFUL` 后，且命令退出码为 0。本次未执行服务启动和浏览器手工验收。

## 11. 主 session 服务/API 验收记录

2026-05-07 主 session 使用纯 Spring Boot 入口启动临时服务：

```powershell
java -Dloader.main=com.htmake.reader.ReaderApplicationKt -cp build/libs/reader-2.5.4.jar org.springframework.boot.loader.PropertiesLauncher --spring.profiles.active=prod --reader.server.port=18080 --reader.app.showUI=false --reader.app.storagePath=storage-api
```

验证结果：

1. `/health` 返回 200，响应体：`"ok!"`。
2. `/` 返回 200，包含前端 app 根节点，未出现 `Resource not found`。
3. `/reader3/getSystemInfo` 返回 200，`isSuccess=true`。
4. 使用 `src/test/resources/booksource/mixed_types.json` 调用 `/reader3/saveBookSources` 成功。
5. 随后调用 `/reader3/getBookSources`，保存结果为 2 个文本源：`趣书网手机版[分页]`、`精华书阁`；样本中的音频源和漫画源未进入保存结果。

验证后已停止临时服务进程。

## 12. 主 session 联网指定源验收记录

2026-05-07 主 session 使用 `build/manual-verify/verify-remote-source.ps1` 执行指定源验收：

```powershell
.\build\manual-verify\verify-remote-source.ps1 -LocalSourceFile .\src\test\resources\booksource\894_full.json -Port 18082 -MaxSourcesToTry 25
```

结果：

1. 指定 URL 下载成功：`https://gcore.jsdelivr.net/gh/mumuceo/file01/202604/894_31e14ea5d95f46a5319bdacd1daa99b4.json`。
2. 远程源列表共 382 个源。
3. 文本源 279 个；音频源 23 个；图片/漫画源 80 个；文件源 0 个；未知类型 0 个。
4. `/reader3/saveBookSources` 导入成功：`imported=279`，`skipped=103`。
5. 剔除统计：`audio=23`，`image=80`，`file=0`，`unknown=0`。
6. `/reader3/getBookSources` 保存结果为 279 个文本源。
7. 联网主链路最终使用 `晋江APP端` 跑通：
   - 搜索关键字：`斗破苍穹`。
   - 搜索结果：20 条。
   - 详情成功，书名：`斗破苍穹`。
   - 目录成功，章节数：6。
   - 正文成功，第一章正文长度：348。

尝试过程中部分候选源失败，均属于外部站点或单源规则问题，不影响本轮整体兼容结论：

1. `趣书网手机版[分页]`：连接超时。
2. `精华书阁`：DNS 解析失败。
3. `📺 资源采集22`：源内 JS 规则语法错误。
4. `🌹晋江`：请求超时。

验证后已停止临时服务进程，`18082` 无遗留监听。
