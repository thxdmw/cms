// 把带 parentId 的扁平列表在客户端组装成树。菜单树（store.js）和分类树（CategoryListView.js）
// 都是同样的"扁平列表 + 父子字段"结构，抽成共用函数，避免同样的分组逻辑写两遍。
export function buildTree(flatList, { idKey = 'id', parentIdKey = 'parentId', topLevelValue = 0 } = {}) {
    const byParent = new Map();
    (flatList || []).forEach((item) => {
        const key = item[parentIdKey];
        const list = byParent.get(key) || [];
        list.push(item);
        byParent.set(key, list);
    });
    function attachChildren(list) {
        (list || []).forEach((item) => {
            item.children = byParent.get(item[idKey]) || [];
            attachChildren(item.children);
        });
    }
    const roots = byParent.get(topLevelValue) || [];
    attachChildren(roots);
    return roots;
}
