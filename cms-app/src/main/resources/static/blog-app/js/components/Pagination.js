import { computed } from 'vue';

// 原生 Vue 实现，替代 jquery.pagination.min.js（算法照抄该插件源码里的窗口计算逻辑，
// CSS 类名沿用 jquery.pagination.css 里的 .ui-pagination-container/.ui-pagination-page-item，
// 视觉与原来完全一致，只是不再需要 jQuery 插件本身）
const COUNT = 5; // 可见页码数，和原插件默认值一致

export default {
    props: {
        currentPage: { type: Number, required: true },
        totalPage: { type: Number, required: true }
    },
    emits: ['change'],
    setup(props, { emit }) {
        const pageWindow = computed(() => {
            const total = props.totalPage;
            const current = props.currentPage;
            if (total <= COUNT) {
                return Array.from({ length: total }, (_, i) => i + 1);
            }
            const half = Math.floor(COUNT / 2);
            let low, high;
            if (current <= COUNT / 2) {
                low = 1;
                high = COUNT;
            } else {
                low = current - half;
                high = current + half;
                if (high > total) {
                    low -= (high - total);
                    high = total;
                }
            }
            return Array.from({ length: high - low + 1 }, (_, i) => low + i);
        });

        function go(page) {
            if (page === props.currentPage) return;
            emit('change', page);
        }

        return { pageWindow, go };
    },
    template: `
    <div v-show="totalPage > 1" id="pagebar">
        <div id="pagination" class="page">
            <div class="ui-pagination-container">
                <a v-if="currentPage > 1" href="javascript:void(0);" class="ui-pagination-page-item" @click="go(1)">首页</a>
                <a v-if="currentPage > 1" href="javascript:void(0);" class="ui-pagination-page-item" @click="go(currentPage - 1)">上一页</a>
                <a v-for="p in pageWindow" :key="p" href="javascript:void(0);"
                   class="ui-pagination-page-item" :class="{ active: p === currentPage }"
                   @click="go(p)">{{ p }}</a>
                <a v-if="currentPage < totalPage" href="javascript:void(0);" class="ui-pagination-page-item" @click="go(currentPage + 1)">下一页</a>
                <a v-if="currentPage < totalPage" href="javascript:void(0);" class="ui-pagination-page-item" @click="go(totalPage)">尾页</a>
            </div>
        </div>
    </div>
    `
};
