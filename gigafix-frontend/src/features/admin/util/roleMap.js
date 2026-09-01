// src/utils/roleMap.js
const ROLE_LABELS = {
  ROLE_SUPER_ADMIN: '總管理員',
  ROLE_DEPUTY_ADMIN: '副管理員',
  ROLE_FORUM_ADMIN: '論壇管理員',
  ROLE_ECOMMERCE_ADMIN: '訂單商品管理員',
  ROLE_REPAIR_ADMIN: '手機維修管理員'
}

export function getRoleLabel(role) {
  if (!role) return ''
  return ROLE_LABELS[role] || role
}