package com.thx.module.agent.service;

import com.thx.common.log.LogMessagePublisher;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 日志查询与统计服务。继承 {@link LogMessagePublisher} 是为了同时承担"接收 Appender 推来的
 * 实时日志并转发给前端"这个职责（具体实现见 LogServiceImpl.sendLogMessage），
 * 这样 common 包下的 CustomizeAppender 只需要认识 LogMessagePublisher 这一个小接口，
 * 不需要知道 LogService 这个更大、属于业务 module 的接口。
 */
public interface LogService extends LogMessagePublisher {

    /**
     * 从日志文件中查询日志
     */
    Map<String, Object> searchLogs(Date startTime, Date endTime, String level, String keyword, int page, int size);

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
