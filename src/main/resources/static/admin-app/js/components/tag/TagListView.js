import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm } from '../../store.js';

export default {
    setup() {
        const tags = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const filters = reactive({ name: '', description: '' });
        const pageNumber = ref(1);
        const pageSize = ref(10);

        const dialogVisible = ref(false);
        const dialogTitle = ref('新增标签');
        const saving = ref(false);
        const form = reactive({ id: null, name: '', description: '' });

        async function load() {
            loading.value = true;
            try {
                const res = await api.getTagsPaged({
                    pageNumber: pageNumber.value,
                    pageSize: pageSize.value,
                    name: filters.name || undefined,
                    description: filters.description || undefined
                });
                tags.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { filters.name = ''; filters.description = ''; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        function openAdd() {
            form.id = null;
            form.name = '';
            form.description = '';
            dialogTitle.value = '新增标签';
            dialogVisible.value = true;
        }
        function openEdit(row) {
            form.id = row.id;
            form.name = row.name;
            form.description = row.description;
            dialogTitle.value = '编辑标签';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.name || !form.description) {
                ElMessage.warning('请完整填写标签信息');
                return;
            }
            saving.value = true;
            try {
                const res = form.id ? await api.editTag(form) : await api.addTag(form);
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
            const res = await api.deleteTag(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的标签'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteTags(selected.value.map((r) => r.id));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            tags, total, loading, filters, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange,
            dialogVisible, dialogTitle, saving, form,
            openAdd, openEdit, save, removeOne, removeBatch, hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="名称">
                    <el-input v-model="filters.name" placeholder="标签名称" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item label="描述">
                    <el-input v-model="filters.description" placeholder="标签描述" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('tag:add')" type="primary" @click="openAdd">新增</el-button>
                <el-button v-if="hasPerm('tag:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="tags" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="name" label="标签名称" align="center"></el-table-column>
                <el-table-column prop="description" label="标签描述" align="center"></el-table-column>
                <el-table-column label="操作" align="center" width="150">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('tag:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="hasPerm('tag:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
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

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
            <el-form label-width="90px">
                <el-form-item label="标签名称"><el-input v-model="form.name" placeholder="填写标签名称"></el-input></el-form-item>
                <el-form-item label="标签描述"><el-input v-model="form.description" placeholder="填写标签描述"></el-input></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
