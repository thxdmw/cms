package com.thx.module.admin.vo;

import lombok.Data;

/**
 * 修改密码表单，供 {@link com.thx.module.admin.service.UserService#changePassword} 使用。
 */
@Data
public class ChangePasswordVo {

    /** 旧密码（明文，用于校验身份） */
    String oldPassword;
    /** 新密码（明文，保存时会加盐加密） */
    String newPassword;
    /** 确认新密码，需要和 newPassword 一致 */
    String confirmNewPassword;

}
