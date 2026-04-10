/** 列表项（与后端 DTO 对齐后再映射的展示结构） */
export interface SpecialItem {
  id: string | number | undefined
  title: string
  /** 对应后端 banner */
  cover: string
  /** 专题简介 */
  introduction: string
  /** 是否已关注（后端可能为字符串 "true"/"false"） */
  isFollowing: boolean
  /** 展示用更新时间 */
  updateLabel: string
  /** 展示用浏览量 */
  visitLabel: string
  /** 展示用关注人数 */
  followersLabel: string
}

export interface SpecialPageResult {
  list: SpecialItem[]
  total: number
}
