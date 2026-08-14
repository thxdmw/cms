import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm, store } from '../../store.js';

export default {
    setup() {
        const onlineUsers = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const username = ref('');
        const pageNumber = ref(1);
        const pageSize = ref(10);

        async function load() {
            loading.value = true;
            try {
                const res = await api.getOnlineUsers({ pageNumber: pageNumber.value, pageSize: pageSize.value, username: username.value || undefined });
                onlineUsers.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { username.value = ''; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        async function kickoutOne(row) {
            try {
                await ElMessageBox.confirm('确定踢除该用户？', '提示');
            } catch (e) { return; }
            const res = await api.kickoutUser(row.sessionId, row.username);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function kickoutBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要踢出的用户'); return; }
            try {
                await ElMessageBox.confirm(`确定踢出选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const sessions = selected.value.map((r) => ({ sessionId: r.sessionId, username: r.username }));
            const res = await api.batchKickoutUsers(sessions);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            store, onlineUsers, total, loading, username, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange,
            kickoutOne, kickoutBatch, hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="用户名">
                    <el-input v-model="username" placeholder="用户名" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('onlineUser:batchKickout')" type="danger" @click="kickoutBatch">批量踢出</el-button>
            </div>
            <el-table :data="onlineUsers" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="sessionId" label="会话id" align="center"></el-table-column>
                <el-table-column prop="username" label="用户名" align="center"></el-table-column>
                <el-table-column prop="host" label="主机地址" align="center"></el-table-column>
                <el-table-column label="最后访问时间" align="center">
                    <template #default="{row}">{{ row.lastAccess ? new Date(row.lastAccess).toLocaleString() : '' }}</template>
                </el-table-column>
                <el-table-column label="操作" align="center" :width="store.isMobile ? 70 : 120">
                    <template #default="{row}">
                        <template v-if="!store.isMobile">
                            <el-button v-if="hasPerm('onlineUser:kickout')" size="small" type="danger" @click="kickoutOne(row)">强制下线</el-button>
                        </template>
                        <el-dropdown v-else trigger="click">
                            <el-button size="small" text><i class="fas fa-ellipsis-vertical"></i></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-if="hasPerm('onlineUser:kickout')" @click="kickoutOne(row)">强制下线</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination
                style="margin-top:16px; justify-content:flex-end; display:flex;"
                layout="total, sizes, prev, pager, next, jumper"
                :total="total" :current-page="pageNumber" :page-size="pageSize"
                @current-change="onPageChange" @size-change="onSizeChange">
            </el-pagination>
        </el-card>
    </div>
    `
};
