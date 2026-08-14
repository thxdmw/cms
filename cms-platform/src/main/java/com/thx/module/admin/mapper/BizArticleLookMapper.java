package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizArticleLook;
import com.thx.module.admin.vo.CountVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 文章浏览记录 Mapper，记录用户/游客对文章的访问行为，用于短时间防重复计数和浏览量统计。
 */
public interface BizArticleLookMapper extends BaseMapper<BizArticleLook> {

    /**
     * 统计指定文章在 lookTime 之后、来自同一 IP 的浏览记录数，用于判断该 IP 是否在防重复窗口期内已访问过（防刷量）。
     *
     * @param articleId 文章 id
     * @param userIp    访问者 IP
     * @param lookTime  时间下界，调用方通常传入"当前时间减去防重复窗口"
     * @return 命中的记录数，大于 0 表示窗口期内已访问过
     */
    int checkArticleLook(@Param("articleId") String articleId, @Param("userIp") String userIp, @Param("lookTime") Date lookTime);

    /**
     * 统计最近 day 天内，每天的文章浏览次数（按天分组 count(1)）。
     *
     * @param day 统计天数（最近 N 天）
     * @return 按天分组的浏览量列表
     */
    List<CountVo> lookCountByDay(int day);

    /**
     * 统计最近 day 天内每天的浏览记录数。
     * 注意：当前实现与 {@link #lookCountByDay(int)} 的 SQL 完全相同（按天 count(1)），
     * 并未按 user_id/user_ip 去重，因此返回的并非独立访客数，命名与实际口径存在差异，使用时需留意。
     *
     * @param day 统计天数（最近 N 天）
     * @return 按天分组的记录数列表
     */
    List<CountVo> userCountByDay(int day);
}
