import { inject, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api.js';
import { applyDocumentMeta, store } from '../store.js';
import { shortDate } from '../format.js';
import CommentSection from './CommentSection.js';

export default {
    components: { CommentSection },
    setup() {
        const route = useRoute();
        const article = ref(null);
        const loveCount = ref(0);
        // 目录组件实际渲染在 App.js 里（必须和原模板一样是 .pb-container 的同级，见 App.js 里的注释），
        // 这里通过 provide/inject 拿到它的实例引用，在文章内容渲染完后调用它的 build()
        const toc = inject('tocRef');
        const notFound = ref(false);

        function addCopyButtons() {
            if (!window.jQuery) return;
            const $ = window.jQuery;
            $('#editor-md-preview pre').each(function () {
                const container = $("<div class='code-container'></div>");
                $(this).wrap(container);
                $(this).parent().prepend("<button class='copy-btn'>复制</button>");
            });
            $('.copy-btn').off('click').on('click', function () {
                const pre = this.nextElementSibling;
                const text = pre ? pre.innerText : '';
                navigator.clipboard.writeText(text).then(() => {
                    if (window.layer) window.layer.msg('复制成功！', { icon: 1, time: 1000 });
                }).catch(() => {
                    if (window.layer) window.layer.msg('复制失败，请手动复制', { icon: 2, time: 1500 });
                });
            });
        }

        async function renderMarkdown() {
            await nextTick();
            const el = document.getElementById('editor-md-preview');
            if (el) el.innerHTML = ''; // 同一容器可能被复用（同类型路由切换），先清空避免内容叠加
            if (window.editormd && article.value) {
                window.editormd.markdownToHTML('editor-md-preview', {
                    markdown: article.value.contentMd || '',
                    htmlDecode: 'style,script,iframe',
                    emoji: true,
                    taskList: true,
                    tex: true,
                    hex: true,
                    flowChart: true,
                    sequenceDiagram: true,
                    codeFold: true,
                    toc: false,
                    codeLineWrapping: false
                });
            }
            await nextTick();
            addCopyButtons();
            if (toc.value) {
                toc.value.build();
                toc.value.setupMobileToggle();
            }
        }

        async function load() {
            notFound.value = false;
            const id = route.params.articleId;
            const res = await api.getArticle(id);
            if (res.status === 200 && res.data) {
                article.value = res.data;
                loveCount.value = res.data.loveCount || 0;
                applyDocumentMeta({
                    title: article.value.title + ' - ' + store.siteConfig.SITE_NAME,
                    keywords: article.value.keywords,
                    description: article.value.description
                });
                api.articleLook(id);
                await renderMarkdown();
            } else {
                article.value = null;
                notFound.value = true;
            }
        }

        async function like() {
            const res = await api.love(route.params.articleId, 1);
            if (res.status === 200) loveCount.value++;
        }

        onMounted(load);
        // 从一篇文章跳到另一篇文章时组件会被复用，需要重新加载
        watch(() => route.params.articleId, load);

        return { article, loveCount, shortDate, like, notFound };
    },
    template: `
    <div class="pb-main">
        <div v-if="notFound" class="no-article-content hover-shadow" style="border-radius: 12px;">文章不存在或已删除</div>
        <div v-if="article" class="article-main hover-shadow" style="border-radius: 12px;">
            <div class="article-toc-toggle" id="tocToggleBtn">目录 ☰</div>
            <h3 class="article-title">{{ article.title }}</h3>
            <span class="article-original">{{ article.original===1 ? '原创' : '转载' }}</span>
            <div class="article-meta">
                发布于 <span>{{ shortDate(article.createTime) }}</span>
                &nbsp;|&nbsp; <span>{{ article.bizCategory ? article.bizCategory.name : '' }}</span>
                &nbsp;|&nbsp; 浏览（<span>{{ article.lookCount || 0 }}</span>）
                &nbsp;|&nbsp; 评论（<span>{{ article.commentCount || 0 }}</span>）
            </div>
            <hr class="hr0"/>
            <div id="editor-md-preview" class="article-body markdown-body"></div>
            <div class="thumbs-content">
                <span class="thumbs-button fa fa-thumbs-up" style="line-height: 3" @click="like"> 点赞 <span id="loveCount">{{ loveCount }}</span></span>
                <span class="reward-button fa fa-money" style="line-height: 3"> 打赏
                    <p class="reward-content">
                        <span class="reward-img"><img src="/blog-app/libs/theme/pblog/img/weixin.png"><br>微信赞赏</span>
                        <span class="reward-img"><img src="/blog-app/libs/theme/pblog/img/zhifubao.png"><br>支付宝赞赏</span>
                    </p>
                    <span class="reward-bottom"></span>
                    <span class="reward-bottom-top"></span>
                </span>
            </div>
        </div>
        <comment-section v-if="article && article.comment===1" :sid="article.id"></comment-section>
    </div>
    `
};
