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
 * Created with IntelliJ IDEA.
 *
 * @author tanghaixin
 * @date 2021/1/12 3:35 下午
 */
@Service
@AllArgsConstructor
public class BizStatisticServiceImpl implements BizStatisticService {

    private final BizArticleService articleService;
    private final BizCommentService commentService;
    private final BizArticleLookService articleLookService;

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
