export default [
  // 靜態路徑要排在 :articleId 前面，否則會被動態參數攔截（例如 forum/new 會被誤判成 articleId="new"）
  {
    path: 'forum',
    name: 'forumList',
    component: () => import('@/features/forum/view/ArticleListView.vue'),
  },
  {
    path: 'forum/new',
    name: 'forumCreate',
    component: () => import('@/features/forum/view/ArticleFormView.vue'),
  },
  {
    path: 'forum/bookmarks',
    name: 'forumBookmarks',
    component: () => import('@/features/forum/view/MyBookmarksView.vue'),
  },
  {
    path: 'forum/:articleId/edit',
    name: 'forumEdit',
    component: () => import('@/features/forum/view/ArticleFormView.vue'),
    props: true,
  },
  {
    path: 'forum/:articleId',
    name: 'forumDetail',
    component: () => import('@/features/forum/view/ArticleDetailView.vue'),
    props: true,
  },
]
