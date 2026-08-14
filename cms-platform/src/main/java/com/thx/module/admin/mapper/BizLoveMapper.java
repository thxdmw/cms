package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizLove;
import org.apache.ibatis.annotations.Param;

/**
 * 点赞记录 Mapper，记录用户/游客对文章、评论等业务对象（由 bizId + bizType 标识）的点赞行为。
 */
public interface BizLoveMapper extends BaseMapper<BizLove> {

    /**
     * 根据业务对象 id 和用户 IP 查询点赞记录，用于判断该 IP 是否已对该业务对象点过赞（防重复点赞）。
     *
     * @param bizId  业务对象 id（如文章 id、评论 id）
     * @param userIp 用户 IP
     * @return 命中的点赞记录；未点过赞则返回 null
     */
    BizLove checkLove(@Param("bizId") String bizId, @Param("userIp") String userIp);
}
