package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.Pagination;
import com.thx.module.admin.mapper.BizCommentMapper;
import com.thx.module.admin.entity.BizComment;
import com.thx.module.admin.service.BizCommentService;
import com.thx.module.admin.vo.CommentConditionVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Service
@AllArgsConstructor
public class BizCommentServiceImpl extends ServiceImpl<BizCommentMapper, BizComment> implements BizCommentService {

    private final BizCommentMapper commentMapper;

    @Override
    public IPage<BizComment> selectComments(CommentConditionVo vo, Integer pageNumber, Integer pageSize) {
        IPage<BizComment> page = new Pagination<>(pageNumber, pageSize);
        page.setRecords(commentMapper.selectComments(page, vo));
        return page;
    }

    @Override
    public int deleteBatch(String[] ids) {
        return commentMapper.deleteBatch(ids);
    }
}
