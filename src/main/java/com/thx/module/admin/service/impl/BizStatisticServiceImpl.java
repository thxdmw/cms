package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.thx.module.admin.entity.BizArticleLook;
import com.thx.module.admin.service.BizArticleLookService;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.service.BizCommentService;
import com.thx.module.admin.service.BizStatisticService;
import com.thx.module.admin.vo.StatisticVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 后台首页统计服务实现，组合调用文章、评论、浏览记录三个服务汇总统计数据。
 */
@Service
@AllArgsConstructor
public class BizStatisticServiceImpl implements BizStatisticService {

    private final BizArticleService articleService;
    private final BizCommentService commentService;
    private final BizArticleLookService articleLookService;

    /**
     * 汇总文章总数、评论总数、总浏览量、独立访客数（按 user_ip 去重计数），
     * 以及最近 6 天的每日浏览量与每日访问记录数趋势。
     */
    @Override
    public StatisticVo indexStatistic() {
        long articleCount = articleService.count();
        long commentCount = commentService.count();
        long lookCount = articleLookService.count();
        long userCount = articleLookService.count(Wrappers.<BizArticleLook>query().select("DISTINCT user_ip"));
        int recentDays = 6;
        Map<String, Integer> lookCountByDay = articleLookService.lookCountByDay(recentDays);
        Map<String, Integer> userCountByDay = articleLookService.userCountByDay(recentDays);
        return StatisticVo.builder().articleCount(articleCount).commentCount(commentCount).lookCount(lookCount).userCount(userCount).lookCountByDay(lookCountByDay).userCountByDay(userCountByDay).build();
    }
}
