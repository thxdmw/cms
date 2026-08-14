import { onMounted } from 'vue';
import { store, loadCurrentUser } from '../store.js';

export default {
    setup() {
        onMounted(async () => {
            await loadCurrentUser();
        });
        return { store };
    },
    template: `
    <router-view v-if="store.ready"></router-view>
    <div v-else class="admin-boot-loading">加载中...</div>
    `
};
