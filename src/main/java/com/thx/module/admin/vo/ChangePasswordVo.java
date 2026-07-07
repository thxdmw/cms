package com.thx.module.admin.vo;

import lombok.Data;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
public class ChangePasswordVo {

    String oldPassword;
    String newPassword;
    String confirmNewPassword;

}
