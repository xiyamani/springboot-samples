# zhihu-vue

Vue 3 + TypeScript + Vite：**Axios**（`src/utils/http.ts`）+ **Element Plus** + **Vue Router**。专题列表 `/special`，详情 `/special/:id`（详情接口默认为 `GET /v1/special/selectById?id=`，可按后端在 `src/api/special.ts` 修改）。

## 快速开始

```bash
npm install
npm run dev
```

请先在 `http://localhost:8080` 启动提供 `GET /api/v1/special/selectByTitle` 的后端服务；开发环境通过 Vite 将 `/api` 代理到该地址。

## 详细文档

专题页实现说明、接口约定、数据字段映射、代理与部署注意事项见：

**[docs/专题列表开发文档.md](./docs/专题列表开发文档.md)**

## 脚本

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 类型检查并构建生产包 |
| `npm run preview` | 预览生产构建 |

更多 Vue + TS 说明见 [Vue 文档](https://vuejs.org/guide/typescript/overview.html)。
