import { store } from '../../store.js';

// Druid 自带的监控页面（/druid/sql.html）是第三方页面，不是本项目的 Vue 组件，
// 页面本身没有做过响应式适配，在手机小屏里嵌一个桌面向的监控台可用性很差，
// 不强行在移动端塞 iframe，改成提示引导去桌面端查看，比强行展示一个操作不了的页面更实际。
export default {
    setup() {
        return { store };
    },
    template: `
    <div style="height:calc(100vh - 100px);">
        <el-empty v-if="store.isMobile" description="数据库监控页面暂不支持移动端，请在电脑浏览器中打开查看">
        </el-empty>
        <iframe v-else src="/druid/sql.html" style="width:100%; height:100%; border:none;"></iframe>
    </div>
    `
};
