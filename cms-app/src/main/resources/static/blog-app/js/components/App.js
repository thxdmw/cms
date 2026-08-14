import { computed, onMounted, provide, ref } from 'vue';
import { useRoute } from 'vue-router';
import { store, loadCommonData } from '../store.js';
import Navbar from './Navbar.js';
import Sidebar from './Sidebar.js';
import Footer from './Footer.js';
import TableOfContents from './TableOfContents.js';

export default {
    components: { Navbar, Sidebar, Footer, TableOfContents },
    setup() {
        const route = useRoute();
        // 目录容器必须和原模板一样是 #content-with-bg 的直接子元素（.pb-container 的同级），
        // 而不是嵌套在 ArticleDetailView 内部：.article-toc-container 是 position:fixed 但没写 top，
        // 垂直位置由“静态位置”决定，嵌套层级一变多出来的 margin-top 就会导致位置整体偏移。
        // 用 provide/inject 把 TOC 实例暴露给 ArticleDetailView，供它在文章内容渲染完后调用 build()。
        const showToc = computed(() => route.path.startsWith('/blog/article/'));
        const tocRef = ref(null);
        provide('tocRef', tocRef);

        onMounted(async () => {
            await loadCommonData();

            // 顶部导航滚动隐藏/显示（原 pb-blog.js window.onload 里的逻辑）
            const header = document.getElementById('header');
            let lastScrollPosition = 0;
            window.addEventListener('scroll', () => {
                const current = window.scrollY;
                if (current > lastScrollPosition && current > 55) {
                    header.classList.remove('slideDown');
                    header.classList.add('slideUp');
                } else if (current < lastScrollPosition) {
                    header.classList.remove('slideUp');
                    header.classList.add('slideDown');
                }
                lastScrollPosition = current;

                // 返回顶部按钮淡入淡出（原 pb-blog.js $(window).scroll 里的逻辑）
                if (window.jQuery) {
                    if (current > 100) window.jQuery('.return_top').fadeIn(500);
                    else window.jQuery('.return_top').fadeOut(500);
                }
            });
        });

        function scrollToTop() {
            if (window.jQuery) {
                window.jQuery('html,body').animate({ scrollTop: 0 }, 'fast');
            } else {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        }

        return { store, scrollToTop, showToc, tocRef };
    },
    template: `
    <div v-if="store.loaded">
        <header id="header">
            <Navbar />
        </header>
        <div id="content-with-bg" class="content-with-bg">
            <table-of-contents ref="tocRef" v-if="showToc"></table-of-contents>
            <div class="pb-container pb-content">
                <router-view></router-view>
                <Sidebar />
            </div>
        </div>
        <Footer />
        <div class="return_top" style="display:none"><a href="javascript:void(0)" class="fa fa-arrow-up" @click="scrollToTop"></a></div>
    </div>
    `
};
