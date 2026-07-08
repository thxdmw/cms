package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizArticleLook;

import java.util.Date;
import java.util.Map;

/**
 * 文章浏览记录服务：记录文章访问行为，支持短时间内防重复计数，并提供按天维度的浏览量统计。
 */
public interface BizArticleLookService extends IService<BizArticleLook> {

    /**
     * 判断指定 IP 在 lookTime 之后是否已浏览过该文章，用于短时间内的防重复计数（刷量拦截）。
     *
     * @param articleId 文章 id
     * @param userIp    访问者 IP
     * @param lookTime  时间下界（当前时间减去防重复窗口）
     * @return 命中的记录数，大于 0 表示窗口期内已访问过
     */
    int checkArticleLook(String articleId, String userIp, Date lookTime);

    /**
     * 统计最近 day 天每天的文章浏览量，返回结果会补全为连续 day+1 天的日期序列（无数据的日期补 0，
     * 演示模式 {@code CoreConst.ENABLE_DEMO_DATA} 开启时补随机数），避免图表出现日期缺失。
     *
     * @param day 统计天数（最近 N 天）
     * @return key 为日期（yyyy-MM-dd），value 为当天浏览量，按日期升序排列
     */
    Map<String, Integer> lookCountByDay(int day);

    /**
     * 统计最近 day 天每天的浏览记录数，日期补全规则同 {@link #lookCountByDay(int)}。
     * 注意：底层查询与浏览量统计口径相同，并未按用户/IP 去重，并非真正的独立访客数。
     *
     * @param day 统计天数（最近 N 天）
     * @return key 为日期（yyyy-MM-dd），value 为当天记录数，按日期升序排列
     */
    Map<String, Integer> userCountByDay(int day);
}
