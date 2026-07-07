import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { buildTree } from '../../tree.js';
import { hasPerm } from '../../store.js';

export default {
    setup() {
        const treeData = ref([]);
        const topLevelOptions = ref([]); // 顶层分类，供"上级分类"下拉选择
        const dialogVisible = ref(false);
        const dialogTitle = ref('新增分类');
        const saving = ref(false);
        const form = reactive({ id: null, pid: '0', name: '', description: '', sort: 0 });

        async function load() {
            const flat = (await api.getCategories(false)) || [];
            treeData.value = buildTree(flat, { idKey: 'id', parentIdKey: 'pid', topLevelValue: '0' });
            topLevelOptions.value = treeData.value; // 顶层分类本身就是 treeData 的根节点
        }

        function openAdd() {
            form.id = null;
            form.pid = '0';
            form.name = '';
            form.description = '';
            form.sort = 0;
            dialogTitle.value = '新增分类';
            dialogVisible.value = true;
        }

        function openEdit(node) {
            form.id = node.id;
            form.pid = node.pid || '0';
            form.name = node.name;
            form.description = node.description;
            form.sort = node.sort;
            dialogTitle.value = '编辑分类';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.name || !form.description || (form.sort === null || form.sort === undefined || form.sort === '')) {
                ElMessage.warning('请完整填写分类信息');
                return;
            }
            saving.value = true;
            try {
                const res = form.id ? await api.editCategory(form) : await api.addCategory(form);
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
                await ElMessageBox.confirm('确定删除该分类？', '提示');
            } catch (e) {
                return;
            }
            const res = await api.deleteCategory(node.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            treeData, topLevelOptions, dialogVisible, dialogTitle, saving, form,
            openAdd, openEdit, save, removeNode, hasPerm
        };
    },
    template: `
    <div>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('category:add')" type="primary" @click="openAdd">新增</el-button>
            </div>
            <el-table :data="treeData" row-key="id" border>
                <el-table-column prop="name" label="分类名称"></el-table-column>
                <el-table-column prop="description" label="分类描述" align="center"></el-table-column>
                <el-table-column prop="sort" label="排序" align="center" width="90"></el-table-column>
                <el-table-column label="操作" align="center" width="150">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('category:edit')" size="small" type="primary" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="hasPerm('category:delete')" size="small" type="danger" @click="removeNode(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
            <el-form label-width="90px">
                <el-form-item label="上级分类">
                    <el-select v-model="form.pid" style="width:100%">
                        <el-option label="无（顶层分类）" value="0"></el-option>
                        <el-option v-for="c in topLevelOptions" :key="c.id" :label="c.name" :value="c.id" :disabled="c.id === form.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="分类名称"><el-input v-model="form.name" placeholder="填写分类名称"></el-input></el-form-item>
                <el-form-item label="分类描述"><el-input v-model="form.description" placeholder="填写分类描述"></el-input></el-form-item>
                <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0"></el-input-number></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
