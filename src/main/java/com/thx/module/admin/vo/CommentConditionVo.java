package com.thx.module.admin.vo;

import lombok.Data;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
public class CommentConditionVo {
    private String userId;
    private String sid;
    private String pid;
    private String qq;
    private String email;
    private String url;
    private Integer status;

}

