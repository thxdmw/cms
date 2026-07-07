package com.thx.module.agent.service;

import com.thx.module.admin.pojo.LogMessage;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface LogService {

    /**
     * 从日志文件中查询日志
     */
    Map<String, Object> searchLogs(Date startTime, Date endTime, String level, String keyword, int page, int size);

    /**
     * 发给 websocket
     *
     * @param logMessage
     */
    void sendLogMessage(LogMessage logMessage);

    /**
     * 获取日志统计信息
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 包含各级别日志数量、错误类型分布等统计信息的Map
     */
    Map<String, Object> getLogStatistics(Date startTime, Date endTime);

    /**
     * 获取最近的错误日志
     * @param count 返回条数
     * @return 错误日志列表
     */
    List<Map<String, Object>> getRecentErrors(int count);

}
