package com.thx.module.admin.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.service.ArticleImageCleanupService;
import com.thx.module.admin.service.BizArticleService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * 定时任务
 */
@Component
@Slf4j
public class ScheduledTask {

    @Resource
    private BizArticleService bizArticleService;

    @Resource
    private ArticleImageCleanupService articleImageCleanupService;


    /**
     * 定时任务1（获取三国杀武将编号信息定时任务）
     */
    //@Scheduled(cron = "0/10 * * * * ?")
    @Scheduled(cron = "0 0 0 * * ?")
    @CacheEvict(value = "article", allEntries = true)
    public void job1() {
        log.info("获取三国杀武将编号信息定时任务执行！");
        LambdaQueryWrapper<BizArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizArticle::getTitle, "三国杀武将编号");
        BizArticle article = bizArticleService.getOne(queryWrapper);
        if (article == null) {
            log.info("获取三国杀武将编号文章不存在！定时器执行结束！");
            return;
        }
        List<List<String>> sg = null;
        try {
            sg = sg();
        } catch (IOException e) {
            log.error("获取三国杀武将编号信息接口失败！");
            return;
        }
        // 构建 Markdown 表格字符串
        try {
            StringBuilder md = new StringBuilder();
            log.info("获取三国杀武将编号信息:{}", sg);
            List<String> header = sg.get(0);
            md.append("| ").append(String.join(" | ", header)).append(" |\n");

            md.append("|");
            for (int i = 0; i < header.size(); i++) {
                md.append(" --- |");
            }
            md.append("\n");

            for (int i = 1; i < sg.size(); i++) {
                md.append("| ").append(String.join(" | ", sg.get(i))).append(" |\n");
            }
            //更新文章内容
            //article.setContent(convertMarkdownToHtml(md.toString()));
            article.setContentMd(md.toString());
            bizArticleService.updateById(article);
            log.info("获取三国杀武将编号信息定时任务执行成功！");
        } catch (Exception e) {
            article.setContentMd("暂无数据！");
            log.error("更新三国杀武将编号文章数据失败！");
        }
    }

    /**
     * 定时任务2（早上九点发送邮件提醒每日事项）
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void job2() {
        log.info("邮件提醒定时任务执行！");

    }

    /**
     * 定时任务3（每天凌晨2点清理未被文章引用的孤立图片）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrphanArticleImages() {
        log.info("开始执行孤立图片清理定时任务");
        try {
            articleImageCleanupService.cleanupOrphanArticleImages();
            log.info("孤立图片清理定时任务执行成功");
        } catch (Exception e) {
            log.error("执行孤立图片清理定时任务失败", e);
        }
    }


    /**
     * 获取三国杀武将编号信息
     *
     * @return
     * @throws IOException
     */
    public static List<List<String>> sg() throws IOException {
        // 目标网址
        String url = "https://zh.moegirl.org.cn/%E4%B8%89%E5%9B%BD%E6%9D%80/%E6%AD%A6%E5%B0%86%E6%88%98%E5%8A%9F%26%E7%BC%96%E5%8F%B7%E8%A1%A8/%E7%BA%AF%E7%BC%96%E5%8F%B7%E8%A1%A8";
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();
        // 可以精确选择器限定 table
        Element table = doc.selectFirst("table");
        List<List<String>> result = new ArrayList<>();

        if (table != null) {
            Elements rows = table.select("tr");
            for (Element row : rows) {
                List<String> rowData = new ArrayList<>();
                for (Element cell : row.select("th, td")) {
                    rowData.add(cell.text());
                }
                if (!rowData.isEmpty()) {
                    result.add(rowData);
                }
            }
        }
        return result;
    }
}
