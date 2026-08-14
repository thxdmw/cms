package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizCategory;

import java.util.List;

/**
 * 文章分类 Mapper，分类通过 pid 形成树形父子结构。
 */
public interface BizCategoryMapper extends BaseMapper<BizCategory> {

    /**
     * 按条件查询分类列表（不分页），支持按名称、描述模糊匹配，按状态、父分类 id 精确匹配；
     * 结果按 sort 升序排列，并通过嵌套查询自动为每条记录填充 parent（父分类）与 children（子分类列表）。
     *
     * @param bizCategory 查询条件：name、description 模糊匹配，status、pid 精确匹配，字段为空则不参与过滤
     * @return 分类列表
     */
    List<BizCategory> selectCategories(BizCategory bizCategory);

    /**
     * 根据 id 数组批量删除分类。
     *
     * @param ids 分类 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

    /**
     * 根据 id 查询分类详情，并关联查出父分类的 pid、name、description。
     *
     * @param id 分类 id
     * @return 分类详情
     */
    BizCategory getById(String id);
}
