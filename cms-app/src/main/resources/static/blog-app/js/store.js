import { reactive } from 'vue';
import { api } from './api.js';

// 极简全局状态，不引入 Pinia：一次性加载的侧边栏 + 全局配置数据
export const store = reactive({
    loaded: false,
    categoryList: [],
    tagList: [],
    recentList: [],
    recommendedList: [],
    hotList: [],
    linkList: [],
    siteInfo: {},
    siteConfig: {}
});

export async function loadCommonData() {
    const res = await api.getCommonData();
    if (res.status === 200 && res.data) {
        const d = res.data;
        store.categoryList = d.CATEGORY_LIST || [];
        store.tagList = d.TAG_LIST || [];
        store.recentList = d.RECENT_LIST || [];
        store.recommendedList = d.RECOMMENDED_LIST || [];
        store.hotList = d.HOT_LIST || [];
        store.linkList = d.LINK_LIST || [];
        store.siteInfo = d.SITE_INFO || {};
        store.siteConfig = d.SITE_CONFIG || {};
    }
    store.loaded = true;
}

/** 应用站点标题/关键词/描述到 document（对应旧模板里的 <title>/<meta> 动态渲染） */
export function applyDocumentMeta({ title, keywords, description } = {}) {
    document.title = title || store.siteConfig.SITE_NAME || '';
    const kwdEl = document.getElementById('meta-keywords');
    const descEl = document.getElementById('meta-description');
    if (kwdEl) kwdEl.setAttribute('content', keywords || store.siteConfig.SITE_KWD || '');
    if (descEl) descEl.setAttribute('content', description || store.siteConfig.SITE_DESC || '');
}
