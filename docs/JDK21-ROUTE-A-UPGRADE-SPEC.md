# 依赖升级规格文档 - 路线 A

> 生成时间：2026-05-07  
> 推荐路线：路线 A，保守可落地升级  
> 目标运行方式：只以 `./build.sh serve` 作为默认验收入口  
> 重要说明：本轮目标是 JDK 21 LTS。

---

## 一、结论

本轮依赖升级建议走路线 A：

```text
JDK 11 -> JDK 21 LTS
Gradle 6.1.1 -> Gradle 8.14.x
Kotlin 1.5.21 -> Kotlin 1.9.25
Spring Boot 2.1.6.RELEASE -> Spring Boot 3.5.x
Vert.x 3.8.1 -> Vert.x 4.5.x
前端 Vue 2 / Element UI / Vue CLI 暂时保持
```

路线 A 的目标不是“全部升到最新”，而是把后端运行栈拉到仍受维护、生态成熟、可被 `./build.sh serve` 稳定验收的区间。前端框架迁移不纳入本轮，以避免把后端依赖升级和前端框架重写混在一起。

---

## 二、当前约束

### 2.1 用户约束

- 默认只考虑 `./build.sh serve` 运行方式。
- 不以 Windows exe / 安装包打包作为默认验收标准。
- 每个阶段完成后都必须能说明验证结果。
- 不要在执行 plan 时自动做 git 操作，提交由用户自行完成。

### 2.2 当前项目状态

| 层级 | 组件 | 当前版本 / 状态 | 说明 |
|------|------|----------------|------|
| JDK | Java | 11 运行，1.8 编译目标 | `build.sh` 当前锁 JDK 11，拒绝高版本 |
| 构建 | Gradle | 6.1.1 | 高版本 JDK 下 Groovy/Gradle 不兼容 |
| 语言 | Kotlin | 1.5.21 | 当前能在 JDK 11 下编译 |
| 框架 | Spring Boot | 2.1.6.RELEASE | 已 EOL，升级到 3.x 需要 Jakarta 迁移 |
| Web | Vert.x | 3.8.1 | 与 controller / RestVerticle 深度耦合 |
| HTTP | OkHttp | 4.9.1 | 可先保持，后续小步升级 |
| 前端 | Vue | 2.6.10 | 本轮保持 |
| UI | Element UI | 2.15.9 | 本轮保持 |
| 构建 | Vue CLI | 4.x | 本轮保持 |
| 状态 | Vuex | 3.x | 本轮保持 |
| 路由 | vue-router | 3.x | 本轮不迁 Vue Router 4 |

### 2.3 为什么选 JDK 21

JDK 21 是 LTS，Spring Boot 3.x、Gradle 8.x、Kotlin 1.9.x、Vert.x 4.5.x 的生态支持更成熟。路线 A 的核心目标是稳定升级，不追逐最新版本。

---

## 三、目标版本矩阵

| 层级 | 当前 | 路线 A 目标 | 是否本轮升级 | 说明 |
|------|------|-------------|--------------|------|
| JDK | 11 | 21 LTS | 是 | 优先稳定，使用 LTS 版本 |
| Gradle | 6.1.1 | 8.14.x | 是 | 支持 JDK 21，保留较成熟 Gradle 8 线 |
| Kotlin | 1.5.21 | 1.9.25 | 是 | 避免同时切换新编译器 |
| Kotlin Spring Plugin | 1.3.61 | 1.9.25 | 是 | 必须保留，用于 Spring 代理类 open 化 |
| Spring Boot | 2.1.6 | 3.5.x | 是 | 进入维护期，处理 Jakarta 迁移 |
| Spring Framework | 5.1.x | 6.2.x | 随 Boot | 由 Boot 管理 |
| Vert.x | 3.8.1 | 4.5.x | 是 | 保持在成熟维护线 |
| OkHttp | 4.9.1 | 4.12.x 或暂不动 | 可选 | 不和框架升级混在第一阶段 |
| Gson | 2.8.5 | 2.11.x 或暂不动 | 可选 | 小步升级 |
| Jackson | 2.13.+ | Boot 管理版本 | 是 | 优先让 Boot BOM 管理 |
| JavaFX | 11.0.2 | 21.x | 可选 | `serve` 不依赖 UI，可后置 |
| Vue | 2.6.10 | 保持 | 否 | 前端大迁移后置 |
| Vue CLI | 4.x | 保持 | 否 | 保持 serve 资源同步链路稳定 |

---

## 四、范围

### 4.1 本轮做

1. 让项目可在 JDK 21 下通过后端编译。
2. 让 `./build.sh serve` 使用 JDK 21 运行。
3. 将 Gradle 升到 8.x，修复构建脚本兼容问题。
4. 将 Kotlin 升到 1.9.x，修复编译问题。
5. 将 Spring Boot 升到 3.5.x，完成 Jakarta 迁移。
6. 将 Vert.x 升到 4.5.x，完成必要 API 适配。
7. 保持当前 Web 端 Vue 2 体验可用。
8. 回归核心功能：书架、书源、搜索、阅读、本地书籍、章节缓存、远程书源导入。

### 4.2 本轮不做

1. 不升级到非 LTS JDK。
2. 不升级到下一代主版本构建链。
3. 不切换 Kotlin K2 编译器。
4. 不升级到下一代 Spring 主版本。
5. 不升级到下一代 Vert.x 主版本。
6. 不迁移前端框架。
7. 不迁移前端 UI 框架。
8. 不迁移前端状态管理。
9. 不迁移前端构建工具。
10. 不做 exe / installer 打包验收。

---

## 五、分阶段策略

### Phase 0：基线冻结

目标：确认升级前项目在当前环境可用。

验收：

```bash
./build.sh sync
./build.sh serve
```

至少确认：

- 首页可打开。
- 书架可加载。
- 书源管理能显示已导入书源。
- 搜索能返回结果。
- 阅读页能打开章节。
- 本地书籍目录不报错。

### Phase 1：JDK 21 + Gradle 8

目标：只改构建基础设施，不动业务代码。

建议版本：

```text
JDK: 21 LTS
Gradle: 8.14.x
```

主要文件：

- `gradle/wrapper/gradle-wrapper.properties`
- `build.sh`
- `build.gradle.kts`
- `cli.gradle`

验收：

```bash
bash -n build.sh
./gradlew.bat --version
./gradlew.bat tasks
./build.sh serve
```

风险：

- Gradle 6 到 8 会触发部分 DSL / task API 兼容问题。
- `build.sh` 当前会主动寻找 JDK 11，需要改成优先 JDK 21。
- `cli.gradle` 也要同步检查，不能只改 `build.gradle.kts`。

### Phase 2：Kotlin 1.9.x

目标：语言版本升级，但不启用新编译器。

建议版本：

```text
Kotlin: 1.9.25
org.jetbrains.kotlin.plugin.spring: 1.9.25
```

注意：

- 不要删除 Kotlin Spring plugin。
- `kotlinOptions.jvmTarget` 建议先设为 `"17"` 或 `"21"`，以实际编译支持为准。
- 优先修编译错误，不做业务重构。

验收：

```bash
./gradlew.bat classes
./gradlew.bat test
./build.sh serve
```

### Phase 3：Spring Boot 3.5.x

目标：进入 Boot 3 维护线。

主要工作：

- Spring Boot plugin 升到 3.5.x。
- 处理 `javax.* -> jakarta.*`，但不要改 JDK 自带的 `javax.crypto.*`、`javax.net.ssl.*`、`javax.security.*`。
- 检查 `@ConfigurationProperties` 注册方式。
- 检查 Spring Boot 3 配置项变化。
- 检查测试依赖和 JUnit 运行。

验收：

```bash
./gradlew.bat classes
./gradlew.bat test
./build.sh serve
```

重点回归：

- 应用启动无 Spring 配置错误。
- API 路由能注册。
- session / 登录逻辑不回归。

### Phase 4：Vert.x 4.5.x

目标：将 Vert.x 3 升到 Vert.x 4.5 维护线。

主要工作：

- `vertx-core`
- `vertx-web`
- `vertx-web-client`
- `vertx-lang-kotlin`
- `vertx-lang-kotlin-coroutines`

重点文件：

- `src/main/java/com/htmake/reader/verticle/RestVerticle.kt`
- `src/main/java/com/htmake/reader/api/YueduApi.kt`
- `src/main/java/com/htmake/reader/api/controller/*.kt`
- `src/main/java/com/htmake/reader/utils/VertExt.kt`
- `src/main/java/com/htmake/reader/utils/Ext.kt`

验收：

```bash
./gradlew.bat classes
./gradlew.bat test
./build.sh serve
```

重点回归：

- `http://localhost:8080` 可打开。
- `/reader3/getBookshelf` 正常。
- `/reader3/getBookSources` 正常。
- 搜索接口正常。
- 阅读章节接口正常。
- SSE 章节缓存接口仍能返回进度。

### Phase 5：后端小依赖升级

目标：逐个小步升级，避免和框架升级混在一起。

候选：

- OkHttp 4.9.1 -> 4.12.x
- logging-interceptor 对齐 OkHttp
- Gson 2.8.5 -> 2.11.x
- Jsoup 1.14.1 -> 1.17.x 或更高稳定版
- json-path 2.6.0 -> 2.9/2.10
- hutool-crypto 5.8.0.M1 -> 5.8.x 稳定版

原则：

- 每次只升级一组强相关依赖。
- 每组升级后都跑测试和 serve。
- 不碰前端大依赖。

### Phase 6：前端保持与 serve 集成

目标：保持当前 Vue 2 前端能被 `./build.sh serve` 构建和访问。

主要工作：

- 保留 Vue CLI。
- 保留 Vue 2 / Element UI / Vuex。
- 保留 `runWebPackageManager` 的 yarn/npm fallback。
- 保留 localhost 下禁用 service worker 的策略，避免 serve 吃旧缓存。

验收：

```bash
./build.sh sync
./build.sh serve
```

### Phase 7：全链路回归

核心场景：

- 首页打开。
- 书架加载。
- 网络书籍未读章节显示。
- 书源管理显示已导入源。
- 远程书源导入。
- 失效书源检测。
- 搜索结果聚合和书源切换。
- 加入书架。
- 阅读页打开章节。
- 阅读设置切换浅色/深色不重置字号/间距。
- 章节缓存到服务端本地。
- 本地书籍目录加载。
- 封面代理加载。

---

## 六、风险矩阵

| 风险 | 等级 | 阶段 | 应对 |
|------|------|------|------|
| Gradle 8 DSL 不兼容 | 中 | Phase 1 | 先跑 `tasks/classes`，不动业务 |
| `build.sh serve` 失效 | 高 | 全阶段 | 每阶段都必须跑 serve |
| Kotlin 1.9 类型检查暴露旧问题 | 中 | Phase 2 | 只修编译，不重构 |
| Jakarta 迁移漏改 | 高 | Phase 3 | 用 `rg "javax\\."` 分批确认 |
| Spring Boot 配置变化 | 高 | Phase 3 | 启动日志逐项处理 |
| Vert.x 4 API 差异 | 高 | Phase 4 | 先编译，再接口回归 |
| 本地 JAR 不兼容 | 中 | Phase 3-5 | 单独验证 rhino/xmlpull |
| 前端 PWA 缓存干扰 | 中 | Phase 6 | localhost 禁用 service worker |
| 过度升级导致定位困难 | 高 | 全阶段 | 每阶段只改一个层级 |

---

## 七、回滚策略

每个阶段都要形成可独立回滚的改动集合。执行者不要自动提交，但每阶段完成后应向用户提示：

```text
本阶段涉及文件：
- ...

验证命令：
- ...

建议用户此时自行提交。
```

如果阶段失败：

- 不继续进入下一阶段。
- 优先恢复本阶段改动。
- 保留错误日志和已定位结论。
- 更新 plan 中对应阶段的状态和失败原因。

---

## 八、下一 session 执行规则

下一 session 应从 `docs/superpowers/plans/2026-05-07-backend-route-a-upgrade.md` 开始执行。

执行要求：

1. 不自动做 git 操作。
2. 不一次性跨多个 phase。
3. 每个 phase 完成后停下，让用户确认是否继续。
4. 默认验收命令必须包含 `./build.sh serve`。
5. 不启动前端框架迁移。
6. 如果发现必须偏离路线 A，先更新 spec，再继续执行。

