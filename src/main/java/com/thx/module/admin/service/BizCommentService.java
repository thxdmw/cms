package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizComment;
import com.thx.module.admin.vo.CommentConditionVo;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizCommentService extends IService<BizComment> {

    IPage<BizComment> selectComments(CommentConditionVo vo, Integer pageNumber, Integer pageSize);

    int deleteBatch(String[] ids);

}
