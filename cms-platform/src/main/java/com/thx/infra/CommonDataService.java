package com.thx.infra;

import cn.hutool.core.map.MapUtil;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.entity.BizLink;
import com.thx.module.admin.entity.BizTags;
import com.thx.module.admin.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 供 Thymeleaf 模板（前台页面、登录/错误页等仍走服务端渲染的场景）统一取"公共展示数据"
 * 用的门面类，如侧边栏分类列表、热门文章、友链等。moduleName 对应内部 {@link DataTypeEnum}
 * 的枚举名，get() 内部 catch 了所有异常并记录日志、返回 null，避免某个数据源出问题
 * 导致整个页面渲染失败。
 */
@Slf4j
@Component("commonDataService")
@AllArgsConstructor
public class CommonDataService {

    private final BizCategoryService bizCategoryService;
    private final BizArticleService bizArticleService;
    private final BizTagsService bizTagsService;
    private final BizLinkService bizLinkService;
    private final BizSiteInfoService siteInfoService;
    private final SysConfigService sysConfigService;

    public Object get(String moduleName) {
        try {
            DataTypeEnum dataTypeEnum = DataTypeEnum.valueOf(moduleName);
            switch (dataTypeEnum) {
                case CATEGORY_LIST:
                    BizCategory bizCategory = new BizCategory();
                    bizCategory.setStatus(CoreConst.STATUS_VALID);
                    return bizCategoryService.selectCategories(bizCategory);
                case TAG_LIST:
                    return bizTagsService.selectTags(new BizTags());
                case SLIDER_LIST:
                    return bizArticleService.sliderList();
                case RECENT_LIST:
                    return bizArticleService.recentList(CoreConst.PAGE_SIZE);
                case RECOMMENDED_LIST:
                    return bizArticleService.recommendedList(CoreConst.PAGE_SIZE);
                case HOT_LIST:
                    return bizArticleService.hotList(CoreConst.PAGE_SIZE);
                case RANDOM_LIST:
                    return bizArticleService.randomList(CoreConst.PAGE_SIZE);
                case LINK_LIST:
                    BizLink bizLink = new BizLink();
                    bizLink.setStatus(CoreConst.STATUS_VALID);
                    return bizLinkService.selectLinks(bizLink);
                case SITE_INFO:
                    return siteInfoService.getSiteInfo();
                case SITE_CONFIG:
                    return sysConfigService.selectAll();
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("获取网站公共信息[{}]发生异常: {}", moduleName, e.getMessage(), e);
        }
        return null;
    }

    public Map<String, Object> getAllCommonData() {
        Map<String, Object> result = MapUtil.newHashMap(DataTypeEnum.values().length);
        for (DataTypeEnum dataTypeEnum : DataTypeEnum.values()) {
            result.put(dataTypeEnum.name(), get(dataTypeEnum.name()));
        }
        return result;
    }

    private enum DataTypeEnum {
        // 分类
        CATEGORY_LIST,
        // 标签
        TAG_LIST,
        //轮播文章
        SLIDER_LIST,
        //最近文章
        RECENT_LIST,
        //推荐文章
        RECOMMENDED_LIST,
        //热门文章
        HOT_LIST,
        //随机文章
        RANDOM_LIST,
        //友链
        LINK_LIST,
        //网站信息统计
        SITE_INFO,
        //网站基本信息配置
        SITE_CONFIG
    }
}
