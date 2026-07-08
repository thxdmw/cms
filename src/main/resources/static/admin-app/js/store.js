import { reactive } from 'vue';
import { api } from './api.js';
import { buildTree } from './tree.js';

// 移动端断点：和 css/admin-responsive.css 里的 @media (max-width: 768px) 保持一致，
// 两处任何一处改动都要同步改另一处，否则 JS 判断的"是否移动端"和实际生效的响应式样式会对不上
const MOBILE_BREAKPOINT_QUERY = '(max-width: 768px)';

// 极简全局状态，不引入 Pinia：当前登录用户信息 + 权限字符串集合 + 菜单树 + 移动端布局状态
export const store = reactive({
    ready: false,
    username: '',
    nickname: '',
    perms: new Set(),
    menuTree: [],
    // 当前是否处于移动端窄屏布局
    isMobile: window.matchMedia(MOBILE_BREAKPOINT_QUERY).matches,
    // 移动端抽屉式导航菜单是否展开（桌面端固定侧边栏不受这个状态影响）
    mobileMenuVisible: false
});

// 监听断点变化：横竖屏切换、浏览器窗口拖拽跨越断点时，实时更新 isMobile，
// 从桌面切到移动端时顺带收起可能残留展开状态的抽屉菜单
window.matchMedia(MOBILE_BREAKPOINT_QUERY).addEventListener('change', (e) => {
    store.isMobile = e.matches;
    if (!store.isMobile) {
        store.mobileMenuVisible = false;
    }
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
