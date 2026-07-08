package com.thx.common.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 一条日志记录的载体。本质是日志基础设施的数据结构（供 {@link CustomizeAppender} 采集、
 * 经 {@link LogMessagePublisher} 推送到前端日志页），不是 admin 模块的业务数据，
 * 因此放在 common.log 包下，而不是某个具体业务模块里。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogMessage {

    /** 日志唯一标识 */
    private String id;

    /** 日志产生时间 */
    private Date timestamp;

    /** 日志级别，如 INFO/WARN/ERROR */
    private String level;

    /** 产生该日志的 logger 名称（通常是类的全限定名） */
    private String logger;

    /** 日志内容 */
    private String message;
}
