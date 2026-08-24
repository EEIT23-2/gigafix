<script setup>
import { REPORT_STATUS_MAP, statusBadgeClass, statusLabel } from '../../adminStatusMaps'

defineProps({
  reports: { type: Array, default: () => [] },
})

const emit = defineEmits(['view'])

function formatDate(value) {
  return value?.slice(0, 19)?.replace('T', ' ') ?? '-'
}

function targetLabel(report) {
  return report.articleId != null ? `文章 #${report.articleId}` : `留言 #${report.commentId}`
}
</script>

<template>
  <div class="table-responsive">
    <table class="table table-hover align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>檢舉目標</th>
          <th>檢舉人</th>
          <th>原因</th>
          <th>狀態</th>
          <th>檢舉時間</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="report in reports" :key="report.reportId">
          <td>{{ report.reportId }}</td>
          <td>
            <button class="target-button fw-semibold" type="button" @click="emit('view', report)">
              {{ targetLabel(report) }}
            </button>
          </td>
          <td>{{ report.reporterNickName }}（#{{ report.reporterId }}）</td>
          <td class="reason-cell">{{ report.reason }}</td>
          <td>
            <span class="badge" :class="statusBadgeClass(REPORT_STATUS_MAP, report.status)">
              {{ statusLabel(REPORT_STATUS_MAP, report.status) }}
            </span>
          </td>
          <td>{{ formatDate(report.reportCreatedTime) }}</td>
        </tr>
        <tr v-if="reports.length === 0">
          <td colspan="6" class="text-center text-secondary py-5">沒有檢舉資料</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.target-button {
  padding: 0;
  color: #111827;
  background: transparent;
  border: 0;
  cursor: pointer;
}
.target-button:hover {
  color: #0d6efd;
  text-decoration: underline;
}
.reason-cell {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
th {
  white-space: nowrap;
  font-size: 0.8rem;
  letter-spacing: 0.03em;
}
td {
  font-size: 0.875rem;
}
</style>
