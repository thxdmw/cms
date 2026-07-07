import { store } from '../../store.js';
import TopBar from './TopBar.js';
import SideMenu from './SideMenu.js';

export default {
    components: { TopBar, SideMenu },
    setup() {
        return { store };
    },
    template: `
    <el-container class="admin-shell">
        <el-header class="admin-topbar-outer" style="padding:0; flex-shrink:0;"><top-bar></top-bar></el-header>
        <el-container style="min-height:0; flex:1;">
            <el-aside width="220px" style="height:100%; overflow-y:auto;">
                <el-menu router :default-active="$route.path">
                    <side-menu v-for="node in store.menuTree" :key="node.id" :node="node"></side-menu>
                </el-menu>
            </el-aside>
            <el-main style="height:100%; overflow-y:auto;"><router-view></router-view></el-main>
        </el-container>
    </el-container>
    `
};
