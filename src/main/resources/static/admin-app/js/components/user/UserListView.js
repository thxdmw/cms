import { nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm } from '../../store.js';

export default {
    setup() {
        const users = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const filters = reactive({ username: '', email: '', phone: '' });
        const pageNumber = ref(1);
        const pageSize = ref(10);

        const dialogVisible = ref(false);
        const dialogTitle = ref('新增用户');
        const saving = ref(false);
        const isEdit = ref(false);
        const form = reactive({ userId: null, username: '', password: '', confirmPassword: '', email: '', phone: '', sex: 1, age: null });

        const assignRoleVisible = ref(false);
        const assignRoleSaving = ref(false);
        const roleOptions = ref([]);
        const roleTableRef = ref(null);
        const userIdChecked = ref(null);

        async function load() {
            loading.value = true;
            try {
                const res = await api.getUsers({
                    pageNumber: pageNumber.value,
                    pageSize: pageSize.value,
                    username: filters.username || undefined,
                    email: filters.email || undefined,
                    phone: filters.phone || undefined
                });
                users.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { filters.username = ''; filters.email = ''; filters.phone = ''; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        function rolesText(row) {
            return (row.roles || []).map((r) => r.name).join(',');
        }

        function openAdd() {
            isEdit.value = false;
            form.userId = null;
            form.username = '';
            form.password = '';
            form.confirmPassword = '';
            form.email = '';
            form.phone = '';
            form.sex = 1;
            form.age = null;
            dialogTitle.value = '新增用户';
            dialogVisible.value = true;
        }

        function openEdit(row) {
            isEdit.value = true;
            form.userId = row.userId;
            form.username = row.username;
            form.password = '';
            form.confirmPassword = '';
            form.email = row.email;
            form.phone = row.phone;
            form.sex = row.sex;
            form.age = row.age;
            dialogTitle.value = '编辑用户';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.username || !form.email || !form.phone || !form.sex || !form.age) {
                ElMessage.warning('请完整填写用户信息');
                return;
            }
            if (!isEdit.value) {
                if (!form.password || !form.confirmPassword) {
                    ElMessage.warning('请填写密码');
                    return;
                }
                if (form.password !== form.confirmPassword) {
                    ElMessage.warning('两次密码不一致');
                    return;
                }
            }
            saving.value = true;
            try {
                const res = isEdit.value ? await api.editUser(form) : await api.addUser(form);
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
            const res = await api.deleteUser(row.userId);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的用户'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteUsers(selected.value.map((r) => r.userId));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function openAssignRole(row) {
            userIdChecked.value = row.userId;
            const res = await api.getUserAssignRoleList(row.userId);
            roleOptions.value = res.rows || [];
            const hasRoles = res.hasRoles || [];
            assignRoleVisible.value = true;
            await nextTick();
            if (roleTableRef.value) {
                roleTableRef.value.clearSelection();
                roleOptions.value.forEach((role) => {
                    if (hasRoles.includes(role.roleId)) {
                        roleTableRef.value.toggleRowSelection(role, true);
                    }
                });
            }
        }

        const checkedRoles = ref([]);
        function onRoleSelectionChange(rows) { checkedRoles.value = rows; }

        async function saveAssignRole() {
            assignRoleSaving.value = true;
            try {
                const roleIdStr = checkedRoles.value.map((r) => r.roleId).join(',');
                const res = await api.saveUserAssignRole(userIdChecked.value, roleIdStr);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    assignRoleVisible.value = false;
                    load();
                }
            } finally {
                assignRoleSaving.value = false;
            }
        }

        onMounted(load);

        return {
            users, total, loading, filters, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange, rolesText,
            dialogVisible, dialogTitle, saving, isEdit, form,
            openAdd, openEdit, save, removeOne, removeBatch,
            assignRoleVisible, assignRoleSaving, roleOptions, roleTableRef,
            openAssignRole, onRoleSelectionChange, saveAssignRole,
            hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="用户名">
                    <el-input v-model="filters.username" placeholder="用户名" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="filters.email" placeholder="邮箱" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item label="电话">
                    <el-input v-model="filters.phone" placeholder="电话" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('user:add')" type="primary" @click="openAdd">新增</el-button>
                <el-button v-if="hasPerm('user:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="users" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="username" label="用户名" align="center"></el-table-column>
                <el-table-column prop="email" label="邮箱" align="center"></el-table-column>
                <el-table-column prop="phone" label="电话" align="center"></el-table-column>
                <el-table-column label="角色" align="center">
                    <template #default="{row}">{{ rolesText(row) }}</template>
                </el-table-column>
                <el-table-column label="用户状态" align="center" width="100">
                    <template #default="{row}">{{ row.status ? '启用' : '禁用' }}</template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="220">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('user:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="hasPerm('user:assignRole')" size="small" type="primary" @click="openAssignRole(row)">分配角色</el-button>
                        <el-button v-if="hasPerm('user:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
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
                <el-form-item label="用户名">
                    <el-input v-model="form.username" :readonly="isEdit" placeholder="填写用户名"></el-input>
                </el-form-item>
                <template v-if="!isEdit">
                    <el-form-item label="密码">
                        <el-input v-model="form.password" type="password" show-password placeholder="请填写密码"></el-input>
                    </el-form-item>
                    <el-form-item label="确认密码">
                        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次填写密码"></el-input>
                    </el-form-item>
                </template>
                <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请填写邮箱"></el-input></el-form-item>
                <el-form-item label="电话"><el-input v-model="form.phone" placeholder="请填写电话"></el-input></el-form-item>
                <el-form-item label="性别">
                    <el-select v-model="form.sex" style="width:100%">
                        <el-option label="男" :value="1"></el-option>
                        <el-option label="女" :value="2"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="年龄"><el-input-number v-model="form.age" :min="0"></el-input-number></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="assignRoleVisible" title="分配角色" width="600px">
            <el-table ref="roleTableRef" :data="roleOptions" row-key="roleId" @selection-change="onRoleSelectionChange">
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="name" label="角色名称" align="center"></el-table-column>
                <el-table-column prop="description" label="角色描述" align="center"></el-table-column>
                <el-table-column label="角色状态" align="center" width="90">
                    <template #default="{row}">{{ row.status ? '有效' : '删除' }}</template>
                </el-table-column>
            </el-table>
            <template #footer>
                <el-button @click="assignRoleVisible = false">取消</el-button>
                <el-button type="primary" :loading="assignRoleSaving" @click="saveAssignRole">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
