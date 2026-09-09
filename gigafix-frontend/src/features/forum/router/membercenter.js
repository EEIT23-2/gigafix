// 編輯與發文用的是跟前台完全相同的元件，只是掛在會員中心底下多註冊一次路由，
// 這樣頁面會渲染在 MemberCenterLayout 裡（側邊欄保留），元件也能依路由名稱決定「離開時要回哪」。
// 前台的 forumCreate / forumEdit 維持原樣，行為不受影響。
export default [
  // 靜態路徑要排在 :articleId 前面，否則會被動態參數攔截（沿用 client.js 的慣例）
  {
    path: 'forum',
    name: 'member-forum',
    component: () => import('@/features/forum/view/membercenter/MyForumView.vue'),
  },
  {
    path: 'forum/new',
    name: 'member-forum-create',
    component: () => import('@/features/forum/view/ArticleFormView.vue'),
  },
  {
    path: 'forum/:articleId/edit',
    name: 'member-forum-edit',
    component: () => import('@/features/forum/view/ArticleFormView.vue'),
    props: true,
  },
  {
    // 文章與樓層共用同一頁，頁面內依 parentArticleId 分流
    path: 'forum/:articleId',
    name: 'member-forum-detail',
    component: () => import('@/features/forum/view/membercenter/MyArticleManageView.vue'),
    props: true,
  },
]
