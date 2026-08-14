import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../api.js';
import { hasPerm } from '../../store.js';

export default {
    setup() {
        const saving = ref(false);
        const form = reactive({
            SITE_NAME: '', SITE_KWD: '', SITE_DESC: '', SITE_LOGO: '',
            SITE_PERSON_PIC: '', SITE_PERSON_NAME: '', SITE_PERSON_DESC: '', BAIDU_PUSH_URL: ''
        });

        async function load() {
            const res = await api.getSiteInfo();
            const data = (res && res.data) || {};
            Object.keys(form).forEach((key) => { form[key] = data[key] || ''; });
        }

        async function save() {
            if (!form.SITE_NAME) {
                ElMessage.warning('请填写网站名称');
                return;
            }
            saving.value = true;
            try {
                const res = await api.editSiteInfo({ ...form });
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
            } finally {
                saving.value = false;
            }
        }

        onMounted(load);

        return { form, saving, save, hasPerm };
    },
    template: `
    <div>
        <el-card>
            <el-form label-width="120px" style="max-width:700px;">
                <el-form-item label="网站名称"><el-input v-model="form.SITE_NAME" placeholder="填写网站名称"></el-input></el-form-item>
                <el-form-item label="网站关键字"><el-input v-model="form.SITE_KWD" type="textarea" :rows="2" placeholder="填写网站关键字"></el-input></el-form-item>
                <el-form-item label="网站描述"><el-input v-model="form.SITE_DESC" type="textarea" :rows="2" placeholder="填写网站描述"></el-input></el-form-item>
                <el-form-item label="站点logo"><el-input v-model="form.SITE_LOGO" placeholder="填写站点logo地址"></el-input></el-form-item>
                <el-form-item label="站长头像"><el-input v-model="form.SITE_PERSON_PIC" placeholder="填写站长头像地址"></el-input></el-form-item>
                <el-form-item label="站长名称"><el-input v-model="form.SITE_PERSON_NAME" placeholder="填写站长名称"></el-input></el-form-item>
                <el-form-item label="站长描述"><el-input v-model="form.SITE_PERSON_DESC" type="textarea" :rows="2" placeholder="填写站长描述"></el-input></el-form-item>
                <el-form-item label="百度推送地址"><el-input v-model="form.BAIDU_PUSH_URL" placeholder="填写百度推送地址"></el-input></el-form-item>
                <el-form-item v-if="hasPerm('siteinfo:edit')">
                    <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
    `
};
