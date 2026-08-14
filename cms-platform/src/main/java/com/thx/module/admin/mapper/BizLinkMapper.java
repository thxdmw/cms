package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.BizLink;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 友情链接 Mapper。
 */
public interface BizLinkMapper extends BaseMapper<BizLink> {

    /**
     * 分页查询友链列表，支持按名称、URL 模糊匹配，按状态精确匹配。
     *
     * @param page    分页参数
     * @param bizLink 查询条件：name、url 模糊匹配（LIKE %xxx%），status 精确匹配，字段为空则不参与过滤
     * @return 分页结果
     */
    List<BizLink> selectLinks(@Param("page") IPage<BizLink> page, @Param("bizLink") BizLink bizLink);

    /**
     * 根据 id 数组批量删除友链。
     *
     * @param ids 友链 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

}
