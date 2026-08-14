package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizCategory;

import java.util.List;

/**
 * 文章分类服务，分类通过 pid 形成树形父子结构。
 */
public interface BizCategoryService extends IService<BizCategory> {

    /**
     * 按条件查询分类列表（含父子关联信息），结果整体做了缓存（固定 key "tree"）。
     * 注意：缓存 key 未包含 bizCategory 参数，若按不同条件调用可能读到同一份缓存结果。
     *
     * @param bizCategory 查询条件：name、description 模糊匹配，status、pid 精确匹配
     * @return 分类列表
     */
    List<BizCategory> selectCategories(BizCategory bizCategory);

    /**
     * 批量删除分类，成功后清空分类缓存。
     *
     * @param ids 分类 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

    /**
     * 根据 id 查询分类详情（含父分类基本信息）。
     *
     * @param id 分类 id
     * @return 分类详情
     */
    BizCategory selectById(String id);

    /**
     * 根据父分类 id 查询其直接子分类列表。
     *
     * @param pid 父分类 id
     * @return 子分类列表
     */
    List<BizCategory> selectByPid(String pid);

}
