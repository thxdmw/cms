package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizLove;

/**
 * 点赞服务，记录用户/游客对文章、评论等业务对象的点赞行为。
 */
public interface BizLoveService extends IService<BizLove> {

    /**
     * 根据业务对象 id 和用户 IP 查询点赞记录，用于判断该 IP 是否已对该业务对象点过赞。
     *
     * @param bizId  业务对象 id（如文章 id、评论 id）
     * @param userIp 用户 IP
     * @return 命中的点赞记录；未点过赞则返回 null
     */
    BizLove checkLove(String bizId, String userIp);
}
