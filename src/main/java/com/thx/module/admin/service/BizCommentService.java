package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizComment;
import com.thx.module.admin.vo.CommentConditionVo;

/**
 * 文章评论服务，支持楼中楼（父子评论）结构。
 */
public interface BizCommentService extends IService<BizComment> {

    /**
     * 按条件分页查询评论列表，关联查出父评论、所属文章标题、点赞数，按创建时间倒序排列。
     *
     * @param vo         查询条件：userId、sid（所属文章 id）、pid（父评论 id）、qq、status 均为精确匹配，为空则不过滤
     * @param pageNumber 页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<BizComment> selectComments(CommentConditionVo vo, Integer pageNumber, Integer pageSize);

    /**
     * 批量删除评论：同时删除给定 id 命中的评论及其直接子评论（回复）。
     *
     * @param ids 评论 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

}
