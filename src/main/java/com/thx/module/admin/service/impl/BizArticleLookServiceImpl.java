package com.thx.module.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.CoreConst;
import com.thx.common.util.DateUtil;
import com.thx.module.admin.mapper.BizArticleLookMapper;
import com.thx.module.admin.entity.BizArticleLook;
import com.thx.module.admin.service.BizArticleLookService;
import com.thx.module.admin.vo.CountVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link BizArticleLookService} 实现：文章浏览记录的写入与按天统计（供仪表盘访问趋势图使用）。
 */
@Service
@AllArgsConstructor
public class BizArticleLookServiceImpl extends ServiceImpl<BizArticleLookMapper, BizArticleLook> implements BizArticleLookService {

    private final BizArticleLookMapper articleLookMapper;

    @Override
    public int checkArticleLook(String articleId, String userIp, Date lookTime) {
        return articleLookMapper.checkArticleLook(articleId, userIp, lookTime);
    }

    @Override
    public Map<String, Integer> lookCountByDay(int day) {
        List<CountVo> list = articleLookMapper.lookCountByDay(day);
        Map<String, Integer> lookCountByDayMap = buildRecentDayMap(day + 1);
        if (CollUtil.isNotEmpty(list)) {
            lookCountByDayMap.putAll(list.stream().collect(Collectors.toMap(CountVo::getDay, CountVo::getCount)));
        }
        return lookCountByDayMap;
    }

    @Override
    public Map<String, Integer> userCountByDay(int day) {
        List<CountVo> list = articleLookMapper.userCountByDay(day);
        Map<String, Integer> userCountByDayMap = buildRecentDayMap(day + 1);
        if (CollUtil.isNotEmpty(list)) {
            userCountByDayMap.putAll(list.stream().collect(Collectors.toMap(CountVo::getDay, CountVo::getCount)));
        }
        return userCountByDayMap;
    }

    /**
     * 构造最近 day 天、日期从早到晚有序的统计 Map，默认值为 0（或演示环境下的随机数）；
     * 调用方再用真实统计数据覆盖有记录的日期，保证图表横轴每天都有点，不会因为某天没数据就断线
     */
    private static Map<String, Integer> buildRecentDayMap(int day) {
        Date now = new Date();
        Map<String, Integer> map = MapUtil.newHashMap(true);
        for (int i = day - 1; i >= 0; i--) {
            int count = CoreConst.ENABLE_DEMO_DATA ? RandomUtil.randomInt(20, 100) : 0;
            map.put(DateUtil.format(DateUtil.addDays(now, -i), DateUtil.webFormat), count);
        }
        return map;
    }
}
