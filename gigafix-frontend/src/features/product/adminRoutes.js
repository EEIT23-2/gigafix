export default [
  {
    path: "repairs",
    name: "repairs",
    component: () => import("./view/RepairsListView.vue"),
  },
  {
    path: "repairs/:repairId",
    name: "repairs/:repairId",
    component: () => import("./view/RepairDetailView.vue"),
    props: true,
  },
  {
    path: "technicians",
    name: "technicians",
    component: () => import("./view/TechniciansView.vue"),
  },
  {
    path: "stores",
    name: "stores",
    component: () => import("./view/StoresView.vue"),
  },
];