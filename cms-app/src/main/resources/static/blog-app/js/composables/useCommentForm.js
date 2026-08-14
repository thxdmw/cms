import { nextTick, ref } from 'vue';

function getCookie(name) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : '';
}
function setCookie(name, value, days) {
    const d = new Date();
    d.setTime(d.getTime() + days * 24 * 60 * 60 * 1000);
    document.cookie = name + '=' + (value || '') + '; expires=' + d.toUTCString() + '; path=/';
}
function fetchQqInfo(qq, cb) {
    // 沿用原来的 JSONP 方式请求 QQ 头像/昵称接口
    window.portraitCallBack = function (json) {
        if (json[qq] === undefined || (json[qq][6] || '').trim() === '') {
            if (window.layer) window.layer.msg('qq信息不存在！');
            return;
        }
        cb({ nickname: json[qq][6], avatar: json[qq][0] });
    };
    const s = document.createElement('script');
    s.src = 'http://r.qzone.qq.com/fcg-bin/cgi_get_portrait.fcg?uins=' + qq + '&callback=portraitCallBack';
    document.body.appendChild(s);
    s.onload = () => s.remove();
}
function finishSubmit(res, onOk) {
    if (window.layer) {
        window.layer.msg(res.msg, { offset: '30%', time: 800 }, function () {
            if (res.status === 200) onOk();
        });
    } else if (res.status === 200) {
        onOk();
    }
}

/**
 * 一个评论表单（主评论 或 单条回复）的完整状态与行为，供 CommentSection.js 分别为
 * 主表单和回复表单各实例化一次，避免两份状态/逻辑几乎一样的代码各写一遍。
 *
 * @param {Object} opts
 * @param {string} opts.placeholder        SimpleMDE 输入框占位文字
 * @param {boolean} opts.prefillFromCookie 创建时是否立即从 cookie 预填昵称/QQ/邮箱（主表单=true；回复表单在 toggleReply 时机手动调用 prefill()）
 * @param {boolean} opts.alwaysWriteCookie 提交成功后是否总是覆盖写 cookie（主表单=true），还是仅当尚无 cookie 时才写（回复=false）
 * @param {Function} opts.submitFn         实际提交函数：({nickname,qq,email,content}) => Promise<res>，由调用方决定要不要带 pid
 * @param {Function} opts.onSuccess        提交成功后的回调（如重新加载评论列表）
 */
export function useCommentForm(opts) {
    const nickname = ref(opts.prefillFromCookie ? getCookie('pb-cms-username') : '');
    const qq = ref(opts.prefillFromCookie ? getCookie('pb-cms-qq') : '');
    const email = ref(opts.prefillFromCookie ? getCookie('pb-cms-email') : '');
    const mdeEl = ref(null);
    let mde = null;

    function prefill() {
        nickname.value = getCookie('pb-cms-username');
        qq.value = getCookie('pb-cms-qq');
        email.value = getCookie('pb-cms-email');
    }

    function onQqBlur() {
        if (!qq.value || nickname.value) return;
        if (isNaN(qq.value)) { if (window.layer) window.layer.msg('qq格式不正确！'); return; }
        fetchQqInfo(qq.value, (info) => { nickname.value = info.nickname; });
    }

    function initMde() {
        return nextTick(() => {
            destroyMde();
            if (mdeEl.value && window.SimpleMDE) {
                mde = new window.SimpleMDE({
                    element: mdeEl.value, toolbar: [], autoDownloadFontAwesome: false,
                    placeholder: opts.placeholder, renderingConfig: { codeSyntaxHighlighting: true }, tabSize: 4, status: false
                });
            }
        });
    }
    function destroyMde() {
        if (mde) { try { mde.toTextArea(); } catch (e) {} mde = null; }
    }

    async function submit() {
        if (!nickname.value) { if (window.layer) window.layer.msg('请输入昵称'); return; }
        const raw = mde ? mde.value() : '';
        if (!raw) { if (window.layer) window.layer.msg('说点什么吧'); return; }
        const content = mde.markdown(raw);
        const res = await opts.submitFn({ nickname: nickname.value, qq: qq.value, email: email.value, content });
        finishSubmit(res, () => {
            if (opts.alwaysWriteCookie || !getCookie('pb-cms-username')) {
                setCookie('pb-cms-username', nickname.value, 30);
                setCookie('pb-cms-qq', qq.value, 30);
                setCookie('pb-cms-email', email.value, 30);
            }
            if (mde) mde.value('');
            opts.onSuccess();
        });
    }

    return { nickname, qq, email, mdeEl, onQqBlur, initMde, destroyMde, submit, prefill };
}
