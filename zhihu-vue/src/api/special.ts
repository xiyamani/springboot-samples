import { httpGet } from '../utils/http'
import {
  normalizeItem,
  pickOneRecord,
  pickRows,
  pickTotal,
  asRecord,
} from './specialNormalize'
import type { SpecialItem, SpecialPageResult } from '../types/special'

export async function fetchSpecialByTitle(params: {
  title: string
  pageNum: number
  pageSize: number
}): Promise<SpecialPageResult> {
  const data = await httpGet<unknown>('/v1/special/page', {
    params: {
      title: params.title,
      pageNum: params.pageNum,
      pageSize: params.pageSize,
    },
  })
  const list = pickRows(data)
    .map((r) => asRecord(r))
    .filter(Boolean)
    .map((r) => normalizeItem(r!))
  let total = pickTotal(data)
  if (total <= 0 && list.length > 0) total = list.length
  return { list, total }
}

/** 详情：默认 `GET /v1/special/detail?id=` */
export async function fetchSpecialById(id: string): Promise<SpecialItem | null> {
  const data = await httpGet<unknown>('/v1/special/detail', {
    params: { id },
  })
  const row = pickOneRecord(data)
  return row ? normalizeItem(row) : null
}
