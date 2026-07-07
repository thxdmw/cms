package com.thx.module.agent.service;

import java.util.Map;

/**
 * 系统资源监控服务
 * 负责获取CPU、内存、磁盘等系统资源使用情况
 */
public interface SystemMonitorService {

    /**
     * 获取系统资源使用概况
     * @return 包含CPU、内存、磁盘等信息的Map
     */
    Map<String, Object> getSystemOverview();

    /**
     * 获取CPU使用率
     * @return CPU使用率百分比
     */
    double getCpuUsage();

    /**
     * 获取内存使用情况
     * @return 包含总内存、已用内存、可用内存等信息的Map
     */
    Map<String, Object> getMemoryInfo();

    /**
     * 获取磁盘使用情况
     * @return 包含磁盘总空间、已用空间、可用空间等信息的Map
     */
    Map<String, Object> getDiskInfo();

    /**
     * 获取JVM信息
     * @return 包含JVM堆内存、非堆内存、GC等信息的Map
     */
    Map<String, Object> getJvmInfo();

    /**
     * 获取线程信息
     * @return 包含线程总数、活跃线程数、守护线程数等信息的Map
     */
    Map<String, Object> getThreadInfo();
}
