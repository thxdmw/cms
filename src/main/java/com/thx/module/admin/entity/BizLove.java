package com.thx.module.admin.entity;


import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizLove extends BaseVo {
    private static final long serialVersionUID = 6825108677279625433L;

    private String bizId;
    private Integer bizType;
    private String userId;
    private String userIp;
    private Integer status;

}
