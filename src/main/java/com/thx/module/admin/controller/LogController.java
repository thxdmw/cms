package com.thx.module.admin.controller;

import com.thx.module.agent.service.LogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * @author tanghaixin
 * 系统日志接口
 * 注：原来的 /log/page（渲染日志页面）已删除，该页面已经迁移到 Vue SPA 的 /admin/log/page，
 * 由 AdminWebController 统一重定向过去；本类只保留真正的数据接口（/search）。
 */
@Controller
@RequestMapping("/log")
public class LogController {

    @Resource
    private LogService logService;

    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> searchLogs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date endTime,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {

        // 使用文件日志服务查询日志文件
        return logService.searchLogs(startTime, endTime, logLevel, keyword, page, size);
    }

}
