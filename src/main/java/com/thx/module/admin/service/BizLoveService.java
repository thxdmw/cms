package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizLove;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizLoveService extends IService<BizLove> {

    BizLove checkLove(String bizId, String userIp);
}
