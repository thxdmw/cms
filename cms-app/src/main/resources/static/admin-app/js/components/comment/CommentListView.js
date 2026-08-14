import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm, store } from '../../store.js';

export default {
    setup() {
        const comments = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const status = ref(null);
        const pageNumber = ref(1);
        const pageSize = ref(10);

        const dialogVisible = ref(false);
        const saving = ref(false);
        const auditForm = reactive({ id: null, sid: null, status: 1, replyContent: '' });

        async function load() {
            loading.value = true;
            try {
                const res = await api.getComments({ pageNumber: pageNumber.value, pageSize: pageSize.value, status: status.value });
                comments.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { status.value = null; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        function statusText(v) {
            if (v === 0) return '待审核';
            if (v === 1) return '审核通过';
            if (v === 2) return '审核失败';
            return '';
        }

        function openAudit(row) {
            auditForm.id = row.id;
            auditForm.sid = row.sid;
            auditForm.status = 1;
            auditForm.replyContent = '';
            dialogVisible.value = true;
        }

        async function saveAudit() {
            saving.value = true;
            try {
                const res = await api.auditComment({ id: auditForm.id, sid: auditForm.sid, status: auditForm.status, replyContent: auditForm.replyContent });
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    dialogVisible.value = false;
                    load();
                }
            } finally {
                saving.value = false;
            }
        }

        async function removeOne(row) {
            try {
                await ElMessageBox.confirm('确定删除？', '提示');
            } catch (e) { return; }
            const res = await api.deleteComment(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的评论'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteComments(selected.value.map((r) => r.id));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            store, comments, total, loading, status, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange, statusText,
            dialogVisible, saving, auditForm, openAudit, saveAudit, removeOne, removeBatch, hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="状态">
                    <el-select v-model="status" placeholder="选择状态" clearable style="width:140px">
                        <el-option label="待审核" :value="0"></el-option>
                        <el-option label="审核通过" :value="1"></el-option>
                        <el-option label="审核失败" :value="2"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('comment:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="comments" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="nickname" label="作者" align="center" width="110"></el-table-column>
                <el-table-column prop="qq" label="QQ" align="center" width="100"></el-table-column>
                <el-table-column prop="email" label="邮箱" align="center"></el-table-column>
                <el-table-column prop="ip" label="IP" align="center" width="120"></el-table-column>
                <el-table-column label="文章标题/留言板" align="center" width="150">
                    <template #default="{row}">
                        <span v-if="row.sid === -1" style="color:red">留言板</span>
                        <span v-else style="color:#2c93fd">{{ row.article && row.article.title && row.article.title.length > 15 ? row.article.title.slice(0,15) + '...' : (row.article ? row.article.title : '') }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="内容" align="left" width="220">
                    <template #default="{row}">
                        <div>{{ row.content }}</div>
                        <div v-if="row.parent" class="comment-parent" style="color:#909399; font-size:12px; margin-top:4px;">原评：{{ row.parent.content }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="赞/踩" align="center" width="90">
                    <template #default="{row}">{{ row.loveCount }}/{{ row.oppose }}</template>
                </el-table-column>
                <el-table-column label="状态" align="center" width="90">
                    <template #default="{row}">{{ statusText(row.status) }}</template>
                </el-table-column>
                <el-table-column label="操作" align="center" :width="store.isMobile ? 70 : 150">
                    <template #default="{row}">
                        <template v-if="!store.isMobile">
                            <el-button v-if="hasPerm('comment:audit') && row.status === 0" size="small" type="primary" @click="openAudit(row)">审核</el-button>
                            <el-button v-if="hasPerm('comment:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
                        </template>
                        <el-dropdown v-else trigger="click">
                            <el-button size="small" text><i class="fas fa-ellipsis-vertical"></i></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-if="hasPerm('comment:audit') && row.status === 0" @click="openAudit(row)">审核</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('comment:delete')" @click="removeOne(row)" divided>删除</el-dropdown-item>
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

        <el-dialog v-model="dialogVisible" title="审核评论" width="500px">
            <el-form label-width="90px">
                <el-form-item label="审核结果">
                    <el-radio-group v-model="auditForm.status">
                        <el-radio :value="1">审核通过</el-radio>
                        <el-radio :value="2">审核失败</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="回复内容">
                    <el-input v-model="auditForm.replyContent" type="textarea" :rows="4" placeholder="选填，填写后会作为回复发布"></el-input>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveAudit">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
