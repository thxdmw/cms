import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm } from '../../store.js';

export default {
    setup() {
        const router = useRouter();
        const articles = ref([]);
        const total = ref(0);
        const loading = ref(false);
        const selected = ref([]);
        const filters = reactive({ keywords: '', categoryId: null, status: null });
        const pageNumber = ref(1);
        const pageSize = ref(10);
        const categories = ref([]);

        async function loadCategories() {
            categories.value = (await api.getCategories(false)) || [];
        }

        async function load() {
            loading.value = true;
            try {
                const res = await api.getArticles({
                    pageNumber: pageNumber.value,
                    pageSize: pageSize.value,
                    keywords: filters.keywords || undefined,
                    categoryId: filters.categoryId || undefined,
                    status: filters.status
                });
                // /article/list 是裸 {rows, total} 响应，没有 {status,msg,data} 外层
                articles.value = res.rows || [];
                total.value = res.total || 0;
            } finally {
                loading.value = false;
            }
        }

        function search() {
            pageNumber.value = 1;
            load();
        }
        function reset() {
            filters.keywords = '';
            filters.categoryId = null;
            filters.status = null;
            search();
        }

        function onPageChange(p) { pageNumber.value = p; load(); }
        function onSizeChange(s) { pageSize.value = s; pageNumber.value = 1; load(); }

        function goAdd() { router.push('/admin/articles/add'); }
        function goEdit(id) { router.push('/admin/articles/' + id + '/edit'); }

        async function changeStatus(row) {
            const target = row.status === 1 ? 0 : 1;
            const tip = target === 1 ? '发布该文章？' : '取消发布该文章？';
            try {
                await ElMessageBox.confirm(tip, '提示');
            } catch (e) { return; }
            const res = await api.changeArticleStatus(row.id, target);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeOne(row) {
            try {
                await ElMessageBox.confirm('确定删除该文章？', '提示');
            } catch (e) { return; }
            const res = await api.deleteArticle(row.id);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        async function removeBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要删除的文章'); return; }
            try {
                await ElMessageBox.confirm(`确定删除选中的${selected.value.length}条记录？`, '提示');
            } catch (e) { return; }
            const ids = selected.value.map((r) => r.id);
            const res = await api.batchDeleteArticles(ids);
            ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            if (res.status === 200) load();
        }

        function downloadBatch() {
            if (!selected.value.length) { ElMessage.warning('请先选择要下载的文章'); return; }
            // 文件流响应，fetch 拿不到浏览器"另存为"效果，用隐藏表单 submit（照抄原有 jQuery 逻辑）
            const form = document.createElement('form');
            form.method = 'post';
            form.action = '/article/batch/download';
            form.style.display = 'none';
            selected.value.forEach((row) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'ids[]';
                input.value = row.id;
                form.appendChild(input);
            });
            document.body.appendChild(form);
            form.submit();
            form.remove();
        }

        function onSelectionChange(rows) { selected.value = rows; }

        onMounted(() => {
            loadCategories();
            load();
        });

        return {
            articles, total, loading, filters, pageNumber, pageSize, categories,
            search, reset, onPageChange, onSizeChange, goAdd, goEdit,
            changeStatus, removeOne, removeBatch, downloadBatch, onSelectionChange, hasPerm
        };
    },
    template: `
    <div>
        <el-card style="margin-bottom:16px;">
            <el-form :inline="true" @submit.prevent="search">
                <el-form-item label="关键字">
                    <el-input v-model="filters.keywords" placeholder="标题/内容关键字" clearable @keyup.enter="search"></el-input>
                </el-form-item>
                <el-form-item label="分类">
                    <el-select v-model="filters.categoryId" placeholder="选择分类" clearable style="width:160px">
                        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="filters.status" placeholder="选择状态" clearable style="width:120px">
                        <el-option label="已发布" :value="1"></el-option>
                        <el-option label="草稿" :value="0"></el-option>
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
                <el-button v-if="hasPerm('article:add')" type="primary" @click="goAdd">写文章</el-button>
                <el-button v-if="hasPerm('article:batchDelete')" type="danger" @click="removeBatch">批量删除</el-button>
                <el-button v-if="hasPerm('article:download')" @click="downloadBatch">下载文章</el-button>
            </div>
            <el-table :data="articles" v-loading="loading" @selection-change="onSelectionChange" border>
                <el-table-column type="selection" width="45"></el-table-column>
                <el-table-column prop="title" label="标题" align="center"></el-table-column>
                <el-table-column label="分类" align="center" width="110">
                    <template #default="{row}">{{ row.bizCategory ? row.bizCategory.name : '' }}</template>
                </el-table-column>
                <el-table-column label="标签" align="center">
                    <template #default="{row}">
                        <el-tag v-for="t in row.tags" :key="t.id" size="small" style="margin-right:4px">{{ t.name }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="缩略图" align="center" width="110">
                    <template #default="{row}">
                        <el-image v-if="row.coverImage" :src="row.coverImage" :preview-src-list="[row.coverImage]" preview-teleported
                                  style="width:80px;height:50px" fit="cover"></el-image>
                    </template>
                </el-table-column>
                <el-table-column label="轮播" align="center" width="70">
                    <template #default="{row}">{{ row.slider ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column label="置顶" align="center" width="70">
                    <template #default="{row}">{{ row.top ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column label="推荐" align="center" width="70">
                    <template #default="{row}">{{ row.recommended ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column label="状态" align="center" width="80">
                    <template #default="{row}">{{ row.status ? '已发布' : '草稿' }}</template>
                </el-table-column>
                <el-table-column prop="lookCount" label="浏览" align="center" width="70"></el-table-column>
                <el-table-column prop="commentCount" label="评论" align="center" width="70"></el-table-column>
                <el-table-column prop="loveCount" label="喜欢" align="center" width="70"></el-table-column>
                <el-table-column label="操作" align="center" width="220">
                    <template #default="{row}">
                        <el-button v-if="hasPerm('article:edit')" size="small" type="primary" @click="goEdit(row.id)">编辑</el-button>
                        <el-button v-if="row.status===1" size="small" type="danger" @click="changeStatus(row)">取消发布</el-button>
                        <el-button v-else size="small" type="primary" @click="changeStatus(row)">发布</el-button>
                        <el-button v-if="hasPerm('article:delete')" size="small" type="danger" @click="removeOne(row)">删除</el-button>
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
