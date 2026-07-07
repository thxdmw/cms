import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { store } from '../store.js';
import SearchModal from './SearchModal.js';

export default {
    components: { SearchModal },
    setup() {
        const route = useRoute();
        const searchModal = ref(null);

        const isIndex = computed(() => route.path === '/' || route.path.startsWith('/blog/index'));
        const isComment = computed(() => route.path === '/blog/comment');
        const currentCategoryId = computed(() => route.params.categoryId ? String(route.params.categoryId) : null);

        // 只展示顶层分类（pid 为空或 '0'），子分类放进 dropdown
        // BizCategory.pid 现在是 UUID 字符串，顶层分类用字符串 '0' 表示，不能再用数字 0 比较
        // （字符串 '0' 在 JS 里是 truthy，!c.pid 和 c.pid === 0 都会判定为 false）
        const topCategories = computed(() =>
            (store.categoryList || []).filter((c) => !c.pid || c.pid === '0')
        );

        function categoryActiveClass(category) {
            if (String(category.id) === currentCategoryId.value) return 'active';
            return category.children && category.children.length ? 'dropdown' : '';
        }

        function openSearch() {
            searchModal.value && searchModal.value.open();
        }

        // 原来靠 Bootstrap 的 data-toggle="collapse"/"dropdown" + bootstrap.min.js 驱动，
        // 现在用两个 ref 原生实现，绑定的 in/open 类名沿用 bootstrap.min.css 已有的样式约定，
        // 视觉和交互（含点击外部关闭下拉，原来是 Bootstrap JS 自带的行为）保持一致
        const mobileMenuOpen = ref(false);
        const openDropdownId = ref(null);

        function toggleMobileMenu() {
            mobileMenuOpen.value = !mobileMenuOpen.value;
        }
        function toggleDropdown(categoryId) {
            openDropdownId.value = openDropdownId.value === categoryId ? null : categoryId;
        }
        function closeDropdown() {
            openDropdownId.value = null;
        }

        function onDocumentClick(e) {
            if (!e.target.closest('.dropdown-toggle, .dropdown-menu')) {
                closeDropdown();
            }
        }
        onMounted(() => document.addEventListener('click', onDocumentClick));
        onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick));

        return {
            isIndex, isComment, topCategories, categoryActiveClass, openSearch, searchModal, store,
            mobileMenuOpen, toggleMobileMenu, openDropdownId, toggleDropdown, closeDropdown
        };
    },
    template: `
    <nav id="navbar" class="navbar navbar-default">
        <div class="pb-container">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" @click="toggleMobileMenu">
                    <span class="sr-only">导航</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="logo-a" href="/">
                    <img :src="store.siteConfig.SITE_LOGO || '/img/logo.ico'" alt="logo"/>
                </a>
                <a class="logo-a" href="https://tools.thxdxw.cn" target="_blank">
                    <img src="https://tools.thxdxw.cn/favicon.ico" alt="logo" title="工具合集"/>
                </a>
            </div>
            <div class="collapse navbar-collapse navbar-right" :class="{ in: mobileMenuOpen }" aria-expanded="false">
                <ul class="nav navbar-nav">
                    <li :class="isIndex ? 'active' : ''">
                        <router-link to="/">首页</router-link>
                    </li>
                    <li v-for="category in topCategories" :key="category.id"
                        :class="[categoryActiveClass(category), { open: openDropdownId === category.id }]">
                        <router-link v-if="!(category.children && category.children.length)"
                                     :to="'/blog/category/' + category.id">
                            <span>{{ category.name }}</span>
                        </router-link>
                        <a v-else href="javascript:void(0)" class="dropdown-toggle" @click="toggleDropdown(category.id)">
                            <span>{{ category.name }}</span>
                            <b class="caret"></b>
                        </a>
                        <ul v-if="category.children && category.children.length" class="dropdown-menu">
                            <li v-for="node in category.children" :key="node.id">
                                <router-link :to="'/blog/category/' + node.id" @click="closeDropdown">{{ node.name }}</router-link>
                            </li>
                        </ul>
                    </li>
                    <li>
                        <router-link to="/blog/comment" :class="isComment ? 'active' : ''">留言板</router-link>
                    </li>
                    <li>
                        <a href="javascript:void(0);" @click="openSearch"><i class="fa fa-search search-btn"></i></a>
                    </li>
                </ul>
            </div>
        </div>
        <search-modal ref="searchModal"></search-modal>
    </nav>
    `
};
