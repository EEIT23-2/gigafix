export default [
  {
    path: "repairs",
    name: "admin-repairs",
    component: () => import("../view/RepairsListView.vue"),
  },
  {
    path: "repairs/stats",
    name: "admin-repairs-stats",
    component: () => import("../view/RepairStatsView.vue"),
  },
  {
    path: "repairs/:repairId",
    name: "admin-repair-detail",
    component: () => import("../view/RepairDetailView.vue"),
    props: true,
  },
  {
    path: "technicians",
    name: "admin-technicians",
    component: () => import("../view/TechniciansView.vue"),
  },
  {
    path: "stores",
    name: "admin-stores",
    component: () => import("../view/StoresView.vue"),
  },
];
