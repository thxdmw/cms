package com.thx.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizLink;
import com.thx.module.admin.service.BizLinkService;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 后台友情链接管理
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Controller
@RequestMapping("link")
@AllArgsConstructor
public class LinksController {

    private final BizLinkService linkService;

    @PostMapping("list")
    @ResponseBody
    public PageResultVo loadLinks(BizLink bizLink, Integer pageNumber, Integer pageSize) {
        IPage<BizLink> bizLinkPage = linkService.pageLinks(bizLink, pageNumber, pageSize);
        return ResultUtil.table(bizLinkPage.getRecords(), bizLinkPage.getTotal());
    }

    @PostMapping("/add")
    @ResponseBody
    @CacheEvict(value = "link", allEntries = true)
    public ResponseVo add(BizLink bizLink) {
        Date date = new Date();
        bizLink.setCreateTime(date);
        bizLink.setUpdateTime(date);
        boolean i = linkService.save(bizLink);
        if (i) {
            return ResultUtil.success("新增友链成功");
        } else {
            return ResultUtil.error("新增友链失败");
        }
    }

    @PostMapping("/edit")
    @ResponseBody
    @CacheEvict(value = "link", allEntries = true)
    public ResponseVo edit(BizLink bizLink) {
        bizLink.setUpdateTime(new Date());
        boolean i = linkService.updateById(bizLink);
        if (i) {
            return ResultUtil.success("编辑友链成功");
        } else {
            return ResultUtil.error("编辑友链失败");
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id) {
        return deleteBatch(new String[]{id});
    }

    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids) {
        int i = linkService.deleteBatch(ids);
        if (i > 0) {
            return ResultUtil.success("删除友链成功");
        } else {
            return ResultUtil.error("删除友链失败");
        }
    }

}
