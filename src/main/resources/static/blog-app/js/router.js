import { createRouter, createWebHistory } from 'vue-router';
import ArticleListView from './components/ArticleListView.js';
import ArticleDetailView from './components/ArticleDetailView.js';
import CommentBoardView from './components/CommentBoardView.js';

// 路由路径和原来 BlogWebController 的 URL 完全保持一致，不破坏旧链接
const routes = [
    { path: '/', component: ArticleListView },
    { path: '/blog/index/:pageNumber', component: ArticleListView },
    { path: '/blog/category/:categoryId', component: ArticleListView },
    { path: '/blog/category/:categoryId/:pageNumber', component: ArticleListView },
    { path: '/blog/tag/:tagId', component: ArticleListView },
    { path: '/blog/tag/:tagId/:pageNumber', component: ArticleListView },
    { path: '/blog/article/:articleId', component: ArticleDetailView },
    { path: '/blog/comment', component: CommentBoardView }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior() {
        return { top: 0 };
    }
});

export default router;
