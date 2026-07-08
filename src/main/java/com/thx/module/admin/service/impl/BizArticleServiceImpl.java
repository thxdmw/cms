package com.thx.module.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.CoreConst;
import com.thx.common.util.Pagination;
import com.thx.module.admin.mapper.BizArticleMapper;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.vo.ArticleConditionVo;
import com.thx.module.blog.vo.BizArticleSearchVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * {@link BizArticleService} 实现：文章的条件查询、首页各类榜单（轮播/推荐/最新/随机/热门）、
 * 全文搜索、批量下载导出等。榜单类查询都加了 {@link Cacheable}（前台首页高频访问、数据变化不频繁），
 * 对应的写操作（新增/删除）用 {@link CacheEvict} 让缓存失效，避免脏数据。
 */
@Service
@AllArgsConstructor
@Slf4j
public class BizArticleServiceImpl extends ServiceImpl<BizArticleMapper, BizArticle> implements BizArticleService {

    private final BizArticleMapper bizArticleMapper;

    @Override
    public List<BizArticle> findByCondition(IPage<BizArticle> page, ArticleConditionVo vo) {
        List<BizArticle> list = bizArticleMapper.findByCondition(page, vo);
        if (CollUtil.isNotEmpty(list)) {
            List<String> ids = new ArrayList<>();
            for (BizArticle bizArticle : list) {
                ids.add(bizArticle.getId());
            }
            List<BizArticle> listTag = bizArticleMapper.listTagsByArticleId(ids);
            // listTag, 重新组装数据为{id: Article}
            Map<String, BizArticle> tagMap = new LinkedHashMap<>(listTag.size());
            for (BizArticle bizArticle : listTag) {
                tagMap.put(bizArticle.getId(), bizArticle);
            }

            for (BizArticle bizArticle : list) {
                BizArticle tagArticle = tagMap.get(bizArticle.getId());
                if (Objects.nonNull(tagArticle)) {
                    bizArticle.setTags(tagArticle.getTags());
                }
            }
        }
        return list;
    }

    @Override
    @Cacheable(value = "article", key = "'slider'")
    public List<BizArticle> sliderList() {
        ArticleConditionVo vo = new ArticleConditionVo();
        vo.setSlider(true);
        vo.setStatus(CoreConst.STATUS_VALID);
        return this.findByCondition(null, vo);
    }

    @Override
    @Cacheable(value = "article", key = "'recommended'")
    public List<BizArticle> recommendedList(int pageSize) {
        ArticleConditionVo vo = new ArticleConditionVo();
        vo.setRecommended(true);
        vo.setStatus(CoreConst.STATUS_VALID);
        vo.setPageSize(pageSize);
        IPage<BizArticle> page = new Pagination<>(vo.getPageNumber(), vo.getPageSize());
        return this.findByCondition(page, vo);
    }

    @Override
    @Cacheable(value = "article", key = "'recent'")
    public List<BizArticle> recentList(int pageSize) {
        ArticleConditionVo vo = new ArticleConditionVo();
        vo.setPageSize(pageSize);
        vo.setStatus(CoreConst.STATUS_VALID);
        vo.setRecentFlag(true);
        IPage<BizArticle> page = new Pagination<>(vo.getPageNumber(), vo.getPageSize());
        return this.findByCondition(page, vo);
    }

    @Override
    @Cacheable(value = "article", key = "'random'")
    public List<BizArticle> randomList(int pageSize) {
        ArticleConditionVo vo = new ArticleConditionVo();
        vo.setRandom(true);
        vo.setStatus(CoreConst.STATUS_VALID);
        vo.setPageSize(pageSize);
        IPage<BizArticle> page = new Pagination<>(vo.getPageNumber(), vo.getPageSize());
        return this.findByCondition(page, vo);
    }

    @Override
    @Cacheable(value = "article", key = "'hot'")
    public List<BizArticle> hotList(int pageSize) {
        IPage<BizArticle> page = new Pagination<>(1, pageSize);
        return bizArticleMapper.hotList(page);
    }

    @Override
    @Cacheable(value = "article", key = "#id")
    public BizArticle selectById(String id) {
        return bizArticleMapper.getById(id);
    }

    @Override
    @CacheEvict(value = "article", allEntries = true)
    public BizArticle insertArticle(BizArticle bizArticle) {
        Date date = new Date();
        bizArticle.setCreateTime(date);
        bizArticle.setUpdateTime(date);
        bizArticleMapper.insert(bizArticle);
        return bizArticle;
    }

    @Override
    @CacheEvict(value = "article", allEntries = true)
    public int deleteBatch(String[] ids) {
        return bizArticleMapper.deleteBatch(ids);
    }

    @Override
    public List<BizArticle> selectByCategoryId(String categoryId) {
        return bizArticleMapper.selectList(Wrappers.<BizArticle>lambdaQuery().eq(BizArticle::getCategoryId, categoryId));
    }

    @Override
    public List<BizArticleSearchVo> search(String keyword) {
        LambdaQueryWrapper<BizArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizArticle::getStatus, CoreConst.STATUS_VALID)
                .and(wrapper -> {
                    wrapper.like(BizArticle::getTitle, keyword).or().like(BizArticle::getContent, keyword);
                });
        List<BizArticle> list = this.list(queryWrapper);
        List<BizArticleSearchVo> vos = new ArrayList<>();
        if (CollUtil.isNotEmpty(list)) {
            list.forEach(bizArticle -> {
                BizArticleSearchVo vo = BeanUtil.copyProperties(bizArticle, BizArticleSearchVo.class);
                vo.setSkipUrl("blog/article/" + bizArticle.getId().toString());
                vos.add(vo);
            });
        }
        return vos;
    }

    @Override
    public void downloadArticles(List<String> ids, HttpServletResponse response) {
        try {
            List<BizArticle> articles = this.listByIds(ids);
            if (articles == null || articles.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("未找到选中的文章");
                return;
            }

            if (articles.size() == 1) {
                // 单篇文章
                BizArticle article = articles.get(0);
                String fileName = sanitizeFileName(article.getTitle()) + ".md";

                // ✅ 编码文件名
                String encodedFileName = encodeFileName(fileName);

                response.setContentType("text/markdown; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

                String markdownContent = article.getContentMd();
                if (markdownContent == null) {
                    markdownContent = "# " + article.getTitle() + "\n\n" + article.getContent();
                }

                response.getOutputStream().write(markdownContent.getBytes(StandardCharsets.UTF_8));
                response.getOutputStream().flush();

            } else {
                // 多篇文章
                String zipFileName = "articles_" + System.currentTimeMillis() + ".zip";
                String encodedFileName = encodeFileName(zipFileName);

                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

                try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                    for (BizArticle article : articles) {
                        String mdFileName = sanitizeFileName(article.getTitle()) + ".md";
                        ZipEntry zipEntry = new ZipEntry(mdFileName);
                        zos.putNextEntry(zipEntry);

                        String markdownContent = article.getContentMd();
                        if (markdownContent == null) {
                            markdownContent = "# " + article.getTitle() + "\n\n" + article.getContent();
                        }

                        byte[] data = markdownContent.getBytes(StandardCharsets.UTF_8);
                        zos.write(data, 0, data.length);
                        zos.closeEntry();
                    }
                    zos.finish();
                }
            }
        } catch (Exception e) {
            log.error("Error downloading articles: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理文件名编码（支持中文）
     */
    private String encodeFileName(String fileName) throws UnsupportedEncodingException {
        // 针对大多数浏览器进行编码
        return new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
