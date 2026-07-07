// fetch 封装。注意：后台接口的响应结构不是统一的——
// 列表接口（如 /article/list）裸返回 {rows, total}，没有外层信封；
// 增删改接口返回 {status, msg, data}。这里不强行统一包一层，
// 由调用方根据具体接口自己解构，避免用一个假的"统一格式"掩盖真实的差异。
//
// 会话过期时的处理：fetch 默认会跟随重定向，未登录/会话过期访问受保护接口时，
// Shiro 会把请求重定向到 /login，fetch 拿到的是 200 状态的 /login 页面 HTML，
// 而不是 302——直接 res.json() 会因为解析不了 HTML 而抛异常。这里统一在解析前
// 检查 content-type，不是 JSON 就当作会话已过期处理，跳转登录页，
// 避免每个调用方都要单独处理这个情况
function isSessionExpired(res) {
    const contentType = res.headers.get('content-type') || '';
    return !contentType.includes('application/json');
}

async function parseJsonOrRedirectToLogin(res) {
    if (isSessionExpired(res)) {
        window.location.href = '/login';
        return new Promise(() => {}); // 挂起，不再继续往下走，等浏览器完成跳转
    }
    return res.json();
}

async function postForm(path, params) {
    const body = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
        if (Array.isArray(value)) {
            // 数组参数：调用方传入的 key 决定了具体的编码约定——
            // 传 'tag' 会编码成 tag=1&tag=2（Spring 原生重复同名字段绑定 Integer[]），
            // 传 'ids[]' 会编码成 ids%5B%5D=1&ids%5B%5D=2（@RequestParam("ids[]") 那种写法）
            value.forEach((v) => body.append(key, v));
        } else if (value !== undefined && value !== null) {
            body.append(key, value);
        }
    });
    const res = await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body
    });
    return parseJsonOrRedirectToLogin(res);
}

async function getJson(path, params) {
    const qs = new URLSearchParams();
    Object.entries(params || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null) qs.append(key, value);
    });
    const query = qs.toString();
    const res = await fetch(path + (query ? '?' + query : ''));
    return parseJsonOrRedirectToLogin(res);
}

async function uploadFile(path, file, fieldName, extraFields) {
    const form = new FormData();
    form.append(fieldName, file);
    Object.entries(extraFields || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null) form.append(key, value);
    });
    const res = await fetch(path, { method: 'POST', body: form });
    return parseJsonOrRedirectToLogin(res);
}

// 极少数接口（在线用户批量踢出）用的是 @RequestBody JSON，不是表单编码
async function postJson(path, body) {
    const res = await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
        body: JSON.stringify(body)
    });
    return parseJsonOrRedirectToLogin(res);
}

export const api = {
    // 当前登录用户信息 + 菜单：{status, msg, data}
    getCurrentUser() { return postForm('/currentUser', {}); },
    getMenu() { return postForm('/menu', {}); },

    // 文章：list 是裸 {rows, total}，其余是 {status, msg, data}
    getArticles(params) { return postForm('/article/list', params); },
    getArticleDetail(id) { return getJson('/article/detail', { id }); },
    addArticle(params) { return postForm('/article/add', params); },
    editArticle(params) { return postForm('/article/edit', params); },
    deleteArticle(id) { return postForm('/article/delete', { id }); },
    batchDeleteArticles(ids) { return postForm('/article/batch/delete', { 'ids[]': ids }); },
    changeArticleStatus(id, status) { return postForm('/article/changeArticleStatus', { id, status }); },

    // 分类：List<BizCategory>，裸数组。isFistLevel=false 用于列表页筛选下拉（全部分类平铺，含子分类，
    // 每条数据虽然也带 children 但不使用）；isFistLevel=true 用于新增/编辑表单的分组下拉
    // （只返回顶层分类，每条自带 children，配合 el-option-group 渲染，避免子分类被重复渲染）
    getCategories(isFistLevel) { return postForm('/category/list', { isFistLevel: !!isFistLevel }); },
    addCategory(params) { return postForm('/category/add', params); },
    editCategory(params) { return postForm('/category/edit', params); },
    deleteCategory(id) { return postForm('/category/delete', { id }); },
    batchDeleteCategories(ids) { return postForm('/category/batch/delete', { 'ids[]': ids }); },

    // 标签：/tag/all 是裸 List<BizTags>（不分页，供文章表单用）；/tag/list 是裸 {rows,total}（分页列表页用）
    getAllTags() { return postForm('/tag/all', {}); },
    getTagsPaged(params) { return postForm('/tag/list', params); },
    addTag(params) { return postForm('/tag/add', params); },
    editTag(params) { return postForm('/tag/edit', params); },
    deleteTag(id) { return postForm('/tag/delete', { id }); },
    batchDeleteTags(ids) { return postForm('/tag/batch/delete', { 'ids[]': ids }); },

    // 文章封面/幻灯片图上传：{success, message, url, fileId}，字段名固定 editormd-image-file
    uploadArticleImage(file) { return uploadFile('/attachment/uploadForEditor', file, 'editormd-image-file'); },

    // 评论：list 是裸 {rows,total}；reply/audit 会顺带写入回复评论
    getComments(params) { return postForm('/comment/list', params); },
    replyComment(params) { return postForm('/comment/reply', params); },
    deleteComment(id) { return postForm('/comment/delete', { id }); },
    batchDeleteComments(ids) { return postForm('/comment/batch/delete', { 'ids[]': ids }); },
    auditComment(params) { return postForm('/comment/audit', params); },

    // 用户：list 裸 {rows,total}；assign/role/list 是 {rows: List<Role>, hasRoles: Set<String>}（不是分页信封）
    getUsers(params) { return postForm('/user/list', params); },
    addUser(params) { return postForm('/user/add', params); },
    editUser(params) { return postForm('/user/edit', params); },
    deleteUser(userId) { return postForm('/user/delete', { userId }); },
    batchDeleteUsers(ids) { return postForm('/user/batch/delete', { 'ids[]': ids }); },
    getUserAssignRoleList(userId) { return postForm('/user/assign/role/list', { userId }); },
    saveUserAssignRole(userId, roleIdStr) { return postForm('/user/assign/role', { userId, roleIdStr }); },
    changePassword(params) { return postForm('/user/changePassword', params); },

    // 角色：list 裸 {rows,total}；assign/permission/list 是裸 List<PermissionTreeListVo>（含预计算的 checked）
    getRoles(params) { return postForm('/role/list', params); },
    addRole(params) { return postForm('/role/add', params); },
    editRole(params) { return postForm('/role/edit', params); },
    deleteRole(roleId) { return postForm('/role/delete', { roleId }); },
    batchDeleteRoles(ids) { return postForm('/role/batch/delete', { 'ids[]': ids }); },
    getRoleAssignPermissionList(roleId) { return postForm('/role/assign/permission/list', { roleId }); },
    saveRoleAssignPermission(roleId, permissionIdStr) { return postForm('/role/assign/permission', { roleId, permissionIdStr }); },

    // 权限：list 裸 List<Permission>（flag: '1'=全部, '2'=仅菜单）
    getPermissions(flag) { return postForm('/permission/list', { flag }); },
    addPermission(params) { return postForm('/permission/add', params); },
    editPermission(params) { return postForm('/permission/edit', params); },
    deletePermission(permissionId) { return postForm('/permission/delete', { permissionId }); },

    // 友情链接：list 裸 {rows,total}
    getLinksPaged(params) { return postForm('/link/list', params); },
    addLink(params) { return postForm('/link/add', params); },
    editLink(params) { return postForm('/link/edit', params); },
    deleteLink(id) { return postForm('/link/delete', { id }); },
    batchDeleteLinks(ids) { return postForm('/link/batch/delete', { 'ids[]': ids }); },

    // 主题：list 裸 {rows,total}；use 用于激活某个主题
    getThemesPaged(params) { return postForm('/theme/list', params); },
    addTheme(params) { return postForm('/theme/add', params); },
    editTheme(params) { return postForm('/theme/edit', params); },
    useTheme(id) { return postForm('/theme/use', { id }); },
    deleteTheme(id) { return postForm('/theme/delete', { id }); },
    batchDeleteThemes(ids) { return postForm('/theme/batch/delete', { 'ids[]': ids }); },

    // 在线用户：list 内存分页；batchKickout 是 @RequestBody JSON（跟其它接口不同）
    getOnlineUsers(params) { return postForm('/online/user/list', params); },
    kickoutUser(sessionId, username) { return postForm('/online/user/kickout', { sessionId, username }); },
    batchKickoutUsers(sessions) { return postJson('/online/user/batch/kickout', sessions); },

    // 站点设置
    getSiteInfo() { return getJson('/siteinfo/detail', {}); },
    editSiteInfo(params) { return postForm('/siteinfo/edit', params); },

    // 日志
    searchLogs(params) { return getJson('/log/search', params); },

    // 工作台统计
    getDashboardStatistic() { return getJson('/dashboard/statistic', {}); },

    // 服务器文件：list 裸 {rows,total}；下载不走这里，前端直接 window.open('/serverFile/download?id=')
    getServerFiles(params) { return postForm('/serverFile/list', params); },
    uploadServerFile(file, remark) { return uploadFile('/serverFile/upload', file, 'file', { remark }); },
    previewServerFile(id) { return getJson('/serverFile/preview', { id }); },
    saveServerFile(id, content) { return postForm('/serverFile/save', { id, content }); },
    deleteServerFile(id) { return postForm('/serverFile/delete', { id }); },
    batchDeleteServerFiles(ids) { return postForm('/serverFile/batch/delete', { 'ids[]': ids }); }
};
