import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { buildTree } from '../../tree.js';
import { hasPerm } from '../../store.js';
import IconPicker from '../common/IconPicker.js';

const TYPE_LABELS = { 0: '目录', 1: '菜单', 2: '按钮' };
const TYPE_TAG_TYPE = { 0: 'primary', 1: '', 2: 'info' };

export default {
    components: { IconPicker },
    setup() {
        const treeData = ref([]);
        const parentTreeData = ref([]);
        const loading = ref(false);

        const dialogVisible = ref(false);
        const dialogTitle = ref('新增资源');
        const saving = ref(false);
        const isEdit = ref(false);
        const form = reactive({
            permissionId: null, type: 0, name: '', parentId: '0',
            url: '', perms: '', orderNum: 0, icon: '', description: ''
        });

        async function load() {
            loading.value = true;
            try {
                const flat = (await api.getPermissions('1')) || [];
                treeData.value = buildTree(flat, { idKey: 'id', parentIdKey: 'parentId', topLevelValue: '0' });
            } finally {
                loading.value = false;
            }
        }

        async function loadParentOptions() {
            const flat = (await api.getPermissions('2')) || [];
            const children = buildTree(flat, { idKey: 'id', parentIdKey: 'parentId', topLevelValue: '0' });
            parentTreeData.value = [{ id: '0', name: '顶层菜单', children }];
        }

        function typeLabel(type) { return TYPE_LABELS[type] || ''; }

        function openAdd() {
            isEdit.value = false;
            form.permissionId = null;
            form.type = 0;
            form.name = '';
            form.parentId = '0';
            form.url = '';
            form.perms = '';
            form.orderNum = 0;
            form.icon = '';
            form.description = '';
            dialogTitle.value = '新增资源';
            loadParentOptions();
            dialogVisible.value = true;
        }

        function openEdit(node) {
            isEdit.value = true;
            form.permissionId = node.permissionId;
            form.type = node.type;
            form.name = node.name;
            form.parentId = node.parentId;
            form.url = node.url;
            form.perms = node.perms;
            form.orderNum = node.orderNum;
            form.icon = node.icon;
            form.description = node.description;
            dialogTitle.value = '编辑资源';
            loadParentOptions();
            dialogVisible.value = true;
        }

        function validate() {
            if (!form.name || !form.description) {
                ElMessage.warning('请完整填写资源名称和描述');
                return false;
            }
            if (form.type === 1 && !form.url) {
                ElMessage.warning('请填写资源url');
                return false;
            }
            if (form.type !== 0 && !form.perms) {
                ElMessage.warning('请填写资源标识');
                return false;
            }
            if (form.type !== 2 && (form.orderNum === null || form.orderNum === undefined || form.orderNum === '')) {
                ElMessage.warning('请填写排序');
                return false;
            }
            if (form.type !== 2 && !form.icon) {
                ElMessage.warning('请填写图标');
                return false;
            }
            return true;
        }

        async function save() {
            if (!validate()) return;
            saving.value = true;
            try {
                const res = isEdit.value ? await api.editPermission(form) : await api.addPermission(form);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    dialogVisible.value = false;
                    load();
                }
            } finally {
                saving.value = false;
            }
        }

        async function removeNode(node) {
            try {
                await ElMessageBox.confirm('确定删除该资源？', '提示');
            } catch (e) { return; }
            const res = await api.deletePermission(node.permissionId);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            treeData, parentTreeData, loading, typeLabel, TYPE_TAG_TYPE,
            dialogVisible, dialogTitle, saving, isEdit, form,
            openAdd, openEdit, save, removeNode, hasPerm
        };
    },
    template: `
    <div>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('permission:add')" type="primary" @click="openAdd">新增</el-button>
            </div>
            <el-table :data="treeData" row-key="id" v-loading="loading" border>
                <el-table-column prop="name" label="菜单名称" min-width="160">
                    <template #default="{row}">
                        <i v-if="row.icon && row.type !== 2" :class="row.icon" style="margin-right:6px;"></i>{{ row.name }}
                    </template>
                </el-table-column>
                <el-table-column prop="url" label="菜单URL" align="center"></el-table-column>
                <el-table-column prop="perms" label="权限标识" align="center"></el-table-column>
                <el-table-column label="类型" align="center" width="80">
                    <template #default="{row}"><el-tag size="small" :type="TYPE_TAG_TYPE[row.type]">{{ typeLabel(row.type) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="图标" align="center" width="70">
                    <template #default="{row}"><i v-if="row.icon && row.type !== 2" :class="row.icon"></i></template>
                </el-table-column>
                <el-table-column prop="orderNum" label="排序" align="center" width="70"></el-table-column>
                <el-table-column prop="description" label="权限描述" align="center"></el-table-column>
                <el-table-column label="操作" align="center" width="150">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('permission:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="hasPerm('permission:delete')" size="small" type="danger" @click="removeNode(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
            <el-form label-width="90px">
                <el-form-item label="类型">
                    <el-radio-group v-model="form.type" :disabled="isEdit">
                        <el-radio :value="0">目录</el-radio>
                        <el-radio :value="1">菜单</el-radio>
                        <el-radio :value="2">按钮</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="资源名称"><el-input v-model="form.name" placeholder="请填写资源名称"></el-input></el-form-item>
                <el-form-item label="上级资源">
                    <el-tree-select v-model="form.parentId" :data="parentTreeData" node-key="id" check-strictly
                                     :props="{label: 'name', children: 'children'}" default-expand-all style="width:100%">
                    </el-tree-select>
                </el-form-item>
                <el-form-item v-if="form.type === 1" label="资源url"><el-input v-model="form.url" placeholder="请填写资源url"></el-input></el-form-item>
                <el-form-item v-if="form.type !== 0" label="资源标识"><el-input v-model="form.perms" placeholder="请填写资源标识，如 article:add"></el-input></el-form-item>
                <el-form-item v-if="form.type !== 2" label="排序"><el-input-number v-model="form.orderNum" :min="0"></el-input-number></el-form-item>
                <el-form-item v-if="form.type !== 2" label="图标">
                    <icon-picker v-model="form.icon"></icon-picker>
                </el-form-item>
                <el-form-item label="描述"><el-input v-model="form.description" placeholder="请填写描述"></el-input></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
