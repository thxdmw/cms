package com.thx.module.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.file.model.StorageCleanupTask;

/**
 * storage_cleanup_task 表 Mapper
 * 基础 CRUD（按状态/重试时间查询待处理任务等）由 MyBatis-Plus 自动实现，无需额外方法
 */
public interface StorageCleanupTaskMapper extends BaseMapper<StorageCleanupTask> {
}
