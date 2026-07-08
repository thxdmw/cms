import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm, store } from '../../store.js';

export default {
    setup() {
        const themes = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const pageNumber = ref(1);
        const pageSize = ref(10);

        const dialogVisible = ref(false);
        const dialogTitle = ref('新增主题');
        const saving = ref(false);
        const form = reactive({ id: null, name: '', description: '', img: '' });

        async function load() {
            loading.value = true;
            try {
                const res = await api.getThemesPaged({ pageNumber: pageNumber.value, pageSize: pageSize.value });
                themes.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        function openAdd() {
            form.id = null;
            form.name = '';
            form.description = '';
            form.img = '';
            dialogTitle.value = '新增主题';
            dialogVisible.value = true;
        }

        function openEdit(row) {
            form.id = row.id;
            form.name = row.name;
            form.description = row.description;
            form.img = row.img;
            dialogTitle.value = '编辑主题';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.name || !form.description || !form.img) {
                ElMessage.warning('请完整填写主题信息');
                return;
            }
            saving.value = true;
            try {
                const res = form.id ? await api.editTheme(form) : await api.addTheme(form);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    dialogVisible.value = false;
                    load();
                }
            } finally {
                saving.value = false;
            }
        }

        async function useTheme(row) {
            try {
                await ElMessageBox.confirm('确定启用该主题？', '提示');
            } catch (e) { return; }
            const res = await api.useTheme(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeOne(row) {
            try {
                await ElMessageBox.confirm('确定删除？', '提示');
            } catch (e) { return; }
            const res = await api.deleteTheme(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的主题'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteThemes(selected.value.map((r) => r.id));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            store, themes, total, loading, pageNumber, pageSize,
            onPageChange, onSizeChange, onSelectionChange,
            dialogVisible, dialogTitle, saving, form,
            openAdd, openEdit, save, useTheme, removeOne, removeBatch, hasPerm
        };
    },
    template: `
    <div>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('theme:add')" type="primary" @click="openAdd">新增</el-button>
                <el-button v-if="hasPerm('theme:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="themes" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="name" label="主题名称" align="center"></el-table-column>
                <el-table-column prop="description" label="主题描述" align="center"></el-table-column>
                <el-table-column label="主题预览" align="center" width="130">
                    <template #default="{row}">
                        <el-image v-if="row.img" :src="row.img" :preview-src-list="[row.img]" preview-teleported style="width:100px;height:60px" fit="cover"></el-image>
                        <span v-else>无</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" align="center" width="90">
                    <template #default="{row}">{{ row.status ? '当前使用' : '未启用' }}</template>
                </el-table-column>
                <el-table-column label="操作" align="center" :width="store.isMobile ? 70 : 220">
                    <template #default="{row}">
                        <template v-if="!store.isMobile">
                            <el-button v-if="hasPerm('theme:use') && row.status === 0" size="small" type="danger" @click="useTheme(row)">启用</el-button>
                            <el-button v-if="hasPerm('theme:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button v-if="hasPerm('theme:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
                        </template>
                        <el-dropdown v-else trigger="click">
                            <el-button size="small" text><i class="fas fa-ellipsis-vertical"></i></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-if="hasPerm('theme:use') && row.status === 0" @click="useTheme(row)">启用</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('theme:edit')" @click="openEdit(row)">编辑</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('theme:delete')" @click="removeOne(row)" divided>删除</el-dropdown-item>
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

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
            <el-form label-width="90px">
                <el-form-item label="主题名称"><el-input v-model="form.name" placeholder="填写主题名称"></el-input></el-form-item>
                <el-form-item label="主题描述"><el-input v-model="form.description" placeholder="填写主题描述"></el-input></el-form-item>
                <el-form-item label="主题图片"><el-input v-model="form.img" placeholder="填写主题图片地址"></el-input></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
