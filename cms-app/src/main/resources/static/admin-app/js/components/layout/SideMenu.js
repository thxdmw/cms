// 递归渲染菜单树。DB 里的 Permission.url 是旧的顶层路径写法（如 /articles），
// 这里客户端渲染时统一加上 /admin 前缀，不改 DB 数据，纯展示层转换
function toRoutePath(url) {
    if (!url) return '/admin';
    return url.startsWith('/admin') ? url : ('/admin' + url);
}

// 是否有子菜单决定渲染成 el-sub-menu 还是 el-menu-item，
// 不依赖 Permission.type 字段的具体取值（更稳妥，不用假设后端返回的 type 集合）
const SideMenuItem = {
    name: 'SideMenuItem',
    props: { node: { type: Object, required: true } },
    setup() {
        return { toRoutePath };
    },
    template: `
    <el-sub-menu v-if="node.children && node.children.length" :index="'dir-' + node.id">
        <template #title>
            <i v-if="node.icon" :class="node.icon"></i>
            <span>{{ node.name }}</span>
        </template>
        <side-menu-item v-for="child in node.children" :key="child.id" :node="child"></side-menu-item>
    </el-sub-menu>
    <el-menu-item v-else :index="toRoutePath(node.url)">
        <i v-if="node.icon" :class="node.icon"></i>
        <template #title>{{ node.name }}</template>
    </el-menu-item>
    `
};
SideMenuItem.components = { SideMenuItem };

export default SideMenuItem;
