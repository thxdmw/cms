package com.thx.module.admin.controller.article;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.Pagination;
import com.thx.common.util.PushArticleUtil;
import com.thx.common.util.ResultUtil;
import com.thx.enums.SysConfigKey;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.*;
import com.thx.module.admin.vo.ArticleConditionVo;
import com.thx.module.admin.vo.BaiduPushResVo;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 后台文章管理接口，对应前端 admin-app 的"文章管理"模块：文章列表查询、新增、编辑、上下线状态调整、
 * 删除、批量导出下载，以及向百度站长平台批量推送 URL 收录。
 */
@Controller
@RequestMapping("article")
@AllArgsConstructor
public class ArticleController {

    private final BizArticleService articleService;
    private final BizArticleTagsService articleTagsService;
    private final SysConfigService configService;

    /**
     * 分页查询文章列表。固定将 sliderFlag 置为 true，使焦点图（slider）文章排在结果最前面
     * （见 BizArticleMapper.xml 中 ORDER BY 对应的动态 SQL），其余过滤条件由 ArticleConditionVo 各字段决定。
     */
    @PostMapping("list")
    @ResponseBody
    public PageResultVo loadArticle(ArticleConditionVo articleConditionVo, Integer pageNumber, Integer pageSize) {
        articleConditionVo.setSliderFlag(true);
        IPage<BizArticle> page = new Pagination<>(pageNumber, pageSize);
        List<BizArticle> articleList = articleService.findByCondition(page, articleConditionVo);
        return ResultUtil.table(articleList, page.getTotal());
    }

    /**
     * 获取单篇文章详情（后台 Vue SPA 编辑表单回显用），复用 GET /edit 视图方法里同样的查询
     */
    @GetMapping("/detail")
    @ResponseBody
    public ResponseVo<BizArticle> detail(String id) {
        BizArticle bizArticle = articleService.selectById(id);
        if (bizArticle == null) {
            return ResultUtil.error("文章不存在");
        }
        return ResultUtil.success("获取成功", bizArticle);
    }

    @PostMapping("/add")
    @ResponseBody
    @Transactional
    @CacheEvict(value = "article", allEntries = true)
    public ResponseVo add(BizArticle bizArticle, String[] tag) {
        try {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            bizArticle.setUserId(user.getUserId());
            bizArticle.setAuthor(user.getNickname());
            BizArticle article = articleService.insertArticle(bizArticle);
            articleTagsService.insertList(tag, article.getId());
            return ResultUtil.success("保存文章成功");
        } catch (Exception e) {
            return ResultUtil.error("保存文章失败");
        }
    }

    @PostMapping("/edit")
    @ResponseBody
    @CacheEvict(value = "article", allEntries = true)
    public ResponseVo edit(BizArticle article, String[] tag) {
        articleService.updateById(article);
        articleTagsService.removeByArticleId(article.getId());
        articleTagsService.insertList(tag, article.getId());
        return ResultUtil.success("编辑文章成功");
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id) {
        return deleteBatch(new String[]{id});
    }

    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = articleService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除文章成功");
        } else {
            return ResultUtil.error("删除文章失败");
        }
    }

    @PostMapping("/batch/push")
    @ResponseBody
    public ResponseVo pushBatch(@RequestParam("urls[]") String[] urls) {
        try {
            String url = configService.selectAll().get(SysConfigKey.BAIDU_PUSH_URL.getValue());
            BaiduPushResVo baiduPushResVo = JSON.parseObject(PushArticleUtil.postBaidu(url, urls), BaiduPushResVo.class);
            if (baiduPushResVo.getNotSameSite() == null && baiduPushResVo.getNotValid() == null) {
                return ResultUtil.success("推送文章成功");
            } else {
                return ResultUtil.error("推送文章失败", baiduPushResVo);
            }
        } catch (Exception e) {
            return ResultUtil.error("推送文章失败,请检查百度推送接口！");
        }

    }

    @PostMapping("/batch/download")
    public void downloadArticles(@RequestParam("ids[]") List<String> ids, HttpServletResponse response) {
        articleService.downloadArticles(ids, response);
    }

    @PostMapping("/changeArticleStatus")
    @ResponseBody
    public ResponseVo changeArticleStatus(BizArticle articleDTO) {
        BizArticle article = articleService.getById(articleDTO.getId());
        if (article == null) {
            return ResultUtil.error("文章不存在");
        }
        article.setStatus(articleDTO.getStatus());
        articleService.updateById(article);
        return ResultUtil.success("请求成功");
    }

}
