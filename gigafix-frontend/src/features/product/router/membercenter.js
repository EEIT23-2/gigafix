export default [
  {
    path: "recycle",
    name: "member-recycle-applications",
    component: () => import("../view/client/MemberApplysFormListView.vue"),
    meta: {
      requiresMember: true,
    },
  },
  {
    path: "recycle/:applyId",
    name: "member-recycle-application-detail",
    component: () => import("../view/client/MemberApplyFormDetailView.vue"),
    props: true,
    meta: {
      requiresMember: true,
    },
  },
];
