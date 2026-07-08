package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizServerFile;

/**
 * 服务器文件记录表 Mapper，对应后台"服务器文件"功能所管理的文件元数据记录。
 * 未定义自定义查询方法，增删改查直接使用 MyBatis-Plus 提供的通用 BaseMapper 能力。
 */
public interface BizServerFileMapper extends BaseMapper<BizServerFile> {

}
