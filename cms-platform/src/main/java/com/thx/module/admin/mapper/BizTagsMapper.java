package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.BizTags;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章标签 Mapper。
 */
public interface BizTagsMapper extends BaseMapper<BizTags> {

    /**
     * 分页查询标签列表，支持按名称、描述模糊匹配。
     *
     * @param page    分页参数
     * @param bizTags 查询条件：name、description 均为模糊匹配（LIKE %xxx%），字段为空则不参与过滤
     * @return 分页结果
     */
    List<BizTags> selectTags(@Param("page") IPage<BizTags> page, @Param("bizTags") BizTags bizTags);

}
