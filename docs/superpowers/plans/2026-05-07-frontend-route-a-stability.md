# Frontend Stability Plan - Route A

> 本计划只描述路线 A 的前端稳定性要求。  
> 本文档的作用是约束下一 session 不要启动前端框架迁移。  
> 默认验收入口：`./build.sh serve`。  
> 不自动执行 git 操作。

---

## Goal

路线 A 的前端目标是：

```text
保持当前 Vue 2 + Element UI + Vuex + Vue CLI 可用
确保后端依赖升级后 Web 端不回归
确保 ./build.sh sync / ./build.sh serve 继续可用
```

本轮前端不是现代化迁移工程。

---

## Current Frontend Stack

| 组件 | 当前版本 | 路线 A 操作 |
|------|----------|-------------|
| Vue | 2.6.10 | 保持 |
| Element UI | 2.15.9 | 保持 |
| Vuex | 3.1.1 | 保持 |
| vue-router | 3.1.3 | 保持 |
| Vue CLI | 4.x | 保持 |
| axios | 0.21.1 | 暂时保持 |
| localforage | 1.10.0 | 保持 |
| register-service-worker | 1.7.1 | 保持，但 localhost serve 下禁用 |

---

## Non-Goals

本轮明确不做：

- 不安装新的前端主框架版本。
- 不安装 `@vue/compat`。
- 不迁移前端构建工具。
- 不迁移前端状态管理。
- 不迁移前端 UI 框架。
- 不迁移前端路由主版本。
- 不升级 ESLint/Prettier 大版本。
- 不批量升级 npm 依赖。
- 不重写组件。
- 不改变当前 UI 布局。

---

## Why Keep Frontend Stable

当前 Web 端刚完成一批功能修复：

- 远程书源导入。
- 书源过滤。
- WebView 源兼容边界。
- 未读章节展示。
- 搜索结果聚合和书源切换。
- 服务端章节缓存。
- 阅读设置浅色/深色切换修复。
- PWA 缓存规避。

前端框架迁移会同时触碰：

- `web/src/main.js`
- `web/src/App.vue`
- `web/src/plugins/vuex.js`
- `web/src/router/index.js`
- 所有 `.vue` 组件
- Element UI 组件语法
- PWA 构建逻辑
- `build.sh sync` 产物路径

这会把后端升级风险和前端框架迁移风险叠加，不符合路线 A 的目标。

---

## Route A Frontend Responsibilities

路线 A 中，前端只承担三类工作：

1. 保证构建链路不坏。
2. 保证 serve 模式不被 service worker 旧缓存干扰。
3. 做后端升级后的 Web 端回归验证。

---

## Phase F0: Baseline Check

### Purpose

在后端升级前确认当前前端可构建、可访问。

### Commands

```bash
./build.sh sync
./build.sh serve
```

### Browser URL

```text
http://localhost:8080/?nopwa=1
```

### Expected

- [ ] 页面能打开。
- [ ] 不出现白屏。
- [ ] 书架加载。
- [ ] 书源管理能打开。
- [ ] 搜索入口可用。
- [ ] 阅读页可打开。

---

## Phase F1: Keep build.sh Sync Stable

### Current Behavior

当前 `build.sh` 已经有 `runWebPackageManager`：

```bash
runWebPackageManager()
{
    if command -v yarn >/dev/null 2>&1; then
        yarn "$@"
    elif command -v npm >/dev/null 2>&1; then
        if [[ $# -eq 0 ]]; then
            npm install --legacy-peer-deps
        else
            npm run "$@"
        fi
    else
        echo "Neither yarn nor npm was found. Please install Node.js with npm, or install yarn."
        exit 1
    fi
}
```

因此本轮不要把 `sync` 改回硬编码 `yarn build` 或硬编码 `npm run build`。

### Allowed Changes

只在构建失败时做最小修复：

- 修正 npm/yarn fallback。
- 修正资源同步路径。
- 修正 Windows Git Bash 路径兼容。

### Not Allowed

- 不迁移前端构建工具。
- 不移动 `web/public/index.html`。
- 不删除 Vue CLI。
- 不修改 `web/package.json` 的核心框架依赖。

---

## Phase F2: Service Worker Policy for serve

### Current Requirement

用户后续只考虑 `./build.sh serve`。

本地 serve 下，service worker 容易造成旧资源和新资源混用，所以 localhost 应继续禁用或注销 service worker。

### File

- `web/src/registerServiceWorker.js`

### Expected Policy

- `localhost`
- `127.0.0.1`
- `::1`
- `?nopwa=1`

这些场景下不注册 service worker，并尽量注销已有 registration。

### Verification

```bash
cd web
npm run lint -- --no-fix src/registerServiceWorker.js
cd ..
./build.sh sync
./build.sh serve
```

浏览器强刷：

```text
Ctrl + F5
```

---

## Phase F3: Web Regression After Backend Phase

每完成一个后端 Phase，都要跑一次前端 smoke test。

### Smoke Test URL

```text
http://localhost:8080/?nopwa=1
```

### Checklist

- [ ] 首页不是白屏。
- [ ] 控制台无阻断性 JS 错误。
- [ ] 后端 API 根路径仍是 `/reader3`。
- [ ] 书架请求能返回。
- [ ] 书源请求能返回。
- [ ] 图片/封面代理仍可用。

---

## Phase F4: Full Web Regression

后端所有 Phase 完成后，执行完整 Web 回归。

### Bookshelf

- [ ] 书架显示书籍。
- [ ] 封面显示。
- [ ] 未读章节标题显示。
- [ ] 未读章节数量显示真实数字。
- [ ] 本地书籍不报目录错误。

### Book Sources

- [ ] 书源管理能显示已导入源。
- [ ] 远程书源 URL 导入可用。
- [ ] 导入结果提示不会一闪而逝。
- [ ] 漫画/图片/音频源仍被过滤。
- [ ] WebView 源兼容策略不回退。
- [ ] 失效书源检测结果即时显示。

### Search

- [ ] 单源搜索可用。
- [ ] 多源搜索可用。
- [ ] 同一本书聚合显示。
- [ ] 多书源下拉可切换。
- [ ] 加入书架使用当前选中书源。
- [ ] 搜索卡片元素不重叠。

### Reader

- [ ] 网络章节能打开。
- [ ] 本地章节能打开。
- [ ] 阅读进度保存。
- [ ] 浅色/深色切换不重置字号、行距、段距。
- [ ] 切换主题后再改设置不会跳回浅色。

### Cache

- [ ] 服务端章节缓存入口可用。
- [ ] 单章失败不阻断后续章节缓存。
- [ ] 缓存进度里的成功/失败数量合理。

---

## If Frontend Breaks During Backend Upgrade

处理顺序：

1. 先确认 `./build.sh serve` 是否启动成功。
2. 再访问 `http://localhost:8080/?nopwa=1&errorAlert=1`。
3. 如果是 API 失败，优先查后端接口兼容。
4. 如果是 JS 白屏，查浏览器控制台和 `src/main/resources/web` 产物。
5. 不要直接启动前端框架迁移。

---

## Handoff Notes

下一 session 如果被要求“执行路线 A”，前端侧只做：

1. 读本文档。
2. 跑 `./build.sh sync`。
3. 跑 `./build.sh serve`。
4. 做 Web smoke test。
5. 如有白屏或 API 错误，按现有 Vue 2 架构修复。

不要启动前端框架迁移。

