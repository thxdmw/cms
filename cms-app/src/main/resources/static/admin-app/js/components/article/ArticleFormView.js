import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../api.js';
import { store } from '../../store.js';
import ArticleUploadField from './ArticleUploadField.js';

// 移动端窄屏下编辑器初始高度调低，避免一进页面就要滚动一大段空白才能看到设置面板；
// 桌面端保持原来的 1200，两个值都要跟 initEditor/toggleSidebar 里的 resize 调用同步
const EDITOR_HEIGHT_DESKTOP = 1200;
const EDITOR_HEIGHT_MOBILE = 520;

export default {
    components: { ArticleUploadField },
    setup() {
        const route = useRoute();
        const router = useRouter();
        const isEdit = computed(() => !!route.params.id);

        const form = reactive({
            id: null,
            title: '',
            categoryId: null,
            coverImage: '',
            sliderImg: '',
            original: 1,
            slider: 0,
            top: 0,
            recommended: 0,
            comment: 1,
            description: '',
            keywords: ''
        });
        const selectedTags = ref([]);
        const categories = ref([]);
        const allTags = ref([]);
        const sidebarCollapsed = ref(false);
        const editorFullscreen = ref(false);
        let editor = null;

        async function loadPickers() {
            const [cats, tags] = await Promise.all([api.getCategories(true), api.getAllTags()]);
            categories.value = cats || [];
            allTags.value = tags || [];
        }

        async function loadArticleForEdit() {
            const res = await api.getArticleDetail(route.params.id);
            if (!res || res.status !== 200 || !res.data) {
                ElMessage.error((res && res.msg) || '文章不存在');
                return;
            }
            const a = res.data;
            form.id = a.id;
            form.title = a.title;
            form.categoryId = a.categoryId;
            form.coverImage = a.coverImage;
            form.sliderImg = a.sliderImg;
            form.original = a.original;
            form.slider = a.slider;
            form.top = a.top;
            form.recommended = a.recommended;
            form.comment = a.comment;
            form.description = a.description;
            form.keywords = a.keywords;
            selectedTags.value = (a.tags || []).map((t) => t.id);
            return a.contentMd || '';
        }

        function initEditor(initialMarkdown) {
            if (!window.editormd) return;
            editor = window.editormd('editor-md', {
                width: '100%',
                height: store.isMobile ? EDITOR_HEIGHT_MOBILE : EDITOR_HEIGHT_DESKTOP,
                path: '/libs/editor.md/lib/',
                pluginPath: '/libs/editor.md/plugins/',
                placeholder: '请输入文章内容（Markdown）...',
                saveHTMLToTextarea: true,
                markdown: initialMarkdown || '',
                syncScrolling: 'single',
                emoji: true,
                taskList: true,
                toc: true,
                codeFold: true,
                htmlDecode: true,
                hex: true,
                sequenceDiagram: true,
                flowChart: true,
                imageUpload: true,
                imageFormats: ['jpg', 'jpeg', 'gif', 'png', 'bmp', 'webp'],
                imageUploadURL: '/attachment/uploadForEditor',
                // Editor.md 全屏时用 position:fixed 把编辑器本身铺满视口，但没有设置 z-index，
                // 右侧设置栏（同级的 el-col）跟它没有层叠关系，视觉上还会叠在编辑器上面——
                // 与其去抢 z-index，不如直接在全屏时把设置栏隐藏掉，更彻底也更不容易受浏览器差异影响
                onfullscreen: () => { editorFullscreen.value = true; },
                onfullscreenExit: () => { editorFullscreen.value = false; }
            });
        }

        function toggleSidebar() {
            sidebarCollapsed.value = !sidebarCollapsed.value;
            nextTick(() => {
                setTimeout(() => {
                    if (editor) editor.resize('100%', store.isMobile ? EDITOR_HEIGHT_MOBILE : EDITOR_HEIGHT_DESKTOP);
                }, 200);
            });
        }

        async function save(status) {
            if (!editor) return;
            const contentMd = editor.getMarkdown();
            if (!contentMd || !contentMd.trim()) {
                ElMessage.warning('请输入文章内容！');
                return;
            }
            if (!selectedTags.value.length) {
                ElMessage.warning('请选择文章标签！');
                return;
            }
            try {
                await ElMessageBox.confirm('确认保存文章？', '提示');
            } catch (e) {
                return;
            }
            const content = editor.getHTML();
            const payload = { ...form, status, contentMd, content, tag: selectedTags.value };

            if (isEdit.value) {
                const res = await api.editArticle(payload);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            } else {
                const res = await api.addArticle(payload);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) {
                    // 照抄原有行为：新增成功后清空表单，方便连续写下一篇
                    form.id = null;
                    form.title = '';
                    form.categoryId = null;
                    form.coverImage = '';
                    form.sliderImg = '';
                    form.original = 1;
                    form.slider = 0;
                    form.top = 0;
                    form.recommended = 0;
                    form.comment = 1;
                    form.description = '';
                    form.keywords = '';
                    selectedTags.value = [];
                    editor.setMarkdown('');
                }
            }
        }

        onMounted(async () => {
            await loadPickers();
            if (isEdit.value) {
                const md = await loadArticleForEdit();
                initEditor(md);
            } else {
                initEditor('');
            }
        });

        onBeforeUnmount(() => {
            // Editor.md 没有官方的 destroy API，交给整个 #editor-md 容器随组件卸载一起被 Vue 移除
            editor = null;
        });

        return {
            store, isEdit, form, selectedTags, categories, allTags, sidebarCollapsed, editorFullscreen,
            toggleSidebar, save
        };
    },
    template: `
    <div>
        <el-row :gutter="16" style="align-items:flex-start;">
            <el-col :xs="24" :sm="sidebarCollapsed ? 24 : 16">
                <el-card>
                    <div style="display:flex; align-items:center; gap:12px; margin-bottom:12px;">
                        <el-input v-model="form.title" placeholder="请输入文章标题"></el-input>
                        <el-button size="small" @click="toggleSidebar" style="flex-shrink:0;">{{ sidebarCollapsed ? '« 展开' : '折叠 »' }}</el-button>
                    </div>
                    <div id="editor-md"><textarea style="display:none;"></textarea></div>
                </el-card>
            </el-col>
            <el-col :xs="24" :sm="8" v-show="!sidebarCollapsed && !editorFullscreen" :style="store.isMobile ? '' : 'position:sticky; top:0;'">
                <el-card>
                    <el-form label-position="top">
                        <el-form-item label="文章分类">
                            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
                                <template v-for="cat in categories" :key="cat.id">
                                    <el-option-group v-if="cat.children && cat.children.length" :label="cat.name">
                                        <el-option v-for="child in cat.children" :key="child.id" :label="child.name" :value="child.id"></el-option>
                                    </el-option-group>
                                    <el-option v-else :label="cat.name" :value="cat.id"></el-option>
                                </template>
                            </el-select>
                        </el-form-item>

                        <el-form-item label="文章标签">
                            <el-checkbox-group v-model="selectedTags">
                                <el-checkbox v-for="t in allTags" :key="t.id" :value="t.id">{{ t.name }}</el-checkbox>
                            </el-checkbox-group>
                        </el-form-item>

                        <el-form-item label="文章封面">
                            <article-upload-field v-model="form.coverImage" placeholder="文章封面地址"></article-upload-field>
                        </el-form-item>

                        <el-form-item label="文章类型">
                            <el-radio-group v-model="form.original">
                                <el-radio :value="1">原创</el-radio>
                                <el-radio :value="0">转载</el-radio>
                            </el-radio-group>
                        </el-form-item>

                        <el-form-item label="是否轮播">
                            <el-radio-group v-model="form.slider">
                                <el-radio :value="1">是</el-radio>
                                <el-radio :value="0">否</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item label="轮播图" v-if="form.slider === 1">
                            <article-upload-field v-model="form.sliderImg" placeholder="轮播图地址（建议800*300）"></article-upload-field>
                        </el-form-item>

                        <el-form-item label="是否置顶">
                            <el-radio-group v-model="form.top">
                                <el-radio :value="1">是</el-radio>
                                <el-radio :value="0">否</el-radio>
                            </el-radio-group>
                        </el-form-item>

                        <el-form-item label="是否推荐">
                            <el-radio-group v-model="form.recommended">
                                <el-radio :value="1">是</el-radio>
                                <el-radio :value="0">否</el-radio>
                            </el-radio-group>
                        </el-form-item>

                        <el-form-item label="开启评论">
                            <el-radio-group v-model="form.comment">
                                <el-radio :value="1">是</el-radio>
                                <el-radio :value="0">否</el-radio>
                            </el-radio-group>
                        </el-form-item>

                        <el-form-item label="概要">
                            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="文章概要"></el-input>
                        </el-form-item>

                        <el-form-item label="关键词">
                            <el-input v-model="form.keywords" type="textarea" :rows="2" placeholder="文章关键词（SEO优化）"></el-input>
                        </el-form-item>

                        <el-form-item>
                            <template v-if="!isEdit">
                                <el-button type="primary" size="large" @click="save(1)">发布文章</el-button>
                                <el-button size="large" @click="save(0)">保存草稿</el-button>
                            </template>
                            <template v-else>
                                <el-button type="primary" size="large" @click="save(1)">发布更新</el-button>
                                <el-button size="large" @click="save(0)">更新草稿</el-button>
                            </template>
                        </el-form-item>
                    </el-form>
                </el-card>
            </el-col>
        </el-row>
    </div>
    `
};
