import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm, store } from '../../store.js';

function formatSize(bytes) {
    if (bytes == null) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB';
}

export default {
    setup() {
        const files = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const keyword = ref('');
        const pageNumber = ref(1);
        const pageSize = ref(10);

        async function load() {
            loading.value = true;
            try {
                const res = await api.getServerFiles({ keyword: keyword.value, pageNumber: pageNumber.value, pageSize: pageSize.value });
                files.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() { pageNumber.value = 1; load(); }
        function reset() { keyword.value = ''; search(); }
        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }
        function onSelectionChange(rows) { selected.value = rows; }

        // 上传：原生 input[type=file]，跟 ArticleUploadField 一致的模式，但不限制文件类型（含压缩包）
        const uploadDialogVisible = ref(false);
        const uploadFileRef = ref(null);
        const pickedFile = ref(null);
        const uploadRemark = ref('');
        const uploading = ref(false);

        function openUpload() {
            pickedFile.value = null;
            uploadRemark.value = '';
            uploadDialogVisible.value = true;
        }
        function pickFile() { uploadFileRef.value && uploadFileRef.value.click(); }
        function onFileChange(e) {
            pickedFile.value = (e.target.files && e.target.files[0]) || null;
            e.target.value = '';
        }
        async function submitUpload() {
            if (!pickedFile.value) { ElMessage.warning('请先选择要上传的文件'); return; }
            uploading.value = true;
            try {
                const res = await api.uploadServerFile(pickedFile.value, uploadRemark.value);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    uploadDialogVisible.value = false;
                    load();
                }
            } finally {
                uploading.value = false;
            }
        }

        // 预览/在线编辑：同一个对话框，editing 控制 textarea 是否只读
        const previewDialogVisible = ref(false);
        const previewTitle = ref('');
        const previewContent = ref('');
        const previewLoading = ref(false);
        const editing = ref(false);
        const saving = ref(false);
        const currentId = ref(null);

        async function openPreview(row, startEditing) {
            currentId.value = row.id;
            previewTitle.value = row.originalName;
            editing.value = !!startEditing;
            previewContent.value = '';
            previewDialogVisible.value = true;
            previewLoading.value = true;
            try {
                const res = await api.previewServerFile(row.id);
                if (res.status === 200) {
                    previewContent.value = res.data || '';
                } else {
                    ElMessage.error(res.msg);
                    previewDialogVisible.value = false;
                }
            } finally {
                previewLoading.value = false;
            }
        }

        async function saveEdit() {
            saving.value = true;
            try {
                const res = await api.saveServerFile(currentId.value, previewContent.value);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    editing.value = false;
                    load();
                }
            } finally {
                saving.value = false;
            }
        }

        function downloadFile(row) {
            const link = document.createElement('a');
            link.href = '/serverFile/download?id=' + encodeURIComponent(row.id);
            link.download = row.originalName || 'download';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        async function removeOne(row) {
            try {
                await ElMessageBox.confirm('确定删除该文件？删除后无法恢复', '提示');
            } catch (e) { return; }
            const res = await api.deleteServerFile(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的文件'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}个文件？删除后无法恢复`, '提示');
            } catch (e) { return; }
            const res = await api.batchDeleteServerFiles(selected.value.map((r) => r.id));
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        onMounted(load);

        return {
            store, files, total, loading, keyword, pageNumber, pageSize,
            search, reset, onPageChange, onSizeChange, onSelectionChange,
            uploadDialogVisible, uploadFileRef, pickedFile, uploadRemark, uploading,
            openUpload, pickFile, onFileChange, submitUpload,
            previewDialogVisible, previewTitle, previewContent, previewLoading, editing, saving,
            openPreview, saveEdit, downloadFile, removeOne, removeBatch,
            formatSize, hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="文件名">
                    <el-input v-model="keyword" placeholder="按文件名搜索" clearable></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="search">查询</el-button>
                    <el-button @click="reset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card>
            <div style="margin-bottom:16px;">
                <el-button v-if="hasPerm('serverFile:upload')" type="primary" @click="openUpload">上传文件</el-button>
                <el-button v-if="hasPerm('serverFile:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
            </div>
            <el-table :data="files" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip></el-table-column>
                <el-table-column label="大小" align="center" width="100">
                    <template #default="{row}">{{ formatSize(row.size) }}</template>
                </el-table-column>
                <el-table-column prop="extension" label="类型" align="center" width="90"></el-table-column>
                <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip></el-table-column>
                <el-table-column prop="uploader" label="上传人" align="center" width="110"></el-table-column>
                <el-table-column prop="createTime" label="上传时间" align="center" width="160"></el-table-column>
                <el-table-column label="操作" align="center" :width="store.isMobile ? 70 : 240">
                    <template #default="{row}">
                        <template v-if="!store.isMobile">
                            <el-button v-if="hasPerm('serverFile:list') && row.editable" size="small" @click="openPreview(row, false)">预览</el-button>
                            <el-button v-if="hasPerm('serverFile:edit') && row.editable" size="small" type="primary" @click="openPreview(row, true)">编辑</el-button>
                            <el-button v-if="hasPerm('serverFile:download')" size="small" @click="downloadFile(row)">下载</el-button>
                            <el-button v-if="hasPerm('serverFile:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
                        </template>
                        <el-dropdown v-else trigger="click">
                            <el-button size="small" text><i class="fas fa-ellipsis-vertical"></i></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-if="hasPerm('serverFile:list') && row.editable" @click="openPreview(row, false)">预览</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('serverFile:edit') && row.editable" @click="openPreview(row, true)">编辑</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('serverFile:download')" @click="downloadFile(row)">下载</el-dropdown-item>
                                    <el-dropdown-item v-if="hasPerm('serverFile:delete')" @click="removeOne(row)" divided>删除</el-dropdown-item>
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

        <el-dialog v-model="uploadDialogVisible" title="上传文件" width="480px">
            <div style="margin-bottom:12px;">
                <el-button @click="pickFile">选择文件</el-button>
                <input ref="uploadFileRef" type="file" style="display:none" @change="onFileChange">
                <span v-if="pickedFile" style="margin-left:8px;">{{ pickedFile.name }}（{{ formatSize(pickedFile.size) }}）</span>
            </div>
            <el-input v-model="uploadRemark" placeholder="备注（选填）"></el-input>
            <template #footer>
                <el-button @click="uploadDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="previewDialogVisible" :title="previewTitle" width="800px">
            <el-input v-model="previewContent" type="textarea" :rows="20" :readonly="!editing"
                      v-loading="previewLoading" style="font-family:monospace;"></el-input>
            <template #footer>
                <el-button @click="previewDialogVisible = false">关闭</el-button>
                <el-button v-if="!editing && hasPerm('serverFile:edit')" type="primary" @click="editing = true">编辑</el-button>
                <el-button v-if="editing" type="primary" :loading="saving" @click="saveEdit">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
