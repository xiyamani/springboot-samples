import axios, { type AxiosRequestConfig } from 'axios'

function baseURL() {
  const v = import.meta.env.VITE_API_BASE?.trim()
  return v ? v.replace(/\/$/, '') : '/api'
}

const instance = axios.create({
  baseURL: baseURL(),
  timeout: 30_000,
})

if (import.meta.env.DEV) {
  console.info('[http] baseURL =', instance.defaults.baseURL)
}

/** 统一封装：返回响应 body（`data`），错误仍抛 AxiosError */
export async function httpGet<T = unknown>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<T> {
  const res = await instance.get<T>(url, config)
  return res.data
}

export { instance as http }
