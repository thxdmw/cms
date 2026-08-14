import { store } from '../../store.js';
import TopBar from './TopBar.js';
import SideMenu from './SideMenu.js';

// 桌面端：侧边栏固定 220px，跟内容区并排（原有布局，不变）。
// 移动端（store.isMobile）：侧边栏不再占用页面宽度，改成 el-drawer 覆盖式抽屉，
// 由 TopBar 上的汉堡按钮触发展开，同一份 <side-menu> 菜单树内容在两种布局下共用，
// 不用维护两套菜单渲染逻辑。
export default {
    components: { TopBar, SideMenu },
    setup() {
        return { store };
    },
    template: `
    <el-container class="admin-shell">
        <el-header class="admin-topbar-outer" style="padding:0; flex-shrink:0;"><top-bar></top-bar></el-header>
        <el-container style="min-height:0; flex:1;">
            <el-aside v-if="!store.isMobile" width="220px" style="height:100%; overflow-y:auto;">
                <el-menu router :default-active="$route.path">
                    <side-menu v-for="node in store.menuTree" :key="node.id" :node="node"></side-menu>
                </el-menu>
            </el-aside>
            <el-drawer v-else v-model="store.mobileMenuVisible" direction="ltr" size="240px" :with-header="false" class="admin-mobile-drawer">
                <el-menu router :default-active="$route.path">
                    <side-menu v-for="node in store.menuTree" :key="node.id" :node="node"></side-menu>
                </el-menu>
            </el-drawer>
            <el-main style="height:100%; overflow-y:auto;"><router-view></router-view></el-main>
        </el-container>
    </el-container>
    `
};
