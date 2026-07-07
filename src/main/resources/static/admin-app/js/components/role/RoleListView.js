import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { buildTree } from '../../tree.js';
import { hasPerm } from '../../store.js';

export default {
    setup() {
        const roles = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const roleName = ref('');
        const pageNumber = ref(1);
        const pageSize = ref(10);

        const dialogVisible = ref(false);
        const dialogTitle = ref('新增角色');
        const saving = ref(false);
        const form = reactive({ roleId: null, name: '', description: '' });

        const assignPermVisible = ref(false);
        const assignPermSaving = ref(false);
        const permTree = ref([]);
        const defaultCheckedKeys = ref([]);
        const permTreeRef = ref(null);
        const roleIdChecked = ref(null);

        async function load() {
            loading.value = true;
            try {
                const res = await api.getRoles({ pageNumber: pageNumber.value, pageSize: pageSize.value, name: roleName.value || undefined });
                roles.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { roleName.value = ''; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        function openAdd() {
            form.roleId = null;
            form.name = '';
            form.description = '';
            dialogTitle.value = '新增角色';
            dialogVisible.value = true;
        }

        function openEdit(row) {
            form.roleId = row.roleId;
            form.name = row.name;
            form.description = row.description;
            dialogTitle.value = '编辑角色';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.name || !form.description) {
                ElMessage.warning('请完整填写角色信息');
                return;
            }
            saving.value = true;
            try {
                const res = form.roleId ? await api.editRole(form) : await api.addRole(form);
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
            const res = await api.deleteRole(row.roleId);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的角色'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteRoles(selected.value.map((r) => r.roleId));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function openAssignPermission(row) {
            roleIdChecked.value = row.roleId;
            const flat = (await api.getRoleAssignPermissionList(row.roleId)) || [];
            permTree.value = buildTree(flat, { idKey: 'id', parentIdKey: 'parentId', topLevelValue: '0' });
            defaultCheckedKeys.value = flat.filter((p) => p.checked).map((p) => p.id);
            assignPermVisible.value = true;
            await nextTick();
            if (permTreeRef.value) {
                permTreeRef.value.setCheckedKeys(defaultCheckedKeys.value);
            }
        }

        async function saveAssignPermission() {
            assignPermSaving.value = true;
            try {
                const nodes = permTreeRef.value.getCheckedNodes(false, false);
                const permissionIdStr = nodes.map((n) => n.permissionId).join(',');
                const res = await api.saveRoleAssignPermission(roleIdChecked.value, permissionIdStr);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) assignPermVisible.value = false;
            } finally {
                assignPermSaving.value = false;
            }
        }

        onMounted(load);

        return {
            roles, total, loading, roleName, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange,
            dialogVisible, dialogTitle, saving, form, openAdd, openEdit, save, removeOne, removeBatch,
            assignPermVisible, assignPermSaving, permTree, defaultCheckedKeys, permTreeRef,
            openAssignPermission, saveAssignPermission,
            hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="角色名称">
                    <el-input v-model="roleName" placeholder="角色名称" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('role:add')" type="primary" @click="openAdd">新增</el-button>
                <el-button v-if="hasPerm('role:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="roles" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="name" label="角色名称" align="center"></el-table-column>
                <el-table-column prop="description" label="角色描述" align="center"></el-table-column>
                <el-table-column prop="createTime" label="创建时间" align="center" width="170"></el-table-column>
                <el-table-column label="操作" align="center" width="220">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('role:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="hasPerm('role:assignPerms')" size="small" type="primary" @click="openAssignPermission(row)">分配权限</el-button>
                        <el-button v-if="hasPerm('role:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
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
                <el-form-item label="角色名称"><el-input v-model="form.name" placeholder="请填写角色名称"></el-input></el-form-item>
                <el-form-item label="角色描述"><el-input v-model="form.description" placeholder="请填写角色描述"></el-input></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="assignPermVisible" title="分配权限" width="500px">
            <el-tree ref="permTreeRef" :data="permTree" node-key="id" show-checkbox default-expand-all
                     :props="{label: 'name', children: 'children'}" :default-checked-keys="defaultCheckedKeys">
            </el-tree>
            <template #footer>
                <el-button @click="assignPermVisible = false">取消</el-button>
                <el-button type="primary" :loading="assignPermSaving" @click="saveAssignPermission">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
