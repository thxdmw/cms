import { reactive } from 'vue';
import { api } from './api.js';
import { buildTree } from './tree.js';

// 极简全局状态，不引入 Pinia：当前登录用户信息 + 权限字符串集合 + 菜单树
export const store = reactive({
    ready: false,
    username: '',
    nickname: '',
    perms: new Set(),
    menuTree: []
});

// 把 /menu 返回的扁平列表（带 parentId）在客户端组装成菜单树。
// 没有直接用后端现成的 selectMenuTreeByUserId，是因为那个方法会把 url 里的开头 "/" 替换成 "#"
// （服务于旧的 hash 路由方案），而新 SPA 用的是干净路径路由，用扁平数据自己组装，
// 保留原始 url，交给 SideMenu 组件按需加 /admin 前缀
// Permission.id/parentId 是 UUID 字符串（顶层用字符串 "0"），topLevelValue 要用字符串才能匹配上
function buildMenuTree(flatList) {
    return buildTree(flatList, { idKey: 'id', parentIdKey: 'parentId', topLevelValue: '0' });
}

export async function loadCurrentUser() {
    const [userRes, menuList] = await Promise.all([api.getCurrentUser(), api.getMenu()]);
    if (!userRes || userRes.status !== 200 || !userRes.data) {
        // 未登录/会话过期：/currentUser 被 Shiro 的 annoOrLogin 兜底规则拦截，不会是预期的 JSON
        window.location.href = '/login';
        return;
    }
    store.username = userRes.data.username;
    store.nickname = userRes.data.nickname;
    store.perms = new Set(userRes.data.perms || []);
    store.menuTree = buildMenuTree(menuList);
    store.ready = true;
}

export function hasPerm(permission) {
    return store.perms.has(permission);
}
