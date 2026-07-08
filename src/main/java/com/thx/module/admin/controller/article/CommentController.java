package com.thx.module.admin.controller.article;

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
 * 后台评论管理接口，对应前端 admin-app 的评论管理模块：评论列表查询、管理员回复/发表评论、审核、删除。
 */
@RestController
@RequestMapping("comment")
@AllArgsConstructor
public class CommentController {

    private final BizCommentService commentService;

    /**
     * 分页查询评论列表。方法名 loadNotify 为历史遗留命名，与"消息通知"无关，实际功能就是评论列表分页查询。
     */
    @PostMapping("list")
    public PageResultVo loadNotify(CommentConditionVo vo, Integer pageNumber, Integer pageSize) {

        IPage<BizComment> commentPage = commentService.selectComments(vo, pageNumber, pageSize);
        return ResultUtil.table(commentPage.getRecords(), commentPage.getTotal());
    }

    /**
     * 保存一条新评论记录，评论人身份信息（userId/昵称/邮箱/头像/IP）强制取自当前登录管理员，不采信前端传参；
     * 前端可在表单中额外携带 pid/sid 字段，从而实现"以管理员身份回复某条评论"的效果。
     */
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

    /**
     * 删除单条评论，内部复用批量删除接口实现。
     */
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

    /**
     * 根据 ID 数组批量物理删除评论。
     */
    @PostMapping("/batch/delete")
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = commentService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除评论成功");
        } else {
            return ResultUtil.error("删除评论失败");
        }
    }

    /**
     * 审核评论：更新评论自身状态（如通过/驳回）；若同时提交了非空的 replyContent，则额外以当前登录管理员
     * 身份追加保存一条回复评论，其 pid/sid 沿用被审核评论的 id/sid。
     * 注意：异常分支这里调用的是 {@code ResultUtil.success(...)} 而非 error，即使审核失败，响应的 success
     * 标志位依旧为 true，只是提示文案变为"审核失败"，这是既有历史行为，前端如果依赖 success 字段判断成败需留意。
     */
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

    /**
     * 补全评论的操作人相关字段：从当前 Shiro 登录用户与当前请求中提取 userId/昵称/邮箱/头像/IP 写入评论对象，
     * 并将评论状态置为有效。供 {@link #edit} 与 {@link #audit} 追加回复时复用。
     */
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
