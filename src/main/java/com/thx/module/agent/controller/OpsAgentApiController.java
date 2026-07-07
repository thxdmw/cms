package com.thx.module.agent.controller;

import com.thx.common.annotation.AnonymousAccess;
import com.thx.common.util.ResultUtil;
import com.thx.exception.ApiException;
import com.thx.module.agent.service.DatabaseMonitorService;
import com.thx.module.agent.service.LogService;
import com.thx.module.agent.service.SystemMonitorService;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Ops Agent API控制器
 * 为运维Agent提供系统监控、日志分析、数据库性能等接口
 */
@Slf4j
@RestController
@RequestMapping("/agent/api/ops/")
@AllArgsConstructor
public class OpsAgentApiController {

    @Resource
    private SystemMonitorService systemMonitorService;

    @Resource
    private DatabaseMonitorService databaseMonitorService;

    @Resource
    private LogService logService;

    @GetMapping("/system/overview")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getSystemOverview() {
        try {
            Map<String, Object> overview = systemMonitorService.getSystemOverview();
            return ResultUtil.success("获取系统概览成功", overview);
        } catch (Exception e) {
            log.error("获取系统概览失败", e);
            throw new ApiException("获取系统概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/system/cpu")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getCpuUsage() {
        try {
            double cpuUsage = systemMonitorService.getCpuUsage();
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("cpuUsage", cpuUsage);
            result.put("unit", "%");
            return ResultUtil.success("获取CPU使用率成功", result);
        } catch (Exception e) {
            log.error("获取CPU使用率失败", e);
            throw new ApiException("获取CPU使用率失败: " + e.getMessage());
        }
    }

    @GetMapping("/system/memory")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getMemoryInfo() {
        try {
            Map<String, Object> memoryInfo = systemMonitorService.getMemoryInfo();
            return ResultUtil.success("获取内存信息成功", memoryInfo);
        } catch (Exception e) {
            log.error("获取内存信息失败", e);
            throw new ApiException("获取内存信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/system/disk")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getDiskInfo() {
        try {
            Map<String, Object> diskInfo = systemMonitorService.getDiskInfo();
            return ResultUtil.success("获取磁盘信息成功", diskInfo);
        } catch (Exception e) {
            log.error("获取磁盘信息失败", e);
            throw new ApiException("获取磁盘信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/system/jvm")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getJvmInfo() {
        try {
            Map<String, Object> jvmInfo = systemMonitorService.getJvmInfo();
            return ResultUtil.success("获取JVM信息成功", jvmInfo);
        } catch (Exception e) {
            log.error("获取JVM信息失败", e);
            throw new ApiException("获取JVM信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/system/threads")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getThreadInfo() {
        try {
            Map<String, Object> threadInfo = systemMonitorService.getThreadInfo();
            return ResultUtil.success("获取线程信息成功", threadInfo);
        } catch (Exception e) {
            log.error("获取线程信息失败", e);
            throw new ApiException("获取线程信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/database/overview")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getDatabaseOverview() {
        try {
            Map<String, Object> overview = databaseMonitorService.getDataSourceOverview();
            return ResultUtil.success("获取数据库概览成功", overview);
        } catch (Exception e) {
            log.error("获取数据库概览失败", e);
            throw new ApiException("获取数据库概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/database/slow-sql")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getSlowSqlList(@RequestParam(defaultValue = "10") int topN) {
        try {
            int limit = Math.min(topN, 100);
            List<Map<String, Object>> slowSqlList = databaseMonitorService.getSlowSqlList(limit);
            return ResultUtil.success("获取慢SQL列表成功", slowSqlList);
        } catch (Exception e) {
            log.error("获取慢SQL列表失败", e);
            throw new ApiException("获取慢SQL列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/database/sql-statistics")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getSqlStatistics() {
        try {
            Map<String, Object> statistics = databaseMonitorService.getSqlStatistics();
            return ResultUtil.success("获取SQL统计成功", statistics);
        } catch (Exception e) {
            log.error("获取SQL统计失败", e);
            throw new ApiException("获取SQL统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/database/info")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getDatabaseInfo() {
        try {
            Map<String, Object> dbInfo = databaseMonitorService.getDatabaseInfo();
            return ResultUtil.success("获取数据库信息成功", dbInfo);
        } catch (Exception e) {
            log.error("获取数据库信息失败", e);
            throw new ApiException("获取数据库信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/log/statistics")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getLogStatistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date endTime) {
        try {
            Map<String, Object> statistics = logService.getLogStatistics(startTime, endTime);
            return ResultUtil.success("获取日志统计成功", statistics);
        } catch (Exception e) {
            log.error("获取日志统计失败", e);
            throw new ApiException("获取日志统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/log/recent-errors")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getRecentErrors(@RequestParam(defaultValue = "10") int count) {
        try {
            int limit = Math.min(count, 100);
            List<Map<String, Object>> recentErrors = logService.getRecentErrors(limit);
            return ResultUtil.success("获取最近错误日志成功", recentErrors);
        } catch (Exception e) {
            log.error("获取最近错误日志失败", e);
            throw new ApiException("获取最近错误日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/log/search")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo searchLogs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date endTime,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Map<String, Object> result = logService.searchLogs(startTime, endTime, logLevel, keyword, page, size);
            return ResultUtil.success("搜索日志成功", result);
        } catch (Exception e) {
            log.error("搜索日志失败", e);
            throw new ApiException("搜索日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/health/report")
    @ResponseBody
    @AnonymousAccess
    public ResponseVo getHealthReport() {
        try {
            Map<String, Object> report = new java.util.HashMap<>();
            report.put("system", systemMonitorService.getSystemOverview());
            report.put("database", databaseMonitorService.getDataSourceOverview());
            
            Calendar cal = Calendar.getInstance();
            Date endTime = cal.getTime();
            cal.add(Calendar.HOUR_OF_DAY, -24);
            Date startTime = cal.getTime();
            report.put("logs", logService.getLogStatistics(startTime, endTime));
            
            String healthStatus = evaluateHealthStatus(report);
            report.put("healthStatus", healthStatus);
            
            return ResultUtil.success("获取健康报告成功", report);
        } catch (Exception e) {
            log.error("获取健康报告失败", e);
            throw new ApiException("获取健康报告失败: " + e.getMessage());
        }
    }

    private String evaluateHealthStatus(Map<String, Object> report) {
        try {
            Double cpuUsage = (Double) ((Map<?, ?>) report.get("system")).get("cpu");
            if (cpuUsage != null && cpuUsage > 90) {
                return "CRITICAL";
            }
            
            Map<?, ?> memory = (Map<?, ?>) ((Map<?, ?>) report.get("system")).get("memory");
            if (memory != null) {
                Double memUsage = (Double) memory.get("usagePercent");
                if (memUsage != null && memUsage > 90) {
                    return "CRITICAL";
                }
            }
            
            Map<?, ?> database = (Map<?, ?>) report.get("database");
            if (database != null) {
                Integer activeCount = (Integer) database.get("activeCount");
                Integer maxActive = (Integer) database.get("maxActive");
                if (activeCount != null && maxActive != null && maxActive > 0) {
                    double dbUsage = (double) activeCount / maxActive * 100;
                    if (dbUsage > 90) {
                        return "WARNING";
                    }
                }
            }
            
            Map<?, ?> logs = (Map<?, ?>) report.get("logs");
            if (logs != null) {
                Double errorPercent = (Double) logs.get("errorPercent");
                if (errorPercent != null && errorPercent > 10) {
                    return "WARNING";
                }
            }
            
            return "HEALTHY";
        } catch (Exception e) {
            log.warn("评估健康状态失败", e);
            return "UNKNOWN";
        }
    }
}
