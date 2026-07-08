package com.thx.module.admin.controller.article;

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
 * 后台友情链接管理接口，对应前端 admin-app 的友情链接管理模块，提供标准的分页查询与增删改。
 */
@Controller
@RequestMapping("link")
@AllArgsConstructor
public class LinksController {

    private final BizLinkService linkService;

    /**
     * 分页查询友情链接列表。
     */
    @PostMapping("list")
    @ResponseBody
    public PageResultVo loadLinks(BizLink bizLink, Integer pageNumber, Integer pageSize) {
        IPage<BizLink> bizLinkPage = linkService.pageLinks(bizLink, pageNumber, pageSize);
        return ResultUtil.table(bizLinkPage.getRecords(), bizLinkPage.getTotal());
    }

    /**
     * 新增友情链接。
     */
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

    /**
     * 编辑友情链接。
     */
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

    /**
     * 删除单条友情链接，内部复用批量删除接口实现。
     */
    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id) {
        return deleteBatch(new String[]{id});
    }

    /**
     * 根据 ID 数组批量物理删除友情链接。
     */
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
