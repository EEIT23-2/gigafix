// 跟後端SecurityConfig的/api/admin/repair/**允許的角色一致；前端路由先擋，不用等API回403才彈走
// (分店列表打的是公開API不會403，沒有這層的話權限不足的人也能停在分店管理頁)
const meta = {
  roles: ["ROLE_REPAIR_ADMIN", "ROLE_DEPUTY_ADMIN", "ROLE_SUPER_ADMIN"],
};

export default [
  {
    path: "repairs",
    name: "admin-repairs",
    component: () => import("../view/RepairsListView.vue"),
    meta,
  },
  {
    path: "repairs/stats",
    name: "admin-repairs-stats",
    component: () => import("../view/RepairStatsView.vue"),
    meta,
  },
  {
    path: "repairs/:repairId",
    name: "admin-repair-detail",
    component: () => import("../view/RepairDetailView.vue"),
    props: true,
    meta,
  },
  {
    path: "technicians",
    name: "admin-technicians",
    component: () => import("../view/TechniciansView.vue"),
    meta,
  },
  {
    path: "stores",
    name: "admin-stores",
    component: () => import("../view/StoresView.vue"),
    meta,
  },
];
