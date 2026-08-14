import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm, store } from '../../store.js';

export default {
    setup() {
        const links = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const filters = reactive({ name: '', url: '', status: null });
        const pageNumber = ref(1);
        const pageSize = ref(10);

        const dialogVisible = ref(false);
        const dialogTitle = ref('新增友链');
        const saving = ref(false);
        const form = reactive({ id: null, origin: 1, name: '', url: '', description: '', img: '', email: '', qq: '', remark: '', status: 1 });

        async function load() {
            loading.value = true;
            try {
                const res = await api.getLinksPaged({
                    pageNumber: pageNumber.value,
                    pageSize: pageSize.value,
                    name: filters.name || undefined,
                    url: filters.url || undefined,
                    status: filters.status
                });
                links.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { filters.name = ''; filters.url = ''; filters.status = null; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        function openAdd() {
            form.id = null;
            form.origin = 1;
            form.name = '';
            form.url = '';
            form.description = '';
            form.img = '';
            form.email = '';
            form.qq = '';
            form.remark = '';
            form.status = 1;
            dialogTitle.value = '新增友链';
            dialogVisible.value = true;
        }

        function openEdit(row) {
            form.id = row.id;
            form.origin = row.origin;
            form.name = row.name;
            form.url = row.url;
            form.description = row.description;
            form.img = row.img;
            form.email = row.email;
            form.qq = row.qq;
            form.remark = row.remark;
            form.status = row.status;
            dialogTitle.value = '编辑友链';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.name || !form.url || !form.description || !form.img) {
                ElMessage.warning('请完整填写站点名称/链接/描述/站长图片');
                return;
            }
            saving.value = true;
            try {
                const res = form.id ? await api.editLink(form) : await api.addLink(form);
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
            const res = await api.deleteLink(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的友链'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteLinks(selected.value.map((r) => r.id));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            store, links, total, loading, filters, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange,
            dialogVisible, dialogTitle, saving, form,
            openAdd, openEdit, save, removeOne, removeBatch, hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="站点名称">
                    <el-input v-model="filters.name" placeholder="站点名称" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item label="站点链接">
                    <el-input v-model="filters.url" placeholder="站点链接" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filters.status" placeholder="选择状态" clearable style="width:120px">
                        <el-option label="启用" :value="1"></el-option>
                        <el-option label="禁用" :value="0"></el-option>
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
                <el-button v-if="hasPerm('link:add')" type="primary" @click="openAdd">新增</el-button>
                <el-button v-if="hasPerm('link:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="links" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="name" label="站点名称" align="center"></el-table-column>
                <el-table-column prop="url" label="站点链接" align="center"></el-table-column>
                <el-table-column prop="description" label="站点描述" align="center"></el-table-column>
                <el-table-column label="站长图片" align="center" width="110">
                    <template #default="{row}">
                        <el-image v-if="row.img" :src="row.img" :preview-src-list="[row.img]" preview-teleported style="width:80px;height:50px" fit="cover"></el-image>
                    </template>
                </el-table-column>
                <el-table-column prop="email" label="站长邮箱" align="center"></el-table-column>
                <el-table-column prop="qq" label="站长QQ" align="center" width="100"></el-table-column>
                <el-table-column label="状态" align="center" width="80">
                    <template #default="{row}">{{ row.status ? '启用' : '禁用' }}</template>
                </el-table-column>
                <el-table-column label="操作" align="center" :width="store.isMobile ? 70 : 150">
                    <template #default="{row}">
                        <template v-if="!store.isMobile">
                            <el-button v-if="hasPerm('link:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                            <el-button v-if="hasPerm('link:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
                        </template>
                        <el-dropdown v-else trigger="click">
                            <el-button size="small" text><i class="fas fa-ellipsis-vertical"></i></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-if="hasPerm('link:edit')" @click="openEdit(row)">编辑</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('link:delete')" @click="removeOne(row)" divided>删除</el-dropdown-item>
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
                <el-form-item label="站点名称"><el-input v-model="form.name" placeholder="填写站点名称"></el-input></el-form-item>
                <el-form-item label="站点链接"><el-input v-model="form.url" placeholder="填写站点链接"></el-input></el-form-item>
                <el-form-item label="站点描述"><el-input v-model="form.description" placeholder="填写站点描述"></el-input></el-form-item>
                <el-form-item label="站长图片"><el-input v-model="form.img" placeholder="填写站长图片地址"></el-input></el-form-item>
                <el-form-item label="站长邮箱"><el-input v-model="form.email" placeholder="选填"></el-input></el-form-item>
                <el-form-item label="站长QQ"><el-input v-model="form.qq" placeholder="选填"></el-input></el-form-item>
                <el-form-item label="备注"><el-input v-model="form.remark" placeholder="选填"></el-input></el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
