# Reader 工程知识库

生成日期：2026-05-06  
工程路径：`D:\Code\agent\codex\github.com\curdfu\reader`

## 1. 项目定位

`reader` 是“阅读3服务器版”的 Web/服务端工程。它把阅读 App 的书源解析、书架管理、搜索、阅读、本地书导入、RSS、WebDAV 同步、多用户能力搬到服务器端，并通过 Vue 单页前端提供浏览器阅读体验。

当前 README 明确说明：

- 项目版本为开放源码历史版本，完整源码开放到 `v2.5.4`。
- 目标是不依赖手机即可运行阅读服务。
- 主要功能包括书源管理、书架、搜索、换源、阅读器、WebDAV、文字替换、听书、本地书导入、RSS、定时更新、多用户等。

从代码结构看，这是一个单仓库、单 Gradle 后端模块，加一个独立 `web` Vue 前端工程。后端构建时会把前端产物打入服务资源目录。

## 2. 总体架构

### 2.1 架构形态

工程不是标准 Spring MVC 项目，而是：

1. Spring Boot 负责应用生命周期、配置加载、Bean 管理、定时任务。
2. Spring 启动后部署一个 Vert.x `Verticle`。
3. Vert.x 负责 HTTP Server、路由、Session、CORS、静态资源、API。
4. 控制器不是 Spring `@Controller`，而是由 `YueduApi` 手工实例化。
5. 持久化不是数据库，而是 JSON 文件和本地文件目录。
6. 前端是 Vue 2 SPA，通过 `/reader3/*` API 与后端通信。

### 2.2 主链路

后端启动链路：

```text
ReaderUIApplicationKt 或 ReaderApplicationKt
  -> SpringApplication.run(ReaderApplication)
  -> ReaderApplication.deployVerticle()
  -> Vertx.deployVerticle(yueduApi)
  -> RestVerticle.start()
  -> YueduApi.initRouter()
  -> 注册静态页面、API、WebDAV、定时任务
```

关键文件：

- `src/main/java/com/htmake/reader/ReaderApplication.kt`
- `src/main/java/com/htmake/reader/ReaderUIApplication.kt`
- `src/main/java/com/htmake/reader/verticle/RestVerticle.kt`
- `src/main/java/com/htmake/reader/api/YueduApi.kt`

前端启动链路：

```text
web/src/main.js
  -> Vue 初始化
  -> Vuex store
  -> Vue Router
  -> Element UI
  -> Axios request wrapper
  -> App.vue
```

关键文件：

- `web/src/main.js`
- `web/src/App.vue`
- `web/src/router/index.js`
- `web/src/plugins/vuex.js`
- `web/src/plugins/axios.js`

## 3. 技术栈

### 3.1 后端

来源：`build.gradle.kts`

- Java：源码和目标版本为 Java 8。
- Kotlin：`1.5.21`。
- Spring Boot：`2.1.6.RELEASE`。
- Vert.x：`3.8.1`，包括 `vertx-core`、`vertx-web`、`vertx-web-client`、Kotlin coroutines。
- JavaFX：`11.0.2`，用于桌面 UI 包装。
- JSON：Gson、Jackson Kotlin module、JsonPath。
- HTTP：OkHttp、Retrofit、Vert.x WebClient。
- HTML/XML/规则解析：Jsoup、JsoupXpath、JsonPath、Rhino、xmlpull、kxml2。
- 加解密：Hutool crypto，另有自定义 MD5 密码散列工具。
- EPUB/UMD：仓库内包含 `me.ag2s.epublib`、`me.ag2s.umdlib` 等源码。

### 3.2 前端

来源：`web/package.json`

- Vue：`^2.6.10`
- Vuex：`^3.1.1`
- Vue Router：`^3.1.3`
- Element UI：`^2.15.9`
- Axios：`^0.21.1`
- Vue CLI：`^4.0.0`
- localforage：本地缓存
- vue-lazyload：图片懒加载
- sortablejs：拖拽排序
- register-service-worker：PWA 支持

## 4. 目录结构说明

根目录关键文件：

```text
build.gradle.kts          后端主构建脚本，桌面 UI 打包入口也在这里
cli.gradle                服务端/CLI 构建脚本
build.sh                  Linux/macOS 构建和运行脚本
reader.sh                 服务器一键部署脚本
Dockerfile                基于预构建镜像的 Dockerfile
Dockerfile.source         从源码构建镜像
docker-compose.yml        Docker Compose 示例
README.md                 项目介绍
doc.md                    使用文档
UPDATELOG.md              更新日志
src                       后端源码和资源
web                       前端 Vue 工程
server                    发布包中的启动脚本和配置示例
assets                    桌面打包图标
imgs                      README/预览图片
```

后端主要目录：

```text
src/main/java/com/htmake/reader
  api                     Vert.x API 和控制器
  api/controller          书籍、书源、用户、WebDAV、RSS、替换规则、书签等控制器
  config                  应用配置对象
  entity                  Reader 自有实体
  init                    初始化上下文
  utils                   通用工具、存储、JSON、文件、Spring 上下文
  verticle                Vert.x 基类

src/main/java/io/legado/app
  data/entities           阅读/Legado 数据实体
  model                   书源解析、WebBook、RSS、本地书处理等
  help                    HTTP、缓存、协程等辅助能力
  utils                   字符串、编码、文件、网络、JSON 等工具

src/main/java/me/ag2s
  epublib                 EPUB 读写相关代码
  umdlib                  UMD 读取相关代码

src/main/java/org/kxml2
  XML 解析相关代码
```

前端主要目录：

```text
web/src/views             页面级视图，主要是 Index 和 Reader
web/src/components        书架、书源、阅读设置、WebDAV、RSS、书签等组件
web/src/plugins           Axios、Vuex、缓存、配置、工具函数、Element UI 初始化
web/src/router            路由
web/src/assets            字体、图标、主题图片
web/public                PWA、静态调试页、背景图、favicon
```

## 5. 后端核心模块

### 5.1 `ReaderApplication`

文件：`src/main/java/com/htmake/reader/ReaderApplication.kt`

职责：

- Spring Boot 主应用。
- 启用定时任务：`@EnableScheduling`。
- 初始化 Vert.x 单例。
- 注册 Kotlin JSON mapper。
- 部署 `YueduApi`。
- 创建全局 Vert.x `WebClient`。

注意点：

- `WebClientOptions.isTrustAll = true`。
- `HttpClientOptions.setTrustAll(true)`。
- 这能提高复杂书源兼容性，但会弱化 TLS 校验。

### 5.2 `ReaderUIApplication`

文件：`src/main/java/com/htmake/reader/ReaderUIApplication.kt`

职责：

- JavaFX 桌面端包装。
- 在 JavaFX 应用内启动 Spring Boot。
- 读取窗口配置 `windowConfig`。
- 根据 `reader.app.showUI` 决定是否展示 WebView。
- 用 JavaFX `WebView` 加载本地服务的 Web 页面。

适用场景：

- 桌面打包。
- 本地带 UI 运行。

服务端 Docker/CLI 构建通常会排除或移除该文件，避免 JavaFX 依赖影响纯服务端构建。

### 5.3 `RestVerticle`

文件：`src/main/java/com/htmake/reader/verticle/RestVerticle.kt`

职责：

- 创建 Vert.x Router。
- 配置 Session。
- 配置 CORS。
- 配置 BodyHandler。
- 配置请求日志。
- 注册 `/health`。
- 提供 `coroutineHandler` 和 `coroutineHandlerWithoutRes` 两个协程包装工具。

重要行为：

- Session Cookie 名为 `reader.session`。
- Session timeout 为 7 天。
- 响应结束时会把 Cookie max age 延长为 2 天。
- CORS 逻辑会回显请求 `Origin`，并允许 `Access-Control-Allow-Credentials: true`。
- 对 `/reader3/*` 请求会记录 method、absoluteURI，短 body 也会记录。

风险点：

- 如果 URL query 中包含 `accessToken` 或 `secureKey`，可能被写入日志。
- CORS 过宽，公网部署时需要重新评估。

### 5.4 `YueduApi`

文件：`src/main/java/com/htmake/reader/api/YueduApi.kt`

职责：

- 读取端口配置。
- 做历史数据目录迁移。
- 注册 Web 静态页面。
- 注册 `/assets/*` 静态文件。
- 注册 `/epub/*` 静态文件和 EPUB 章节脚本注入。
- 注册全部 `/reader3/*` API。
- 初始化各业务控制器。
- 发布 Spring 启动成功/失败事件。
- 定时刷新书架。
- 定时清理不活跃用户。

该文件是后端路由中心，体积较大，新增 API 通常会在这里注册。

主要 API 模块：

- 书源：`BookSourceController`
- 书籍/书架/搜索/阅读/本地书：`BookController`
- 用户：`UserController`
- WebDAV：`WebdavController`
- RSS：`RssSourceController`
- 替换规则：`ReplaceRuleController`
- 书签：`BookmarkController`

## 6. 数据存储模型

### 6.1 配置入口

默认配置文件：

- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`

关键配置：

```yaml
reader:
  app:
    storagePath: storage
    showUI: false
    debug: false
    packaged: false
    secure: false
    inviteCode: ""
    secureKey: ""
    cacheChapterContent: true
    userLimit: 50
    userBookLimit: 200
    autoClearInactiveUser: 0
  server:
    port: 8080
    webUrl: http://localhost:${reader.server.port}
```

### 6.2 文件存储工具

文件：`src/main/java/com/htmake/reader/utils/VertExt.kt`

核心函数：

- `getWorkDir(...)`
- `getStoragePath()`
- `saveStorage(...)`
- `getStorage(...)`
- `asJsonArray(...)`
- `asJsonObject(...)`
- `toDataClass(...)`
- `toMap(...)`

存储策略：

- `getStoragePath()` 读取 `reader.app.storagePath`。
- 如果是相对路径，则拼到工作目录下。
- `saveStorage("data", "users", value = userMap)` 会写入类似 `storage/data/users.json`。
- `saveUserStorage(userNameSpace, "bookshelf", bookshelf)` 会写入类似 `storage/data/{userNameSpace}/bookshelf.json`。

### 6.3 典型存储路径

```text
storage/
  data/
    users.json                    多用户信息
    default/
      bookshelf.json              默认用户书架
      bookSource.json             默认用户书源
      bookGroup.json              默认用户书籍分组
      replaceRule.json            替换规则
      rssSources.json             RSS 源
      bookmarks.json              书签
    {username}/
      bookshelf.json
      bookSource.json
      userConfig.json
      webdav/
      {bookName_author}/
  assets/
    reader.css                    全局自定义 CSS
    {username}/images/...         用户上传资源
  cache/
    invalidBookSourceCache/
```

## 7. 鉴权与用户模型

### 7.1 鉴权入口

文件：`src/main/java/com/htmake/reader/api/controller/BaseController.kt`

关键方法：

- `checkAuth(context)`
- `checkManagerAuth(context)`
- `getUserNameSpace(context)`
- `getUserStorage(context, ...)`
- `saveUserStorage(context, path, value)`

### 7.2 普通鉴权

如果 `reader.app.secure = false`：

- `checkAuth` 直接返回 `true`。
- 所有数据默认归属 `default` 用户空间。

如果 `reader.app.secure = true`：

- 优先从 Session 中取 `username`。
- 如果 Session 无效，再尝试从 query 参数 `accessToken` 自动登录。
- `accessToken` 格式为 `username:token`。
- Token 校验通过后会刷新 session。

### 7.3 管理权限

`checkManagerAuth` 逻辑：

- 如果 `secure=false`，直接返回 `true`。
- 如果 `secureKey` 为空，直接返回 `true`。
- 否则从 query 参数读取 `secureKey`。
- 如果 `secureKey` 匹配，则允许管理操作。
- 管理模式还可通过 `userNS` 切换用户命名空间。

风险点：

- `secureKey` 放在 query 中，容易泄露到日志和浏览器历史。
- `secure=true` 但 `secureKey` 为空时，管理校验会直接通过。

### 7.4 密码处理

文件：`src/main/java/com/htmake/reader/utils/VertExt.kt`

`genEncryptedPassword(password, salt)` 使用双层 MD5：

```text
MD5(MD5(password + salt) + salt)
```

风险：

- MD5 不适合密码存储。
- 盐长度为 8，来源是 Kotlin `random()`，不是安全随机数。
- 建议迁移到 BCrypt、Argon2 或 PBKDF2。

## 8. API 模块说明

所有业务 API 基本挂在 `/reader3/*` 下。

### 8.1 书源模块

控制器：`BookSourceController`

主要能力：

- 保存单个/多个书源。
- 获取单个/多个书源。
- 删除书源。
- 上传书源文件。
- 读取远程书源文件。
- 设置默认书源。
- 删除用户书源文件。

典型接口：

- `POST /reader3/saveBookSource`
- `POST /reader3/saveBookSources`
- `GET|POST /reader3/getBookSource`
- `GET|POST /reader3/getBookSources`
- `POST /reader3/deleteBookSource`
- `POST /reader3/deleteBookSources`
- `POST /reader3/readSourceFile`
- `POST /reader3/readRemoteSourceFile`

### 8.2 书籍与书架模块

控制器：`BookController`

主要能力：

- 书架读写。
- 搜索。
- 并发搜索。
- SSE 搜索。
- 书籍详情。
- 章节列表。
- 章节内容。
- 保存阅读进度。
- 换源。
- 本地书导入。
- TXT 章节规则。
- 书籍分组。
- 本地书仓。
- 缓存章节。
- 导出书籍。
- 全文搜索。

典型接口：

- `GET /reader3/getBookshelf`
- `GET /reader3/getShelfBook`
- `POST /reader3/saveBook`
- `POST /reader3/deleteBook`
- `GET|POST /reader3/searchBook`
- `GET|POST /reader3/searchBookMulti`
- `GET /reader3/searchBookMultiSSE`
- `GET|POST /reader3/getBookInfo`
- `GET|POST /reader3/getChapterList`
- `GET|POST /reader3/getBookContent`
- `POST /reader3/saveBookProgress`
- `GET /reader3/cover`
- `GET /reader3/cacheBookSSE`
- `POST|GET /reader3/exportBook`

### 8.3 用户模块

控制器：`UserController`

主要能力：

- 注册/登录。
- 登出。
- 获取用户信息。
- 用户列表。
- 添加用户。
- 删除用户。
- 重置密码。
- 更新用户能力开关。
- 上传/删除用户资源文件。
- 用户配置备份与恢复。

典型接口：

- `POST /reader3/login`
- `POST /reader3/logout`
- `GET /reader3/getUserInfo`
- `GET /reader3/getUserList`
- `POST /reader3/addUser`
- `POST /reader3/deleteUsers`
- `POST /reader3/resetPassword`
- `POST /reader3/updateUser`
- `POST /reader3/uploadFile`
- `POST /reader3/deleteFile`
- `POST /reader3/saveUserConfig`
- `GET /reader3/getUserConfig`

### 8.4 WebDAV 模块

控制器：`WebdavController`

能力：

- WebDAV 协议入口：`/reader3/webdav*`
- 支持 `OPTIONS`、`PROPFIND`、`MKCOL`、`PUT`、`GET`、`DELETE`、`MOVE`、`COPY`、`LOCK`、`UNLOCK`。
- 同时提供普通 API 形式的 WebDAV 文件管理和备份恢复。

风险点：

- 多处使用 `File(home + path)`。
- `path` 来自请求路径解码后直接拼接。
- `Destination` header 也会参与路径拼接。
- 删除、移动、复制会递归操作文件。
- 当前未看到 canonical path 是否仍在用户 WebDAV home 内的强制校验。

这是安全修复的最高优先级模块。

### 8.5 RSS、替换规则、书签

控制器：

- `RssSourceController`
- `ReplaceRuleController`
- `BookmarkController`

共同特点：

- 基本都是 JSON 文件读写。
- 数据按用户命名空间隔离。
- 通过 `checkAuth` 控制访问。

## 9. 前端结构说明

### 9.1 Vuex 状态

文件：`web/src/plugins/vuex.js`

重要状态：

- `api`：API 前缀，默认 `location.host + "/reader3"`。
- `token`：登录 token，来自本地缓存 `api_token`。
- `secureKey`：管理密码，内存态保存。
- `isManagerMode`：管理模式。
- `userNS`：管理模式下操作的用户命名空间。
- `shelfBooks`：书架。
- `readingBook`：当前阅读书籍。
- `config`：阅读配置。
- `bookSourceList`：书源。
- `bookGroupList`：书籍分组。
- `rssSourceList`：RSS 源。
- `bookmarks`：书签。

### 9.2 Axios 封装

文件：`web/src/plugins/axios.js`

行为：

- `baseURL` 使用 `store.getters.api`。
- `withCredentials = true`。
- 请求超时为 5 分钟。
- 如果存在 `store.state.token`，追加 query：`accessToken`。
- 如果是管理模式且存在 `secureKey`，追加 query：`secureKey` 和 `userNS`。
- 所有请求追加时间戳参数 `v` 防止缓存。
- 业务响应依赖 `ReturnData` 中的 `isSuccess`、`errorMsg`、`data`。

风险：

- token 和 secureKey 在 query 中传输。
- 后端请求日志记录 absoluteURI 时可能记录这些敏感参数。

### 9.3 路由

文件：`web/src/router/index.js`

当前只有两个页面级路由：

- `/` -> `views/Index.vue`
- `/reader` -> `views/Reader.vue`

功能主要在组件内部通过状态切换实现，而不是多页面路由拆分。

## 10. 构建与运行

### 10.1 后端构建

主要脚本：

- `build.gradle.kts`
- `cli.gradle`
- `build.sh`

`build.gradle.kts` 默认主类：

```text
com.htmake.reader.ReaderUIApplicationKt
```

这表示默认构建偏桌面 UI 版本。

服务端构建脚本会移除或避开 `ReaderUIApplication.kt`，使用 `cli.gradle` 构建服务端 jar。

### 10.2 前端构建

`web/package.json`：

```json
"scripts": {
  "serve": "vue-cli-service serve",
  "build": "vue-cli-service build",
  "lint": "vue-cli-service lint",
  "sync": "yarn build && rm -rf ../src/main/resources/web && mv dist ../src/main/resources/web"
}
```

注意：

- `sync` 会删除并替换 `src/main/resources/web`。
- 当前仓库中没有看到 `src/main/resources/web` 文件列表，但后端 `StaticHandler.create("web")` 会从 classpath/web 或工作目录 web 查找静态资源。

### 10.3 Docker

`Dockerfile`：

- 基于 `hectorqin/reader` 镜像。
- 暴露 8080。
- 启动 `/app/bin/reader.jar`。

`Dockerfile.source`：

- 第一阶段用 Node 构建前端。
- 第二阶段用 Gradle 6.1.1 + JDK 8 构建 jar。
- 第三阶段用 Amazon Corretto 8 JRE 运行。

`docker-compose.yml`：

- 默认映射宿主机 `4396:8080`。
- 映射 `/logs` 和 `/storage`。
- 示例中开启多用户：
  - `READER_APP_SECURE=true`
  - `READER_APP_SECUREKEY=adminpwd`
  - `READER_APP_INVITECODE=registercode`

## 11. 测试现状

测试目录：

```text
src/test/java/com/htmake/reader/ReaderApplicationTests.java
```

当前只有一个 Spring Boot context load 空测试：

```java
@SpringBootTest
public class ReaderApplicationTests {
    @Test
    public void contextLoads() {
    }
}
```

缺口：

- 没有 API 行为测试。
- 没有文件路径安全测试。
- 没有用户隔离测试。
- 没有 WebDAV 测试。
- 没有书源解析回归测试。
- 没有前端单元测试或 E2E 测试配置证据。

## 12. 关键安全风险

### 12.1 默认鉴权关闭

默认：

```yaml
reader.app.secure: false
```

后果：

- `checkAuth` 直接通过。
- API 可直接访问。
- 数据归属 `default` 用户空间。

建议：

- 公网部署必须覆盖为 `secure=true`。
- 服务启动时如果监听非 localhost 且 `secure=false`，建议输出强警告或拒绝启动。

### 12.2 CORS 过宽

`RestVerticle` 会回显任意 Origin：

```text
Access-Control-Allow-Origin: 请求 Origin
Access-Control-Allow-Credentials: true
```

后果：

- 如果浏览器携带 Cookie，跨站页面可能调用接口。
- 与默认无鉴权结合时风险更高。

建议：

- 引入允许域名白名单。
- 默认只允许同源。
- 管理接口不允许跨源。

### 12.3 敏感参数进入 URL

前端将以下字段放入 query：

- `accessToken`
- `secureKey`
- `userNS`

后端日志会记录 absoluteURI。

后果：

- Token 可能出现在浏览器历史、反向代理日志、应用日志、异常日志中。

建议：

- `accessToken` 改用 `Authorization: Bearer ...`。
- `secureKey` 改用请求体或专用 header。
- 日志记录前脱敏 query。

### 12.4 WebDAV 路径穿越

高风险模式：

```kotlin
var file = File(home + path)
```

出现场景：

- list
- mkdir
- upload
- download
- delete
- move
- copy
- 普通 WebDAV 文件 API

后果：

- 如果 path 中包含 `..`、编码绕过、反斜杠变体等，可能越出用户 WebDAV home。
- 删除、移动、复制操作会放大破坏面。

建议：

- 所有用户输入路径先 `URLDecoder.decode` 后统一规范化。
- 用 `Path.resolve(...).normalize()`。
- 用 `toRealPath` 或 canonical path 检查目标必须以 home canonical path 开头。
- 删除/覆盖前再次校验。
- Windows 下同时考虑 `\`、盘符、UNC 路径。

### 12.5 上传文件名未充分约束

`UserController.uploadFile` 直接使用上传文件名拼路径：

```kotlin
File(getWorkDir("storage", "assets", userNameSpace, type, fileName))
```

风险：

- 文件名如果包含路径分隔符或特殊名称，可能污染目录结构。

建议：

- 对上传文件名做 basename 提取。
- 拒绝路径分隔符、控制字符、空名、保留设备名。
- 限制文件扩展名和大小。

### 12.6 TLS 校验关闭

`ReaderApplication.webClient()` 中设置 `trustAll=true`。

风险：

- 远程书源请求可能被中间人攻击。

权衡：

- 阅读书源生态复杂，关闭校验可提升兼容性。
- 生产安全模式下应允许配置是否 trustAll。

### 12.7 密码散列偏弱

当前使用 MD5 自定义散列。

建议：

- 新用户使用 BCrypt/Argon2/PBKDF2。
- 老用户登录成功后自动迁移密码 hash。
- Token 生成使用 `SecureRandom`。

## 13. 维护风险

### 13.1 依赖偏旧

风险组件：

- Spring Boot 2.1.6
- Vert.x 3.8.1
- Vue 2.6
- Axios 0.21
- Vue CLI 4

影响：

- 安全补丁落后。
- 升级跨度大。
- 新 Node/JDK 环境下可能构建失败。

建议：

- 短期只做安全补丁和兼容修复。
- 中期固定构建环境。
- 长期分阶段升级：后端依赖、前端依赖、构建链路。

### 13.2 路由集中在单文件

`YueduApi.kt` 手工注册所有路由，随着功能增长会越来越难维护。

建议：

- 拆分为模块级 Router 注册器。
- 每个业务模块维护自己的 `registerRoutes(router)`。
- 保留 `YueduApi` 作为组合入口。

### 13.3 控制器不受 Spring 管理

控制器由 `YueduApi` 手工 `BookController(coroutineContext)` 创建。

影响：

- 不方便注入依赖。
- 不方便单元测试。
- 不方便统一 AOP/拦截器/事务。

当前控制器通过 `SpringContextUtils.getBean` 获取配置和环境，属于服务定位器模式。

建议：

- 短期保持现状。
- 中期把公共依赖显式传入构造函数。
- 长期考虑让控制器成为 Spring Bean 或模块服务。

### 13.4 JSON 文件并发写风险

多个请求可能同时读写同一个 JSON 文件，例如：

- `bookshelf.json`
- `bookSource.json`
- `users.json`

风险：

- 最后写入覆盖先前修改。
- 写入中断导致 JSON 文件损坏。

建议：

- 引入文件级锁。
- 写入时先写临时文件，再原子替换。
- 高频数据考虑迁移 SQLite。

## 14. 推荐改造优先级

### P0：安全底线

1. WebDAV 和所有文件 API 增加路径归一化与根目录校验。
2. 请求日志脱敏 `accessToken`、`secureKey`。
3. 公网部署默认强制安全配置或启动警告。
4. 上传文件名做安全过滤。
5. CORS 改为白名单或默认同源。

### P1：测试补齐

1. WebDAV 路径穿越测试。
2. 用户空间隔离测试。
3. 鉴权开关测试。
4. 上传/删除文件测试。
5. 书源读写测试。
6. 书架保存和并发写测试。

### P2：结构整理

1. 拆分 `YueduApi` 路由注册。
2. 收敛控制器公共逻辑。
3. 建立统一 `PathResolver` 或 `StorageService`。
4. 抽象 `ReturnData` 响应工厂。

### P3：依赖升级

1. 固定当前可复现构建环境。
2. 先升级后端小版本安全补丁。
3. 再处理前端 Vue 2 生态依赖。
4. 最后评估迁移 Vue 3 或 Spring Boot 新版本。

## 15. 后续开发切入点

如果要修安全：

- 先看 `WebdavController.kt`。
- 再看 `UserController.uploadFile/deleteFile`。
- 再看 `BookController` 中本地书导入、导出、删除路径。
- 最后看通用文件工具 `Ext.kt` 和 `VertExt.kt`。

如果要改鉴权：

- 先看 `BaseController.checkAuth`。
- 再看 `BaseController.checkManagerAuth`。
- 前端同步看 `axios.js` 和 `vuex.js`。

如果要改存储：

- 先看 `VertExt.getStorage/saveStorage`。
- 再看 `BaseController.getUserStorage/saveUserStorage`。
- 逐个控制器替换调用点。

如果要改前端阅读体验：

- 先看 `web/src/views/Reader.vue`。
- 再看 `ReadSettings.vue`、`Content.vue`、`PopCatalog.vue`、`BookShelf.vue`。
- 状态源头主要在 `vuex.js` 和 `config.js`。

如果要做构建/部署：

- 桌面端看 `build.gradle.kts` 和 `ReaderUIApplication.kt`。
- 服务端看 `cli.gradle`、`Dockerfile.source`、`docker-compose.yml`。
- Web 资源同步看 `web/package.json` 的 `sync`。

## 16. 当前静态分析限制

本 KB 基于文件静态阅读生成，未执行以下动作：

- 未运行 Gradle 构建。
- 未运行 NPM/Yarn 构建。
- 未启动服务。
- 未跑测试。
- 未做动态安全验证。
- 未做 Git 操作。

因此本文适合作为工程导航和风险初筛。若进入修复阶段，应先补最小化回归测试，再做代码修改。
