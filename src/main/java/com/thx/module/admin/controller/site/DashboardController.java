package com.thx.module.admin.controller.site;

import com.thx.common.util.ResultUtil;
import com.thx.module.admin.service.BizStatisticService;
import com.thx.module.admin.vo.StatisticVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台首页仪表盘统计数据接口，对应前端 DashboardView.js 的文章/评论数量、访问趋势图表等。
 */
@RestController
@AllArgsConstructor
public class DashboardController {

    private final BizStatisticService statisticService;

    @GetMapping("/dashboard/statistic")
    @ResponseBody
    public ResponseVo<StatisticVo> statistic() {
        return ResultUtil.success("获取成功", statisticService.indexStatistic());
    }

}
