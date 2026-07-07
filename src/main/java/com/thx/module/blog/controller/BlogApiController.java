package com.thx.module.blog.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.*;
import com.thx.component.CommonDataService;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.entity.BizArticleLook;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.entity.BizComment;
import com.thx.module.admin.entity.BizLove;
import com.thx.module.admin.service.BizArticleLookService;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.service.BizCategoryService;
import com.thx.module.admin.service.BizCommentService;
import com.thx.module.admin.service.BizLoveService;
import com.thx.module.admin.vo.ArticleConditionVo;
import com.thx.module.admin.vo.CommentConditionVo;
import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.blog.vo.BizArticleInfoVo;
import com.thx.module.blog.vo.BizArticleSearchVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 给前台页面提供的接口，包括针对文章进行的添加评论、获取评论、增加浏览次数和点赞操作，
 * 以及给 Vue SPA 博客前端用的只读查询接口（文章列表/详情/轮播/分类/侧边栏公共数据）
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Slf4j
@RestController
@RequestMapping("blog/api")
@AllArgsConstructor
public class BlogApiController {

    private final BizCommentService commentService;
    private final BizArticleLookService articleLookService;
    private final BizLoveService loveService;
    private final BizArticleService bizArticleService;
    private final BizCategoryService bizCategoryService;
    private final CommonDataService commonDataService;


    @PostMapping("comments")
    public IPage<BizComment> getComments(CommentConditionVo vo, Integer pageNumber, Integer pageSize) {
        return commentService.selectComments(vo, pageNumber, pageSize);
    }

    @PostMapping("comment/save")
    public ResponseVo saveComment(HttpServletRequest request, BizComment comment) throws UnsupportedEncodingException {
        if (StrUtil.isEmpty(comment.getNickname())) {
            return ResultUtil.error("请输入昵称");
        }
        if (StrUtil.isNotBlank(comment.getEmail()) && !Validator.isEmail(comment.getEmail())) {
            return ResultUtil.error("邮箱格式不正确");
        }
        Date date = new Date();
        comment.setNickname(HtmlUtil.filter(comment.getNickname()));
        comment.setContent(HtmlUtil.filter(comment.getContent()));
        comment.setIp(HtmlUtil.filter(IpUtil.getIpAddr(request)));
        comment.setCreateTime(date);
        comment.setUpdateTime(date);
        if (StrUtil.isNotBlank(comment.getQq())) {
            comment.setAvatar("http://q1.qlogo.cn/g?b=qq&nk=" + comment.getQq() + "&s=100");
        } else if (StrUtil.isNotBlank(comment.getEmail())) {
            String entry = null;
            try {
                entry = MD5.md5Hex(comment.getEmail());
            } catch (NoSuchAlgorithmException e) {
                log.error("MD5出现异常{}", e.getMessage(), e);
            }
            comment.setAvatar("http://www.gravatar.com/avatar/" + entry + "?d=mp");
        }
        boolean a = commentService.save(comment);
        if (a) {
            return ResultUtil.success("评论提交成功,系统正在审核");
        } else {
            return ResultUtil.error("评论提交失败");
        }
    }


    @PostMapping("article/look")
    public ResponseVo checkLook(HttpServletRequest request, String articleId) {
        /*浏览次数*/
        Date date = new Date();
        String ip = IpUtil.getIpAddr(request);
        int checkCount = articleLookService.checkArticleLook(articleId, ip, DateUtil.addHours(date, -1));
        if (checkCount == 0) {
            BizArticleLook articleLook = new BizArticleLook();
            articleLook.setArticleId(articleId);
            articleLook.setUserIp(ip);
            articleLook.setLookTime(date);
            articleLook.setCreateTime(date);
            articleLook.setUpdateTime(date);
            articleLookService.save(articleLook);
            return ResultUtil.success("浏览次数+1");
        } else {
            return ResultUtil.error("一小时内重复浏览不增加次数哦");
        }
    }


    @PostMapping("love")
    public ResponseVo love(HttpServletRequest request, String bizId, Integer bizType) {
        Date date = new Date();
        String ip = IpUtil.getIpAddr(request);
        BizLove bizLove = loveService.checkLove(bizId, ip);
        if (bizLove == null) {
            bizLove = new BizLove();
            bizLove.setBizId(bizId);
            bizLove.setBizType(bizType);
            bizLove.setUserIp(ip);
            bizLove.setStatus(CoreConst.STATUS_VALID);
            bizLove.setCreateTime(date);
            bizLove.setUpdateTime(date);
            loveService.save(bizLove);
            return ResultUtil.success("点赞成功");
        } else {
            return ResultUtil.error("您已赞过了哦~");
        }
    }

    @PostMapping("search")
    public ResponseVo search(String keyword) {
        List<BizArticleSearchVo> vos = bizArticleService.search(keyword);
        return ResultUtil.success("搜索成功", vos);
    }

    /**
     * 分页查询文章列表，供 Vue SPA 首页/分类/标签列表页使用
     * 复用和 BlogWebController.loadMainPage 完全相同的查询逻辑
     */
    @GetMapping("articles")
    public ResponseVo<IPage<BizArticle>> getArticles(ArticleConditionVo vo) {
        vo.setStatus(CoreConst.STATUS_VALID);
        Pagination<BizArticle> page = new Pagination<>(vo.getPageNumber(), vo.getPageSize());
        List<BizArticle> list = bizArticleService.findByCondition(page, vo);
        page.setRecords(list);
        return ResponseVo.success(page);
    }

    /**
     * 文章详情，供 Vue SPA 文章详情页使用
     */
    @GetMapping("articles/{id}")
    public ResponseVo<BizArticle> getArticleDetail(@PathVariable String id) {
        BizArticle article = bizArticleService.selectById(id);
        if (article == null || CoreConst.STATUS_INVALID.equals(article.getStatus())) {
            return ResponseVo.notFound("文章不存在或已删除");
        }
        return ResponseVo.success(article);
    }

    /**
     * 首页轮播文章
     */
    @GetMapping("slider")
    public ResponseVo<List<BizArticle>> getSlider() {
        return ResponseVo.success(bizArticleService.sliderList());
    }

    /**
     * 分类详情（列表页顶部标题需要用到分类名称）
     */
    @GetMapping("category/{id}")
    public ResponseVo<BizCategory> getCategoryById(@PathVariable String id) {
        BizCategory category = bizCategoryService.getById(id);
        if (category == null) {
            return ResponseVo.notFound("分类不存在");
        }
        return ResponseVo.success(category);
    }

    /**
     * 侧边栏 + 全局站点配置数据，一次性返回，供 Vue SPA 启动时加载一次
     * 复用 CommonDataService.getAllCommonData()，和原来 CommonDataInterceptor 塞进 Thymeleaf Model 的数据完全一致
     */
    @GetMapping("common-data")
    public ResponseVo<Map<String, Object>> getCommonData() {
        return ResponseVo.success(commonDataService.getAllCommonData());
    }

}
