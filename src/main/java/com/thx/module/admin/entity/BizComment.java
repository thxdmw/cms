package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章评论，支持匿名评论（不要求登录，靠 nickname/email/qq 等自行填写的信息展示身份）和楼中楼回复
 * （通过 pid 指向被回复的评论）。os/browser 等是从提交时的 User-Agent 解析出来的展示信息。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizComment extends BaseVo {
    private static final long serialVersionUID = -7221371985694751121L;

    /** 评论者用户 id（游客评论时为空） */
    private String userId;
    /** 评论所属文章 id */
    private String sid;
    /** 被回复的父评论 id，顶层评论为空 */
    private String pid;
    /** 评论者 QQ 号，用于拉取 QQ 头像 */
    private String qq;
    /** 评论者昵称 */
    private String nickname;
    /** 评论者头像地址 */
    private String avatar;
    /** 评论者邮箱 */
    private String email;
    /** 评论者填写的个人主页地址 */
    private String url;
    /** 审核状态：1 已通过 0 待审核 */
    private Integer status;
    /** 提交评论时的 IP 地址 */
    private String ip;
    /** 根据 IP 解析出的经度 */
    private String lng;
    /** 根据 IP 解析出的纬度 */
    private String lat;
    /** 根据 IP 解析出的归属地 */
    private String address;
    /** 根据 User-Agent 解析出的操作系统全称 */
    private String os;
    /** 操作系统简称，用于列表页展示图标 */
    private String osShortName;
    /** 根据 User-Agent 解析出的浏览器全称 */
    private String browser;
    /** 浏览器简称，用于列表页展示图标 */
    private String browserShortName;
    /** 评论内容 */
    private String content;
    /** 管理员备注（后台内部使用，不对外展示） */
    private String remark;
    /** 支持（点赞）数 */
    private Integer support;
    /** 反对数 */
    private Integer oppose;
    /** 点赞数（统计自 BizLove，非本表字段） */
    @TableField(exist = false)
    private Integer loveCount;
    /** 被回复的父评论对象，按 pid 查出，非本表字段 */
    @TableField(exist = false)
    BizComment parent;
    /** 所属文章对象，按 sid 查出，非本表字段 */
    @TableField(exist = false)
    BizArticle article;

}
