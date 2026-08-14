// 轻量 fetch 封装，不引入 axios。所有函数返回后端 ResponseVo 结构（status/msg/data）或 IPage 结构。

async function postForm(path, params) {
    const body = new URLSearchParams();
    if (params) {
        Object.keys(params).forEach((k) => {
            if (params[k] !== undefined && params[k] !== null) {
                body.append(k, params[k]);
            }
        });
    }
    const res = await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body
    });
    return res.json();
}

async function getJson(path, params) {
    let qs = '';
    if (params) {
        const usp = new URLSearchParams();
        Object.keys(params).forEach((k) => {
            if (params[k] !== undefined && params[k] !== null) {
                usp.append(k, params[k]);
            }
        });
        const s = usp.toString();
        if (s) qs = '?' + s;
    }
    const res = await fetch(path + qs);
    return res.json();
}

export const api = {
    // Phase 2 新增的只读接口
    getArticles(params) { return getJson('/blog/api/articles', params); },
    getArticle(id) { return getJson(`/blog/api/articles/${id}`); },
    getSlider() { return getJson('/blog/api/slider'); },
    getCategory(id) { return getJson(`/blog/api/category/${id}`); },
    getCommonData() { return getJson('/blog/api/common-data'); },

    // 原有接口，原样复用
    getComments(params) { return postForm('/blog/api/comments', params); },
    saveComment(params) { return postForm('/blog/api/comment/save', params); },
    articleLook(articleId) { return postForm('/blog/api/article/look', { articleId }); },
    love(bizId, bizType) { return postForm('/blog/api/love', { bizId, bizType }); },
    search(keyword) { return postForm('/blog/api/search', { keyword }); }
};
