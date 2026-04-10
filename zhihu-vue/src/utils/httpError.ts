import axios from 'axios'

export function axiosErrorMessage(
  e: unknown,
  fallback = '请求失败',
): string {
  if (!axios.isAxiosError(e)) return fallback
  if (e.response?.status) {
    const url = e.config?.baseURL
      ? `${e.config.baseURL}${e.config.url ?? ''}`
      : e.config?.url
    return `接口 ${e.response.status}${url ? `：${url}` : ''}`
  }
  return e.message || fallback
}
