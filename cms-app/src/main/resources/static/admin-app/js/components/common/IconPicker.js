import { onMounted, onUnmounted, ref } from 'vue';

// 后台目录/菜单/按钮场景下常用的一批 FontAwesome 图标，供可视化选择；
// 输入框本身仍可直接手打其它 FontAwesome class，不局限于这份精选列表
const COMMON_ICONS = [
    'fas fa-home', 'fas fa-th-large', 'fas fa-list', 'fas fa-file', 'fas fa-file-alt',
    'fas fa-folder', 'fas fa-edit', 'fas fa-plus', 'fas fa-trash', 'fas fa-tags',
    'fas fa-tag', 'fas fa-comment', 'fas fa-comments', 'fas fa-users', 'fas fa-user',
    'fas fa-user-shield', 'fas fa-key', 'fas fa-lock', 'fas fa-shield-alt', 'fas fa-cog',
    'fas fa-cogs', 'fas fa-link', 'fas fa-image', 'fas fa-images', 'fas fa-paint-brush',
    'fas fa-palette', 'fas fa-globe', 'fas fa-server', 'fas fa-database', 'fas fa-chart-bar',
    'fas fa-chart-pie', 'fas fa-chess-queen', 'fas fa-book', 'fas fa-bookmark', 'fas fa-bell',
    'fas fa-envelope', 'fas fa-cloud-upload-alt', 'fas fa-cloud-download-alt', 'fas fa-search',
    'fas fa-star', 'fas fa-heart', 'fas fa-thumbtack', 'fas fa-eye', 'fas fa-clock',
    'fas fa-calendar', 'fas fa-map-marker-alt', 'fas fa-info-circle', 'fas fa-exclamation-circle',
    'fas fa-check-circle', 'fas fa-desktop', 'fas fa-mobile-alt', 'fas fa-wrench', 'fas fa-sitemap'
];

// 不用 el-popover：它依赖 Element Plus 内部的 Popper + 点击外部关闭机制，
// 曾经在对话框里反复打开关闭时触发过内部报错（点击图标没反应）。
// 这里用一个绝对定位的面板 + 手动挂在 document 上的 click 监听自己实现，
// 逻辑简单可控，组件卸载时能正确清理监听，不依赖任何第三方定位库
export default {
    props: {
        modelValue: { type: String, default: '' }
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
        const open = ref(false);
        const wrapperRef = ref(null);

        function toggle() { open.value = !open.value; }
        function choose(icon) {
            emit('update:modelValue', icon);
            open.value = false;
        }
        function onDocClick(e) {
            if (open.value && wrapperRef.value && !wrapperRef.value.contains(e.target)) {
                open.value = false;
            }
        }

        onMounted(() => document.addEventListener('click', onDocClick, true));
        onUnmounted(() => document.removeEventListener('click', onDocClick, true));

        return { COMMON_ICONS, open, wrapperRef, toggle, choose };
    },
    template: `
    <div ref="wrapperRef" style="position:relative; display:flex; gap:8px;">
        <el-input :model-value="modelValue" placeholder="填写 FontAwesome class，如 fas fa-user"
                  @update:model-value="$emit('update:modelValue', $event)">
            <template #prepend><i v-if="modelValue" :class="modelValue"></i><i v-else class="fas fa-question"></i></template>
        </el-input>
        <el-button style="flex-shrink:0;" @click.stop="toggle">选择图标</el-button>
        <div v-show="open" class="icon-picker-panel" @click.stop>
            <div class="icon-picker-grid">
                <div v-for="icon in COMMON_ICONS" :key="icon" :title="icon"
                     class="icon-picker-item" :class="{ 'is-active': modelValue === icon }"
                     @click="choose(icon)">
                    <i :class="icon"></i>
                </div>
            </div>
        </div>
    </div>
    `
};
