import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api.js';
import { applyDocumentMeta, store } from '../store.js';
import ArticleCard from './ArticleCard.js';
import Pagination from './Pagination.js';

export default {
    components: { ArticleCard, Pagination },
    setup() {
        const route = useRoute();
        const router = useRouter();

        const articles = ref([]);
        const sliderList = ref([]);
        const currentPage = ref(1);
        const totalPage = ref(1);
        const categoryName = ref(null);
        const loading = ref(true);

        // 根据当前路由判断是首页 / 分类 / 标签 三种模式之一
        const mode = computed(() => {
            if (route.path.startsWith('/blog/category/')) return 'category';
            if (route.path.startsWith('/blog/tag/')) return 'tag';
            return 'index';
        });
        const isIndexFirstPage = computed(() => mode.value === 'index' && (!route.params.pageNumber || Number(route.params.pageNumber) === 1));

        function initSwiperIfNeeded() {
            nextTick(() => {
                if (window.jQuery && window.jQuery('.swiper-container').length && window.Swiper) {
                    new window.Swiper('.swiper-container', {
                        spaceBetween: 30,
                        centeredSlides: true,
                        loop: true,
                        autoplay: { delay: 2500, disableOnInteraction: false },
                        pagination: { el: '.swiper-pagination', clickable: true },
                        navigation: { nextEl: '.swiper-button-next', prevEl: '.swiper-button-prev' }
                    });
                }
            });
        }

        async function load() {
            loading.value = true;
            categoryName.value = null;
            sliderList.value = [];

            const params = { pageSize: 10 };
            const pageNumber = route.params.pageNumber ? Number(route.params.pageNumber) : 1;
            params.pageNumber = pageNumber;

            if (mode.value === 'category') {
                params.categoryId = route.params.categoryId;
                const catRes = await api.getCategory(route.params.categoryId);
                if (catRes.status === 200 && catRes.data) categoryName.value = catRes.data.name;
            } else if (mode.value === 'tag') {
                params.tagId = route.params.tagId;
            }

            const res = await api.getArticles(params);
            if (res.status === 200 && res.data) {
                articles.value = res.data.records || [];
                currentPage.value = res.data.current || pageNumber;
                totalPage.value = res.data.pages || 1;
            } else {
                articles.value = [];
            }

            if (isIndexFirstPage.value) {
                const sliderRes = await api.getSlider();
                if (sliderRes.status === 200) sliderList.value = sliderRes.data || [];
            }

            applyDocumentMeta({
                title: categoryName.value ? categoryName.value + ' - ' + store.siteConfig.SITE_NAME : store.siteConfig.SITE_NAME
            });

            loading.value = false;
            initSwiperIfNeeded();
        }

        function onPageChange(pageNo) {
            if (mode.value === 'category') router.push(`/blog/category/${route.params.categoryId}/${pageNo}`);
            else if (mode.value === 'tag') router.push(`/blog/tag/${route.params.tagId}/${pageNo}`);
            else router.push(`/blog/index/${pageNo}`);
        }

        onMounted(load);
        // 同一个 ArticleListView 组件在 分类A -> 分类B、页1 -> 页2 之间会被 Vue Router 复用，不会重新挂载，需要监听参数变化重新拉取数据
        watch(() => [route.path], load);

        const handleImageError = (event) => {
            // 图片加载失败时显示占位图
            event.target.src = '/img/slider-placeholder.svg';
            event.target.classList.add('img-error');
        };

        return { articles, sliderList, currentPage, totalPage, categoryName, loading, onPageChange, handleImageError };
    },
    template: `
    <div class="pb-main">
        <div class="swiper-container mb-20 hover-shadow" v-if="sliderList.length" style="border-radius: 12px;">
            <div class="swiper-wrapper">
                <div v-for="item in sliderList" :key="item.id" class="swiper-slide">
                    <router-link :to="'/blog/article/' + item.id">
                        <div class="slider-img-wrapper">
                            <img :src="item.sliderImg" :alt="item.title" @error="handleImageError">
                            <p class="slider-title">{{ item.title }}</p>
                        </div>
                    </router-link>
                </div>
            </div>
            <div class="swiper-pagination"></div>
            <div class="swiper-button-prev"><i class="fa fa-chevron-circle-left"></i></div>
            <div class="swiper-button-next"><i class="fa fa-chevron-circle-right"></i></div>
        </div>
        <div class="no-article-content hover-shadow" v-if="!loading && articles.length===0" style="border-radius: 12px;">
            Sorry, 暂未发现任何文章~
        </div>
        <article-card v-for="item in articles" :key="item.id" :item="item"></article-card>
        <pagination :current-page="currentPage" :total-page="totalPage" @change="onPageChange"></pagination>
    </div>
    `
};
