import { ref } from 'vue';
import { api } from '../api.js';
import { useCommentForm } from '../composables/useCommentForm.js';

export default {
    props: { sid: { type: [Number, String], required: true } },
    setup(props) {
        const comments = ref([]);
        const hasMore = ref(false);
        const nextPage = ref(1);
        const replyingTo = ref(null);
        const userInfoOpen = ref(false);

        async function loadComments(page) {
            const res = await api.getComments({ sid: props.sid, pageNumber: page || 1, pageSize: 10, status: 1 });
            const list = res.records || [];
            comments.value = page && page > 1 ? comments.value.concat(list) : list;
            hasMore.value = !!res.hasNextPage;
            nextPage.value = res.nextPage;
        }
        function loadMore() {
            loadComments(nextPage.value);
        }

        const mainForm = useCommentForm({
            placeholder: '你的评论可以一针见血',
            prefillFromCookie: true,
            alwaysWriteCookie: true,
            submitFn: (p) => api.saveComment({ sid: props.sid, ...p }),
            onSuccess: () => loadComments(1)
        });
        userInfoOpen.value = !mainForm.nickname.value;
        function toggleUserInfo() {
            userInfoOpen.value = !userInfoOpen.value;
        }

        const replyForm = useCommentForm({
            placeholder: '说点什么好呢',
            prefillFromCookie: false, // 打开回复框时手动调用 replyForm.prefill()
            alwaysWriteCookie: false,
            submitFn: (p) => api.saveComment({ sid: props.sid, pid: replyingTo.value, ...p }),
            onSuccess: () => { replyingTo.value = null; loadComments(1); }
        });

        function toggleReply(comment) {
            if (replyingTo.value === comment.id) {
                replyingTo.value = null;
                replyForm.destroyMde();
                return;
            }
            replyingTo.value = comment.id;
            replyForm.prefill();
            replyForm.initMde();
        }

        async function likeComment(comment) {
            const res = await api.love(comment.id, 2);
            if (res.status === 200) comment.loveCount = (comment.loveCount || 0) + 1;
        }

        function scrollToComment(id) {
            const el = document.getElementById('comment-' + id);
            if (el) window.scrollTo({ top: el.offsetTop - 55, behavior: 'smooth' });
        }

        loadComments(1);
        mainForm.initMde();

        return {
            comments, hasMore, loadMore, userInfoOpen, toggleUserInfo,
            mainForm, replyingTo, toggleReply, replyForm, likeComment, scrollToComment
        };
    },
    template: `
    <div id="comment" class="comment comment-main hover-shadow" style="border-radius: 12px;">
        <div class="comment-title">发表评论</div>
        <form class="form-horizontal mt-10" @submit.prevent>
            <div class="user-name-content" v-show="mainForm.nickname.value">欢迎您：<b @click="toggleUserInfo" style="cursor:pointer">{{ mainForm.nickname.value }}</b></div>
            <div class="form-group" v-show="userInfoOpen">
                <div class="col-sm-4">
                    <input v-model="mainForm.nickname.value" type="text" class="form-control" placeholder="昵称(必填)">
                </div>
                <div class="col-sm-4">
                    <input v-model="mainForm.qq.value" @blur="mainForm.onQqBlur" type="text" class="form-control" placeholder="QQ（可获取头像和昵称）">
                </div>
                <div class="col-sm-4">
                    <input v-model="mainForm.email.value" type="text" class="form-control" placeholder="邮箱">
                </div>
            </div>
            <div class="form-group">
                <div class="col-xs-12">
                    <textarea :ref="el => mainForm.mdeEl.value = el"></textarea>
                </div>
            </div>
            <div>
                <button type="button" class="btn btn-pri" @click="mainForm.submit">发表评论</button>
            </div>
        </form>
        <hr class="hr0 mt-15"/>
        <ul id="comment-ul" class="comment">
            <li v-if="comments.length===0" class="no-comment">暂无评论，快来占领宝座</li>
            <li v-for="c in comments" :key="c.id">
                <div class="comment-body" :id="'comment-' + c.id">
                    <div class="comment-user-img">
                        <img :src="c.avatar || '/img/user-default.png'" onerror="this.src='/img/user-default.png'"/>
                    </div>
                    <div class="comment-info">
                        <div class="comment-top">
                            <span class="comment-nickname"><a href="javascript:void(0)">{{ c.nickname }}</a></span>
                            <span class="comment-time">{{ c.createTime }}</span>
                        </div>
                        <div class="comment-content">
                            <div class="comment-parent" v-if="c.parent">
                                <div class="comment-parent-user">
                                    <a class="comment-link" href="javascript:void(0)" @click="scrollToComment(c.parent.id)">@{{ c.parent.nickname }}</a>
                                </div>
                                <div class="comment-parent-content">{{ c.parent.content }}</div>
                            </div>
                            {{ c.content }}
                        </div>
                        <div class="comment-footer">
                            <span class="reply mr-5" @click="toggleReply(c)">{{ replyingTo===c.id ? '取消回复' : '回复' }}</span>
                            <span class="comment-support pointer fa fa-thumbs-o-up" @click="likeComment(c)">{{ c.loveCount || 0 }}</span>
                        </div>
                    </div>
                </div>
                <form v-if="replyingTo===c.id" class="form-horizontal mt-10" @submit.prevent>
                    <div class="form-group" v-show="!replyForm.nickname.value">
                        <div class="col-sm-4"><input v-model="replyForm.nickname.value" type="text" class="form-control" placeholder="昵称(必填)"></div>
                        <div class="col-sm-4"><input v-model="replyForm.qq.value" @blur="replyForm.onQqBlur" type="text" class="form-control" placeholder="QQ（可显示头像和昵称）"></div>
                        <div class="col-sm-4"><input v-model="replyForm.email.value" type="text" class="form-control" placeholder="邮箱"></div>
                    </div>
                    <div class="form-group"><div class="col-xs-12"><textarea :ref="el => replyForm.mdeEl.value = el"></textarea></div></div>
                    <div><button type="button" class="btn btn-primary" @click="replyForm.submit">发表评论</button></div>
                </form>
            </li>
        </ul>
        <div v-if="hasMore" class="comment-more" @click="loadMore">加载更多</div>
    </div>
    `
};
