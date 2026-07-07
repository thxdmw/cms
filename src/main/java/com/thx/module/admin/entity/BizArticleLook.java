package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizArticleLook extends BaseVo {
    private static final long serialVersionUID = 1052723347580827581L;

    private String articleId;
    private String userId;
    private String userIp;
    private Date lookTime;

}
