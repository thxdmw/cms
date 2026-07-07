package com.thx.module.agent.service;

import java.util.List;
import java.util.Map;

/**
 * 数据库性能监控服务
 * 负责获取Druid连接池状态、慢SQL等信息
 */
public interface DatabaseMonitorService {

    /**
     * 获取数据库连接池概况
     * @return 包含连接池状态、活跃连接数等信息的Map
     */
    Map<String, Object> getDataSourceOverview();

    /**
     * 获取慢SQL列表
     * @param topN 返回前N条最慢的SQL
     * @return 慢SQL列表
     */
    List<Map<String, Object>> getSlowSqlList(int topN);

    /**
     * 获取SQL执行统计
     * @return SQL执行的统计数据
     */
    Map<String, Object> getSqlStatistics();

    /**
     * 获取当前活跃的连接信息
     * @return 活跃连接列表
     */
    List<Map<String, Object>> getActiveConnections();

    /**
     * 获取数据库基本信息
     * @return 数据库版本、URL等信息
     */
    Map<String, Object> getDatabaseInfo();
}
