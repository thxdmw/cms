package com.thx.module.agent.service.impl;

import com.thx.common.log.LogMessage;
import com.thx.infra.WebSocketService;
import com.thx.module.agent.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link LogService} 的默认实现，同时承担两件不太一样的事情：
 * <ul>
 *     <li>历史日志查询/统计：直接读取磁盘上的日志文件（{@code logging.file.path} 目录下的
 *     {@code cms.log}/{@code cms-yyyy-MM-dd.log}），按正则解析出每条日志的时间/级别/logger/内容，
 *     供 {@code searchLogs}/{@code getLogStatistics}/{@code getRecentErrors} 做筛选、分页、统计；</li>
 *     <li>实时日志推送：实现父接口 {@link com.thx.common.log.LogMessagePublisher#sendLogMessage}，
 *     被 {@link com.thx.common.log.CustomizeAppender} 在每条日志产生时调用，再通过
 *     {@link WebSocketService} 广播给正在看后台日志页面的前端，无需刷新即可看到最新日志。</li>
 * </ul>
 * 这几个方法最终都是给 {@code com.thx.module.agent.controller.OpsAgentApiController} 使用，
 * 即供外部运维 Agent 调用的日志分析能力。
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);
    private static final Pattern LOG_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(.*?)\\] (\\w+) (.*?) - (.*)");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static {
        FILE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }

    @Value("${logging.file.path:logs}")
    private String logPath;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 从日志文件中查询日志
     */
    @Override
    public Map<String, Object> searchLogs(Date startTime, Date endTime, String level, String keyword, int page, int size) {
        List<LogMessage> allLogs = new ArrayList<>();

        try {
            // 获取所有符合条件的日志文件
            List<File> logFiles = getLogFilesInDateRange(startTime, endTime);

            // 从每个文件中读取日志
            for (File logFile : logFiles) {
                List<LogMessage> logsFromFile = readLogsFromFile(logFile, startTime, endTime, level, keyword);
                allLogs.addAll(logsFromFile);
            }

            // 按时间排序
            allLogs.sort(Comparator.comparing(LogMessage::getTimestamp));

            // 计算分页
            int total = allLogs.size();
            int totalPages = (int) Math.ceil((double) total / size);

            int start = (page - 1) * size;
            int end = Math.min(start + size, total);

            List<LogMessage> pagedLogs = start < total ? allLogs.subList(start, end) : new ArrayList<>();

            Map<String, Object> result = new HashMap<>();
            result.put("content", pagedLogs);
            result.put("totalElements", total);
            result.put("totalPages", totalPages);
            result.put("size", size);
            result.put("page", page);

            return result;
        } catch (Exception e) {
            logger.error("Error searching logs from files", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("content", new ArrayList<>());
            errorResult.put("totalElements", 0);
            errorResult.put("totalPages", 0);
            errorResult.put("size", size);
            errorResult.put("page", page);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 接收 {@link com.thx.common.log.CustomizeAppender} 推来的一条实时日志，转发给 WebSocket 广播。
     * 不做持久化（历史日志的查询能力靠直接读日志文件实现，见 {@link #searchLogs}）。
     */
    @Override
    public void sendLogMessage(LogMessage logMessage) {
        // 保存到仓库
        //logRepository.save(logMessage);

        // 通过WebSocket发送
        webSocketService.sendLogMessage(logMessage);
    }

    /**
     * 获取指定日期范围内的日志文件
     */
    private List<File> getLogFilesInDateRange(Date startTime, Date endTime) throws IOException {
        Path logsDir = Paths.get(logPath);
        if (!Files.exists(logsDir)) {
            return new ArrayList<>();
        }

        // 如果没有指定开始时间，默认为7天前
        if (startTime == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            startTime = cal.getTime();
        }

        // 如果没有指定结束时间，默认为当前时间
        if (endTime == null) {
            endTime = new Date();
        }

        // 获取所有日志文件
        try (Stream<Path> paths = Files.list(logsDir)) {
            Date finalStartTime = startTime;
            Date finalEndTime = endTime;
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        // 匹配当前日志文件或者按日期命名的日志文件
                        return fileName.equals("cms.log") ||
                                fileName.startsWith("cms-") && fileName.endsWith(".log");
                    })
                    .map(Path::toFile)
                    .filter(file -> {
                        // 如果是当前日志文件，总是包含在内
                        if (file.getName() .equals("cms.log")) {
                            return true;
                        }

                        // 解析文件名中的日期
                        try {
                            String dateStr = file.getName().replace("cms-", "").replace(".log", "");
                            Date fileDate = FILE_DATE_FORMAT.parse(dateStr);

                            // 检查文件日期是否在指定范围内
                            return !fileDate.before(getDateWithoutTime(finalStartTime)) &&
                                    !fileDate.after(getDateWithoutTime(finalEndTime));
                        } catch (ParseException e) {
                            logger.warn("Cannot parse date from filename: {}", file.getName());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * 从文件中读取符合条件的日志
     */
    private List<LogMessage> readLogsFromFile(File file, Date startTime, Date endTime, String level, String keyword) {
        List<LogMessage> logs = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder logEntry = new StringBuilder();
            boolean isMultiLine = false;
            Date logTimestamp = null;
            String logLevel = null;
            String logLogger = null;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_PATTERN.matcher(line);

                if (matcher.matches()) {
                    // 如果之前有多行日志，先处理它
                    if (isMultiLine && logTimestamp != null) {
                        processLogEntry(logs, logTimestamp, logLevel, logLogger, logEntry.toString(), startTime, endTime, level, keyword);
                    }

                    // 解析新的日志行
                    String timestampStr = matcher.group(1);
                    String thread = matcher.group(2);
                    logLevel = matcher.group(3);
                    logLogger = matcher.group(4);
                    String message = matcher.group(5);

                    try {
                        logTimestamp = DATE_FORMAT.parse(timestampStr);
                        logEntry = new StringBuilder(message);
                        isMultiLine = true;
                    } catch (ParseException e) {
                        logger.warn("Cannot parse timestamp: {}", timestampStr);
                        isMultiLine = false;
                    }
                } else if (isMultiLine) {
                    // 多行日志，追加到当前日志条目
                    logEntry.append("\n").append(line);
                }
            }

            // 处理最后一条日志
            if (isMultiLine && logTimestamp != null) {
                processLogEntry(logs, logTimestamp, logLevel, logLogger, logEntry.toString(), startTime, endTime, level, keyword);
            }

        } catch (IOException e) {
            logger.error("Error reading log file: {}", file.getAbsolutePath(), e);
        }

        return logs;
    }

    /**
     * 处理单条日志条目
     */
    private void processLogEntry(List<LogMessage> logs, Date timestamp, String level, String logger, String message,
                                 Date startTime, Date endTime, String filterLevel, String keyword) {
        // 检查时间范围
        if (startTime != null && timestamp.before(startTime)) {
            return;
        }
        if (endTime != null && timestamp.after(endTime)) {
            return;
        }

        // 检查日志级别
        if (filterLevel != null && !filterLevel.isEmpty() && !level.equals(filterLevel)) {
            return;
        }

        // 检查关键字
        if (keyword != null && !keyword.isEmpty()) {
            String lowerMessage = message.toLowerCase();
            String lowerLogger = logger.toLowerCase();
            String lowerKeyword = keyword.toLowerCase();

            if (!lowerMessage.contains(lowerKeyword) && !lowerLogger.contains(lowerKeyword)) {
                return;
            }
        }

        // 创建日志记录并添加到结果列表
        LogMessage LogMessage = new LogMessage();
        LogMessage.setId(null); // 临时ID
        LogMessage.setTimestamp(timestamp);
        LogMessage.setLevel(level);
        LogMessage.setLogger(logger);
        LogMessage.setMessage(message);

        logs.add(LogMessage);
    }

    /**
     * 获取不包含时间部分的日期
     */
    private Date getDateWithoutTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 统计指定时间范围内的日志：按级别（ERROR/WARN/INFO/DEBUG）计数与占比、按小时统计分布、
     * 按错误类型（从异常信息里提取的类名）聚合，并附带最近 10 条错误日志的摘要
     */
    @Override
    public Map<String, Object> getLogStatistics(Date startTime, Date endTime) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // 获取所有符合条件的日志文件
            List<File> logFiles = getLogFilesInDateRange(startTime, endTime);
            
            // 统计变量
            int totalLogs = 0;
            int errorCount = 0;
            int warnCount = 0;
            int infoCount = 0;
            int debugCount = 0;
            
            Map<String, Integer> errorTypeMap = new HashMap<>();
            Map<String, Integer> hourlyDistribution = new HashMap<>();
            List<Map<String, Object>> recentErrors = new ArrayList<>();
            
            // 从每个文件中读取并统计日志
            for (File logFile : logFiles) {
                List<LogMessage> logsFromFile = readLogsFromFile(logFile, startTime, endTime, null, null);
                
                for (LogMessage log : logsFromFile) {
                    totalLogs++;
                    
                    // 按级别统计
                    String level = log.getLevel();
                    switch (level) {
                        case "ERROR":
                            errorCount++;
                            break;
                        case "WARN":
                            warnCount++;
                            break;
                        case "INFO":
                            infoCount++;
                            break;
                        case "DEBUG":
                            debugCount++;
                            break;
                    }
                    
                    // 按小时分布统计
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(log.getTimestamp());
                    String hourKey = String.format("%02d:00", cal.get(Calendar.HOUR_OF_DAY));
                    hourlyDistribution.merge(hourKey, 1, Integer::sum);
                    
                    // 收集错误类型
                    if ("ERROR".equals(level)) {
                        String message = log.getMessage();
                        String errorType = extractErrorType(message);
                        errorTypeMap.merge(errorType, 1, Integer::sum);
                        
                        // 收集最近的错误（最多10条）
                        if (recentErrors.size() < 10) {
                            Map<String, Object> errorInfo = new HashMap<>();
                            errorInfo.put("timestamp", log.getTimestamp());
                            errorInfo.put("logger", log.getLogger());
                            errorInfo.put("message", message.length() > 500 ? message.substring(0, 500) + "..." : message);
                            recentErrors.add(errorInfo);
                        }
                    }
                }
            }
            
            // 计算百分比
            double errorPercent = totalLogs > 0 ? (double) errorCount / totalLogs * 100 : 0;
            double warnPercent = totalLogs > 0 ? (double) warnCount / totalLogs * 100 : 0;
            double infoPercent = totalLogs > 0 ? (double) infoCount / totalLogs * 100 : 0;
            
            // 组装统计结果
            statistics.put("totalLogs", totalLogs);
            statistics.put("errorCount", errorCount);
            statistics.put("warnCount", warnCount);
            statistics.put("infoCount", infoCount);
            statistics.put("debugCount", debugCount);
            statistics.put("errorPercent", Math.round(errorPercent * 100.0) / 100.0);
            statistics.put("warnPercent", Math.round(warnPercent * 100.0) / 100.0);
            statistics.put("infoPercent", Math.round(infoPercent * 100.0) / 100.0);
            statistics.put("hourlyDistribution", hourlyDistribution);
            statistics.put("errorTypes", errorTypeMap);
            statistics.put("recentErrors", recentErrors);
            
        } catch (Exception e) {
            logger.error("获取日志统计信息失败", e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }

    /**
     * 获取最近的错误日志：固定只看最近 24 小时的日志文件，从最新的文件开始往回读，凑够 {@code count}
     * 条 ERROR 级别日志即停止，最后按时间倒序（最新的在前）返回
     */
    @Override
    public List<Map<String, Object>> getRecentErrors(int count) {
        List<Map<String, Object>> recentErrors = new ArrayList<>();
        
        try {
            // 默认查询最近24小时的日志
            Calendar cal = Calendar.getInstance();
            Date endTime = cal.getTime();
            cal.add(Calendar.HOUR_OF_DAY, -24);
            Date startTime = cal.getTime();
            
            // 获取日志文件
            List<File> logFiles = getLogFilesInDateRange(startTime, endTime);
            
            // 从最新的文件开始读取
            Collections.reverse(logFiles);
            
            for (File logFile : logFiles) {
                if (recentErrors.size() >= count) {
                    break;
                }
                
                List<LogMessage> logsFromFile = readLogsFromFile(logFile, startTime, endTime, "ERROR", null);
                
                // 只取需要的数量
                int remaining = count - recentErrors.size();
                List<LogMessage> limitedLogs = logsFromFile.subList(0, Math.min(remaining, logsFromFile.size()));
                
                for (LogMessage log : limitedLogs) {
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("timestamp", log.getTimestamp());
                    errorInfo.put("logger", log.getLogger());
                    errorInfo.put("message", log.getMessage());
                    recentErrors.add(errorInfo);
                }
            }
            
            // 按时间倒序排列（最新的在前）
            recentErrors.sort((a, b) -> {
                Date timeA = (Date) a.get("timestamp");
                Date timeB = (Date) b.get("timestamp");
                return timeB.compareTo(timeA);
            });
            
        } catch (Exception e) {
            logger.error("获取最近错误日志失败", e);
        }
        
        return recentErrors;
    }

    /**
     * 从错误消息中提取错误类型
     */
    private String extractErrorType(String message) {
        if (message == null || message.isEmpty()) {
            return "Unknown";
        }
        
        // 尝试提取异常类名
        int colonIndex = message.indexOf(":");
        if (colonIndex > 0) {
            String beforeColon = message.substring(0, colonIndex).trim();
            // 检查是否包含异常类名特征
            if (beforeColon.contains("Exception") || beforeColon.contains("Error")) {
                // 只取最后一部分（类名）
                String[] parts = beforeColon.split("\\s+");
                return parts[parts.length - 1];
            }
        }
        
        // 如果没有找到明显的异常类型，返回前50个字符作为标识
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
}