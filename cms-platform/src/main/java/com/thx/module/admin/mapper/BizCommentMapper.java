package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.BizComment;
import com.thx.module.admin.vo.CommentConditionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章评论 Mapper，支持楼中楼（pid 指向父评论）结构。
 */
public interface BizCommentMapper extends BaseMapper<BizComment> {

    /**
     * 按条件分页查询评论列表，关联查出父评论（parent，用于楼中楼展示）、所属文章标题（article），
     * 以及该评论获得的点赞数（loveCount，统计 biz_love 中 biz_type=2 且 status=1 的记录）。
     *
     * @param page 分页参数
     * @param vo   查询条件：userId、sid（所属文章 id）、pid（父评论 id）、qq、status 均为精确匹配，为 null 则不参与过滤
     * @return 分页结果，按创建时间倒序排列
     */
    List<BizComment> selectComments(@Param("page") IPage<BizComment> page, @Param("vo") CommentConditionVo vo);

    /**
     * 批量删除评论：删除 id 命中数组的评论，同时删除这些评论的直接子评论（pid 命中数组的回复）。
     *
     * @param ids 评论 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);
}
