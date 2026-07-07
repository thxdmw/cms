package com.thx.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.CoreConst;
import com.thx.common.util.IpUtil;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizComment;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.BizCommentService;
import com.thx.module.admin.vo.CommentConditionVo;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import cn.hutool.core.util.StrUtil;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 后台评论管理
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@RestController
@RequestMapping("comment")
@AllArgsConstructor
public class CommentController {

    private final BizCommentService commentService;

    @PostMapping("list")
    public PageResultVo loadNotify(CommentConditionVo vo, Integer pageNumber, Integer pageSize) {

        IPage<BizComment> commentPage = commentService.selectComments(vo, pageNumber, pageSize);
        return ResultUtil.table(commentPage.getRecords(), commentPage.getTotal());
    }

    @PostMapping("/reply")
    public ResponseVo edit(BizComment comment) {
        completeComment(comment);
        boolean i = commentService.save(comment);
        if (i) {
            return ResultUtil.success("回复评论成功");
        } else {
            return ResultUtil.error("回复评论失败");
        }
    }

    @PostMapping("/delete")
    public ResponseVo delete(String id) {
        String[] ids = {id};
        int i = commentService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除评论成功");
        } else {
            return ResultUtil.error("删除评论失败");
        }
    }

    @PostMapping("/batch/delete")
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = commentService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除评论成功");
        } else {
            return ResultUtil.error("删除评论失败");
        }
    }

    @PostMapping("/audit")
    public ResponseVo audit(BizComment bizComment, String replyContent) {
        try {
            commentService.updateById(bizComment);
            if (StrUtil.isNotBlank(replyContent)) {
                BizComment replyComment = new BizComment();
                replyComment.setPid(bizComment.getId());
                replyComment.setSid(bizComment.getSid());
                replyComment.setContent(replyContent);
                completeComment(replyComment);
                commentService.save(replyComment);
            }
            return ResultUtil.success("审核成功");
        } catch (Exception e) {
            return ResultUtil.success("审核失败");
        }
    }

    private void completeComment(BizComment comment) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        comment.setUserId(user.getUserId());
        comment.setNickname(user.getNickname());
        comment.setEmail(user.getEmail());
        comment.setAvatar(user.getImg());
        comment.setIp(IpUtil.getIpAddr(request));
        comment.setStatus(CoreConst.STATUS_VALID);
    }

}
