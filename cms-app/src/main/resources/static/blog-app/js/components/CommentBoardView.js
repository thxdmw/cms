import { onMounted } from 'vue';
import { applyDocumentMeta, store } from '../store.js';
import CommentSection from './CommentSection.js';

export default {
    components: { CommentSection },
    setup() {
        onMounted(() => applyDocumentMeta({ title: store.siteConfig.SITE_NAME }));
        return {};
    },
    template: `
    <div class="pb-main comment">
        <div class="comment-describe hover-shadow" style="border-radius: 12px;">
            <h3 class="comment-describe-title">留言板</h3>
            <div>
                <p>想成为一个强大的程序员吗？</p>
                <p>还不快来一脚？</p>
            </div>
            <hr class="hr0">
        </div>
        <comment-section :sid="-1"></comment-section>
    </div>
    `
};
