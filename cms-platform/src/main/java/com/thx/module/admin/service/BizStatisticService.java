package com.thx.module.admin.service;

import com.thx.module.admin.vo.StatisticVo;

/**
 * 后台首页统计服务，汇总文章、评论、浏览量等维度的整体数据供仪表盘展示。
 */
public interface BizStatisticService {

    /**
     * 查询后台首页统计数据：文章总数、评论总数、总浏览量、独立访客数（按 user_ip 去重），
     * 以及最近若干天的每日浏览量、每日访问记录数趋势。
     *
     * @return 统计结果 VO
     */
    StatisticVo indexStatistic();
}
