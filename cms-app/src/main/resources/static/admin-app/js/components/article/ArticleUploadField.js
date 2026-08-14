import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../api.js';

// 替代原来基于 WebUploader（含 Flash .swf 兜底，浏览器早已不支持 Flash）的上传控件，
// 直接调用既有的 /attachment/uploadForEditor 接口
export default {
    props: {
        modelValue: { type: String, default: '' },
        placeholder: { type: String, default: '图片地址' }
    },
    emits: ['update:modelValue'],
    setup(props, { emit }) {
        const fileInput = ref(null);
        const uploading = ref(false);

        function pickFile() {
            fileInput.value && fileInput.value.click();
        }

        async function onFileChange(e) {
            const file = e.target.files && e.target.files[0];
            e.target.value = ''; // 允许连续选择同一个文件也能触发 change
            if (!file) return;
            uploading.value = true;
            try {
                const res = await api.uploadArticleImage(file);
                if (res && res.success) {
                    emit('update:modelValue', res.url);
                } else {
                    ElMessage.error((res && res.message) || '上传失败');
                }
            } finally {
                uploading.value = false;
            }
        }

        return { fileInput, uploading, pickFile, onFileChange };
    },
    template: `
    <div style="display:flex; gap:8px; align-items:flex-start;">
        <el-input :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
                  :placeholder="placeholder"></el-input>
        <el-button :loading="uploading" @click="pickFile"><i class="fa fa-cloud-upload-alt"></i></el-button>
        <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange">
        <el-image v-if="modelValue" :src="modelValue" :preview-src-list="[modelValue]" preview-teleported
                  style="width:50px;height:50px;flex-shrink:0" fit="cover"></el-image>
    </div>
    `
};
