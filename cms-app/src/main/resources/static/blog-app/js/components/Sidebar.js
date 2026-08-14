import { nextTick, ref } from 'vue';
import { store } from '../store.js';

export default {
    setup() {
        const activeTab = ref('latest'); // latest | recommend | hot
        const avatarZoomed = ref(false);
        const tabContentEl = ref(null);
        const tabContentHeight = ref(null);

        // 原 static/js/sidebar.js 里的高度动画：切换 tab 时把目标面板的实际高度显式设置到
        // .tab-content 上，配合已有 CSS 的 transition: height 0.3s ease 做平滑展开/折叠。
        // 用 ResizeObserver 持续跟踪当前激活面板的真实尺寸，而不是只在切换那一刻测一次，
        // 避免测量到偏小的高度导致内容被 .tab-content 的 overflow:hidden 裁掉
        let paneResizeObserver = null;

        function observeActivePane() {
            if (paneResizeObserver) paneResizeObserver.disconnect();
            if (!tabContentEl.value || !window.ResizeObserver) return;
            const activePane = tabContentEl.value.querySelector('.tab-pane.active');
            if (!activePane) return;
            paneResizeObserver = new ResizeObserver(() => {
                tabContentHeight.value = activePane.scrollHeight;
            });
            paneResizeObserver.observe(activePane);
        }

        function selectTab(tab) {
            activeTab.value = tab;
            nextTick(observeActivePane);
        }
        function zoomAvatar() {
            avatarZoomed.value = true;
        }
        function closeZoom() {
            avatarZoomed.value = false;
        }

        nextTick(observeActivePane);

        // 侧边栏跟随滚动固定：原来用 hc-sticky.js（命令式库，为整页刷新设计，在 SPA 里跨路由
        // 内容高度变化时几何计算会跟不上，多次出过 bug），现在改用 CSS 原生 position: sticky
        // （见 index.html 里 .pb-content::after 的 clearfix + .pb-sidebar 的 sticky 规则），
        // 不再需要任何 JS 生命周期管理

        return { activeTab, selectTab, avatarZoomed, zoomAvatar, closeZoom, tabContentEl, tabContentHeight, store };
    },
    template: `
    <div class="pb-sidebar">
        <div class="about panel panel-default hover-shadow" style="border-radius: 12px;">
            <div class="panel-heading"><h3 class="panel-title">关于本站</h3></div>
            <div class="panel-body">
                <ul>
                    <div class="avatar">
                        <img :src="store.siteConfig.SITE_PERSON_PIC || '/img/avatar.jpg'" alt="站长头像"
                             class="avatar-img" @click="zoomAvatar" style="cursor:pointer">
                    </div>
                    <p class="abname">{{ store.siteConfig.SITE_PERSON_NAME }}</p>
                    <p class="abtext">{{ store.siteConfig.SITE_PERSON_DESC }}</p>
                </ul>
            </div>
        </div>
        <div id="imageModal" class="modal" :style="{display: avatarZoomed ? 'block' : 'none'}" @click="closeZoom">
            <span class="close" @click="closeZoom">&times;</span>
            <img class="modal-content" :src="store.siteConfig.SITE_PERSON_PIC || '/img/avatar.jpg'">
        </div>
        <div class="tag panel panel-default hover-shadow" style="border-radius: 12px;">
            <div class="panel-heading"><h3 class="panel-title">标签云</h3></div>
            <div class="panel-body" id="tagcloud" style="overflow: hidden">
                <router-link v-for="item in store.tagList" :key="item.id" class="btn btn-default btn-xs"
                             :to="'/blog/tag/' + item.id">{{ item.name }}</router-link>
            </div>
        </div>
        <div class="pb-sidebar-tabs hover-shadow" style="border-radius: 12px;">
            <ul class="nav nav-tabs" style="border-top-left-radius: 12px; border-top-right-radius: 12px;">
                <li :class="activeTab==='latest' ? 'active' : ''"><a href="javascript:void(0)" @click="selectTab('latest')" style="border-top-left-radius: 12px;">最新文章</a></li>
                <li :class="activeTab==='recommend' ? 'active' : ''"><a href="javascript:void(0)" @click="selectTab('recommend')">站长推荐</a></li>
                <li :class="activeTab==='hot' ? 'active' : ''"><a href="javascript:void(0)" @click="selectTab('hot')" style="border-top-right-radius: 12px;">点击排行</a></li>
            </ul>
            <div class="tab-content" ref="tabContentEl" :style="tabContentHeight ? {height: tabContentHeight + 'px'} : {}">
                <div class="tab-pane" :class="activeTab==='latest' ? 'in active' : 'fade'">
                    <ol class="article-list">
                        <li v-for="(item, idx) in store.recentList" :key="item.id" class="slide">
                            <span :class="'li-icon li-icon-' + (idx+1)">{{ idx+1 }}</span>
                            <router-link :to="'/blog/article/' + item.id">{{ item.title }}</router-link>
                        </li>
                    </ol>
                </div>
                <div class="tab-pane" :class="activeTab==='recommend' ? 'in active' : 'fade'">
                    <ol class="article-list">
                        <li v-for="(item, idx) in store.recommendedList" :key="item.id" class="slide">
                            <span :class="'li-icon li-icon-' + (idx+1)">{{ idx+1 }}</span>
                            <router-link :to="'/blog/article/' + item.id">{{ item.title }}</router-link>
                        </li>
                    </ol>
                </div>
                <div class="tab-pane" :class="activeTab==='hot' ? 'in active' : 'fade'">
                    <ol class="article-list">
                        <li v-for="(item, idx) in store.hotList" :key="item.id" class="slide">
                            <span :class="'li-icon li-icon-' + (idx+1)">{{ idx+1 }}</span>
                            <router-link :to="'/blog/article/' + item.id">{{ item.title }}</router-link>
                        </li>
                    </ol>
                </div>
            </div>
        </div>
        <div class="link panel panel-default hover-shadow" style="border-radius: 12px;">
            <div class="panel-heading"><h3 class="panel-title">友情链接</h3></div>
            <div class="panel-body">
                <ul>
                    <li v-for="item in store.linkList" :key="item.id"><a :href="item.url" target="_blank">{{ item.name }}</a></li>
                </ul>
            </div>
        </div>
        <div class="webinfo panel panel-default hover-shadow" style="border-radius: 12px;">
            <div class="panel-heading"><h3 class="panel-title">网站信息</h3></div>
            <div class="panel-body">
                <ul>
                    <li><i class="fa fa-file fa-fw"></i> 文章总数：<span>{{ store.siteInfo.articleCount }}</span> 篇</li>
                    <li><i class="fa fa-tags fa-fw"></i> 标签总数：<span>{{ store.siteInfo.tagCount }}</span> 个</li>
                    <li><i class="fa fa-folder-open fa-fw"></i> 分类总数：<span>{{ store.siteInfo.categoryCount }}</span> 个</li>
                    <li><i class="fa fa-comments fa-fw"></i> 留言数量：<span>{{ store.siteInfo.commentCount }}</span> 条</li>
                </ul>
            </div>
        </div>
    </div>
    `
};
