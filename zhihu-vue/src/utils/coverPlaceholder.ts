export function coverPlaceholder(title: string, w = 200, h = 120) {
  const t = encodeURIComponent(title.slice(0, 4) || '专题')
  return `https://via.placeholder.com/${w}x${h}/f0f2f5/8590a6?text=${t}`
}
