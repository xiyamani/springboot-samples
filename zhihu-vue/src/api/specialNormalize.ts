import type { SpecialItem } from '../types/special'

export function asRecord(v: unknown): Record<string, unknown> | null {
  return v && typeof v === 'object' && !Array.isArray(v)
    ? (v as Record<string, unknown>)
    : null
}

export function pickRows(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload
  const root = asRecord(payload)
  if (!root) return []
  const keys = ['list', 'records', 'rows', 'items', 'dataList'] as const
  const tryArrays = (o: Record<string, unknown>) => keys.map((k) => o[k])
  for (const v of tryArrays(root)) {
    if (Array.isArray(v)) return v
  }
  if (Array.isArray(root.data)) return root.data
  const data = asRecord(root.data)
  if (data) {
    for (const v of tryArrays(data)) {
      if (Array.isArray(v)) return v
    }
  }
  return []
}

export function pickTotal(payload: unknown): number {
  const root = asRecord(payload)
  if (!root) return 0
  const dataRec = asRecord(root.data)
  for (const c of [
    root.total,
    root.totalCount,
    dataRec?.total,
    dataRec?.totalCount,
  ]) {
    const n = Number(c)
    if (Number.isFinite(n) && n >= 0) return n
  }
  return 0
}

/** 详情：从 `{ data: { ... } }` 或扁平对象中取一条 */
export function pickOneRecord(payload: unknown): Record<string, unknown> | null {
  const root = asRecord(payload)
  if (!root) return null
  const inner = asRecord(root.data)
  if (inner && ('id' in inner || 'title' in inner)) return inner
  if ('id' in root || 'title' in root) return root
  return null
}

function formatVisit(n: unknown): string {
  const num = Number(n)
  if (!Number.isFinite(num)) return String(n ?? '—')
  if (num >= 10_000) return `${(num / 10_000).toFixed(1)} 万`
  return num.toLocaleString('zh-CN')
}

function formatUpdatedField(n: unknown): string | null {
  const num = Number(n)
  if (!Number.isFinite(num) || num <= 0) return null
  const ms = num < 1e12 ? num * 1000 : num
  const d = new Date(ms)
  if (Number.isNaN(d.getTime())) return null
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day} 更新`
}

function parseFollowing(v: unknown): boolean {
  if (typeof v === 'boolean') return v
  const s = String(v).toLowerCase().trim()
  return s === 'true' || s === '1' || s === 'yes'
}

export function normalizeItem(raw: Record<string, unknown>): SpecialItem {
  const title = String(
    raw.title ?? raw.name ?? raw.specialTitle ?? '未命名专题',
  )
  const cover = String(
    raw.banner ??
      raw.bannerUrl ??
      raw.cover ??
      raw.coverUrl ??
      raw.image ??
      raw.pic ??
      raw.avatar ??
      '',
  )
  const id = raw.id ?? raw.specialId ?? raw.topicId

  const introduction = String(raw.introduction ?? '').trim()
  const updatedTs = formatUpdatedField(raw.updated)
  const updateRaw =
    raw.updateTime ??
    raw.updatedAt ??
    raw.updateDate ??
    raw.gmtModified ??
    raw.lastUpdate

  let updateLabel = '—'
  if (updatedTs) updateLabel = updatedTs
  else if (updateRaw != null && updateRaw !== '') {
    const s = String(updateRaw)
    updateLabel = s.includes('更新') ? s : `${s} 更新`
  }

  const vc = raw.viewCount
  const visit =
    (typeof vc === 'object' && vc !== null && 'views' in (vc as object)
      ? (vc as { views?: unknown }).views
      : vc) ??
    raw.views ??
    raw.readCount ??
    raw.visitCount ??
    raw.count

  const followers = raw.followersCount ?? raw.followers ?? raw.followCount

  return {
    id: id as string | number | undefined,
    title,
    cover,
    introduction,
    isFollowing: parseFollowing(raw.isFollowing),
    updateLabel,
    visitLabel: `${formatVisit(visit)} 次浏览`,
    followersLabel: `${formatVisit(followers)} 关注`,
  }
}
