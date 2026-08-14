package com.thx.module.admin.vo;

/**
 * 角色分配权限时用的权限树节点，供前端"分配权限"对话框渲染成带勾选框的树形控件
 * （见 RoleController#assignRole 的 /role/assign/permission/list 接口）。
 */
public class PermissionTreeListVo {
    /** 权限主键 id */
    private String id;
    /** 权限业务 id */
    private String permissionId;
    /** 权限名称 */
    private String name;
    /** 父级权限 id */
    private String parentId;
    /** 树节点默认是否展开 */
    private Boolean open = true;
    /** 该角色是否已拥有此权限（决定勾选框初始状态） */
    private Boolean checked = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }
}
