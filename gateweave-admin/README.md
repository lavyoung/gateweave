# GateWeave Admin

GateWeave Admin 是 GateWeave 控制面的独立运营管理界面，用于展示和管理网关节点、服务目录、路由与配置发布。

前端工程独立构建和部署，通过 HTTP 与 `gateweave-center` 协作，不加入 Maven Reactor，也不依赖任何 Java 模块。

## 技术栈

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- Vitest
- ESLint
- Prettier

## 环境要求

- Node.js `^22.18.0 || >=24.12.0`
- npm 11 或兼容版本

## 本地开发

```shell
npm install
npm run dev
```

默认访问地址由 Vite 启动日志给出。

## 验证命令

```shell
npm run lint
npm run type-check
npm run test:unit -- --run
npm run build
```

生产构建输出位于 `dist` 目录。

## 目录结构

```text
src
├── app
│   ├── router
│   └── styles
├── features
│   ├── dashboard
│   ├── gateway-node
│   └── not-found
├── layouts
├── App.vue
└── main.ts
```

- `app`：应用级路由、全局样式和工具生成的组件声明。
- `features`：按照管理能力组织页面、业务模型、API 适配及对应测试。
- `layouts`：跨功能页面复用的管理后台布局。
- `main.ts`：Vue 应用及全局插件装配入口。

## 当前能力

网关节点页面已经通过 HTTP 调用 `gateweave-center`，支持：

- 按节点标识、网关分组和多个节点状态筛选；
- 分页查询、调整每页数量和重新加载；
- 加载、空数据及请求失败状态展示；
- 保留 Center 返回的稳定错误码、错误文案和请求标识；
- 通过统一 HTTP 客户端解析 `Result<T>` 响应。

开发环境默认使用以下 Center API 地址：

```dotenv
VITE_GATEWEAVE_CENTER_API_BASE_URL=http://localhost:8080/api/v1
```

部署时应通过环境配置替换实际地址。前端模型只表达管理 API 的 JSON 契约，不直接依赖 Center 领域对象或 Java 模块。
