package com.thx.module.agent.service.Impl;

import cn.hutool.core.util.NumberUtil;
import com.sun.management.OperatingSystemMXBean;
import com.thx.module.agent.service.SystemMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统资源监控服务实现
 */
@Service
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SystemMonitorServiceImpl.class);
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    @Override
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            // CPU信息
            overview.put("cpu", getCpuUsage());
            
            // 内存信息
            overview.put("memory", getMemoryInfo());
            
            // 磁盘信息
            overview.put("disk", getDiskInfo());
            
            // JVM信息
            overview.put("jvm", getJvmInfo());
            
            // 线程信息
            overview.put("threads", getThreadInfo());
            
            // 系统基本信息
            Map<String, String> systemInfo = new HashMap<>();
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("osVersion", System.getProperty("os.version"));
            systemInfo.put("osArch", System.getProperty("os.arch"));
            systemInfo.put("processors", String.valueOf(osBean.getAvailableProcessors()));
            systemInfo.put("systemLoadAverage", formatDouble(osBean.getSystemLoadAverage()));
            
            overview.put("systemInfo", systemInfo);
            
        } catch (Exception e) {
            logger.error("获取系统概览信息失败", e);
        }
        
        return overview;
    }

    @Override
    public double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double loadAverage = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            
            if (loadAverage < 0) {
                return -1; // 不支持
            }
            
            // 计算CPU使用率百分比
            double cpuUsage = (loadAverage / processors) * 100;
            return NumberUtil.round(cpuUsage, 2).doubleValue();
        } catch (Exception e) {
            logger.error("获取CPU使用率失败", e);
            return -1;
        }
    }

    @Override
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            memoryInfo.put("totalMemory", formatBytes(totalMemory));
            memoryInfo.put("usedMemory", formatBytes(usedMemory));
            memoryInfo.put("freeMemory", formatBytes(freeMemory));
            memoryInfo.put("maxMemory", formatBytes(maxMemory));
            memoryInfo.put("usagePercent", NumberUtil.round((double) usedMemory / totalMemory * 100, 2).doubleValue());
            
            // JVM堆内存详情
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            
            Map<String, Object> heapInfo = new HashMap<>();
            heapInfo.put("init", formatBytes(heapUsage.getInit()));
            heapInfo.put("used", formatBytes(heapUsage.getUsed()));
            heapInfo.put("committed", formatBytes(heapUsage.getCommitted()));
            heapInfo.put("max", formatBytes(heapUsage.getMax()));
            memoryInfo.put("heap", heapInfo);
            
            Map<String, Object> nonHeapInfo = new HashMap<>();
            nonHeapInfo.put("init", formatBytes(nonHeapUsage.getInit()));
            nonHeapInfo.put("used", formatBytes(nonHeapUsage.getUsed()));
            nonHeapInfo.put("committed", formatBytes(nonHeapUsage.getCommitted()));
            nonHeapInfo.put("max", formatBytes(nonHeapUsage.getMax()));
            memoryInfo.put("nonHeap", nonHeapInfo);
            
        } catch (Exception e) {
            logger.error("获取内存信息失败", e);
        }
        
        return memoryInfo;
    }

    @Override
    public Map<String, Object> getDiskInfo() {
        Map<String, Object> diskInfo = new HashMap<>();
        
        try {
            // 获取项目根目录所在磁盘的使用情况
            File rootPath = new File(".");
            long totalSpace = rootPath.getTotalSpace();
            long freeSpace = rootPath.getFreeSpace();
            long usableSpace = rootPath.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;
            
            diskInfo.put("totalSpace", formatBytes(totalSpace));
            diskInfo.put("usedSpace", formatBytes(usedSpace));
            diskInfo.put("freeSpace", formatBytes(freeSpace));
            diskInfo.put("usableSpace", formatBytes(usableSpace));
            diskInfo.put("usagePercent", NumberUtil.round((double) usedSpace / totalSpace * 100, 2).doubleValue());
            
        } catch (Exception e) {
            logger.error("获取磁盘信息失败", e);
        }
        
        return diskInfo;
    }

    @Override
    public Map<String, Object> getJvmInfo() {
        Map<String, Object> jvmInfo = new HashMap<>();
        
        try {
            // JVM基本信息
            jvmInfo.put("vmName", System.getProperty("java.vm.name"));
            jvmInfo.put("vmVersion", System.getProperty("java.vm.version"));
            jvmInfo.put("javaVersion", System.getProperty("java.version"));
            jvmInfo.put("vendor", System.getProperty("java.vendor"));
            
            // 运行时信息
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            jvmInfo.put("uptime", formatDuration(runtimeMXBean.getUptime()));
            jvmInfo.put("startTime", runtimeMXBean.getStartTime());
            
            // GC信息
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            List<Map<String, Object>> gcInfoList = new java.util.ArrayList<>();
            
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                Map<String, Object> gcInfo = new HashMap<>();
                gcInfo.put("name", gcBean.getName());
                gcInfo.put("collectionCount", gcBean.getCollectionCount());
                gcInfo.put("collectionTime", formatDuration(gcBean.getCollectionTime()));
                gcInfoList.add(gcInfo);
            }
            
            jvmInfo.put("garbageCollectors", gcInfoList);
            
        } catch (Exception e) {
            logger.error("获取JVM信息失败", e);
        }
        
        return jvmInfo;
    }

    @Override
    public Map<String, Object> getThreadInfo() {
        Map<String, Object> threadInfo = new HashMap<>();
        
        try {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            
            threadInfo.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
            threadInfo.put("threadCount", threadMXBean.getThreadCount());
            threadInfo.put("peakThreadCount", threadMXBean.getPeakThreadCount());
            threadInfo.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
            
            // 线程状态统计
            Thread.State[] states = Thread.State.values();
            Map<String, Integer> stateCounts = new HashMap<>();
            for (Thread.State state : states) {
                stateCounts.put(state.toString(), 0);
            }
            
            long[] threadIds = threadMXBean.getAllThreadIds();
            for (long threadId : threadIds) {
                ThreadInfo info = threadMXBean.getThreadInfo(threadId);
                if (info != null) {
                    Thread.State state = info.getThreadState();
                    stateCounts.merge(state.toString(), 1, Integer::sum);
                }
            }
            
            threadInfo.put("stateDistribution", stateCounts);
            
        } catch (Exception e) {
            logger.error("获取线程信息失败", e);
        }
        
        return threadInfo;
    }

    /**
     * 格式化字节数为可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return DF.format(size) + " " + units[unitIndex];
    }

    /**
     * 格式化毫秒数为可读格式
     */
    private String formatDuration(long millis) {
        if (millis < 0) {
            return "0 ms";
        }
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d天 %d小时 %d分钟", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟 %d秒", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟 %d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 格式化Double值
     */
    private String formatDouble(Double value) {
        if (value == null || value.isNaN()) {
            return "N/A";
        }
        return DF.format(value);
    }
}
