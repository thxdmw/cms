package com.thx.module.admin.controller.article;

import cn.hutool.core.collection.CollUtil;
import com.thx.common.util.CoreConst;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.service.BizCategoryService;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 后台文章分类（类目）管理接口，对应前端 admin-app 的分类管理模块。分类以 pid 关联形成父子层级结构。
 */
@Controller
@RequestMapping("category")
@AllArgsConstructor
public class CategoryController {

    private final BizCategoryService categoryService;
    private final BizArticleService articleService;

    /**
     * 查询分类列表，仅返回状态有效的分类。isFistLevel 为 true 时只返回顶级分类（pid = 顶级菜单 ID），
     * 否则返回全部层级的分类，由前端自行组装成分类树。
     */
    @PostMapping("list")
    @ResponseBody
    public List<BizCategory> loadCategory(boolean isFistLevel) {
        BizCategory bizCategory = new BizCategory();
        bizCategory.setStatus(CoreConst.STATUS_VALID);
        if (isFistLevel) {
            bizCategory.setPid(CoreConst.TOP_MENU_ID);
        }
        return categoryService.selectCategories(bizCategory);
    }

    /**
     * 新增分类。方法体内保留了一段已注释掉的历史校验逻辑（父级分类下存在文章时不允许新增子分类），
     * 当前未启用，新增时不做该项限制。
     */
    @PostMapping("/add")
    @ResponseBody
    @CacheEvict(value = "category", allEntries = true)
    public ResponseVo add(BizCategory bizCategory) {
        //if (!CoreConst.TOP_MENU_ID.equals(bizCategory.getPid())) {
        //    List<BizArticle> bizArticles = articleService.selectByCategoryId(bizCategory.getPid());
        //    if (!ListUtils.isEmpty(bizArticles)) {
        //        return ResultUtil.error("添加失败，父级分类下存在文章");
        //    }
        //}
        Date date = new Date();
        bizCategory.setCreateTime(date);
        bizCategory.setUpdateTime(date);
        bizCategory.setStatus(CoreConst.STATUS_VALID);
        boolean i = categoryService.save(bizCategory);
        if (i) {
            return ResultUtil.success("新增分类成功");
        } else {
            return ResultUtil.error("新增分类失败");
        }
    }

    @PostMapping("/edit")
    @ResponseBody
    @CacheEvict(value = "category", allEntries = true)
    public ResponseVo edit(BizCategory bizCategory) {
        bizCategory.setUpdateTime(new Date());
        boolean i = categoryService.updateById(bizCategory);
        if (i) {
            return ResultUtil.success("编辑分类成功");
        } else {
            return ResultUtil.error("编辑分类失败");
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id) {
        if (CollUtil.isNotEmpty(categoryService.selectByPid(id))) {
            return ResultUtil.error("该分类下存在子分类！");
        }
        if (CollUtil.isNotEmpty(articleService.selectByCategoryId(id))) {
            return ResultUtil.error("该分类下存在文章！");
        }
        return deleteBatch(new String[]{id});
    }

    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = categoryService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除分类成功");
        } else {
            return ResultUtil.error("删除分类失败");
        }
    }

}
