package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizLink;

import java.util.List;

/**
 * 友情链接服务。
 */
public interface BizLinkService extends IService<BizLink> {

    /**
     * 查询全部友链（不分页），结果做了缓存（固定 key "list"）。
     * 注意：缓存 key 未包含 bizLink 参数，若按不同条件调用可能读到同一份缓存结果。
     *
     * @param bizLink 查询条件：name、url 模糊匹配，status 精确匹配
     * @return 友链列表
     */
    List<BizLink> selectLinks(BizLink bizLink);

    /**
     * 分页查询友链列表，过滤条件同 {@link #selectLinks(BizLink)}。
     *
     * @param bizLink    查询条件
     * @param pageNumber 页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<BizLink> pageLinks(BizLink bizLink, Integer pageNumber, Integer pageSize);

    /**
     * 批量删除友链，成功后清空友链缓存。
     *
     * @param ids 友链 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

}
