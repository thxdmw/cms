package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文章实体，CMS 的核心业务数据。正文有 content（渲染用的 HTML）和 contentMd（Markdown 源码）两份，
 * 后台编辑走 Markdown，前台展示直接用已经转换好的 HTML，避免每次访问都重新渲染。
 * 浏览数/评论数/点赞数不是这张表自己的字段（下面几个 @TableField(exist = false) 标注的都是），
 * 而是分别来自 BizArticleLook/BizComment/BizLove 关联表的统计结果，只是为了方便前端展示挂在这个对象上。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class BizArticle extends BaseVo {
    private static final long serialVersionUID = 7238198006412851176L;

    /** 文章标题 */
    private String title;
    /** 作者的用户 id（关联 User） */
    private String userId;
    /** 作者显示名称（冗余存储，避免每次都要联表查用户） */
    private String author;
    /** 封面图地址 */
    private String coverImage;
    /** 文章分享二维码图片路径 */
    private String qrcodePath;
    /** 是否为 Markdown 格式撰写 */
    private Boolean isMarkdown;
    /** 正文 HTML（由 contentMd 转换而来，前台直接渲染这份） */
    private String content;
    /** 正文 Markdown 源码（后台编辑器编辑/保存的是这份） */
    private String contentMd;
    /** 是否置顶：1 是 0 否 */
    private Integer top;
    /** 所属分类 id（关联 BizCategory） */
    private String categoryId;
    /** 文章状态：1 已发布 0 草稿 */
    private Integer status;
    /** 是否推荐：1 是 0 否 */
    private Integer recommended;
    /** 是否参与首页轮播：1 是 0 否 */
    private Integer slider;
    /** 轮播图地址（slider=1 时使用） */
    private String sliderImg;
    /** 是否原创：1 原创 0 转载 */
    private Integer original;
    /** 文章概要/摘要 */
    private String description;
    /** SEO 关键词 */
    private String keywords;
    /** 是否开启评论：1 是 0 否 */
    private Integer comment;
    /** 浏览次数（统计自 BizArticleLook，非本表字段） */
    @TableField(exist = false)
    private Integer lookCount = 0;
    /** 评论数量（统计自 BizComment，非本表字段） */
    @TableField(exist = false)
    private Integer commentCount = 0;
    /** 点赞数量（统计自 BizLove，非本表字段） */
    @TableField(exist = false)
    private Integer loveCount = 0;
    /** 是否为近期新文章的展示标记，非本表字段 */
    @TableField(exist = false)
    private Integer newFlag = 0;
    /** 关联的标签列表（通过 BizArticleTags 关联表查出，非本表字段） */
    @TableField(exist = false)
    List<BizTags> tags;
    /** 所属分类对象（按 categoryId 查出，非本表字段） */
    @TableField(exist = false)
    BizCategory bizCategory;

}
