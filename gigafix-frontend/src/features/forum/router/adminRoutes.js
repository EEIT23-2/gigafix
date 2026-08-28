export default [
  {
    path: 'forum/articles',
    name: 'admin-forum-articles',
    component: () => import('../view/admin/ArticleAdminListView.vue'),
  },
  {
    path: 'forum/articles/:articleId',
    name: 'admin-forum-article-detail',
    component: () => import('../view/admin/ArticleAdminDetailView.vue'),
    props: true,
  },
  {
    path: 'forum/reports',
    name: 'admin-forum-reports',
    component: () => import('../view/admin/ReportAdminListView.vue'),
  },
  {
    path: 'forum/reports/:reportId',
    name: 'admin-forum-report-detail',
    component: () => import('../view/admin/ReportAdminDetailView.vue'),
    props: true,
  },
]
