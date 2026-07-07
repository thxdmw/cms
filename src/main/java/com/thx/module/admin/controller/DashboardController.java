package com.thx.module.admin.controller;

import com.thx.common.util.ResultUtil;
import com.thx.module.admin.service.BizStatisticService;
import com.thx.module.admin.vo.StatisticVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台工作台（首页仪表盘）统计数据
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
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
