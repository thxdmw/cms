package com.thx.module.agent.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidStatManagerFacade;
import com.thx.module.agent.service.DatabaseMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

/**
 * {@link DatabaseMonitorService} 的默认实现。
 * 底层依赖项目实际使用的连接池 {@code DruidDataSource} 及其自带的 {@link DruidStatManagerFacade}
 * 统计数据（连接池状态、SQL 执行/慢 SQL 统计等）；如果注入的 {@link DataSource} 不是 Druid 实现，
 * 相关统计方法会退化为返回空结果或带 error 字段的 Map，而不是抛异常，避免把监控接口本身变成故障点。
 */
@Service
public class DatabaseMonitorServiceImpl implements DatabaseMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMonitorServiceImpl.class);

    @Autowired
    private DataSource dataSource;

    /**
     * 连接池概况：活跃/空闲连接数、连接数配置、使用率，以及 Druid 汇总的 SQL 执行统计（次数/错误数/提交/回滚）
     */
    @Override
    public Map<String, Object> getDataSourceOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            if (dataSource instanceof DruidDataSource) {
                DruidDataSource druidDataSource = (DruidDataSource) dataSource;
                
                // 连接池基本信息
                overview.put("activeCount", druidDataSource.getActiveCount());
                overview.put("poolingCount", druidDataSource.getPoolingCount());
                overview.put("connectCount", druidDataSource.getConnectCount());
                overview.put("closeCount", druidDataSource.getCloseCount());
                overview.put("waitThreadCount", druidDataSource.getWaitThreadCount());
                overview.put("notEmptyWaitCount", druidDataSource.getNotEmptyWaitCount());
                overview.put("notEmptyWaitMillis", druidDataSource.getNotEmptyWaitMillis());
                
                // 配置信息
                overview.put("initialSize", druidDataSource.getInitialSize());
                overview.put("minIdle", druidDataSource.getMinIdle());
                overview.put("maxActive", druidDataSource.getMaxActive());
                overview.put("maxWait", druidDataSource.getMaxWait());
                
                // 使用率
                int maxActive = druidDataSource.getMaxActive();
                int activeCount = druidDataSource.getActiveCount();
                double usagePercent = maxActive > 0 ? (double) activeCount / maxActive * 100 : 0;
                overview.put("usagePercent", NumberUtil.round(usagePercent, 2).doubleValue());
                
                // SQL执行统计
                DruidStatManagerFacade statManager = DruidStatManagerFacade.getInstance();
                Map<String, Object> sqlStats = new HashMap<>();
                
                // 获取数据源监控数据
                List<Map<String, Object>> dataSourceStats = statManager.getDataSourceStatDataList();
                if (dataSourceStats != null && !dataSourceStats.isEmpty()) {
                    Map<String, Object> dsStat = dataSourceStats.get(0);
                    sqlStats.put("executeCount", dsStat.getOrDefault("ExecuteCount", 0));
                    sqlStats.put("errorCount", dsStat.getOrDefault("ErrorCount", 0));
                    sqlStats.put("commitCount", dsStat.getOrDefault("CommitCount", 0));
                    sqlStats.put("rollbackCount", dsStat.getOrDefault("RollbackCount", 0));
                } else {
                    sqlStats.put("executeCount", 0);
                    sqlStats.put("errorCount", 0);
                    sqlStats.put("commitCount", 0);
                    sqlStats.put("rollbackCount", 0);
                }
                overview.put("sqlStatistics", sqlStats);
                
            } else {
                logger.warn("数据源不是DruidDataSource类型，无法获取详细监控信息");
                overview.put("error", "数据源类型不支持");
            }
            
        } catch (Exception e) {
            logger.error("获取数据源概览失败", e);
            overview.put("error", e.getMessage());
        }
        
        return overview;
    }

    /**
     * 慢 SQL 列表：从 Druid 的 SQL 统计中按平均执行时间降序取前 {@code topN} 条
     */
    @Override
    public List<Map<String, Object>> getSlowSqlList(int topN) {
        List<Map<String, Object>> slowSqlList = new ArrayList<>();
        
        try {
            DruidStatManagerFacade statManager = DruidStatManagerFacade.getInstance();
            
            // 获取慢SQL统计
            List<Map<String, Object>> sqlStatList = statManager.getSqlStatDataList(null);
            
            if (sqlStatList != null && !sqlStatList.isEmpty()) {
                // 按平均执行时间排序
                sqlStatList.sort((a, b) -> {
                    double avgA = getDoubleValue(a, "AvgExecuteTime");
                    double avgB = getDoubleValue(b, "AvgExecuteTime");
                    return Double.compare(avgB, avgA); // 降序
                });
                
                // 取前N条
                int limit = Math.min(topN, sqlStatList.size());
                for (int i = 0; i < limit; i++) {
                    Map<String, Object> sqlStat = sqlStatList.get(i);
                    Map<String, Object> slowSql = new HashMap<>();
                    
                    slowSql.put("sql", sqlStat.get("SQL"));
                    slowSql.put("executeCount", sqlStat.get("ExecuteCount"));
                    slowSql.put("totalTime", sqlStat.get("TotalTime"));
                    slowSql.put("avgTime", sqlStat.get("AvgExecuteTime"));
                    slowSql.put("maxTime", sqlStat.get("MaxTimespan"));
                    slowSql.put("errorCount", sqlStat.get("ErrorCount"));
                    slowSql.put("fetchRowCount", sqlStat.get("FetchRowCount"));
                    slowSql.put("inTransactionCount", sqlStat.get("InTransactionCount"));
                    
                    slowSqlList.add(slowSql);
                }
            }
            
        } catch (Exception e) {
            logger.error("获取慢SQL列表失败", e);
        }
        
        return slowSqlList;
    }

    /**
     * SQL 执行统计：总执行/错误/提交/回滚次数、错误率，以及执行次数最多的那条 SQL
     */
    @Override
    public Map<String, Object> getSqlStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            DruidStatManagerFacade statManager = DruidStatManagerFacade.getInstance();
            
            // 获取数据源监控数据
            List<Map<String, Object>> dataSourceStats = statManager.getDataSourceStatDataList();
            if (dataSourceStats != null && !dataSourceStats.isEmpty()) {
                Map<String, Object> dsStat = dataSourceStats.get(0);
                
                long executeCount = getLongValue(dsStat, "ExecuteCount");
                long errorCount = getLongValue(dsStat, "ErrorCount");
                long commitCount = getLongValue(dsStat, "CommitCount");
                long rollbackCount = getLongValue(dsStat, "RollbackCount");
                
                // 总体统计
                statistics.put("executeCount", executeCount);
                statistics.put("errorCount", errorCount);
                statistics.put("commitCount", commitCount);
                statistics.put("rollbackCount", rollbackCount);
                
                // 计算错误率
                double errorRate = executeCount > 0 ? (double) errorCount / executeCount * 100 : 0;
                statistics.put("errorRate", NumberUtil.round(errorRate, 2).doubleValue());
            } else {
                statistics.put("executeCount", 0);
                statistics.put("errorCount", 0);
                statistics.put("commitCount", 0);
                statistics.put("rollbackCount", 0);
                statistics.put("errorRate", 0.0);
            }
            
            // SQL类型统计
            List<Map<String, Object>> sqlStatList = statManager.getSqlStatDataList(null);
            if (sqlStatList != null) {
                statistics.put("totalSqlTypes", sqlStatList.size());
                
                // 找出最频繁的SQL
                if (!sqlStatList.isEmpty()) {
                    Map<String, Object> mostFrequent = sqlStatList.stream()
                        .max((a, b) -> Long.compare(
                            getLongValue(a, "ExecuteCount"), 
                            getLongValue(b, "ExecuteCount")
                        ))
                        .orElse(new HashMap<>());
                    
                    statistics.put("mostFrequentSql", mostFrequent.get("SQL"));
                    statistics.put("mostFrequentSqlCount", mostFrequent.get("ExecuteCount"));
                }
            }
            
        } catch (Exception e) {
            logger.error("获取SQL统计失败", e);
        }
        
        return statistics;
    }

    /**
     * 活跃连接信息。注意 Druid 并不直接暴露每条连接的详细信息，这里只能返回连接池的整体状态
     * （活跃数/空闲数/累计建连数/累计关闭数）作为近似
     */
    @Override
    public List<Map<String, Object>> getActiveConnections() {
        List<Map<String, Object>> activeConnections = new ArrayList<>();
        
        try {
            if (dataSource instanceof DruidDataSource) {
                DruidDataSource druidDataSource = (DruidDataSource) dataSource;
                
                // 注意：Druid不直接提供活跃连接的详细信息
                // 这里返回连接池的基本状态
                Map<String, Object> connectionInfo = new HashMap<>();
                connectionInfo.put("activeCount", druidDataSource.getActiveCount());
                connectionInfo.put("poolingCount", druidDataSource.getPoolingCount());
                connectionInfo.put("connectCount", druidDataSource.getConnectCount());
                connectionInfo.put("closeCount", druidDataSource.getCloseCount());
                
                activeConnections.add(connectionInfo);
            }
            
        } catch (Exception e) {
            logger.error("获取活跃连接信息失败", e);
        }
        
        return activeConnections;
    }

    /**
     * 数据库基本信息：通过 JDBC {@link DatabaseMetaData} 直接查询，与具体使用哪种连接池无关
     * （产品名称/版本、驱动名称/版本、连接 URL、用户名）
     */
    @Override
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> dbInfo = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            dbInfo.put("databaseProductName", metaData.getDatabaseProductName());
            dbInfo.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            dbInfo.put("driverName", metaData.getDriverName());
            dbInfo.put("driverVersion", metaData.getDriverVersion());
            dbInfo.put("url", metaData.getURL());
            dbInfo.put("userName", metaData.getUserName());
            
        } catch (Exception e) {
            logger.error("获取数据库信息失败", e);
            dbInfo.put("error", e.getMessage());
        }
        
        return dbInfo;
    }

    /**
     * 从Map中安全获取Double值
     */
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从Map中安全获取Long值
     */
    private long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
