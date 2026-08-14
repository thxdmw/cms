import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api.js';

// 对应原来 search-box.js 里的 openSearchModal/closeSearchModal/doSearch，改写成 Vue 响应式逻辑
export default {
    setup() {
        const router = useRouter();
        const visible = ref(false);
        const keyword = ref('');
        const results = ref(null); // null=未搜索，[]=无结果，[...]=结果列表
        const searching = ref(false);
        const errorMsg = ref('');

        function open() {
            visible.value = true;
            keyword.value = '';
            results.value = null;
            errorMsg.value = '';
        }

        function close() {
            visible.value = false;
        }

        async function doSearch() {
            const kw = keyword.value.trim();
            if (!kw) {
                errorMsg.value = '请输入搜索关键词';
                return;
            }
            errorMsg.value = '';
            searching.value = true;
            try {
                const res = await api.search(kw);
                if (res.status !== 200) {
                    errorMsg.value = '搜索失败: ' + res.msg;
                    results.value = [];
                } else {
                    results.value = res.data || [];
                }
            } catch (e) {
                errorMsg.value = '搜索请求失败，请稍后再试';
                results.value = [];
            } finally {
                searching.value = false;
            }
        }

        function goTo(item) {
            close();
            // skipUrl 形如 "blog/article/123"
            const path = '/' + String(item.skipUrl).replace(/^\/+/, '');
            router.push(path);
        }

        function onKeydown(e) {
            if (e.key === 'Escape') close();
        }

        return { visible, keyword, results, searching, errorMsg, open, close, doSearch, goTo, onKeydown };
    },
    template: `
    <div id="searchModal"
         :style="{display: visible ? 'block' : 'none', position:'fixed', top:0, left:0, width:'100vw', height:'100vh', background:'rgba(0,0,0,0.5)', zIndex:9999}"
         @keydown="onKeydown">
        <div style="position:relative; width:100%; max-width:700px; margin:40px auto 0; padding:20px; background:#fff; border-radius:8px;" class="hover-shadow">
            <button aria-label="关闭" @click="close"
                    style="position: absolute;top: 0px;right: 0px;background: none;border: none;color: rgb(153, 153, 153);font-size: 20px;font-weight: bold;cursor: pointer;">
                &times;
            </button>
            <form @submit.prevent="doSearch" style="margin-bottom:10px;">
                <input v-model="keyword" type="text" placeholder="输入关键词搜索文章..." autocomplete="off"
                       style="width:100%; padding:14px 18px; font-size:18px; border:1px solid #ccc; border-radius:5px;">
                <button type="submit"
                        style="margin-top:10px; width:100%; padding:12px; font-size:18px; background:#337ab7; color:#fff; border:none; border-radius:5px;">
                    搜索
                </button>
            </form>
            <div style="max-height:400px; overflow-y:auto; border-top:1px solid #eee; padding-top:10px;">
                <p v-if="searching">搜索中...</p>
                <p v-else-if="errorMsg" style="color:red;">{{ errorMsg }}</p>
                <p v-else-if="results && results.length === 0" style="color:#999;">没有找到相关文章</p>
                <ul v-else-if="results && results.length" style="list-style:none; padding-left:0;">
                    <li v-for="item in results" :key="item.id" style="padding:8px 0; border-bottom:1px solid #eee;">
                        <a href="javascript:void(0)" @click="goTo(item)" style="color:#337ab7; text-decoration:none; font-size:16px;">
                            {{ item.title }}
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    `
};
