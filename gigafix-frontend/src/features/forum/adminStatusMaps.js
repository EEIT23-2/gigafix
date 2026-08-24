// 文章狀態：label/badgeClass 對照表。列表頁篩選用全部 7 種；
// 狀態「變更」下拉只能給 ADMIN_ARTICLE_STATUS_OPTIONS 這 6 種（對齊後端 ADMIN_ALLOWED_TARGETS，不含 DRAFT）
export const ARTICLE_STATUS_MAP = {
  DRAFT: { label: '草稿', badgeClass: 'text-bg-secondary' },
  PUBLISHED: { label: '發布', badgeClass: 'text-bg-success' },
  HIDDEN: { label: '作者隱藏', badgeClass: 'text-bg-warning' },
  TAKEN_DOWN: { label: '下架', badgeClass: 'text-bg-danger' },
  CLOSED: { label: '作者關閉', badgeClass: 'text-bg-secondary' },
  FORCE_HIDDEN: { label: '管理員強制隱藏', badgeClass: 'text-bg-dark' },
  FORCE_CLOSED: { label: '管理員強制關閉', badgeClass: 'text-bg-dark' },
}

// 狀態「變更」下拉可選的目標：管理員只能操作發布/下架/強制隱藏/強制關閉
export const ADMIN_ARTICLE_STATUS_OPTIONS = ['PUBLISHED', 'TAKEN_DOWN', 'FORCE_HIDDEN', 'FORCE_CLOSED']

// 文章目前狀態必須落在這個集合內，管理員才能變更狀態（草稿、作者隱藏不開放操作）
export const ADMIN_ARTICLE_SOURCE_STATUSES = ['PUBLISHED', 'CLOSED', 'FORCE_HIDDEN', 'FORCE_CLOSED']

export const COMMENT_STATUS_MAP = {
  VISIBLE: { label: '可見', badgeClass: 'text-bg-success' },
  HIDDEN: { label: '隱藏', badgeClass: 'text-bg-warning' },
  TAKEN_DOWN: { label: '下架', badgeClass: 'text-bg-danger' },
}

export const REPORT_STATUS_MAP = {
  PENDING: { label: '待處理', badgeClass: 'text-bg-warning' },
  RESOLVED: { label: '已處理', badgeClass: 'text-bg-success' },
  CLOSED: { label: '關閉', badgeClass: 'text-bg-secondary' },
}

export function statusLabel(map, status) {
  return map[status]?.label ?? status ?? '未知狀態'
}

export function statusBadgeClass(map, status) {
  return map[status]?.badgeClass ?? 'text-bg-dark'
}
