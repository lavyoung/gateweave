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
- `features`：按照管理能力组织的页面、模型及开发夹具。
- `layouts`：跨功能页面复用的管理后台布局。
- `main.ts`：Vue 应用及全局插件装配入口。

## 当前边界

当前节点列表使用本地开发夹具，用于验证管理后台布局、路由、菜单和表格组件。

真实节点查询、HTTP 客户端和跨域配置将在后续章节引入。前端模型不直接复用 Center 领域对象，跨进程数据必须由管理 API 返回契约定义。
