# Backend Dependency Upgrade Plan - Route A

> 本计划只描述路线 A 后端依赖升级。  
> 本轮只执行 JDK 21 LTS 后端依赖升级。  
> 默认验收入口：`./build.sh serve`。  
> 执行规则：每个 Phase 完成后停下，等待用户确认继续。不要自动执行 git 操作。

---

## Goal

将后端从当前老栈逐步升级到一个成熟、可维护、能用 `./build.sh serve` 稳定运行的版本组合：

```text
JDK 11 -> JDK 21 LTS
Gradle 6.1.1 -> Gradle 8.14.x
Kotlin 1.5.21 -> Kotlin 1.9.25
Spring Boot 2.1.6.RELEASE -> Spring Boot 3.5.x
Vert.x 3.8.1 -> Vert.x 4.5.x
```

前端 Vue 2 暂时保持，详见：

```text
docs/superpowers/plans/2026-05-07-frontend-route-a-stability.md
```

总规格文档：

```text
docs/JDK21-ROUTE-A-UPGRADE-SPEC.md
```

---

## Non-Goals

本计划明确不做：

- 不升级到非 LTS JDK。
- 不升级到下一代主版本构建链。
- 不切换 Kotlin K2 编译器。
- 不升级到下一代 Spring 主版本。
- 不升级到下一代 Vert.x 主版本。
- 不迁移前端框架、UI 框架、状态管理或构建工具。
- 不做 exe / installer 打包验证。
- 不自动 commit。

---

## Global Execution Rules

每个 Phase 都必须遵守：

1. 先读当前文件，再修改。
2. 不跨 Phase 批量改。
3. 修改后先跑最小编译，再跑测试，再跑 `./build.sh serve`。
4. 如果 `./build.sh serve` 失败，不进入下一阶段。
5. 如果需要下载依赖，失败时说明是网络/仓库问题还是代码问题。
6. 每个 Phase 结束后输出：

```text
完成内容：
- ...

修改文件：
- ...

验证：
- ...

未解决风险：
- ...

下一步建议：
- ...
```

7. 停下来等用户确认。

---

## Phase 0: Baseline Verification

### Purpose

确认升级前项目当前状态可运行，建立基线。

### Files

只读：

- `build.sh`
- `build.gradle.kts`
- `cli.gradle`
- `web/package.json`
- `src/main/java/com/htmake/reader/api/controller/BookController.kt`
- `web/src/App.vue`
- `web/src/registerServiceWorker.js`

### Steps

- [ ] 读取当前构建脚本。
- [ ] 确认 `build.sh` 当前会优先寻找 JDK 11。
- [ ] 确认 `syncWebResources` 当前使用 `runWebPackageManager build`。
- [ ] 确认 `./build.sh serve` 是用户指定默认运行方式。
- [ ] 运行前端资源构建：

```bash
./build.sh sync
```

- [ ] 运行服务端：

```bash
./build.sh serve
```

### Manual Smoke Test

浏览器打开：

```text
http://localhost:8080/?nopwa=1
```

确认：

- [ ] 首页能打开。
- [ ] 书架能显示。
- [ ] 书源管理能打开并显示源。
- [ ] 搜索能触发。
- [ ] 阅读页能打开。

### Exit Criteria

- `./build.sh sync` 成功。
- `./build.sh serve` 成功。
- 基础页面可打开。

### Stop Point

完成后停下，让用户确认是否进入 Phase 1。

---

## Phase 1: JDK 21 + Gradle 8 Infrastructure

### Purpose

只升级构建基础设施，让项目能用 JDK 21 和 Gradle 8 解析、编译，不主动改业务逻辑。

### Target Versions

```text
JDK: 21 LTS
Gradle: 8.14.x
```

### Files

可能修改：

- `gradle/wrapper/gradle-wrapper.properties`
- `build.sh`
- `build.gradle.kts`
- `cli.gradle`

### Step 1: Update Gradle Wrapper

将 wrapper 升到 Gradle 8.14.x，例如：

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip
```

不要升到下一代主版本构建链。

### Step 2: Update build.sh Java Detection

目标：

- 优先寻找 JDK 21。
- 允许 JDK 21。
- 保留对 JDK 11 的回退逻辑可选，但最终运行应使用 JDK 21。
- 错误提示要明确说明路线 A 目标是 JDK 21。

建议逻辑：

```bash
if [ -z "$JAVA_HOME" ] || ! { [ -x "$JAVA_HOME/bin/java" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21\.'; }; then
    for jdkHome in \
        "/c/Program Files/Eclipse Adoptium/jdk-21"* \
        "/c/Program Files/Java/jdk-21"* \
        "/c/Program Files/Microsoft/jdk-21"* \
        "/c/Program Files/Amazon Corretto/jdk21"* \
        "/Library/Java/JavaVirtualMachines/"*"/Contents/Home"
    do
        if [ -d "$jdkHome/bin" ] && "$jdkHome/bin/java" -version 2>&1 | grep -q 'version "21\.'; then
            export JAVA_HOME="$jdkHome"
            break
        fi
    done
fi
```

版本限制建议：

```bash
if [[ -z "$javaMajorVersion" || "$javaMajorVersion" -lt "21" ]]; then
    echo "Java version must not be lower than 21 for Route A."
    echo "$javaVersionOutput"
    exit 1
fi

if [[ "$javaMajorVersion" -gt "21" ]]; then
    echo "Warning: Java $javaMajorVersion detected. Route A is validated against JDK 21."
fi
```

如果用户本机没有 JDK 21，不要自动安装。提示用户安装后继续。

### Step 3: Update Gradle DSL Conservatively

检查 `build.gradle.kts`：

- `application { mainClassName = ... }` 在 Gradle 8 可能需要改为：

```kotlin
application {
    mainClass.set("com.htmake.reader.ReaderUIApplicationKt")
}
```

- `tasks.create<...>` 可暂时保留，只有编译报错再迁移到 `tasks.register<...>`。
- `sourceCompatibility` / `targetCompatibility` 不要立即设到 21。第一阶段可以先保持 1.8，待 Kotlin/Spring 迁移稳定后再调整。

### Step 4: Verify

运行：

```bash
bash -n build.sh
./gradlew.bat --version
./gradlew.bat tasks
./gradlew.bat classes
./build.sh serve
```

### Expected Results

- Gradle 输出版本为 8.14.x。
- JVM 为 21.x。
- `tasks` 可打印。
- `classes` 尽量通过；如果因插件版本过旧失败，记录错误，进入本 Phase 内修复，不进入 Phase 2。
- `./build.sh serve` 能启动。

### Failure Handling

如果 Gradle 8 无法解析 `build.gradle.kts`：

- 只修 Gradle DSL。
- 不升级 Kotlin / Spring / Vert.x。
- 不修改业务代码。

---

## Phase 2: Kotlin 1.9.25

### Purpose

升级 Kotlin 到 1.9.x，避免同时引入新编译器风险。

### Target Versions

```text
Kotlin Gradle Plugin: 1.9.25
kotlin-stdlib-jdk8: 1.9.25
org.jetbrains.kotlin.plugin.spring: 1.9.25
```

### Files

可能修改：

- `build.gradle.kts`
- `cli.gradle`
- Kotlin 编译错误涉及的 `.kt` 文件

### Step 1: Update Kotlin Versions

在 `build.gradle.kts` 中统一 Kotlin 版本。

保留 Kotlin Spring plugin：

```kotlin
id("org.jetbrains.kotlin.plugin.spring") version "1.9.25"
```

不要删除该插件。Spring 代理类仍需要它处理 Kotlin 默认 final class 的问题。

### Step 2: Set JVM Target

先尝试：

```kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
```

原因：

- Spring Boot 3 最低 Java 17。
- JDK 21 可以运行 Java 17 bytecode。
- 避免 JVM target 21 在部分插件组合里引入额外问题。

如果后续确实需要 Java 21 bytecode，再单独升级。

### Step 3: Compile

运行：

```bash
./gradlew.bat classes
```

常见修复：

- 更严格的 nullability。
- 更严格的泛型推断。
- unchecked cast 原本 warning 变成 error 的位置。
- 协程 API 版本冲突。

原则：

- 只修编译错误。
- 不做结构重构。
- 不改业务行为。

### Step 4: Tests and Serve

运行：

```bash
./gradlew.bat test
./build.sh serve
```

### Exit Criteria

- `classes` 通过。
- `test` 通过或只存在已记录的历史测试问题。
- `serve` 能启动。

---

## Phase 3: Spring Boot 3.5.x

### Purpose

将 Spring Boot 从 2.1.6 升级到 3.5.x，完成 Jakarta 迁移。

### Target Versions

```text
Spring Boot: 3.5.x
Spring Framework: 6.2.x, managed by Boot
```

### Files

可能修改：

- `build.gradle.kts`
- `cli.gradle`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `src/main/java/com/htmake/reader/**/*.kt`

### Step 1: Update Spring Boot Plugin

示例：

```kotlin
id("org.springframework.boot") version "3.5.0"
```

也要检查 `io.spring.dependency-management` 是否仍需要显式 apply。

### Step 2: Use Boot BOM Where Possible

原则：

- Jackson 优先交给 Spring Boot dependency management。
- 测试依赖优先使用 `spring-boot-starter-test`。
- 不在同一阶段升级 OkHttp / Vert.x。

### Step 3: Jakarta Migration

查找：

```bash
rg -n "import javax\\." src/main/java
```

只迁移 Jakarta EE 相关包：

```text
javax.annotation.* -> jakarta.annotation.*
javax.servlet.* -> jakarta.servlet.*
javax.validation.* -> jakarta.validation.*
javax.persistence.* -> jakarta.persistence.*
```

不要迁移：

```text
javax.crypto.*
javax.net.ssl.*
javax.security.*
javax.xml.*
```

### Step 4: Configuration Properties

检查：

- `AppConfig.kt`
- `BookConfig.kt`
- 主启动类

如果 `@ConfigurationProperties` 不再自动生效，添加：

```kotlin
@ConfigurationPropertiesScan
```

或：

```kotlin
@EnableConfigurationProperties(AppConfig::class, BookConfig::class)
```

以实际编译和启动结果为准。

### Step 5: Compile and Start

运行：

```bash
./gradlew.bat classes
./gradlew.bat test
./build.sh serve
```

### Runtime Checks

启动后检查：

- 无 Spring bean 初始化错误。
- 无配置绑定错误。
- 无 route 注册错误。
- `reader.server.port` 参数仍生效。

### Exit Criteria

- 服务能启动到 `http://localhost:8080`。
- 基础 API 可调用。

---

## Phase 4: Vert.x 4.5.x

### Purpose

从 Vert.x 3.8.1 升级到 Vert.x 4.5.x。不要跨到下一代主版本。

### Target Versions

```text
vertx-core: 4.5.x
vertx-web: 4.5.x
vertx-web-client: 4.5.x
vertx-lang-kotlin: 4.5.x
vertx-lang-kotlin-coroutines: 4.5.x
```

### Files

重点修改：

- `src/main/java/com/htmake/reader/verticle/RestVerticle.kt`
- `src/main/java/com/htmake/reader/api/YueduApi.kt`
- `src/main/java/com/htmake/reader/api/controller/BaseController.kt`
- `src/main/java/com/htmake/reader/api/controller/BookController.kt`
- `src/main/java/com/htmake/reader/api/controller/BookSourceController.kt`
- `src/main/java/com/htmake/reader/api/controller/UserController.kt`
- `src/main/java/com/htmake/reader/api/controller/WebdavController.kt`
- `src/main/java/com/htmake/reader/api/controller/RssSourceController.kt`
- `src/main/java/com/htmake/reader/api/controller/ReplaceRuleController.kt`
- `src/main/java/com/htmake/reader/api/controller/BookmarkController.kt`
- `src/main/java/com/htmake/reader/utils/VertExt.kt`
- `src/main/java/com/htmake/reader/utils/Ext.kt`

### Step 1: Update Dependencies

只更新 Vert.x 相关依赖。

### Step 2: Compile

运行：

```bash
./gradlew.bat classes
```

根据编译错误逐个修：

- `setHandler` -> `onComplete`
- callback shape 变化
- `Future` / `AsyncResult` 泛型变化
- WebClient send API 变化
- BodyHandler / SessionHandler / CorsHandler API 变化

### Step 3: Keep Coroutine Style

如果现有代码已在 `suspend` 函数里，可优先使用 Vert.x coroutine `await()`，但不要大范围把 callback 改成 suspend 架构。只做必要适配。

### Step 4: API Smoke Test

启动：

```bash
./build.sh serve
```

手动或脚本验证：

```text
GET http://localhost:8080/
GET http://localhost:8080/reader3/getBookshelf
GET http://localhost:8080/reader3/getBookSources?simple=1
```

如果开启 secure，需要带登录态或在本地配置确认。

### Step 5: Feature Regression

至少验证：

- 书架加载。
- 书源管理显示。
- 搜索。
- 阅读章节。
- 本地书籍目录。
- 远程书源导入。
- 章节缓存 SSE。

### Exit Criteria

- `classes` 通过。
- `test` 通过。
- `serve` 可用。
- 核心 API 可用。

---

## Phase 5: Secondary Backend Dependencies

### Purpose

在框架升级稳定后，小步升级其他后端依赖。

### Candidate Groups

建议分组执行，每组独立验证：

#### Group A: HTTP

```text
OkHttp 4.9.1 -> 4.12.x
logging-interceptor -> same version
Retrofit 保守升级或暂不动
retrofit-vertx 暂不动，除非编译必须
```

#### Group B: JSON / HTML

```text
Gson 2.8.5 -> 2.11.x
Jsoup 1.14.1 -> 1.17.x / 1.18.x / later stable
json-path 2.6.0 -> 2.9.x / 2.10.x
```

#### Group C: Utility

```text
Guava 28 -> 33.x
hutool-crypto 5.8.0.M1 -> 5.8.x stable
JsoupXpath 2.5.0 -> compatible latest
```

#### Group D: Local JARs

```text
rhino local jar: verify JDK 21 compatibility
xmlpull local jar: verify JDK 21 compatibility
```

### Rules

- 不要一次性升级全部。
- 每组升级后跑：

```bash
./gradlew.bat classes
./gradlew.bat test
./build.sh serve
```

- 如果某组失败，回退该组，不影响前面阶段。

---

## Phase 6: Frontend Integration Preservation

### Purpose

确认后端升级后，当前 Vue 2 前端仍能通过 serve 访问。

### Files

一般不修改，除非 build/serve 集成失败：

- `build.sh`
- `web/package.json`
- `web/src/registerServiceWorker.js`
- `src/main/resources/web/**`

### Checks

运行：

```bash
./build.sh sync
./build.sh serve
```

浏览器打开：

```text
http://localhost:8080/?nopwa=1
```

确认：

- [ ] 首页打开。
- [ ] 书架显示。
- [ ] 书源管理显示。
- [ ] 搜索可用。
- [ ] 阅读页可用。
- [ ] 设置保存可用。

### Notes

localhost 下应继续避免 service worker 干扰。不要在本阶段恢复 PWA 注册。

---

## Phase 7: Full Regression Checklist

### Backend

- [ ] `./gradlew.bat classes`
- [ ] `./gradlew.bat test`
- [ ] `./build.sh sync`
- [ ] `./build.sh serve`

### Web

- [ ] 首页能打开。
- [ ] 登录/免登录模式符合配置。
- [ ] 书架加载。
- [ ] 网络书籍封面加载。
- [ ] 本地书籍目录加载。
- [ ] 书源管理列表显示。
- [ ] 远程书源 URL 导入。
- [ ] 书源过滤仍剔除漫画/图片/音频源。
- [ ] WebView 源兼容策略不被破坏。
- [ ] 失效书源检测结果及时显示。
- [ ] 搜索结果聚合。
- [ ] 多书源切换。
- [ ] 加入书架。
- [ ] 未读章节标题和数量显示。
- [ ] 阅读页打开章节。
- [ ] 阅读设置浅色/深色切换不重置字号/间距。
- [ ] 服务端章节缓存。
- [ ] 封面代理。

---

## Handoff Notes for Next Session

下一 session 开始时先读：

```text
docs/JDK21-ROUTE-A-UPGRADE-SPEC.md
docs/superpowers/plans/2026-05-07-backend-route-a-upgrade.md
docs/superpowers/plans/2026-05-07-frontend-route-a-stability.md
HANDOFF.md
```

然后从 Phase 0 开始，不要直接跳到 Phase 1。

如果用户说“继续执行路线 A”，执行规则是：

1. 做当前 Phase。
2. 跑验证。
3. 输出结果。
4. 停下等确认。

不要自动提交。需要提交时提示用户自行执行 git。

