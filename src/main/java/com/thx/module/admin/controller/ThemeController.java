package com.thx.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.CoreConst;
import com.thx.common.util.Pagination;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizTheme;
import com.thx.module.admin.service.BizThemeService;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 后台主题配置
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Controller
@RequestMapping("theme")
@AllArgsConstructor
public class ThemeController {

    private final BizThemeService bizThemeService;


    @PostMapping("list")
    @ResponseBody
    public PageResultVo loadTheme(Integer pageNumber, Integer pageSize) {
        IPage<BizTheme> page = new Pagination<>(pageNumber, pageSize);
        page = bizThemeService.page(page);
        return ResultUtil.table(page.getRecords(), page.getTotal());
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseVo add(BizTheme bizTheme) {
        Date date = new Date();
        bizTheme.setCreateTime(date);
        bizTheme.setUpdateTime(date);
        bizTheme.setStatus(CoreConst.STATUS_INVALID);
        boolean i = bizThemeService.save(bizTheme);
        if (i) {
            return ResultUtil.success("新增主题成功");
        } else {
            return ResultUtil.error("新增主题失败");
        }
    }

    @PostMapping("/edit")
    @ResponseBody
    public ResponseVo edit(BizTheme bizTheme) {
        bizTheme.setUpdateTime(new Date());
        boolean i = bizThemeService.updateById(bizTheme);
        if (i) {
            return ResultUtil.success("编辑主题成功");
        } else {
            return ResultUtil.error("编辑主题失败");
        }
    }

    @PostMapping("/use")
    @ResponseBody
    public ResponseVo use(String id) {
        int i = bizThemeService.useTheme(id);
        if (i > 0) {
            return ResultUtil.success("启用主题成功");
        } else {
            return ResultUtil.error("启用主题失败");
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id) {
        boolean i = bizThemeService.removeById(id);
        if (i) {
            return ResultUtil.success("删除主题成功");
        } else {
            return ResultUtil.error("删除主题失败");
        }
    }

    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = bizThemeService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除主题成功");
        } else {
            return ResultUtil.error("删除主题失败");
        }
    }

}
