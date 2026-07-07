import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { store } from '../../store.js';
import { api } from '../../api.js';

export default {
    setup() {
        const dialogVisible = ref(false);
        const menuVisible = ref(false);
        const saving = ref(false);
        const form = reactive({ oldPassword: '', newPassword: '', confirmNewPassword: '' });

        function closeMenu() {
            menuVisible.value = false;
        }

        function toggleMenu() {
            menuVisible.value = !menuVisible.value;
        }

        function openChangePassword() {
            closeMenu();
            form.oldPassword = '';
            form.newPassword = '';
            form.confirmNewPassword = '';
            dialogVisible.value = true;
        }

        async function save() {
            if (!form.oldPassword || !form.newPassword || !form.confirmNewPassword) {
                ElMessage.warning('请完整填写密码信息');
                return;
            }
            if (form.newPassword !== form.confirmNewPassword) {
                ElMessage.warning('两次密码输入不一致');
                return;
            }
            saving.value = true;
            try {
                const res = await api.changePassword(form);
                ElMessage({ type: res.status === 200 ? 'success' : 'error', message: res.msg });
                if (res.status === 200) dialogVisible.value = false;
            } finally {
                saving.value = false;
            }
        }

        onMounted(() => document.addEventListener('click', closeMenu));
        onBeforeUnmount(() => document.removeEventListener('click', closeMenu));

        return { store, dialogVisible, menuVisible, saving, form, toggleMenu, openChangePassword, save };
    },
    // 注意：el-dialog 不能和 admin-brand/admin-user 放在同一个 flex 容器里当兄弟节点——
    // 哪怕对话框是关闭状态，el-dialog 自己也会在原地占一个 flex 子项位置，把 admin-user
    // 从"最后一个"挤到中间，视觉上就变成贴着左边的品牌文字了。这里用外层 div 把
    // "flex 排布的 topbar 内容" 和 "el-dialog" 分开，彼此是兄弟关系，不共享 flex 上下文
    template: `
    <div>
        <div class="admin-topbar" style="height:56px; padding:0 20px; display:flex; align-items:center; justify-content:space-between;">
            <span class="admin-brand">CMS 后台管理</span>
            <span class="admin-user">
                <!--<span>{{ store.nickname || store.username }}</span>-->
                <span style="position:relative;" @click.stop>
                    <button type="button"
                            aria-haspopup="menu"
                            :aria-expanded="menuVisible"
                            @click="toggleMenu"
                            style="border:0; background:transparent; padding:8px 0; cursor:pointer; color:#606266; display:flex; align-items:center; gap:4px; font:inherit;">
                        系统<i class="fas fa-angle-down"></i>
                    </button>
                    <div v-show="menuVisible"
                         role="menu"
                         style="position:absolute; top:100%; right:0; z-index:3000; min-width:120px; padding:6px 0; background:#fff; border:1px solid #ebeef5; border-radius:4px; box-shadow:0 2px 12px rgba(0,0,0,.12);">
                        <button type="button"
                                role="menuitem"
                                @click="openChangePassword"
                                style="display:block; width:100%; border:0; background:transparent; padding:8px 16px; text-align:left; white-space:nowrap; cursor:pointer; color:#606266; font:inherit;">
                            修改密码
                        </button>
                        <a href="/logout"
                           role="menuitem"
                           style="display:block; border-top:1px solid #ebeef5; padding:8px 16px; white-space:nowrap; color:#606266; text-decoration:none;">
                            退出登录
                        </a>
                    </div>
                </span>
            </span>
        </div>

        <el-dialog v-model="dialogVisible" title="修改密码" width="450px" append-to-body>
            <el-form label-width="90px">
                <el-form-item label="旧密码"><el-input v-model="form.oldPassword" type="password" show-password placeholder="请填写旧密码"></el-input></el-form-item>
                <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password placeholder="请填写新密码"></el-input></el-form-item>
                <el-form-item label="确认密码"><el-input v-model="form.confirmNewPassword" type="password" show-password placeholder="请再次填写新密码"></el-input></el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </template>
        </el-dialog>
    </div>
    `
};
