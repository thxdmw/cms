import { createRouter, createWebHistory } from 'vue-router';
import AdminLayout from './components/layout/AdminLayout.js';
import DashboardView from './components/dashboard/DashboardView.js';
import ArticleListView from './components/article/ArticleListView.js';
import ArticleFormView from './components/article/ArticleFormView.js';
import CategoryListView from './components/category/CategoryListView.js';
import TagListView from './components/tag/TagListView.js';
import CommentListView from './components/comment/CommentListView.js';
import UserListView from './components/user/UserListView.js';
import RoleListView from './components/role/RoleListView.js';
import PermissionListView from './components/permission/PermissionListView.js';
import SiteInfoView from './components/site/SiteInfoView.js';
import LinkListView from './components/link/LinkListView.js';
import ThemeListView from './components/theme/ThemeListView.js';
import OnlineUsersView from './components/online/OnlineUsersView.js';
import LogView from './components/log/LogView.js';
import DatabaseView from './components/database/DatabaseView.js';
import ServerFileListView from './components/serverfile/ServerFileListView.js';
import NotFoundView from './components/common/NotFoundView.js';

// 子路由路径跟数据库里 Permission.url 去掉前导 '/' 后的值一一对应
// （SideMenu.js 用 node.url.replace(/^\//, '/admin/') 拼出实际导航地址），
// 保证侧边栏菜单点进来的地址和这里注册的路由能对上
const routes = [
    {
        path: '/admin',
        component: AdminLayout,
        children: [
            { path: '', redirect: '/admin/workdest' },
            { path: 'workdest', component: DashboardView },
            { path: 'articles', component: ArticleListView },
            { path: 'articles/add', component: ArticleFormView },
            { path: 'articles/:id/edit', component: ArticleFormView, props: true },
            // 数据库里"发布文章"是文章管理目录下独立的侧边栏菜单项，url 是 /article/add（单数），
            // 跟文章列表页内部"写文章"按钮跳转用的 /articles/add（复数）是两个不同路径，都要能进同一个表单
            { path: 'article/add', component: ArticleFormView },
            { path: 'categories', component: CategoryListView },
            { path: 'tags', component: TagListView },
            { path: 'comments', component: CommentListView },
            { path: 'users', component: UserListView },
            { path: 'roles', component: RoleListView },
            { path: 'permissions', component: PermissionListView },
            { path: 'siteinfo', component: SiteInfoView },
            { path: 'links', component: LinkListView },
            { path: 'themes', component: ThemeListView },
            { path: 'online/users', component: OnlineUsersView },
            { path: 'log/page', component: LogView },
            { path: 'database/monitoring', component: DatabaseView },
            { path: 'serverFile', component: ServerFileListView },
            { path: ':pathMatch(.*)*', component: NotFoundView }
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior() {
        return { top: 0 };
    }
});

export default router;
