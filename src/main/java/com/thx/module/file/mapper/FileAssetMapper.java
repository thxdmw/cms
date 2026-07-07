package com.thx.module.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.file.model.FileAsset;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文件资产 Mapper
 * MyBatis-Plus 基础 CRUD 由框架自动实现
 */
public interface FileAssetMapper extends BaseMapper<FileAsset> {

    /** 统计某个 App 当前有效文件占用的总字节数，用于配额校验 */
    @Select("SELECT COALESCE(SUM(size), 0) FROM file_asset WHERE app_id = #{appId} AND status = 'ACTIVE'")
    long sumActiveSize(@Param("appId") String appId);
}
