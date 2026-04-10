/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * 后端 API 前缀，如 `http://localhost:8080/api`。
   * 不设则开发环境使用 `/api` 并由 Vite 代理到后端（见 vite.config.ts）。
   */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
