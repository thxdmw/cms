package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizTags;

import java.util.List;

/**
 * 文章标签服务。
 */
public interface BizTagsService extends IService<BizTags> {

    /**
     * 查询全部标签（不分页），结果做了缓存（固定 key "list"）。
     * 注意：缓存 key 未包含 bizTags 参数，若按不同条件调用可能读到同一份缓存结果。
     *
     * @param bizTags 查询条件：name、description 模糊匹配
     * @return 标签列表
     */
    List<BizTags> selectTags(BizTags bizTags);

    /**
     * 分页查询标签列表，过滤条件同 {@link #selectTags(BizTags)}。
     *
     * @param bizTags    查询条件
     * @param pageNumber 页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<BizTags> pageTags(BizTags bizTags, Integer pageNumber, Integer pageSize);

    /**
     * 批量删除标签（使用 MyBatis-Plus 通用批量删除），成功后清空标签缓存。
     *
     * @param ids 标签 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);
}
