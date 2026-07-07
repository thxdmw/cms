import { computed } from 'vue';
import { store } from '../store.js';

// 原模板只用到 footer :: copyrightLight 这个 fragment，其余 fragment（footer/copyrightDark）没有被引用，不迁移
export default {
    setup() {
        const year = computed(() => new Date().getFullYear());
        return { year, store };
    },
    template: `
    <div class="copyright-light copyright-section">
        <p>Copyright © {{ year }}. {{ store.siteConfig.SITE_NAME }} · Powered by <a href="https://cms.thxdxw.cn" target="_blank" title="CMS是一款精简的自适应内容管理系统...">CMS</a></p>
    </div>
    `
};
